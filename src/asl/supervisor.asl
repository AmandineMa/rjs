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
	jia.publish_marker(0);
	.print("started");
	.create_agent(robot, "src/asl/robot.asl", [agentArchClass("arch.RobotAgArch"), beliefBaseClass("agent.TimeBB"), agentClass("agent.LimitedAgent")]);
	!check_guiding_goal.

+~connected_srv(S) : true <- .print("service not connected : ", S).

-!start [Failure, error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine), source(self)]: true <-
	if(.substring(Failure, "srv_not_connected")){
		!retry_init_services;
	}.
	
+!retry_init_services : true <-
	retryInitServices;
	jia.publish_marker(0);
	.print("started");
	.create_agent(robot, "src/asl/robot.asl", [agentArchClass("arch.RobotAgArch"), beliefBaseClass("agent.TimeBB"), agentClass("agent.LimitedAgent")]);
	!check_guiding_goal.
	
-!retry_init_services : true <-
	.wait(3000);
	!retry_init_services.
	
+!check_guiding_goal : true <-
	check_guiding_goal;
	.wait(200);
	!check_guiding_goal.
	
+init_over(failed) : true <- .print("robot initialisation failed").

+guiding_goal(ID,Human, Target) : not current_guiding_goal(ID) <- 
	if(jia.believes(current_guiding_goal(_))){
		?current_guiding_goal(IDprev);
		.send(robot, tell, suspend(IDprev));
		+suspended_guiding_goal(IDprev);
		-current_guiding_goal(IDprev);
	}
	.send(robot, achieve, guiding_task(ID, Human, Target));
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

+updated_guiding_goal(ID, Human, PlaceNego) : true <-
	-guiding_goal(ID,_,_)[add_time(_),source(self)];
	+guiding_goal(ID, Human, PlaceNego).

+end_task(Success, ID) : true <-
	-current_guiding_goal(ID);
	// TODO comportement coherent ? obligé d'ajouter les annots à cause des terms du belief qui l'oblige à aller à la ligne 959 de agent
	// belief ne disparait pas de la gui mais n'est plus dans la bb (testé avec ?guiding_goal(ID,_,_)[add_time(_),source(self)] avant et après la suppression du fait)
	-guiding_goal(ID,_,_)[add_time(_),source(self)];
	set_guiding_result(Success, ID).
	
	