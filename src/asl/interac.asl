!start.
+!start : true <- 
	.verbose(2); 
	jia.log_beliefs.

+isEngagedWith(Human,_) : not inSession(_) & not localising <-
	.concat("gaze_human_", Human, HTF);
	human_to_monitor(HTF);
	+inSession(Human);
	.all_names(Agents);
	if(not jia.member(Human, Agents)){
		.create_agent(Human, "src/asl/human.asl", [agentArchClass("arch.HumanAgArch"), beliefBaseClass("agent.TimeBB")]);
	}
	engage(Human);
	jia.get_param("/guiding/dialogue/hwu", "Boolean", Dialogue);
	if(Dialogue == false){
		!speak(Human, hello);	
	}.
	
//+~isEngagedWith(Human, _) : inSession(Human) & isPerceiving(_, Human) & ((monitoring(Human) & inTaskWith(Human)) | not inTaskWith(Human)) <-
//	disengaging_human(Human);
//	.print(disengaging).
	
-inTaskWith(Human) : not  isPerceiving(_, Human)<- 
	.wait(800);
	!bye(Human).

+!bye(Human) : not inTaskWith(_) & inSession(Human) <-
	if(Dialogue == false){
		!speak(Human, goodbye);
	}
	terminate_interaction(Human);
	!clean_facts(Human);
	!loca.
	
+!bye(Human) : inTaskWith(_) <- true.

-!bye(Human)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)] : true <-
	if(.substring(loca, Failure)){
		!loca;
	}.

+!loca: true <-
	+localising;
	human_to_monitor("");
	jia.get_param("/guiding/robot_base/position", "List", P);
	jia.get_param("/guiding/robot_base/orientation", "List", O);
	move_to(map, P, O);
	localise;
	move_to(map, P, O);
	-localising.
	
-!loca: true <- true.// !loca.

+isPerceiving(_, 0) : true <-
	human_to_monitor("gaze_human_0").

-isPerceiving(_, Human) : not inTaskWith(_) & inSession(Human) <-
	human_to_monitor("");
	!wait_human(Human).

+!wait_human(Human) : true <-
	.wait(isPerceiving(_,Human), 6000).
	
-!wait_human(Human) : true <-
	!bye(Human).

 //received by dialogue
+terminate_interaction(Human) : not .intend(bye(_)) <- 
	!clean_facts(Human);
	!loca.

+!clean_facts(Human): true <-
	-inSession(Human);
	-~isEngagedWith(Human, _)[add_time(_), source(_)];
	-isEngagedWith(Human, _)[add_time(_), source(_)].
	
+!speak(Human, ToSay) : true <-
	?inSession(Human);
	.send(Human, tell, ToSay);
	text2speech(Human, ToSay).
	
	
