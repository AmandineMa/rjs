// Agent robot in project supervisor

/* Initial beliefs and rules */
isPerceiving(human).

/* Initial goals */


// TODO handle when place to go already frame name
//place_asked("burger king").
robot_place("robot_infodesk").
shops(["thai_papaya","starbucks","zizzi","marimekko_outlet","marco_polo","intersport","pancho_villa",
	   "daddys_diner","atm_2","atm_1","burger_king","funpark","hairstore","reima","ilopilleri","linkosuo",
	   "gina","h_m","hiustalo"]).
shop_names(["C M Hiustalo","h& m","gina","cafe linkusuo","kahvila ilopilleri","reima","hairstore","funpark",
	        "burger king","atm","daddy s diner","pancho villa","intersport","marco polo","marimekko outlet",
	        "zizzi","starbucks","thai papaya"]).	
//persona_asked(lambda).   

/* Plans */

+!init : true <- 
	// get shops list from ontology
//	get_onto_individual_info(getType, shop, shops);
//	?shops(Shops);
//	for(.member(X, Shops)){
//		get_onto_individual_info(getName, X, shop_name);
//	}
//	.findall(X, shop_name(X), L);
//	+shop_names(L);
//	.abolish(shop_name(_));
	.send(supervisor, tell, init_over(ok)).

-!init : true <- +init_over(failed).

/***** Tasks ******/

//// when receiving a plan from the supervisor, task infos (name of the task and human involved in) are sent to the AgArch
//+!Plan[source(Source)] : Source == supervisor | (Source == self & restart_plan) <- 
//	-restart_plan;
//	Plan =.. [Label, [Human,Param],[]];
//	//TODO mettre ID dans annot a la place de Label et Human
//	+task(ID, Label, Human, Param)[Label,Human];
//	set_task_infos(Label, Human);
//	!Plan.
	
^!guiding(Human, Place)[state(started)] : not started <- +started; !monitoring(Human).

+!guiding(ID, Human, Place): true <-
	!get_optimal_route(ID);
	!go_to_see_target(ID);
	!show_landmarks(ID);
	!clean_task(ID).
	
-!guiding(ID, Human, Place) : true <-
	!clean_task(ID).
	

/******* get route **********/	

+!get_optimal_route(ID): true <-
	?task(ID, guiding, Human, Place);
	// compute a route with Place which can be a place or a list of places
	// if it s a list of places, compute_route select the place with the smallest cost
	?robot_place(From);
	compute_route(From, Place, lambda, false);
	?route(R);
	// if the route has stairs, check if the human can use the stairs
	if(.substring("stairs", R)){
		// remove the old computed route
		.abolish(route(_));
		// to know if the human can climb stairs
		!speak(ID, ask_stairs);
		listen(ask_stairs,["yes","no"]);
		?listen_result(ask_stairs,Word);
		if(.substring(Word,yes)){
			+persona_asked(lambda)[ID];
		}else{
			+persona_asked(disabled)[ID];
		}
		?persona_asked(PA);
		// compute a new route with the persona information
		compute_route(From, Place, PA, false);
	}
	?target_place(Target);
	get_onto_individual_info(getName, Target, verba_name);
	// if the route has an interface (a direction)
	if(.length(R) > 3){
		.nth(2,R,Dir);
		+direction(Dir)[ID];
		get_onto_individual_info(getName, Dir, verba_name);
	}.
	
-!get_optimal_route(ID)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)]: true <-	
	!drop_current_task(ID, get_optimal_route, Failure, Code).
	
/*******  go to see target **********/	

^!go_to_see_target(ID)[state(S)] : S == started | S = resumed <- jia.suspend_all(monitoring(_)). 
//^!go_to_see_target(Human)[state(suspended)[reason(R)]] <- 
//	if(.substring(suspended,R)){
//		jia.suspend_all(monitoring(_));
//	}.
^!go_to_see_target(ID)[state(S)] : S == finished | S = failed <- .resume(monitoring(Human)).

+!go_to_see_target(ID) : true <- 
	?task(ID, guiding, Human, _);
	!get_placements(ID);
	?ld_to_point;
	!be_at_good_pos(ID).
	
-!go_to_see_target(ID)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)] : true <-
	if(not .substring(Code, ld_to_point)){
		!drop_current_task(ID, go_to_see_target, Failure, Code);
	}.
	
+!get_placements(ID): true <-
	?task(ID, guiding, Human, _);
	?target_place(TargetLD);
	// if there is a direction
	if(.count((direction(_)),I) & I > 0){
		?direction(Dir);
		get_placements(TargetLD,Dir,Human);
	}else{
		get_placements(TargetLD,"",Human);
	}.

