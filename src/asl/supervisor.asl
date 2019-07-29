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
	initGuidingAs;
	jia.publish_marker(0);
	.print("started");
	.create_agent(robot, "src/asl/robot.asl", [agentArchClass("arch.RobotAgArch"), beliefBaseClass("agent.TimeBB"), agentClass("agent.LimitedAgent")]).

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
	
+init_over(failed) : true <- .print("robot initialisation failed").

+guiding_goal(ID,Human, Target) : not suspended_guiding_goal(ID) <- 
	!suspend_current_goal;
	.send(robot, achieve, guiding(ID, Human, Target));
	-guiding_goal(ID, Human, Target);
	+current_guiding_goal(ID).

+guiding_goal(ID,Human, Target) : suspended_guiding_goal(ID) <- 
	!suspend_current_goal;
	.print("RESUME");
	.send(robot, tell, resume(ID));
	-suspended_guiding_goal(ID);
	+current_guiding_goal(ID).
	
+!suspend_current_goal : true <-
	if(jia.believes(current_guiding_goal(_))){
		?current_guiding_goal(IDprev);
		.send(robot, tell, suspend(IDprev));
		+suspended_guiding_goal(IDprev);
		-current_guiding_goal(IDprev);
	}.
	
+cancel_goal(ID) : true <-
	.send(robot, tell, cancel(ID));
	-current_guiding_goal(ID);
	-suspended_guiding_goal(ID);
	-cancel_goal(ID).

// To automatically restart a suspended goal after that the other one finished
//-guiding_goal(_,_,_) : suspended_guiding_goal(_) <-
//	if(.count((suspended_guiding_goal(_)),I) & I == 1){
//		?suspended_guiding_goal(ID);
//	}else{
//		jia.more_recent_bel(suspended_guiding_goal(_), B);
//		B =.. [Sgg, [ID], [_,_]];
//	}
//	.send(robot, tell, resume(ID));
//	-suspended_guiding_goal(ID);
//	+current_guiding_goal(ID).

+end_task(Success, ID) : true <-
	-current_guiding_goal(ID);
	set_guiding_result(Success, ID).
	
	