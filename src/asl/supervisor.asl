// Agent supervisor in project supervisor

/* Initial beliefs and rules */
/* Initial goals */
!start.

/* Plans */
//TODO check si les srv, action servers sont connectés
//TODO si initServices fail, reprendre la suite du plan une fois qu'ils se sont connectes via retry
+!start : true <- 
	startParameterLoaderNode;
	startROSNode;
	initServices;
	!check_guiding_goal.
//	.send(interac, achieve,start).
//	.send(robot, achieve, init).

-!start [Failure, error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]: true <-
	if(.substring(Failure, "srv_not_connected")){
		!retry_init_services;
	}.
	
+!retry_init_services : true <-
	retryInitServices.
	
-!retry_init_services : true <-
	.wait(3000);
	!retry_init_services.
	
+!check_guiding_goal : true <-
	check_guiding_goal;
	.wait(200);
	!check_guiding_goal.
	
//+init_over(ok) : true <- 
//	.send(robot, achieve, guiding(0, human, "burger_king")).
////	.send(robot, achieve, guiding_goal_negociation(human, "vfdvd")).
//
//+guiding_goal(Human, To): true <-
//	.random(X);
//	.send(robot, achieve, guiding(X, Human, To)).
	
+init_over(failed) : true <- .print("robot initialisation failed").

+guiding_goal(ID,Human, Target) : true <- 
	if(.count((current_guiding_goal(_)),I) & I > 0){
		?current_guiding_goal(IDprev);
		.send(robot, tell, suspend(IDprev));
		+suspended_guiding_goal(IDprev);
		-current_guiding_goal(IDprev);
	}
	.send(robot, achieve, guiding_goal_negociation(ID, Human, Target));
	+current_guiding_goal(ID).

-guiding_goal(_,_,_) : suspended_guiding_goal(_) <-
	if(.count((suspended_guiding_goal(_)),I) & I == 1){
		?suspended_guiding_goal(ID);
	}else{
		jia.more_recent_bel(suspended_guiding_goal(_), B);
		B =.. [Sgg, [ID], [_,_]];
	}
	.send(robot, tell, resume(ID));
	-suspended_guiding_goal(ID);
	+current_guiding_goal(ID).

+end_task(Success, ID) : true <-
	-current_guiding_goal(ID);
	// TODO comportement coherent ? obligé d'ajouter les annots à cause des terms du belief qui l'oblige à aller à la ligne 959 de agent
	-guiding_goal(ID,_,_)[add_time(_),source(self)];
	set_guiding_result(Success, ID).
	
	