// Agent disambiguation_task in project supervisor

/* Initial beliefs and rules */

/* Initial goals */

!start.

+!start : true <- .verbose(2); jia.log_beliefs.

	
+clicked_object(O): true <-
	disambiguate(O,robot);
	?sparql_result(O,S);
	sparql_verbalization(S);
	?verba(O,V);
	if(.length(V,X) & X > 0){
		say(V);
	}else{
		say("I could not find any disambiguation for this object");
	};
	.abolish(_).
	
