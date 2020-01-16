// Agent disambiguation_task in project supervisor

/* Initial beliefs and rules */

/* Initial goals */

!start.

/* Plans */

+!start : true <-
	configureNode;
	startParameterLoaderNode("/disambi.yaml");
	startROSNodeDisambi;
	initServices;
	.create_agent(robot, "src/asl/disambiguation_task.asl", [agentArchClass("arch.agarch.disambi.RobotAgArch"), beliefBaseClass("agent.TimeBB"), agentClass("agent.LimitedAgent")]);.
	
	
+~connected_srv(S) : true <- .print("service not connected : ", S).