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
//	// get shops list from ontology
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

// when receiving a plan from the supervisor, task infos (name of the task and human involved in) are sent to the AgArch
+!Plan[source(supervisor)] : true <- 
	Plan =.. [Label, [Human,Param],[]];
	+task(Label, Human, Param)[Label,Human];
	set_task_infos(Label, Human);
	!Plan.

+!guiding(Human, Place): true <- 
	!goal_negociation(guiding, Place);
//	!show_landmarks(Human);
//	!go_to_see_target(RobPose, HumPose);
//	!give_directions.
	+succeeded[guiding, Human];
	!end_task(guiding, Human).
	
-!guiding(Human,Place) : true <-
	!end_task(guiding, Human).

+!goal_negociation(guiding, Place): true <-
	!get_optimal_route(Place).
	
+!get_optimal_route(Place): not failure(get_optimal_route) <-
	// special handling if the place to go is toilet or atm
	!handle_atm_toilet(Place);
	// there should be one belief possible_places if it toilet or atm and there is no if it is something else
	.findall(X, possible_places(X), L);
	// if it is not toilet or atm
	if(.empty(L)){
		// get the corresponding name in the ontology	
		get_onto_individual_info(find, Place, onto_place);
		?onto_place(To);
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
		supervisor.time(TimeNow);
		supervisor.abolish(route(_), TimeNow);
		// to know if the human can climb stairs
		get_human_abilities;
		?persona_asked(PA);
		// compute a new route with the persona information
		compute_route(From, To, PA, false);
	}
	// if the route has an interface (a direction)
	if(.length(R) > 3){
		.nth(2,R,Dir);
		+direction(Dir);
	}.
	
// recovery plan
+!get_optimal_route : failure(get_optimal_route, Failure) <-
	if(.substring(Failure, individual_not_found)){
		?task(_, Human, Param);
		!speak(Human, no_place(Param));
		?shop_names(X);
		.concat(X, ["atm", "toilet"], Y);
		// listen to places in the ontology
		listen(Y);
		?listen_result(Word);
		!get_optimal_route(Word);
	}.

// in case of the original plan failure	
-!get_optimal_route(Place)[Failure, _, _, _, _, _]: not failure(get_optimal_route, _) <-
	?task(TaskName, Human, _);
  	+failure(get_optimal_route, Failure)[TaskName,Human];
  	!get_optimal_route.
  	
// in case of the recovery plan failure  	
-!get_optimal_route[NewFailure, _, _, _, _, _]: failure(get_optimal_route, Failure) <-
  	if(.substring(NewFailure, dialogue_as_failed)){
  		stop_listen;
  		!ros_failure(get_optimal_route, NewFailure, listening);
  	}else{
  		+failure(NewFailure);
  		!fail_current_task;
  	}.	
	

+!handle_atm_toilet(To): true <- 
	supervisor.toilet_or_atm(To, AorT);
	// add belief possible_places
	get_onto_individual_info(getType, AorT, possible_places).	

-!handle_atm_toilet(To):true <- true.

	
+not_exp_ans(2) : true <-
	!fail_current_task;
	stop_listen;
	!speak(Human, retire(unknown_words)).
	
+!end_task(Task, Human) : true <-
	.findall(B[Task,Human,source(X),add_time(Y)],B[Task,Human,source(X),add_time(Y)], L);
	.send(history, tell, L);
	.abolish(_[Task,Human]).	
	
// Utils
+!speak(Human, ToSay) : true <-
//	.send(Human, tell, ToSay);
	text2speech(Human, ToSay).
	
+!ros_failure(Goal, Failure, Speech) : true <-
  	?task(TaskName, Human, _);
	+failure(Goal, Failure)[TaskName,Human];
  	!speak(Human, failure(Speech));
  	!fail_current_task.

+!fail_current_task : true <-
	?task(TaskName, Human, Param);
	G =.. [TaskName, [Human,Param],[]];
  	.fail_goal(G).
  	
  	