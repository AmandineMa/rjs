/******* monitoring **********/	

^!monitoring(Human)[state(S)] : S == started | S == resumed <- 
	+monitoring(Human).

^!monitoring(Human)[state(suspended)[reason(R)]] : not look_for_human(Human) <- 
	if(.substring(suspended,R)){
		-monitoring(Human);
	}.
	
^!monitoring(Human)[state(S)] : S == finished | S == failed <- 
	-monitoring(Human).	

+!monitoring(Human) : true <-
//	tracking(Human);
	.wait(1000);
	!monitoring(Human).
	
-isPerceiving(Human) : monitoring(Human) <-
	.wait(3000);
	!look_for_human(Human).

// when monitoring has been suspended by look_for_human	
+isPerceiving(Human) : (monitoring(Human) & .suspended(monitoring(Human),U)) <- 
	.succeed_goal(look_for_human(Human));
	!speak(Human,found_again); 
	.resume(monitoring(Human)).

+!look_for_human(Human) : isPerceiving(Human) <- 
	-look_for_human(Human);
	.resume(monitoring(Human));
	.resume(guiding(Human,_));
	jia.reset_att_counter(look_for_human).

@lfh[max_attempts(3)] +!look_for_human(Human) : not isPerceiving(Human) <- 
	+look_for_human(Human);
	jia.suspend_all(monitoring(_));
	// TODO suspend all ongoing tasks
	.suspend(guiding(Human,_));
//	look_for_human(Human);
	-look_for_human(Human);
	.resume(monitoring(Human));
	.resume(guiding(Human,_));
	jia.reset_att_counter(look_for_human).

-!look_for_human(Human) : isPerceiving(Human) <- 
	-look_for_human(Human);
	.resume(monitoring(Human));
	.resume(guiding(Human,_));
	jia.reset_att_counter(look_for_human).
	
-!look_for_human(Human)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : not isPerceiving(Human) <- 
	if(.substring(Error,max_attempts)){
		!speak(Human,cannot_find); 
		!end_task(_, Human);
	}else{
		!speak(Human,where_are_u);
		.wait(2000);
		!look_for_human(Human);
	}.	

	
