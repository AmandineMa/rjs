// TODO a voir monitoring piloté par les differents asl
//^!guiding_goal_negociation(ID, Human,_)[state(S)] : S == started | S = resumed <- .resume(monitoring(Human)). 
	
+!guiding_goal_negociation(ID, Human,Place): true <-
	+guiding_goal_negociation;
	// special handling if the place to go is toilet or atm
	!handle_atm_toilet(Place);
	// there should be one belief possible_places if it toilet or atm and there is no if it is something else
	.findall(X, possible_places(Place,X), L);
	// if it is not toilet or atm
	if(.empty(L)){
		// get the corresponding name in the ontology	
		get_onto_individual_info(find, Place, onto_place);
		?onto_place(Place,To);
	// if we got a list of toilets or a list of atms
	} else{
		// L is a list of list and normally with only one list, the one we get at index 0
		.nth(0, L, To);
	}
	+guiding_goal_nego(Place,To);
	-guiding_goal_negociation.
	
+!handle_atm_toilet(To): true <-
	jia.toilet_or_atm(To, AorT);
	// add belief possible_places
	get_onto_individual_info(getType, AorT, possible_places).	

-!handle_atm_toilet(To)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] :true <- 
	if(not .substring(ia_failed, Error)){
		!drop_current_task(ID, handle_atm_toilet, Failure, Code);
	}.
	
// recovery plan
+!guiding_goal_negociation(ID, Human) : individual_not_found(Individual) <-
		!speak(ID, no_place(Individual));
		?shop_names(X);
		.concat(X, ["atm", "toilet"], Y);
		// listen to places in the ontology
		// TODO add timeout
		listen(no_place,Y);
		?listen_result(no_place,Word);
		-individual_not_found(Individual);
		!guiding_goal_negociation(ID, Human, Word).
		
+not_exp_ans(4) : guiding_goal_negociation <-
	-guiding_goal_negociation;
	.drop_desire(guiding_goal_negociation(ID, Human,_));
	!speak(ID,max_sorry); 
	!drop_current_task(ID, max_attempts, "multiple wrong answers", max_attempts).

// in case of the original plan failure	
-!guiding_goal_negociation(ID, Human, Place)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)]: true <-
	if(.substring(Failure, individual_not_found)){
		+individual_not_found(Place);
	  	!guiding_goal_negociation(ID, Human);
  	}else{
  		-guiding_goal_negociation;
  		!drop_current_task(ID, guiding_goal_negociation, Failure, Code); 
  	}.
	
// in case of the recovery plan failure  	
-!guiding_goal_negociation(ID, Human)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)]: true <-
	!drop_current_task(ID, guiding_goal_negoiciation, Failure, Code).
	
//+!drop_current_task(Human, Failure, Code, Error): true <-
//	stop_listen;
//	.print("error with ",Code);
//	if(.substring(Failure, dialogue_as_failed) | .substring(Failure, dialogue_as_not_found)){
// 		!speak(Human, failure("listening"));
// 	}elif(.substring(Error,max_attempts)){
// 		!speak(Human, retire(unknown_words));
// 	}else{
// 		!speak(Human, failure("my self"));
// 	};
// 	-guiding_goal_negociation(Human, _);
// 	+failure(Failure)[guiding_goal_negociation, Human].
	
	
