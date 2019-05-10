// Agent robot in project supervisor

/* Initial beliefs and rules */



/* Initial goals */


// TODO handle when place to go already frame name
//place_asked("burger king").
robot_place("robot_infodesk").
shops(["thai_papaya","starbucks","zizzi","marimekko_outlet","marco_polo","intersport","pancho_villa",
	   "daddys_diner","atm_2","atm_1","burger_king","funpark","hairstore","reima","ilopilleri","linkosuo",
	   "gina","h_m","hiustalo"]).
shop_names(["C M Hiustalo","h& m","gina","cafe linkusuo","kahvila ilopilleri","reima","hairstore","funpark",
	        "burger king","atm","daddy s diner","pancho villa","intersport","marco polo","marimekko outlet",
	        "zizzi","starbucks","thai papaya"]).	
//persona_asked(lambda).   

//!guiding(human).
//!test.

/* Plans */

+!init : true <- 
	// get shops list from ontology
//	get_onto_individual_info(getType, shop, shops);
//	?shops(Shops);
//	for(.member(X, Shops)){
//		get_onto_individual_info(getName, X, shop_name);
//	}
//	.findall(X, shop_name(X), L);
//	+shop_names(L);
//	.abolish(shop_name(_));
	.send(supervisor, tell, init_over(ok)).

-!init : true <- +init_over(failed).

/***** Tasks ******/

// when receiving a plan from the supervisor, task infos (name of the task and human involved in) are sent to the AgArch
+!Plan[source(Source)] : Source == supervisor | (Source == self & restart_plan) <- 
	-restart_plan;
	Plan =.. [Label, [Human,Param],[]];
	+task(Label, Human, Param)[Label,Human];
	set_task_infos(Label, Human);
	!Plan.

+!guiding(Human, Place): true <- 
	!get_optimal_route(Place);
	!go_to_see_target(Human);
	!show_landmarks(Human);
	+succeeded[guiding, Human];
	!end_task(guiding, Human).
	
-!guiding(Human,Place) : true <-
	!end_task(guiding, Human).
	
	
/******* get route **********/	
	
+!get_optimal_route(Place): true <-
	// special handling if the place to go is toilet or atm
	!handle_atm_toilet(Place);
	// there should be one belief possible_places if it toilet or atm and there is no if it is something else
	.findall(X, possible_places(Place,X), L);
	// if it is not toilet or atm
	if(.empty(L)){
		// get the corresponding name in the ontology	
		get_onto_individual_info(find, Place, onto_place);
		?onto_place(Place,To);
	// if we got a list of toilets or a list of atms
	} else{
		// L is a list of list and normally with only one list, the one we get at index 0
		.nth(0, L, To);
	}
	// compute a route with Place which can be a place or a list of places
	// if it s a list of places, compute_route select the place with the smallest cost
	?robot_place(From);
	compute_route(From, To, lambda, false);
	?route(R);
	// if the route has stairs, check if the human can use the stairs
	if(.substring("stairs", R)){
		// remove the old computed route
		.abolish(route(_));
		// to know if the human can climb stairs
		!speak(Human, ask_stairs);
		listen(ask_stairs,["yes","no"]);
		?listen_result(ask_stairs,Word);
		if(.substring(Word,yes)){
			+persona_asked(lambda)[TaskName,Human];
		}else{
			+persona_asked(disabled)[TaskName,Human];
		}
		?persona_asked(PA);
		// compute a new route with the persona information
		compute_route(From, To, PA, false);
	}
	?target_place(Target);
	get_onto_individual_info(getName, Target, verba_name);
	// if the route has an interface (a direction)
	if(.length(R) > 3){
		.nth(2,R,Dir);
		+direction(Dir)[TaskName,Human];
		get_onto_individual_info(getName, Dir, verba_name);
	}.
	
// recovery plan
+!get_optimal_route : true <-
	if(.substring(Failure, individual_not_found)){
		?task(Label, Human, Param);
		!speak(Human, no_place(Param));
		?shop_names(X);
		.concat(X, ["atm", "toilet"], Y);
		// listen to places in the ontology
		// TODO add timeout
		listen(no_place,Y);
		?listen_result(no_place,Word);
		+restart_plan;
		!!guiding(Human, Word);
		!drop_current_task(Label, Human, Param);	
	}.

// in case of the original plan failure	
-!get_optimal_route(Place)[Failure, error(Error), code(Code), _, _, _]: true <-
	if(.substring(Failure, individual_not_found)){
		?task(TaskName, Human, _);
	  	+failure(get_optimal_route, Failure)[TaskName,Human];
	  	!get_optimal_route;
  	}else{
  		!drop_current_task(get_optimal_route, Failure, Code); 
  	}.
	
// in case of the recovery plan failure  	
-!get_optimal_route[Failure, error(Error), code(Code), _, _, _]: true <-
	!drop_current_task(get_optimal_route, Failure, Code).
	
