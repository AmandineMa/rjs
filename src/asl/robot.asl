// Agent robot in project supervisor

{ include("monitoring.asl")}
{ include("guiding_goal_negociation.asl")}
{ include("guiding.asl")}

+!guiding_task(ID, Human,Place) : true <-
	.create_agent(Human, "src/asl/human.asl", [agentArchClass("arch.HumanAgArch"), beliefBaseClass("agent.TimeBB")]);
	+task(ID, guiding_task, Human, Place)[ID];
	!guiding_goal_negociation(ID, Human,Place);
	?guiding_goal_nego(PlaceNego, PlaceFrame);
	if(Place \== PlaceNego){
		.send(supervisor, tell, updated_guiding_goal(ID, Human, PlaceNego));
		-task(ID, guiding_task, Human, Place)[ID];
		+task(ID, guiding_task, Human, PlaceNego)[ID];
	}
	!guiding(ID, Human, PlaceFrame);
	+end_task(succeeded, ID)[ID];
	.send(supervisor, tell, end_task(succeeded, ID)).


+!drop_current_task(ID, Subgoal, Failure, Code) : true <-
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
  	.send(supervisor, tell, end_task(failed, ID));
  	.send(supervisor, tell, failure(ID, Subgoal, Failure, Code));
  	if(.string(Speech)){
  		!speak(ID, failure(Speech));
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
