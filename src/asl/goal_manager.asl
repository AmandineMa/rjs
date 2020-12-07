//beliefs for tests
hasOven.
temperatureMin(20).
style(jean,hat).
goal(1, goal1, unsurfaced, null, 3, bob, bob, autonomous_task, false, [hasOven, temperatureMin(20)]).


typeFilter(Type) :- Type \== shared_task.
priorityFilter(Priority) :- Priority > 2.
invariantFilter([H]) :- H.
invariantFilter([H|T]) :- H & invariantFilter(T).
shouldBeSurfaced(Status, Priority, Initiator, Agents, Type, Invariants) :- typeFilter(Type) & priorityFilter(Priority) & invariantFilter(Invariants).

!start.
	
+!checkGoals : true
	<-	for(goal(Id, Name, unsurfaced, Status, Priority, Initiator, Agents, Type, HasPlan, Invariants)){
			if(shouldBeSurfaced(Status, Priority, Initiator, Agents, Type, Invariants)){
				-+goal(Id, Name, managed, Status, Priority, Initiator, Agents, Type, HasPlan, Invariants);
			};
		};
	!checkGoals.
	
//tests
+!start : true <- rjs.jia.log_beliefs; !!checkGoals.
+!test(Invariants) : invariantFilter(Invariants) <- .print("hey").
+!test(Invariants) : true <- .print("bouh").