+!handle_atm_toilet(To): true <- 
	jia.toilet_or_atm(To, AorT);
	// add belief possible_places
	get_onto_individual_info(getType, AorT, possible_places).	

-!handle_atm_toilet(To)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] :true <- 
	if(not .substring(ia_failed, Error)){
		!drop_current_task(handle_atm_toilet, Failure, Code);
	}.

	
/*******  go to see target **********/	

+!go_to_see_target(Human) : true <- 
	!get_placements(Human);
	?ld_to_point;
	!be_at_good_pos(Human).
	
-!go_to_see_target(Human)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)] : true <-
	if(not .substring(Code, ld_to_point)){
		!drop_current_task(go_to_see_target, Failure, Code);
	}.
	
+!get_placements(Human): true <-
	?target_place(TargetLD);
	// if there is a direction
	if(.count((direction(_)),I) & I > 0){
		?direction(Dir);
		get_placements(TargetLD,Dir,Human);
	}else{
		get_placements(TargetLD,"",Human);
	}.

//TODO to see if we drop the task when svp fail, why not continue the task without moving ?	
-!get_placements(Human)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	if(.substring(svp, Failure) | .substring(svp,Error)){
  		!drop_current_task(get_placements, Failure, Code);
  	}.
	
+!be_at_good_pos(Human) : true <- 
	?robot_pose(Rframe,Rposit, Rorient);
	move_to(Rframe,Rposit, Rorient);
	?human_pose(Hframe,Hposit,_);
	tf.get_transform(map, Human, Point,_);
	.nth(2, Point, Z);
	jia.replace(2, Hposit, Z, Pointf);
	look_at(Hframe,Pointf);
	!wait_human(Human).
	
-!be_at_good_pos(Human)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)] : true <-
	!drop_current_task(be_at_good_pos, Failure, Code).

+!wait_human(Human) : true <- 
	if(.count((isPerceiving(Human)),I) & I == 0){
		.wait({+isPerceiving(Human)},4000);
		-~here(Human);
	}
	if(.count((dir_to_point(_)),I) & I > 0){
		?dir_to_point(D);
		has_mesh(D);
		can_be_visible(Human, D);
	}
	if(.count((target_to_point(_)),J) & J > 0){
		?target_to_point(T);
		has_mesh(T);
		can_be_visible(Human, T);
	}.
	
-!wait_human(Human)[Failure, code(_),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	if(.substring(Error, wait_timeout)){
		+~here(Human);
		!speak(Human, come);
		!wait_human(Human);
	}elif(.substring(Failure, not_visible)){
		!speak(Human, move_again);
		!be_at_good_pos(Human);
	}else{
		!drop_current_task(wait_human, Failure, Code);
	}.
	
/*******  show landmarks **********/	

+!show_landmarks(Human) : true <- 
	!show_target(Human);
	!show_direction(Human);
	!speak(Human, ask_understand);
	// TODO add timeout
	listen(ask_understand,["yes","no"]);
	?listen_result(ask_understand,Word1);
	if(.substring(Word1,yes)){
		!speak(Human, happy_end);
	}else{
		!speak(Human, ask_explain_again);
		listen(ask_explain_again,["yes","no"]);
		?listen_result(ask_explain_again,Word2);
		if(.substring(Word2,yes)){
			!show_landmarks(Human);
		}else{
			!speak(Human, hope_find_way);
		}
	}.

+!show_target(Human) : true <-
	?target_place(TargetPlace);
	// if cannot transform, leaves the plan
	tf.can_transform(map, TargetPlace);
	!verbalization(Human, TargetPlace);
	!point_look_at(Human, TargetPlace).
	

-!show_target(Human)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)] : true <-
	if(not .substring(can_transform, Code)){
		!drop_current_task(show_target, Failure, Code);
	}.
	
+!show_direction(Human) : true <-
	?direction(D);
	tf.can_transform(map, D);
	!verbalization(Human, D);
	!point_look_at(Human, D);.

-!show_direction(Human)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	if(.substring(can_transform, Code)){
		?direction(D);
		!verbalization(Human, D);
	}elif(not .substring(test_goal_failed, Error)){
		!drop_current_task(show_direction, Failure, Code);
	}.

landmark_to_see(Ld) :- (target_to_point(T) & T == Ld) | (direction_to_point(D) & D == Ld).

+!point_look_at(Human, Ld) : landmark_to_see(Ld) <-
	// coordination signal with turn head (20 degrees) and point at and look back at human
	can_point_at(Ld);
	+should_check_target_seen(Human,Ld);
//	send_coord_signal();
	.wait(10000);
	-should_check_target_seen(Human,Ld);
	?(isLookingAt(Ld)[source(Human)] | hasSeen(Ld)[source(Human)]);
	?verba_name(Ld,Verba);
	!speak(Human, tell_seen(Verba)).
		

