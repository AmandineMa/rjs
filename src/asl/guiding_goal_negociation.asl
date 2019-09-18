// TODO a voir monitoring piloté par les differents asl
//^!guiding_goal_negociation(ID, Human,_)[state(S)] : S == started | S = resumed <- .resume(monitoring(Human)). 

//^!guiding_goal_negociation(ID, Human, Place)[state(started)] : not started <- +started; +monitoring(ID, Human).	
+!guiding_goal_negociation(ID, Human,Place): true <-
	if(not jia.word_individual(findSub, Place, PlaceOnto)){
		jia.word_class(findSub, Place, Class1);
		if(.list(Class1) & .member("restaurant", Class1)){
			.sort(Class1, Class2);
			.length(Class2, X);
			jia.delete_from_list("restaurant", Class2, ClassL);
			.nth(0, ClassL, Class);
		}else{
			Class=Class1;
		}
		if(jia.word_class(getUp, Class, GU) & .sublist(["product"], GU)){
			.concat(Class, ":sells", Product);
			jia.word_individual(getFrom, Product, List);
		}else{
			.concat(Class, "<1", GetDown);
			if(jia.word_class(getDown, GetDown, TL) & .list(TL)){
				jia.delete_from_list(Class, TL, List);
			}else{
				jia.word_individual(getType, Class, List);
			}
		}
		?robot_place(From);
		if(.substring(Class, atm) | .substring(Class, toilets)){
			jia.compute_route(From, List, lambda, false, 1, Route);
			Route =.. [route, [PlaceOnto, R], []];
		}elif(.string(List)){
			jia.word_individual(findSub, List, PlaceOnto);
		}else{
			jia.verba_name(List, PlacesVerba);
			!speak(ID, list_places(PlacesVerba));
			listen(list_places,PlacesVerba);
			?listen_result(list_places,Goal);
			web_view_start_processing;
			if(not jia.believes(got_answer(list_places,Goal,_))){
				+got_answer(list_places,Goal,0)[ID];
			}else{
				?got_answer(list_places,Goal,N);
				+got_answer(list_places,Goal,N+1)[ID];
			}
			!guiding_goal_negociation(ID, Human,Goal);
			.succeed_goal(guiding_goal_negociation(ID, Human,Place));
		}
	}
//	jia.word_individual(getName, PlaceOnto, PlaceName);
	+guiding_goal_nego(ID, PlaceOnto)[ID].
	
		
//+not_exp_ans(4) : guiding_goal_negociation <-
//	-guiding_goal_negociation;
//	.drop_desire(guiding_goal_negociation(ID, Human,_));
//	!speak(ID,max_sorry); 
//	!drop_current_task(ID, max_attempts, "multiple wrong answers", max_attempts).

// in case of the original plan failure	
-!guiding_goal_negociation(ID, Human, Place)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)]: true <-
	if(.substring(word_individual, Code) | .substring(word_class, Code)){
		+place_not_found(Place)[ID];
		jia.get_param("/guiding/dialogue/hwu", "Boolean", Dialogue);
		if(Dialogue == false){
			!speak(ID, no_place(Place));
		}
		!drop_current_task(ID, guiding_goal_negociation, no_place, Code);
  	}else{
  		!drop_current_task(ID, guiding_goal_negociation, Failure, Code); 
  	}.
	
// in case of the recovery plan failure  	
-!guiding_goal_negociation(ID, Human)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)]: true <-
	!drop_current_task(ID, guiding_goal_negoiciation, Failure, Code).
	
