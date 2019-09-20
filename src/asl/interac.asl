n(-1).
!start.

+!start : true <- 
	.verbose(2); 
	jia.log_beliefs.

+isEngagedWith(Human,_) : not inSession(_,_) & not localising<-
	.concat("human_", Human, HTF);
	human_to_monitor(HTF);
	?n(N);
	-+n(N+1);
	+inSession(Human,N+1)[N+1];
	engage(N+1);
	jia.get_param("/guiding/dialogue/hwu", "Boolean", Dialogue);
	if(Dialogue == false){
		text2speech(hello);
	}.
	
//+~isEngagedWith(Human, _) : inSession(Human,_) & isPerceiving(_, Human) & ((monitoring(Human) & inTaskWith(Human,_)) | not inTaskWith(Human,_)) <-
//	disengaging_human(Human);
//	.print(disengaging).
	
-inTaskWith(Human,ID) : not  isPerceiving(_, Human)<- 
	?n(N);
	+endTask(Human,ID)[N];
	.wait(6000);
	if(not jia.believes(isPerceiving(_,Human))){
		!bye(Human);
	}.

-inTaskWith(Human,ID) : isPerceiving(_, Human) <-
	?n(N);
	+endTask(Human,ID)[N].
	
+inTaskWith(Human,ID) : true <- 
	?n(N);
	+startTask(Human,ID)[N].

+!bye(Human) : not inTaskWith(_,_) & inSession(Human,N) <-
	+bye;
	+overBy(not_perceived)[N];
	jia.get_param("/guiding/dialogue/hwu", "Boolean", Dialogue);
	!wait_end_talking;
	if(Dialogue == false){
		text2speech(goodbye);
	}else{
		terminate_interaction(N);
	}
	!clean_facts(Human);
	!loca;
	-bye.
	
+!bye(Human) : inTaskWith(_,_) <- true.

-!bye(Human)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)] : true <-
	if(.substring(loca, Failure)){
		!loca;
	}.
	
+!wait_end_talking: finished_talking(_) <-
	if(not jia.believes(finished_talking(true))){
		.wait(300);
		!wait_end_talking;
	}.
	
+!wait_end_talking: true <- true.

+!loca: not localising <-
	+localising;
	jia.get_param("/guiding/dialogue/hwu", "Boolean", Dialogue);
	if(Dialogue == false){
		text2speech(localising);
	}
	human_to_monitor("");
	jia.get_param("/guiding/robot_base/position", "List", P);
	jia.get_param("/guiding/robot_base/orientation", "List", O);
	move_to(map, P, O);
	localise;
	move_to(map, P, O);
	-localising.
	
@loc[max_attempts(2)]+!loca: localising <-
	jia.get_param("/guiding/robot_base/position", "List", P);
	jia.get_param("/guiding/robot_base/orientation", "List", O);
	move_to(map, P, O);
	-localising.// !loca.
	
	
-!loca[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)]: true <-
	if(.substring(Error,max_attempts)){
		-localising;
	}else{
		!loca;
	}.
	
+isPerceiving(_, 0) : true <-
	human_to_monitor("human_0").

-isPerceiving(_, Human) : not inTaskWith(_,_) & inSession(Human,_) <-
	!wait_human(Human).

+!wait_human(Human) : true <-
	.wait(isPerceiving(_,Human), 6000).
	
-!wait_human(Human) : true <-
	!bye(Human).

//received by dialogue
+terminate_interaction(Human) : not bye <- 
	?n(N);
	+overBy(dialogue)[N];
	if(jia.believes(inTaskWith(Human,ID))){
		?inTaskWith(Human,ID);
		.send(robot, tell, preempted(ID));
	}
	!clean_facts(Human);
	!loca.

+!clean_facts(Human): true <-
	?n(N);
	.findall(B[N,source(X),add_time(Y)],B[N,source(X),add_time(Y)], L);
	jia.beliefs_to_file(L);
	.abolish(_[N]);
	-isEngagedWith(Human, _)[source(_)];
	-terminate_interaction(Human)[source(_)].
	
-isEngagedWith(Human, O) : inSession(Human,_) <-
	.send(robot, untell, isEngagedWith(Human, O));
	.send(robot, tell, ~isEngagedWith(Human, O)).	

+isEngagedWith(Human, O) : inSession(Human,_) <-
	.send(robot, tell, isEngagedWith(Human, O));
	.send(robot, untell, ~isEngagedWith(Human, O)).
	
	
