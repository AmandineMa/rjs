// Agent supervisor in project supervisor

/* Initial beliefs and rules */

/* Initial goals */

!start.
//!test.

/* Plans */
//TODO check si les srv, action servers sont connectés
//TODO si initServices fail, reprendre la suite du plan une fois qu'ils se sont connectes via retry
+!start : true <- 
	startParameterLoaderNode;
	startROSNode;
	initServices;
//	.create_agent(robot, "src/asl/robot.asl", [agentArchClass("supervisor.RobotAgArch"), beliefBaseClass("supervisor.TimeBB")]);
//	.create_agent(interact_hist, "src/asl/interact_hist.asl", [beliefBaseClass("supervisor.TimeBB")]);
//	.create_agent(human, "src/asl/human.asl", [agentArchClass("supervisor.HumanAgArch"), beliefBaseClass("supervisor.TimeBB")]).
	.send(robot, achieve, init).
//	.send(robot, achieve, test).

-!start [Failure, error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]: true <-
	if(.substring(Failure, "srv_not_connected")){
		!retry_init_services;
	}.
	
+!retry_init_services : true <-
	retryInitServices.
	
-!retry_init_services : true <-
	.wait(3000);
	!retry_init_services.
	
+init_over(ok) : true <- .send(robot, achieve, guiding(human, "kokoj")).
+init_over(failed) : true <- .print("robot initialisation failed").

+!test : true <- .wait(500); !test.