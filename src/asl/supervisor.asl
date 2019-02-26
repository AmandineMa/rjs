// Agent supervisor in project supervisor

/* Initial beliefs and rules */

/* Initial goals */

!start.

/* Plans */
//TODO check si les srv, action servers sont connectés
+!start : true <- 
	startParameterLoaderNode;
	startROSNode;
	.wait(500);
	initServices;
	.create_agent(robot, "src/asl/robot.asl", [agentArchClass("supervisor.RobotAgArch"), beliefBaseClass("supervisor.TimeBB")]);
	.create_agent(human, "src/asl/human.asl", [agentArchClass("supervisor.HumanAgArch"), beliefBaseClass("supervisor.TimeBB")]);
	.send(robot, achieve, guiding(human)).

-!start [Failure, error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]: true <-
	if(.substring(Failure, "srv_not_connected")){
		!retry_init_services;
	}.
	
+!retry_init_services : true <-
	retryInitServices.
	
-!retry_init_services : true <-
	.wait(3000);
	!retry_init_services.
	
