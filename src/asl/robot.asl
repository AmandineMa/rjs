// Agent robot in project supervisor

//{ include("monitoring.asl")}
{ include("guiding_goal_negociation.asl")}
{ include("guiding.asl")}

!start.

^!guiding(ID, Human, Place)[state(started)] : not started <- +started; +monitoring(ID, Human).
^!guiding(ID, Human, Place)[state(S)] : (S == finished | S == failed) & not finished <-
	+finished;
	.concat("human_", Human, H);  
	!face_human(H); 
	.succeed_goal(person_of_interest(Human));
	-monitoring(ID, Human)[add_time(_), source(self)].

+!face_human(H) : true <- face_human(H).
-!face_human(H) : true <- true.

+!start : true <- .verbose(2).

+!guiding(ID, Human, Place) : true <-	
	jia.publish_marker(0);
	.send(interac, tell, inTaskWith(Human));
	+task(ID, guiding, Human, Place)[ID];
	!clean_facts;
	+task(ID, guiding, Human, Place)[ID];
	!guiding_goal_negociation(ID, Human, Place);
	?guiding_goal_nego(ID, PlaceNego);
	-task(ID, guiding, Human, Place)[ID];
	// task belief with onto name
	+task(ID, guiding, Human, PlaceNego)[ID];	
	!get_optimal_route(ID);
	!go_to_see_target(ID);
	!show_landmarks(ID);
	+end_task(succeeded, ID)[ID];
	!clean_task(ID).

-!guiding(ID, Human, Place) : true <-
	!clean_task(ID).
	
+!clean_facts: true <-
	-finished;
	-point_at(point);
	-look_at(look).

+!drop_current_task(ID, Subgoal, Failure, Code) : true <-
	look_at_events(stop_look_at);
	?task(ID, Task, Human, Param);
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
 		if(.substring(wait, Code)){
 			Code=.. [W, [L, T], []];
			L =.. [P, [_], []];
			.concat("waiting ", P, Speech);
 		}else{
 			Speech = "my self";
 		}
 	}
	.print("error with ",Code);
  	+failure(Subgoal, Failure, Code)[ID];
  	+end_task(failed, ID)[ID];
  	if(.string(Speech)){
  		!speak(ID, failed(Speech));
  	}
  	G =.. [Task, [ID,Human,Param],[]];
  	.fail_goal(G).
  
+!log_failure(ID, Subgoal, Failure, Code) : true <- +failure(Subgoal, Failure, Code)[ID].

+!clean_task(ID) : true <-
	?task(ID, Task, Human, Param);
	.findall(B[ID,source(X),add_time(Y)],B[ID,source(X),add_time(Y)], L);
	.send(history, tell, L);
	jia.reset_att_counter;
	.send(interac, untell, inTaskWith(Human));
	.abolish(_[ID]).	

+end_task(Status, ID)[ID] : true <- .send(supervisor, tell, end_task(Status, ID)). //text2speech(Human, Status).

+failure(Subgoal, Failure, Code)[ID] : true <- .send(supervisor, tell, failure(ID, Subgoal, Failure, Code)).
	
// Utils
+!speak(ID, ToSay) : true <-
	?task(ID, _, Human, _);
	.send(Human, tell, ToSay);
	text2speech(Human, ToSay).
	
-!speak(ID, ToSay) : true <-	true.
  	
+cancel(ID) : true <-
	-cancel(ID)[add_time(_), source(_)];
	+end_task(cancelled, ID)[ID];
	?task(ID, Task, Human, Place);
	!clean_task(ID);
  	human_to_monitor("");
	G =.. [Task, [ID,Human,Place],[]];
	.drop_desire(G).
	
+suspend(ID) : true <-
	-suspend(ID);
	+suspended(ID)[ID];
	?task(ID, Task, Human, Place);
	G =.. [Task, [ID,Human,Place],[]];
	.suspend(G).
	
+resume(ID) : true <-
	-resume(ID);
	+resumed(ID)[ID];
	-suspended(ID)[ID];
	?task(ID, Task, Human, Place);
	G =.. [Task, [ID,Human,Place],[]];
	.resume(G).

