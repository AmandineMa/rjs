// Agent human in project supervisor

/* Initial beliefs and rules */

/* Initial goals */


/* Plans */

+!communicate_belief(Belief) : true <-
	.add_plan({@comm_beliefa[Belief] +Belief : true <- .send(robot,tell,Belief)});
	.add_plan({
		@comm_beliefr[Belief] -Belief : true <- 
			if(.substring(isLookingAt,Belief)){
				Belief =.. [Functor,[Arg],[]];
				.send(robot,tell,hasSeen(Arg));
			}
			.send(robot,untell,Belief)
	}).
	
+!stop_communicate_belief(Belief) : true <-
	.remove_plan(comm_beliefa[Belief]);
	.remove_plan(comm_beliefr[Belief]);.

	
	