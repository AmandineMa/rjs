// Agent robot in project supervisor

{ include("monitoring.asl")}
{ include("guiding_goal_negociation.asl")}
{ include("guiding.asl")}


//// test multi goals
//+!guiding(ID, Human, Place) : true <-
//	+task(ID, guiding, Human, Place);
//	.print("guiding");
//	.wait(6000);
//	.send(supervisor, tell, end_task(true, ID)).
//	
//+suspend(ID) : true <-
//	?task(ID, Task, Human, Place);
//	G =.. [Task, [ID,Human,Place],[]];
//	.suspend(G).
//	
//+resume(ID) : true <-
//	?task(ID, Task, Human, Place);
//	G =.. [Task, [ID,Human,Place],[]];
//	.resume(G).
//	
//^!guiding(ID, _, _)[state(suspended)] <- if(.substring(suspended,R)){.print("task ",ID," suspended")}. 
//^!guiding(ID, _, _)[state(resumed)] <- .print("task ",ID, "resumed"). 