//TODO to see if we drop the task when svp fail, why not continue the task without moving ?	
-!get_placements(ID)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_), source(self)] : true <-
	if(not .substring(svp, Failure) | not .substring(svp,Error)){
  		!drop_current_task(ID, get_placements, Failure, Code);
  	}else{
  		+failure(get_placements, Failure, Code)[ID];
  	}.
	
+!be_at_good_pos(ID) : true <- 
	?task(ID, guiding, Human, _);
	?robot_pose(Rframe,Rposit, Rorient);
	move_to(Rframe,Rposit, Rorient);
	?human_pose(Hframe,Hposit,_);
	tf.get_transform(map, Human, Point,_);
	.nth(2, Point, Z);
	jia.replace(2, Hposit, Z, Pointf);
//	look_at(Hframe,Pointf,true);
	!wait_human(ID).
	
-!be_at_good_pos(ID)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)] : true <-
	!drop_current_task(ID, be_at_good_pos, Failure, Code).

+!wait_human(ID) : true <- 
	?task(ID, guiding, Human, _);
	if(.count((isPerceiving(Human)),I) & I == 0){
		.wait({+isPerceiving(Human)},4000);
		-~here(Human);
	}
	if(.count((dir_to_point(_)),I) & I > 0){
		?dir_to_point(D);
		has_mesh(D);
		can_be_visible(Human, D);
	}
	if(.count((target_to_point(_)),J) & J > 0){
		?target_to_point(T);
		has_mesh(T);
		can_be_visible(Human, T);
	}.
	
-!wait_human(ID)[Failure, code(_),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	?task(ID, guiding, Human, _);
	if(.substring(Error, wait_timeout)){
		+~here(Human);
		!speak(ID, come);
		!wait_human(Human);
	}elif(.substring(Failure, not_visible)){
		!speak(ID, move_again);
		!be_at_good_pos(Human);
	}else{
		!drop_current_task(ID, wait_human, Failure, Code);
	}.
	
/*******  show landmarks **********/

^!point_look_at(ID, _)[state(started)] <- jia.suspend_all(monitoring(_)). 
^!point_look_at(ID, _)[state(resumed)] <- jia.suspend_all(monitoring(_)).
^!point_look_at(ID, _)[state(suspended)[reason(R)]] <- 
	if(.substring(suspended,R)){
		jia.suspend_all(monitoring(_));
	}.
^!point_look_at(ID)[state(finished)] <- jia.suspend_all(monitoring(_)).

@sl[max_attempts(3)]+!show_landmarks(ID) : true <- 
	?task(ID, guiding, Human, _);
	!show_target(ID);
	!show_direction(ID);
	!speak(ID, ask_understand);
	// TODO add timeout
	listen(ask_understand,["yes","no"]);
	?listen_result(ask_understand,Word1);
	if(.substring(Word1,yes)){
		!speak(ID, happy_end);
	}else{
		!speak(ID, ask_explain_again);
		listen(ask_explain_again,["yes","no"]);
		?listen_result(ask_explain_again,Word2);
		if(.substring(Word2,yes)){
			!show_landmarks(Human);
		}else{
			!speak(ID, hope_find_way);
		}
	}.
	
-!show_landmarks(ID)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)] : true <-
	if(.substring(Error,max_attempts)){
		!speak(ID,sl_sorry); 
		!drop_current_task(ID, show_landmarks, max_attempts, "multiple wrong answers");
	}else{
		!drop_current_task(ID, show_landmarks, Failure, Code);
	}.


+!show_target(ID) : true <-
	?target_place(TargetPlace);
	// if cannot transform, leaves the plan
	tf.can_transform(map, TargetPlace);
	!verbalization(ID, TargetPlace);
	!point_look_at(ID, TargetPlace);
	jia.reset_att_counter(point_look_at).
	

-!show_target(ID)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)] : true <-
	if(not .substring(can_transform, Code)){
		!drop_current_task(ID, show_target, Failure, Code);
	}.
	
+!show_direction(ID) : true <-
	?direction(D);
	tf.can_transform(map, D);
	!verbalization(ID, D);
	!point_look_at(ID, D);
	jia.reset_att_counter(point_look_at).

-!show_direction(ID)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	?task(ID, guiding, Human, _);
	if(.substring(can_transform, Code)){
		?direction(D);
		!verbalization(Human, D);
	}elif(not .substring(test_goal_failed, Error)){
		!drop_current_task(ID, show_direction, Failure, Code);
	}.

