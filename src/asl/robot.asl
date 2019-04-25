// Agent robot in project supervisor

/* Initial beliefs and rules */



/* Initial goals */


// TODO handle when place to go already frame name
//place_asked("burger king").
robot_place("robot_infodesk").
//!guiding(human).
//!test.

/* Plans */

+!init : true <- 
	supervisor.verbose(2);
	// get shops list from ontology
	get_onto_individual_info(getType, shop, shops);
	?shops(Shops);
	for(.member(X, Shops)){
		get_onto_individual_info(getName, X, shop_name);
	}
	.findall(X, shop_name(X), L);
	+shop_names(L);
	.abolish(shop_name(_));
	.send(supervisor, tell, init_over(ok)).

-!init : true <- +init_over(failed).

/***** Tasks ******/

+!Plan[source(supervisor)] : true <- 
	Plan =.. [Guiding, [Human,_],[]];
	set_task_infos(Guiding, Human);
	!Plan.

+!guiding(Human, Place): true <- 
	!goal_negociation(guiding, Place);
//	!show_landmarks(Human);
//	!go_to_see_target(RobPose, HumPose);
//	!give_directions.
	!end_task(guiding, Human).
	
+!goal_negociation(guiding, Place): true <-
	?robot_place(RobotPlace);
	!get_optimal_route(RobotPlace, Place, lambda, false).
	
	
+!get_optimal_route(From, To, Persona, Signpost): true <-
	// special handling if the place to go is toilet or atm
	!handle_atm_toilet(To);
	// there should be one belief possible_places if it toilet or atm and there is no if it is something else
	.findall(X, possible_places(X), L);
	// if it is not toilet or atm
	if(.empty(L)){
		// get the corresponding name in the ontology	
		get_onto_individual_info(find, To, onto_place);
		?onto_place(Place);
	// if we got a list of toilets or a list of atms
	} else{
		// L is a list of list and normally with only one list, the one we get at index 0
		.nth(0, L, Place);
	}
	// compute a route with Place which can be a place or a list of places
	// if it s a list of places, compute_route select the place with the smallest cost
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

+!handle_atm_toilet(To): true <- 
	supervisor.toilet_or_atm(To, AorT);
	// add belief possible_places
	get_onto_individual_info(getType, AorT, possible_places).	

-!handle_atm_toilet(To):true <- true.

-!get_optimal_route(From, To, Persona, Signpost)[Failure, error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]: true <-
	if(.substring(Failure, individual_not_found)){
		!speak(Human, no_place(Place));
		?shop_names(X);
		listen(X);
	}.
	
+!end_task(Task, Human) : true <-
	.findall(B[Task,Human,source(X),add_time(Y)],B[Task,Human,source(X),add_time(Y)], L);
	.send(interact_hist, tell, L);
	.abolish(_[Task,Human]).	
	
// Utils
+!speak(Human, ToSay) : true <-
//	.send(Human, tell, ToSay);
	text2speech(Human, ToSay).

