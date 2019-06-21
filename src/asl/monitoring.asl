/******* monitoring **********/	

+monitoring(ID, Human) : isPerceiving(Human) <-
	!start_monitoring(ID, Human).
	
+monitoring(ID, Human) : not isPerceiving(Human) <-
	!start_monitoring(ID, Human).

-monitoring(ID,Human) : true <- human_to_monitor("").

+!start_monitoring(ID, Human) : isPerceiving(Human) <-
	.concat("human-", Human, H);
	human_to_monitor(H).

+!start_monitoring(ID, Human) : not isPerceiving(Human) <-
	.concat("human-", Human, H);
	human_to_monitor(H);
	.wait({+isPerceiving(Human)},4000).
	
-!start_monitoring(ID, Human) : not isPerceiving(Human) <-
	!look_for_human(Human).

-isPerceiving(Human) : monitoring(_, Human) <-
	!wait_before_looking(Human).

+!wait_before_looking(Human) : not  look_for_human(Human)<-
	+look_for_human(Human);
	.wait({+isPerceiving(Human)},6000);
	-look_for_human(Human).
	
+!wait_before_looking(Human) : look_for_human(Human) <- true.
	
-!wait_before_looking(Human) : true <-
	!look_for_human(Human).

+!look_for_human(Human) : isPerceiving(Human) <- 
	?task(ID, guiding, Human, Place);
	.resume(guiding(ID, Human,Place));
	jia.reset_att_counter(look_for_human);
	-look_for_human(Human).
	
@lfh[max_attempts(2)] +!look_for_human(Human) : not isPerceiving(Human) & monitoring(_, Human) <- 
	?task(ID, guiding, Human, Place);
	.suspend(guiding(ID, Human,Place));
	!speak(ID,where_are_u);
	.wait({+isPerceiving(Human)},6000);
	?task(ID, _, Human, _);
	!speak(ID,found_again);
	.resume(guiding(ID, Human,Place));
	jia.reset_att_counter(look_for_human);
	-look_for_human(Human).
	
+!look_for_human(Human) : isPerceiving(Human) & not monitoring(_, Human) <- -look_for_human(Human).

-!look_for_human(Human)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : isPerceiving(Human) <- 
	if(not .substring(Error, wait_timeout)){
		?task(ID, _, Human, _);
		!drop_current_task(ID, wait_human, Failure, Code);
	}else{
		?task(ID, _, Human, _);
		!speak(ID,found_again);
		.resume(guiding(ID, Human,Place));
		jia.reset_att_counter(look_for_human);
		-look_for_human(Human)
	}.
	
-!look_for_human(Human)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : not isPerceiving(Human) <- 
	?task(ID, _, Human, _);
	if(.substring(Error,max_attempts)){
		!speak(ID,cannot_find); 
		!clean_task(ID);
	}else{
		!look_for_human(Human);
	}.

	