landmark_to_see(Ld) :- (target_to_point(T) & T == Ld) | (dir_to_point(D) & D == Ld).

@pl_l[max_attempts(3)]+!point_look_at(ID, Ld) : landmark_to_see(Ld) <-
	?task(ID, guiding, Human, _);
	+should_check_target_seen(Human,Ld);
//	point_at(Ld,false,true);
	-should_check_target_seen(Human,Ld);
	?(canSee(Ld)[source(Human)] | hasSeen(Ld)[source(Human)]);
	?verba_name(Ld,Verba);
	!speak(ID, tell_seen(Verba)).
		

@pl_nl[max_attempts(3)]+!point_look_at(ID, Ld) : not landmark_to_see(Ld) <-
	?task(ID, guiding, Human, _).
//	point_at(Ld,false,true).


-!point_look_at(ID, Ld)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	?task(ID, guiding, Human, _);
	if(.substring(test_goal_failed, Error)){
		!ask_seen(ID,Ld);
	}elif(.substring(Error,max_attempts)){
		!speak(ID,pl_sorry); 
	}else{
		!drop_current_task(ID, point_look_at, Failure, Code);
	}.
	
+!ask_seen(ID, Ld) : true <-
	?task(ID, guiding, Human, _);
	?verba_name(Ld,Verba);
	!speak(ID, cannot_tell_seen(Verba));
	listen(cannot_tell_seen,["yes","no"]);
	?listen_result(cannot_tell_seen, Word1);
	if(.substring(Word1,no)){
		!speak(ID, ask_show_again(Ld));
		listen(ask_show_again,["yes","no"]);
		?listen_result(ask_show_again, Word2);
		if(.substring(Word2,yes)){
			// TODO add max_attempts
			!point_look_at(ID,Ld);
		}else{
			!speak(ID, hope_find_way);
		}
	}.
	
-!ask_seen(ID, Ld)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)]: true <-
  	!drop_current_task(ID, ask_seen, Failure, Code).	

+!verbalization(ID, Place) : target_to_point(T) & T == Place & direction(_) <-
	?task(ID, guiding, Human, _);
	?verba_name(Place, Name);
	!speak(ID, visible_target(Name)).
	
+!verbalization(ID, Place) : not target_to_point(_) &  direction(D) & Place \== D <-
	?task(ID, guiding, Human, _);
	?verba_name(Place, Name);
	!speak(ID, not_visible_target(Name)).

+!verbalization(ID, Place) : ( direction(D) & Place == D & dir_to_point(D)) | (target_to_point(T) & T == Place & not direction(_)) <-
	?task(ID, guiding, Human, _);
	!get_verba(ID);
	?verbalization(RouteVerba);
	!speak(ID, route_verbalization(RouteVerba)).

+!verbalization(ID, Place) : ( direction(D) & Place == D & not dir_to_point(D) ) | ( not target_to_point(_) & not direction(_)) <-
	?task(ID, guiding, Human, _);
	!get_verba(ID);
	?verbalization(RouteVerba);
	!speak(ID, route_verbalization_n_vis(RouteVerba)).
	
+!get_verba(ID) : true <-
	?route(Route);
	?robot_place(RobotPlace);
	?target_place(FinaleP);
	get_route_verbalization(Route, RobotPlace, FinaleP).
	
-!verbalization(ID, Place)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	!drop_current_task(ID, verbalization, Failure, Code).

+should_check_target_seen(Human,Ld) : true <-
	.send(Human,tell,state(waiting_for_robot_to_point(Ld)));
	.send(Human,achieve,communicate_belief(canSee(Ld))).
	
-should_check_target_seen(Human,Ld) : true <-
	.send(Human,achieve,stop_communicate_belief(canSee(Ld))).
	
+canSee(Ld)[source(Human)] : (Ld == D & direction(D)) | (Ld == T & target_place(T)) <- 
	.send(Human,untell,state(waiting_for_robot_to_point(Ld))).
		
/********* **********/		


// TODO : differencier les cas des differents listening	
//+not_exp_ans(2) : true <-
//	!drop_current_task;
//	stop_listen;
//	!speak(ID, retire(unknown_words)).

//+!drop_current_task : true <-
//	?task(_, TaskName, Human, Param);
//	G =.. [TaskName, [Human,Param],[]];
//  	.fail_goal(G).
  	
//+!drop_current_task(TaskName, Human, Param) : task(_,T,_,_) & TaskName == T <-
//	G =.. [TaskName, [Human,Param],[]];
//  	.fail_goal(G).
  	

//	!drop_current_task.	
  	