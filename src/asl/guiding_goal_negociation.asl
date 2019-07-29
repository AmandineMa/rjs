// TODO a voir monitoring piloté par les differents asl
//^!guiding_goal_negociation(ID, Human,_)[state(S)] : S == started | S = resumed <- .resume(monitoring(Human)). 
all_places(false).

//^!guiding_goal_negociation(ID, Human, Place)[state(started)] : not started <- +started; +monitoring(ID, Human).	
+!guiding_goal_negociation(ID, Human,Place): true <-
	if(jia.word_class(find, Place, Class)){
		jia.word_class(find, Place, Class);
		jia.word_individual(getType, Class, Places);
		?robot_place(From);
		if(.substring(Class, atm) | .substring(Class, toilets)){
			jia.compute_route(From, Places, lambda, false, 1, Route);
			Route =.. [route, [PlaceOnto, R], []];
		}elif(all_places(X) & X == true){
			jia.verba_name(Places, PlacesVerba);
			!speak(ID, list_places(PlacesVerba));
			listen(list_places,PlacesVerba);
			?listen_result(list_places,Goal);
			!guiding_goal_negociation(ID, Human,Goal);
			.succeed_goal(guiding_goal_negociation(ID, Human,Place));
		}elif(.list(Places)){
			!speak(ID, thinking);
			jia.compute_route(From, Places, lambda, false, 2, Routes);
			.nth(0, Routes, Route1);
			Route1 =.. [_, [Offer1, R1], []];
			.nth(1, Routes, Route2);
			Route2 =.. [_, [Offer2, R2], []];
			Offers = [Offer1, Offer2];
			jia.verba_name(Offers, OffersVerba);
			!speak(ID, closest(OffersVerba));
			listen(closest,OffersVerba);
			?listen_result(closest,Goal);
			!guiding_goal_negociation(ID, Human,Goal);
			.succeed_goal(guiding_goal_negociation(ID, Human,Place));
		}
	}else{
		jia.word_individual(find, Place, PlaceOnto);
	}
//	jia.word_individual(getName, PlaceOnto, PlaceName);
	+guiding_goal_nego(ID, PlaceOnto).
	
	
// recovery plan
//+!guiding_goal_negociation(ID, Human) : individual_not_found(Individual) <-
//		!speak(ID, no_place(Individual));
//		?shop_names(X);
//		.concat(X, ["atm", "toilet"], Y);
//		// listen to places in the ontology
//		// TODO add timeout
//		listen(no_place,Y);
//		?listen_result(no_place,Word);
//		-individual_not_found(Individual);
//		!guiding_goal_negociation(ID, Human, Word).
		
//+not_exp_ans(4) : guiding_goal_negociation <-
//	-guiding_goal_negociation;
//	.drop_desire(guiding_goal_negociation(ID, Human,_));
//	!speak(ID,max_sorry); 
//	!drop_current_task(ID, max_attempts, "multiple wrong answers", max_attempts).

// in case of the original plan failure	
-!guiding_goal_negociation(ID, Human, Place)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)]: true <-
	if(.substring(word_individual, Code)){
		+individual_not_found(Place);
		!speak(ID, no_place(Place));
		!drop_current_task(ID, guiding_goal_negociation, Failure, Code); 
//	  	!guiding_goal_negociation(ID, Human);
  	}else{
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
	
	