+!point_look_at(Human, Ld) : not landmark_to_see(Ld) <-
	// coordination signal with turn head (20 degrees) and point at and look back at human
	can_point_at(Ld).
//	send_coord_signal();


-!point_look_at(Human, Ld)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	if(.substring(cannot_point, Failure)){
		!rotate_base;
		!point_look_at(Human,Ld);
	}elif(.substring(test_goal_failed, Error)){
		!ask_seen(Human,Ld);
	}else{
		!drop_current_task(point_look_at, Failure, Code);
	}.
	
+!ask_seen(Human, Ld) : true <-
	?verba_name(Ld,Verba);
	!speak(Human, cannot_tell_seen(Verba));
	listen(cannot_tell_seen,["yes","no"]);
	?listen_result(cannot_tell_seen, Word1);
	if(.substring(Word1,no)){
		!speak(Human, ask_show_again(Ld));
		listen(ask_show_again,["yes","no"]);
		?listen_result(ask_show_again, Word2);
		if(.substring(Word2,yes)){
			!point_look_at(Human,Ld);
		}else{
			!speak(Human, hope_find_way);
		}
	}.
	
-!ask_seen(Human, Ld)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)]: true <-
  	!drop_current_task(ask_seen, Failure, Code).	
	
+!rotate_base : true <- true.

+!verbalization(Human, Place) : target_to_point(T) & T == Place & direction(_) <-
	?verba_name(Name);
	!speak(Human, visible_target(Name)).
	
+!verbalization(Human, Place) : not target_to_point(_) &  direction(D) & Place \== D <-
	?verba_name(Name);
	!speak(Human, not_visible_target(Name)).

+!verbalization(Human, Place) : ( direction(D) & Place == D & dir_to_point(D)) | (target_to_point(T) & T == Place & not direction(_)) <-
	!get_verba;
	?verbalization(RouteVerba);
	!speak(Human, route_verbalization(RouteVerba)).

+!verbalization(Human, Place) : ( direction(D) & Place == D & not dir_to_point(D) ) | ( not target_to_point(_) & not direction(_)) <-
	!get_verba;
	?verbalization(RouteVerba);
	!speak(Human, route_verbalization_n_vis(RouteVerba)).
	
+!get_verba : true <-
	?route(Route);
	?robot_place(RobotPlace);
	?target_place(FinaleP);
	get_route_verbalization(Route, RobotPlace, FinaleP).
	
-!verbalization(Human, Place)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	!drop_current_task(verbalization, Failure, Code).

+should_check_target_seen(Human,Ld) : true <-
	.send(Human,achieve,communicate_belief(isLookingAt(Ld))).
	
-should_check_target_seen(Human,Ld) : true <-
	.send(Human,achieve,stop_communicate_belief(isLookingAt(Ld))).
		
/********* **********/		
	
+!end_task(Task, Human) : true <-
	+end_task(Task, Human)[TaskName,Human];
	.findall(B[Task,Human,source(X),add_time(Y)],B[Task,Human,source(X),add_time(Y)], L);
	.send(history, tell, L);
	.abolish(_[Task,Human]).	
	
// Utils
+!speak(Human, ToSay) : true <-
//	.send(Human, tell, ToSay);
	text2speech(Human, ToSay).
	
+end_task(Task, Human) : listening <- stop_listen.
	
+not_exp_ans(2) : true <-
	!drop_current_task;
	stop_listen;
	!speak(Human, retire(unknown_words)).

+!drop_current_task : true <-
	?task(TaskName, Human, Param);
	G =.. [TaskName, [Human,Param],[]];
  	.fail_goal(G).
  	
+!drop_current_task(TaskName, Human, Param) : task(T,_,_) & TaskName == T <-
	G =.. [TaskName, [Human,Param],[]];
  	.fail_goal(G).
  	
+!drop_current_task(Subgoal, Failure, Code) : task(T,_,_) & Subgoal \== T <-
 	if(.substring(Failure, dialogue_as_failed)){
 		.print(STOP_LISTEN);
 		stop_listen;
 	}
 	if(.substring(Failure, dialogue_as_failed) | .substring(Failure, dialogue_as_not_found)){
 		Speech = "listening";
 	}elif(.substring(Failure, route_verba_failed)){
 		Speech = "verbalization";
 	}elif(.substring(move_to, Failure)){
 		Speech = "moving";
 	}elif(.substring(svp, Failure)){
 		Speech = "planning";
 	}elif(.substring(Failure, individual_not_found)){
 		Speech = "ontology";
 	}elif(.substring(verbalization, Failure)){
 		Speech = "verbalization";
 	}elif(.substring(self, Failure)){
 		Speech = "my self";
 	}
	.print("error with ",Code);
  	+failure(Subgoal, Failure, Code)[TaskName,Human];
  	!speak(Human, failure(Speech));
	!drop_current_task.	
  	