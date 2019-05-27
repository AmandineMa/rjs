/**** Interaction session *******/

+!start : true <-
	!gather_pot_interactant.

+!gather_pot_interactant : true <- 
	.wait(500);
	// look for all the people that are close and looking at or speaking to the robot
	.findall(X, (isClose(X, robot) & (isLookingAt(X, robot) | isSpeakingTo(X, robot))), L);
	// for all those people, we add in the BB that they are potential interact
	for(.member(X, L)){
		+isPotInteractant(X);
	}
	!gather_pot_interactant.


// Select the first potential interactant added to the BB to become an interactant,  if there is no interactant already
+isPotInteractant(X) : not chosen_interactant(_) <- +chosen_interactant(X).

+chosen_interactant(X) : true <-
	// TODO handle fb from approach, in case the distance increase
//	approach(X);
	!welcome_interactant(X).
	
+!welcome_interactant(X) : true <- true.
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