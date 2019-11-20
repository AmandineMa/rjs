// Agent robot in project supervisor

//{ include("monitoring.asl")}
{ include("guiding_goal_negociation.asl")}
{ include("guiding.asl")}
!start.

^!guiding(ID, Human, Place)[state(started)] : not started[ID] <- 
	pause_asr_and_display_processing;
	.send(interac, tell, inTaskWith(Human,ID)); 
	+started[ID]; 
	+monitoring(ID, Human).
	
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
	!clean_facts;
	+goal_from_dialogue(Place)[ID];
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
				+dir_to_point(D)[ID];
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
	if(not jia.believes(place_not_found(_))){
		jia.qoi_to_file(task, Param, ID);
	}
	jia.reset_att_counter;
	.abolish(_[ID]);
	jia.reinit_qoi_variables;
	.send(interac, untell, inTaskWith(Human,ID)).	

+end_task(Status, ID)[ID] :  true <- 
	?task(ID, _, Human, _); 
	jia.get_task_achievement(TA);
	.send(interac, tell, task_achievement(ID, TA));
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
  	
+preempted(ID)[source(A)] : true <-
	if(.substring(A,robot)){
		.send(supervisor, tell, preempted(ID));
	}
	-preempted(ID)[add_time(_), source(_)];
	+end_task(preempted, ID)[ID];
	+finished[ID];
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
	
+said(ask_stairs,0) : true <-  jia.update_steps_number(increment); jia.executing_step(person_abilities).
+direction(_) : true <- jia.update_steps_number(increment); jia.update_steps_number(increment).
+target_to_point(_) : true <-jia.update_steps_number(increment).
+robot_move(_,_,_) : true <- jia.update_steps_number(increment); jia.executing_step(agents_at_right_place).
+robot_turn(_,_,_) : true <- jia.update_steps_number(increment); jia.executing_step(agents_at_right_place).

+guiding_goal_nego(_,_) : true <-  jia.update_step(increment).
+persona_asked(_) : true <- jia.update_step(increment).
+visible(target,_,true) : not step_agents_at_right_place_added <- +step_agents_at_right_place_added[ID]; jia.update_step(increment).
+visible(direction,_,true) : not step_agents_at_right_place_added <- +step_agents_at_right_place_added[ID]; jia.update_step(increment).
+target_explained : true <-  jia.update_step(increment).
+direction_explained : true <-  jia.update_step(increment).
//+said(happy_end,_) : dir_to_point(D) & not ld_seen(D) <-  jia.update_step(increment).
+ld_seen(_) : true  <- jia.update_step(increment).

+started : true <- jia.executing_step(goal_nego).
+target_verba : true <- jia.executing_step(target_explanation).
+direction_verba : true <- jia.executing_step(direction_explanation).
+said(cannot_tell_seen(_),0) : true <- jia.executing_step(landmark_seen).


