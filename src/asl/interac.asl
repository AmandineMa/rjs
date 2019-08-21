/**** Interaction session *******/
//
//+!start : true <-
//	!gather_pot_interactant.
//
//+!gather_pot_interactant : true <- 
//	.wait(500);
//	// look for all the people that are close and looking at or speaking to the robot
//	.findall(X, (isClose(X, robot) & (isLookingAt(X, robot) | isSpeakingTo(X, robot))), L);
//	// for all those people, we add in the BB that they are potential interact
//	for(.member(X, L)){
//		+isPotInteractant(X);
//	}
//	!gather_pot_interactant.
//
//
//// Select the first potential interactant added to the BB to become an interactant,  if there is no interactant already
//+isPotInteractant(X) : not chosen_interactant(_) <- +chosen_interactant(X).
//
//// si personne n'est pas chosen et n'est plus close, l'enlever des potInteractant
//
//+chosen_interactant(X) : true <-
//	// TODO handle fb from approach, in case the distance increase
////	approach(X);
//	!welcome_interactant(X).
//	
//+!welcome_interactant(X) : true <- true.
//	greetings(X);
// if multiple people close
//	group_or_not;
// detect look at human point at human
// list of interactant
//	choose_activity.

//+ isSpeaking(X) : not in list of interactant <- interrompre tache en cours; ask if together; if no say (excuse me, i'll come back to you in a few'); if yes add interactant

//feedback approach

//disengage :
// receive good bye from dialogue
// human leave
!start.
+!start : true <- .verbose(2).

+isEngagingWith(Human,_) : not inSession(_) <-
	+inSession(Human);
	.all_names(Agents);
	if(not jia.member(Human, Agents)){
		.create_agent(Human, "src/asl/human.asl", [agentArchClass("arch.HumanAgArch"), beliefBaseClass("agent.TimeBB")]);
	}
	engage(Human);
	text2speech(Human, hello);
	.concat("human_", Human, H); 
	tf.is_dist_human2robot_sup(H, 2, Result); 
	if(.substring(Result, true)){
		text2speech(Human, closer);
	}
	.
	
+~isEngagedWith(Human, _) : inSession(Human) & isPerceiving(_, Human) & ((monitoring(Human) & inTaskWith(Human)) | not inTaskWith(Human)) <-
	disengaging_human(Human);
	.print(disengaging).
	
+left_task(Human) : true <- 
	!bye(Human).

+!bye(Human) : not inTaskWith(_) <-
	terminate_interaction(Human);
	!clean_facts(Human);
	text2speech(Human, goodbye).
	
+!bye(Human) : inTaskWith(_) <- true.

-!bye(Human) : true <- true.

-isPerceiving(_, Human) : not inTaskWith(_) & inSession(Human) <-
	!wait_human(Human).

//TODO si robot commence tache avec personne avec autre ID (parce que pb de reidentification), quitte la session en disant goodbye alors que tache en cours	
+!wait_human(Human) : true <-
	.wait(isPerceiving(_,Human), 5000).
	
-!wait_human(Human) : true <-
	!bye(Human).

+terminate_interaction(Human) : true <- !clean_facts(Human). //received by dialogue

+!clean_facts(Human): true <-
	-inSession(Human);
	-~isEngagedWith(Human, _)[add_time(_), source(_)];
	-isEngagedWith(Human, _)[add_time(_), source(_)].
	
