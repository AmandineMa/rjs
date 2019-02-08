// Agent robot in project supervisor

/* Initial beliefs and rules */

//isClose(1).
//isClose(2).
//isClose(3).
//isClose(4).
//isClose(5).
//isLookingAt(2).
//isSpeakingTo(3).
//isLookingAt(4).
//isSpeakingTo(4).
//isLookingAt(5).
//isSpeakingTo(5).

/* Initial goals */


//!look_for_interactant.
//!pause_look_for_interactant.

// TODO handle when place to go already frame name
place_asked("atm").
!guiding(human).
//!test.

/* Plans */

/**** Interaction session *******/

+!look_for_interactant : true <- 
	// look for all the people that are close and looking at or speaking to the robot
	.findall(X, (isClose(X) & (isLookingAt(X) | isSpeakingTo(X))), L);
	// for all those people, we add in the BB that they are potential interact
	for(.member(X, L)){
		+isPotInteractant(X);
	}
	!look_for_interactant.

-!look_for_interactant : true <- !look_for_interactant.

// A plan that pauses the search for interactant for a few seconds and resume it. To avoid a too fast loop.
+!pause_look_for_interactant
  <- .suspend(look_for_interactant); // suspend the look_for_interactant intention
     .wait(2000);     // suspend this intention for 2 second
     .resume(look_for_interactant);  // resume the clean intention
     !pause_look_for_interactant.

// Select the first potential interactant added to the BB to become an interactant,  if there is no interactant already
+isPotInteractant(X) : not interactant(_) <- +interactant(X).

/***** Tasks ******/

//Level 0
	
+!guiding(Human): true <- 
	// wait for the services client to be started
	.wait(1000); 
	?place_asked(Place);
	!get_optimal_route("robot_infodesk", Place, lambda, false);
//	!get_optimal_pos(Human);
	?target_place(TargetLD);
	!get_human_to_be_oriented(Human, TargetLD).

	

//Level 1	
+!get_optimal_route(From, To, Persona, Signpost): true <-
	// special handling if the place to go is toilet or atm
	!handle_atm_toilet(To);
	// there should be one belief possible_places if it toilet or atm and there is no if it is something else
	.setof(X, possible_places(X), L);
	// if it is not toilet or atm
	if(.empty(L)){
		// get the corresponding name in the ontology	
		get_onto_name(To);
		?onto_place(Place);
	// if we got a list of toilets or a list of atms
	} else{
		// L is a list of list and normally with only one list, the one we get at index 0
		.nth(0, L, Place);
	}
	// compute a route
	compute_route(From, Place, Persona, Signpost);
	?route(R);
	// if the route has stairs, check if the human can use the stairs
	if(.substring("stairs", R)){
		// remove the old computed route
		supervisor.time(TimeNow);
		supervisor.abolish(route(_), TimeNow);
		// to know if the human can climb stairs
		get_human_abilities;
		?persona_asked(PA);
		// compute a new route with the persona information
		compute_route(From, Place, PA, Signpost);
	}
	// if the route has an interface (a direction)
	if(.length(R) > 3){
		.nth(2,R,Dir);
		+direction(Dir);
	}.
	
	
+!get_optimal_pos(Human): true <-
	?target_place(TargetLD);
	// if there is a direction
	if(.count((direction(_)),I) & I > 0){
		?direction(Dir);
		get_placements(TargetLD,Dir,Human);
	}else{
		get_placements(TargetLD,"",Human);
	}
	!be_at_good_pos.
	
+!get_human_to_be_oriented(Human, TargetLD): true <- 
	.send(Human, askOne, canSee(TargetLD), Ans, 4000);
	if(not .substring(Ans,"false")){
      	+Ans;
    }
    !adjust_orientation(Human, TargetLD).

	
	
//Level 2
+!handle_atm_toilet(To): true <- 
	supervisor.toilet_or_atm(To, AorT);
	// add belief possible_places
	get_individual_type(AorT).	

-!handle_atm_toilet(To):true <- true.

+!be_at_good_pos : not positions_ok <- !make_human_move; !be_at_good_places.

+!be_at_good_pos : positions_ok <- true.

+!adjust_orientation(Human, TargetLD): canSee(TargetLD)[source(Human)] <- .send(Human, tell, robotKnows(ableToSee)); -canSee(TargetLD)[source(Human)].

+!adjust_orientation(Human, TargetLD): not canSee(TargetLD)[source(Human)] <-
	supervisor.compute_turn_direction(Human, TargetLD, LeftOrRight);
	.send(Human, tell, should(LeftOrRight));
	.wait(4000);
	!get_human_to_be_oriented(Human, TargetLD).



//Level 3
 +!test : true <- supervisor.compute_turn_direction(human, atm_1, LeftOrRight); .print(LeftOrRight).
//+!test : true <- .send(human, achieve, start); .wait({+start}, 1000); .print(heeeey).
//+!test : true <- ?isMoving; .wait(1000); !test.
//-!test : not stop <- .wait(1000); !test.
//-!test : stop <- true.
//+isMoving : true <- -place_asked(X); +stop; .print("bouh").
//-!test : true <- .print("did not receive event").
//+!make_human_move : too_north <- .send(human, achieve, step_forward); .wait({stepped_forward}, 2000).

//-!get_optimal_route(From, To, Persona, Signpost)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]: true <- 
//	.current_intention(I); 
//	I = intention(Id,[X|IntendedMeans]);
//	.print("code body :",CodeBody);
//	!print_im(IntendedMeans).
//
//+!print_im([]).
//+!print_im([im(PlanLabel,Body, Unif, Trigger)|R])
//  <- .print("*        ",Body,"      * unifier: ",Unif);
//     !print_im(R).

//+!guiding(X, Place): interactant(X) & isPerceiving(X) <- 
//	!get_optimal_route(Place);
//	!get_optimal_places;
//	!perform_moves;
//	!check_human_position;
//	!give_explanation.
	
//
//+!perform_moves: robot_move_first <- 
//	move_to;
//	!make_human_move.
//
//+!perform_moves: human_move_first <- 
//	!make_human_move;
//	move_to.
//	
//+!perform_moves: robot_move_alone <- 
//	move_to.
//	
//+!perform_moves: human_move_alone <- 
//	!make_human_move.
//
//+!give_explanation: true <-
//	for (.member(l, landmarks_to_point)){	
//		!point_landmark(l);
//	}
//	get_human_ack.
//	
////Level 2
//+!decide_to_move: true <-
//	.should_human_move;
//	.should_robot_move;
//	.decide_moves.
//
//+!make_human_move: true <-
//	ask_to_move.
//	
//+!point_landmark(L): true <- 
//	point_landmark;
//	get_ack.
//
//+!check_human_position: true <-
//	?areLandmarksVisible.
//	
//-!check_human_position: true <-
//	adjust_human_position;
//	!check_human_position.
	
	


	

