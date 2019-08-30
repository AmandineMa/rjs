// Agent human in project supervisor

/* Initial beliefs and rules */
//isLookingAt("burger_king").
/* Initial goals */


/* Plans */
!start.
+!start : true <- .my_name(N); .print(N).

+!communicate_belief(Belief) : true <-
	if(not jia.believes((Belief))){
		.add_plan({@comm_beliefa[Belief] +Belief : true <- .send(robot,tell,Belief)});
		.add_plan({
			@comm_beliefr[Belief] -Belief : true <- 
				if(.substring(canSee,Belief)){
					Belief =.. [Functor,[Arg],[]];
					.send(robot,tell,hasSeen(Arg));
				}
				.send(robot,untell,Belief)
		})
	}else{
		.send(robot,tell,Belief);
	}.
	
+!stop_communicate_belief(Belief) : true <-
	.remove_plan(comm_beliefa[Belief]);
	.remove_plan(comm_beliefr[Belief]);.

	
-isEngagedWith(O) : true <-
	.my_name(N);
	.term2string(N, S);
	.send(robot, untell, isEngagedWith(S, O));
	.send(robot, tell, ~isEngagedWith(S, O)).	

+isEngagedWith(O) : true <-
	.my_name(N);
	.term2string(N, S);
	.send(robot, tell, isEngagedWith(S, O));
	.send(robot, untell, ~isEngagedWith(S, O)).

		