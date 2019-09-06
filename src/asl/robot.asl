// Agent robot in project supervisor

{ include("monitoring.asl")}
{ include("guiding_goal_negociation.asl")}
{ include("guiding.asl")}
!start.

^!guiding(ID, Human, Place)[state(started)] : not started[ID] <- +started[ID]; +monitoring(ID, Human).
^!guiding(ID, Human, Place)[state(S)] : (S == finished | S == failed) & not finished[ID] <-
	+finished[ID];
	.concat("human_", Human, H);  
	!face_human(H); 
	-monitoring(ID, Human)[add_time(_), source(self)].

+!face_human(H) : true <- face_human(H).
-!face_human(H) : true <- true.

+!start : true <- 
	.verbose(2); 
	jia.get_param("/guiding/perspective/robot_place", String, Rp);
	+robot_place(Rp).

//TODO faire une jia pour changer "Place" dans les params du plan guiding 
+!guiding(ID, Human, Place) : true <-
	jia.log_beliefs;	
	jia.publish_marker(0);
	.all_names(Agents);
	.send(interac, tell, inTaskWith(Human,ID));
	!clean_facts;
	+task(ID, guiding, Human, Place)[ID];
	!guiding_goal_negociation(ID, Human, Place);
	?guiding_goal_nego(ID, PlaceNego);
	-task(ID, guiding, Human, Place)[ID];
	// task belief with onto name
	+task(ID, guiding, Human, PlaceNego)[ID];	
	!get_optimal_route(ID);
	jia.get_param("/guiding/immo", "Boolean", Immo);
	if(Immo == false){
		!go_to_see_target(ID);
	}else{
		?target_place(T);
		.concat("human_", Human, HTF);
		if(jia.can_be_visible(HTF, T)){
			+target_to_point(T)[ID];
		}
		if(jia.believes(direction(_))){
			?direction(D);
			if(jia.can_be_visible(HTF, D)){
				+direction_to_point(D)[ID];
			}
		}
	}
	!show_landmarks(ID);
	+end_task(succeeded, ID)[ID];
	!clean_task(ID).

+bouh: true <-
	?task(ID, Task, Human, Param);
	G =.. [Task, [ID,Human,_],[]];
	.fail_goal(G).

-!guiding(ID, Human, Place) : true <-
	!clean_task(ID).
	
+!clean_facts: true <-
	-need_attentive_human(_);
	-look_for_human(_);
	-move_goal_reached;
	-adjust;
	-explained;
	-finished;
	.abolish(point_at(_));
	.abolish(look_at(_));
	-look_at(look).

+!drop_current_task(ID, Subgoal, Failure, Code) : true <-
	look_at_events(stop_look_at);
	?task(ID, Task, Human, Param);
	.print("error with ",Code);
  	+failure(Subgoal, Failure, Code)[ID];
  	+end_task(failed(Failure), ID)[ID];
  	G =.. [Task, [ID,Human,_],[]];
  	.fail_goal(G).
  
+!log_failure(ID, Subgoal, Failure, Code) : true <- 
	if(not jia.believes(failure(Subgoal, Failure, Code,_))){
		+failure(Subgoal, Failure, Code,0)[ID];
	}else{
		?failure(Subgoal, Failure, Code,N);
		+failure(Subgoal, Failure, Code,N+1)[ID];
	}.
	

+!clean_task(ID) : true <-
	?task(ID, Task, Human, Param);
	.findall(B[ID,source(X),add_time(Y)],B[ID,source(X),add_time(Y)], L);
	jia.beliefs_to_file(L);
	jia.reset_att_counter;
	.send(interac, untell, inTaskWith(Human,ID));
	.abolish(_[ID]).	

+end_task(Status, ID)[ID] :  true <- 
	?task(ID, _, Human, _); 
	.send(supervisor, tell, end_task(Status, ID)).

+failure(Subgoal, Failure, Code)[ID] : true <- .send(supervisor, tell, failure(ID, Subgoal, Failure, Code)).
	
// Utils
+!speak(ID, ToSay) : true <-
	?task(ID, _, Human, _);
	if(not jia.believes(said(ToSay,_))){
		+said(ToSay,0)[ID];
	}else{
		?said(ToSay,N);
		+said(ToSay,N+1)[ID];
	}
	text2speech(ToSay).
	
-!speak(ID, ToSay) : true <-	true.
  	
+preempted(ID) : true <-
	-preempted(ID)[add_time(_), source(_)];
	+end_task(preempted, ID)[ID];
	?task(ID, Task, Human, Place);
	!clean_task(ID);
	G =.. [Task, [ID,Human,_],[]];
	.drop_desire(G).
	
+cancelled(ID) : true <-
	-preempted(ID)[add_time(_), source(_)];
	+end_task(preempted, ID)[ID];
	?task(ID, Task, Human, Place);
	!clean_task(ID);
	G =.. [Task, [ID,Human,_],[]];
	.drop_desire(G).
	
+suspend(ID) : true <-
	-suspend(ID);
	+suspended(ID)[ID];
	?task(ID, Task, Human, Place);
	G =.. [Task, [ID,Human,_],[]];
	.suspend(G).
	
+resume(ID) : true <-
	-resume(ID);
	+resumed(ID)[ID];
	-suspended(ID)[ID];
	?task(ID, Task, Human, Place);
	G =.. [Task, [ID,Human,_],[]];
	.resume(G).

