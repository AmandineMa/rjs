// Agent robot in project supervisor

//{ include("monitoring.asl")}
{ include("guiding_goal_negociation.asl")}
{ include("guiding.asl")}
!start.
+!start : true <- .verbose(2).
+!guiding_task(ID, Human,Place) : true <-
//	text2speech("25", ask_show_again).
	-finished;
	-point_at(point);
	-look_at(look);
	.all_names(Agents);
	if(not .member(Human, Agents)){
		.create_agent(Human, "src/asl/human.asl", [agentArchClass("arch.HumanAgArch"), beliefBaseClass("agent.TimeBB")]);
	}
//	.send(Human, tell, end_task(succeeded, ID)).
	+task(ID, guiding, Human, Place)[ID];
	!guiding_goal_negociation(ID, Human,Place);
	?guiding_goal_nego(PlaceNego, PlaceFrame);
	if(Place \== PlaceNego){
		.send(supervisor, tell, updated_guiding_goal(ID, Human, PlaceNego));
		
	}
	-task(ID, guiding, Human, Place)[ID];
	+task(ID, guiding, Human, PlaceFrame)[ID];	
//	.concat("human-", Human, H);
//	human_to_monitor(H);
	!guiding(ID, Human, PlaceFrame);
//	human_to_monitor(""); 
	// for hwu
	text2speech(Human, succeeded);
	+end_task(succeeded, ID)[ID];
	.send(supervisor, tell, end_task(succeeded, ID)).

+!drop_current_task(ID, Subgoal, Failure, Code) : true <-
	if(.count((look_at(look)),I) & I == 0){
		.wait({+look_at(look)},4000);
		look_at_events(stop_look_at);
	}else{
		look_at_events(stop_look_at);
	}
	?task(ID, Task, Human, Param);
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
  	+failure(Subgoal, Failure, Code)[ID];
  	+end_task(failed, ID)[ID];
  	human_to_monitor("");
  	.send(supervisor, tell, end_task(failed, ID));
  	.send(supervisor, tell, failure(ID, Subgoal, Failure, Code));
  	if(.string(Speech)){
  		!speak(ID, failed(Speech));
  	}
  	G =.. [Task, [ID,Human,Param],[]];
  	.fail_goal(G).

+!clean_task(ID) : true <-
	?task(ID, Task, Human, Param);
//	+end_task(Task, Human)[ID];
	.findall(B[ID,source(X),add_time(Y)],B[ID,source(X),add_time(Y)], L);
//	?task(_,_,_,Param);
//	if(.count(succeeded,I) & I > 0){
//		.send(supervisor, tell, end_task(Task, Human, Param, true));
//	}else{
//		.send(supervisor, tell, end_task(Task, Human, Param, false));
//	}
	.send(history, tell, L);
	jia.reset_att_counter;
	.abolish(_[ID]).	
	
// Utils
+!speak(ID, ToSay) : true <-
	?task(ID, _, Human, _);
	.send(Human, tell, ToSay);
	text2speech(Human, ToSay).
	
-!speak(ID, ToSay) : true <-	true.

+end_task(_, _) : listening <- stop_listen.
  	
	
+suspend(ID) : true <-
	?task(ID, Task, Human, Place);
	G =.. [Task, [ID,Human,Place],[]];
	.suspend(G).
	
+resume(ID) : true <-
	?task(ID, Task, Human, Place);
	G =.. [Task, [ID,Human,Place],[]];
	.resume(G).
	
//^!guiding(ID, _, _)[state(suspended)] <- if(.substring(suspended,R)){.print("task ",ID," suspended")}. 
//^!guiding(ID, _, _)[state(resumed)] <- .print("task ",ID, "resumed"). 
