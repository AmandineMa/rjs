//beliefs for tests
hasOven.
temperatureMin(20).
style(jean,hat).
goal(1, goal1, unsurfaced, null, 3, bob, bob, autonomous_task, false, [hasOven, temperatureMin(20)]).
typeFilterParam([monitoring]).
priorityFilterParam(2).

typeFilter(Type) :- typeFilterParam(Param) & .member(Type, Param).
priorityFilter(Priority) :- priorityFilterParam(Param) & Priority > Param.
invariantFilter([H]) :- H.
invariantFilter([H|T]) :- H & invariantFilter(T).
shouldBeSurfaced(Status, Priority, Type, Invariants) :- typeFilter(Type) & priorityFilter(Priority) & invariantFilter(Invariants).

shouldBeActivated(Priority) :- not ongoingGoal(ID) | goal(ID,_,active,_, PrioG,_,_,_,_,_) & PrioG < Priority.


!start.
	
+!checkUnsurfacedGoals : true
	<-	for(goal(Id, Name, unsurfaced, Status, Priority, Initiator, Agents, Type, HasPlan, Invariants)){
			if(shouldBeSurfaced(Status, Priority, Type, Invariants)){
				-+goal(Id, Name, managed, Status, Priority, Initiator, Agents, Type, HasPlan, Invariants);
			};
		};
		!checkUnsurfacedGoals.
		
+!checkManagedGoals : true
	<- 	for(goal(Id, Name, managed, Status, Priority, Initiator, Agents, Type, HasPlan, Invariants)){
			if(not shouldBeSurfaced(Status, Priority, Type, Invariants)){
				-+goal(Id, Name, unsurfaced, Status, Priority, Initiator, Agents, Type, HasPlan, Invariants);
			}elif(shouldBeActivated(Priority)){
				-+goal(Id, Name, active, Status, Priority, Initiator, Agents, Type, HasPlan, Invariants);
				
			};
		};
		!checkManagedGoals.
		

	
//tests
+!start : true <- rjs.jia.log_beliefs; !!checkUnsurfacedGoals; !!checkManagedGoals.
