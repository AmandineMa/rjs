// Agent robot in project supervisor

/* Initial beliefs and rules */
//isPerceiving(human).

/* Initial goals */

//TODO quand suspension de tache, gestion des beliefs entre les differentes taches
//TODO check logs
//TODO change ID
//TODO make the human approach

// TODO handle when place to go already frame name
//place_asked("burger king").
robot_place("pepper_infodesk").
shops(["thai_papaya","starbucks","zizzi","marimekko_outlet","marco_polo","intersport","pancho_villa",
	   "daddys_diner","atm_2","atm_1","burger_king","funpark","hairstore","reima","ilopilleri","linkosuo",
	   "gina","h_m","hiustalo"]).
shop_names(["C M Hiustalo","h& m","gina","cafe linkusuo","kahvila ilopilleri","reima","hairstore","funpark",
	        "burger king","atm","daddy s diner","pancho villa","intersport","marco polo","marimekko outlet",
	        "zizzi","starbucks","thai papaya"]).	 
	
//shop_names(["Burger King","instrumentarium","Luckiefun","Pizza Hut","Hanko Sushi","dressman","Kahvila Ilopilleri","Arnolds",
//		"cafe linkosuo","Linda Mode","zizzi","bakery cafe","emotion","Kotipizza","Musti ja Mirri","marimekko outlet","empty 8",
//		"Name It","Minimani","robot info desk","kappahl","ten art","Eurokangas","dressmann xl","Shoe Store","Cubus","empty 9",
//		"Vero Moda","Life","carlings","gina tricot","empty 3","nissen","Vila","Your Face and Rils","Apteekki","cafe de lisa",
//		"Glitter","Information desk","Esprit","Farkkujen Tehtaanmyymala","superdry","change","bjorn borg","empty 6","empty 0",
//		"click shoe","H and M","door a","Power","pukimo","juvesport","Tanssiopisto","Ballot","N P","guess","Digiman pop up",
//		"empty 18","empty kristyle","empty toys r us","door b","Avain- ja suutarityo Helsky","empty 13","Nordea","empty 14",
//		"bik bok","empty 10","Hemtex","empty 17","Clas Ohlson","rax","lindex","empty 5","elviira","Stadium","empty 12",
//		"Kultaporssi","pancho villa","ideapark sport areena","kultajousi","Tommy Hilfiger","swamp","reima","makelan",
//		"Logistiikkakeskus","saunalahti","empty kirjaporssi","empty vallila shop","Tekniskamagasinet","empty 1a north","alko",
//		"Espresso House","sievi shop","door D","SubWay","HoviKebab","Din Sko","mango","c m hiustalo","empty netrauta 1","empty 1",
//		"toilet","empty empty front faunatar","Lumo-puoti","iittala outlet","empty 2","empty 15","Suomalainen Kirjakauppa","empty 11",
//		"Brother Clothing","pelaamo","aleksi 13","sinelli","empty empty food1","empty netrauta 2","banking machine","lahi tapiola",
//		"Halonen","empty 16","oma saastopankki","DNA","Kauneus- ja jalkahoitola Manna","Fonum","Telia","passage","Kicks","timanttiset",
//		"digiman","faunatar","new yorker","door C","empty 7","empty elisa","carts","River Co","stairs","escalator","elevator",
//		"Lagoon Fish Foot Spa","Donna Rosa","Aitoleipa","my bag","PiiPoo","intersection","only","empty 4","kukkakauppa","Gant",
//		"Jesper Junior","Hairlekiini","spice ice","marco polo","hesburger","Daddy's Diner","Coyote Grill","sticky wingers","Prisma",
//		"saaga","Mummola Lahjapuoti","pentik","KOO Kenka","Unikulma","Netrauta","masku","finnlandia","bottle recovery","Budget Sport",
//		"easy fit traingin","atm","Intersport","Kalastus Suomi","flying tiger copenhagen","Partioaitta","Top- Sport","hair store",
//		"finla","Body Shop","easyfit","hiljainen huone","Fortum","Ti-Ti Nallen Koti","Zones"]).	
//		
//shops(["Zones_by_Sarkanniemi","Ti_Ti_Nallen_Koti","Fortum","Hiljainen_huone","Easyfit","Body_Shop","Finla","HairStore","Top_Sport",
//	"Partioaitta","Flying_Tiger_Copenhagen","Kalastus_Suomi","Intersport_Ideapark","gf_atm_west","Easy_Fit_Traingin","Budget_Sport",
//	"pullonpalautus","Finnlandia","Masku","Netrauta","Unikulma","KOO_Kenka","Pentik","Mummola_Lahjapuoti","Saaga","Prisma",
//	"Sticky_Wingers","Coyote_Grill","Bella_Roma_and_Daddys_Diner","Hesburger","Marco_Polo","Spice_Ice","Hairlekiini","Jesper_Junior",
//	"Gant","Kukkakauppa","empty_4","only_2","intersection_corridortoiletseast_pallokatu","PiiPoo","My_Bag","Aitoleipa","Donna_Rosa",
//	"Lagoon_Fish_Foot_Spa","elevator_coyote","escalator_kompassikatu","stairs_keskuspuisto","River_and_Co","carts_A","empty_elisa",
//	"empty_7","door_C","carts_C","escalator_A","New_Yorker","intersection_corridor1northwest_corridor1west","Faunatar","Digiman",
//	"Timanttiset","Kicks","intersection_kompassikatu_veistoskatu","old_interface_east","Telia","gf_atm_east","Fonum",
//	"Kauneus__ja_jalkahoitola_Manna","escalator_sauruskatu","DNA","Oma_Saastopankki","empty_16",
//	"intersection_corridor1Dnorth_corridor1northwest","Halonen","LahiTapiola_Service_desk","Banking_machine","empty_netrauta_2",
//	"empty_empty_food1","Sinelli","Aleksi_13","Pelaamo","Brother_Clothing","empty_11","Suomalainen_Kirjakauppa","empty_15","empty_2",
//	"old_interface_south","Iittala_Outlet","Lumo_puoti","escalator_D","empty_empty_front_faunatar","ff_toilet_east","empty_1",
//	"empty_netrauta_1","ff_toilet_west","CM_Hiustalo","Mango","Din_Sko","HoviKebab","stairs_pancho","SubWay","door_D","elevator_east",
//	"Sievi_Shop","elevator_keskuspuisto","Espresso_House","Alko","empty_1A_north","Tekniskamagasinet","empty_vallila_shop",
//	"empty_Kirjaporssi","Saunalahti","Logistiikkakeskus","Makelan_Kauppapuutarna","Reima","swamp","Tommy_Hilfiger","Kultajousi",
//	"Ideapark_Sport_Areena","Pancho_Villa","Kultaporssi","empty_12","Stadium","Elviira","empty_5","carts_B",
//	"intersection_oldcorridorwest_oldopenspace","Lindex","intersection_palapelikatu_sauruskatu","Rax",
//	"intersection_corridor1Anorth_corridor1Asouth","Clas_Ohlson","empty_17","Hemtex","intersection_corridor1B_corridor1east",
//	"empty_10","Bik_Bok","empty_14","intersection_corridorcoffeeeast_corridorkeskuwest","only","Nordea",
//	"intersection_corridortoiletswest_palapelikatu","empty_13","Avain__ja_suutarityo_Helsky",
//	"intersection_Keskuspuisto_corridorkeskueast","door_B","empty_Toys_R_Us","empty_Kristyle","empty_18","elevator_west",
//	"gf_toilet_telia","Digiman_pop_up","Guess","NP","Ballot","intersection_corridor1centralnorth_corridor1west","Tanssiopisto",
//	"gf_toilet_east","gf_toilet_west","Juvesport","Pukimo","intersection_corridor1C_corridor1Dsouth","Power","door_A",
//	"Hennes_and_Mauritz","intersection_corridor1centralnorth_corridorpancho2","Click_Shoe","empty_0","empty_6","Bjorn_Borg",
//	"Change","Superdry","Farkkujen_Tehtaanmyymala","Esprit_Ideapark","Information_desk","intersection_corridorB_kompassikatu",
//	"Glitter","Cafe_de_Lisa","Apteekki_Ideapark","Your_Face_and_Rils","Vila","Nissen","empty_3","intersection_sauruskatu_sitruunakatu",
//	"intersection_corridorcoffeewest_palapelikatu","intersection_corridor1Dnorth_corridor1extremwest","Gina_Tricot",
//	"intersection_corridor1Dnorth_corridor1Dsouth","Carlings","Life","intersection_corridor1centralnorth_corridor1northeast",
//	"Vero_Moda,_JandJ","intersection_corridoreast_pallokatu","empty_9","Cubus","Shoe_Store","intersection_corridor1Dsouth_corridor1food",
//	"Dressmann_XL","Eurokangas","intersection_Keskuspuisto_kompassikatu","old_interface_west","intersection_corridor1C_corridor1food",
//	"Ten_Art","intersection_corridorkeskuwest_palapelikatu","KappAhl","intersection_pallokatu_veistoskatu",
//	"intersection_corridoreast_kompassikatu","intersection_oldcorridoreast_oldopenspace","pepper_infodesk","Minimani",
//	"intersection_corridorwest_palapelikatu","stairs_coyote","intersection_corridorcoffeeeast_palapelikatu","Name_It",
//	"intersection_palapelikatu_sitruunakatu","empty_8","Marimekko_Outlet","intersection_corridorkeskueast_pallokatu",
//	"intersection_corridor1little_corridor1west","intersection_oldcorridorsouth_oldopenspace","intersection_corridor1food_corridor1little",
//	"intersection_corridorwest_sauruskatu","intersection_corridor1food_foodpark","Musti_ja_Mirri","intersection_corridor1C_foodpark",
//	"Kotipizza_and_Rolls_Expert","intersection_corridor1Dsouth_corridor1west","intersection_corridorC_sauruskatu","emotion","Stahlberg",
//	"intersection_Keskuspuisto_corridorkeskuwest","intersection_Keskuspuisto_sauruskatu","intersection_coyoteopenspace_bridge1",
//	"Zizzi","intersection_corridor1Anorth_corridor1northeast","intersection_corridor1east_corridor1northeast",
//	"intersection_corridor1Asouth_corridor1B","intersection_corridor1centralnorth_corridor1east",
//	"intersection_corridor1Asouth_corridor1east","intersection_corridoreast_corridorelevatoreast",
//	"intersection_corridor1Dsouth_foodpark","intersection_corridorelevatorwest_corridorwest","Linda_Mode",
//	"Cafe_Linkosuo","Arnolds","Kahvila_Ilopilleri","Dressman","Hanko_Sushi","carts_D",
//	"intersection_bridge1_foodpark","Pizza_Hut","Luckiefun","Instrumentarium","Burger_King"]).      
	    
//persona_asked(lambda).   

/* Plans */
//!init.
//+!init : true <- 
//	// get shops list from ontology
////	get_onto_individual_info(getType, place, shops);
//	?shops(Shops);
//	for(.member(X, Shops)){
//		get_onto_individual_info(getName, X, shop_name);
//	}
//	.findall(X, shop_name(X), L);
//	+shop_names(L);
//	.abolish(shop_name(_));
//	.send(supervisor, tell, init_over(ok)).
//
//-!init : true <- +init_over(failed).

/***** Tasks ******/

//// when receiving a plan from the supervisor, task infos (name of the task and human involved in) are sent to the AgArch
//+!Plan[source(Source)] : Source == supervisor | (Source == self & restart_plan) <- 
//	-restart_plan;
//	Plan =.. [Label, [Human,Param],[]];
//	//TODO mettre ID dans annot a la place de Label et Human
//	+task(ID, Label, Human, Param)[Label,Human];
//	set_task_infos(Label, Human);
//	!Plan.


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
		// to know if the human can climb stairs
		!speak(ID, ask_stairs);
		listen(ask_stairs,["yes","no"]);
		?listen_result(ask_stairs,Word);
		if(.substring(Word,yes)){
			+persona_asked(lambda)[ID];
		}else{
			+persona_asked(disabled)[ID];
			-route(_);
			// compute a new route with the persona information
			compute_route(From, Place, disabled, false);
		}		
	}
	?route(R2);
	// TODO update quand direction n'est plus stairs
	?target_place(Target);
	get_onto_individual_info(getName, Target, verba_name);
	// if the route has an interface (a direction)
	if(.length(R2) > 3){
		.nth(2,R2,Dir);
		+direction(Dir)[ID];
		get_onto_individual_info(getName, Dir, verba_name);
	}.

	
-!get_optimal_route(ID)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)]: true <-	
	!drop_current_task(ID, get_optimal_route, Failure, Code).
	
/*******  go to see target **********/	

^!go_to_see_target(ID)[state(S)] : S == started | S == resumed <- -monitoring(_, _)[add_time(_), source(self)]. 
//^!go_to_see_target(Human)[state(suspended)[reason(R)]] <- 
//	if(.substring(suspended,R)){
//		jia.suspend_all(monitoring(_));
//	}.
^!go_to_see_target(ID)[state(S)] : S == finished | S == failed <- ?task(ID, _, Human, _); +monitoring(ID, Human).

+!go_to_see_target(ID) : true <-
	?task(ID, guiding, Human, _);
	!get_placements(ID);
	?ld_to_point;
	!be_at_good_pos(ID).
	
-!go_to_see_target(ID)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)] : true <- 
	!log_failure(ID, go_to_see_target, Failure, Code).
//	if(not .substring(Code, ld_to_point)){
//		!drop_current_task(ID, go_to_see_target, Failure, Code);
//	}.
	
+!get_placements(ID): not no_mesh <-
	?task(ID, guiding, Human, _);
	?target_place(TargetLD);
	.concat("human_", Human, HTF);
	if(jia.has_mesh(TargetLD)){
		// if there is a direction
		if(jia.believes(direction(_))){
			?direction(Dir);
			if(jia.has_mesh(Dir)){
				get_placements(TargetLD,Dir,HTF,0);
			}
		}else{
			get_placements(TargetLD,"",HTF,0);
		}
	}else{
		if(jia.believes(direction(_))){
			?direction(Dir);
			if(jia.has_mesh(Dir)){
				get_placements(Dir,"",HTF,1);
			}
		}
	}.
	


-!get_placements(ID)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_), source(self)] : true <- 
	!log_failure(ID, get_placements, Failure, Code).
//	if(not .substring(svp, Failure) | not .substring(svp,Error)){
//  		!drop_current_task(ID, get_placements, Failure, Code);
//  	}else{
//  		!log_failure(ID, get_placements, Failure, Code);
//  	}.
	
+!be_at_good_pos(ID) : true <- 
	?task(ID, guiding, Human, _);
	?robot_pose(RframeC,RpositC, RorientC);
	jia.publish_marker(RframeC,RpositC, yellow);
	?human_pose(Hframe,Hposit,_);
	jia.publish_marker(Hframe, Hposit, blue);
	if(jia.believes(robot_move(_,_, _))){
		?robot_move(Rframe,Rposit, Rorient);
		!speak(ID, going_to_move);
	}else{
		?robot_turn(Rframe,Rposit, Rorient);
	}
	if(jia.believes(human_first(_))){
		?human_first(Side);
		!speak(ID, step, Side);
		.concat("human_", Human, HTF);
		!check_dist(ID, HTF, Rposit, 0.5, false);
		jia.reset_att_counter(check_dist);
	}
//	tf.quat_face_human(Rposit, Hposit, Q);
	move_to(Rframe,Rposit, Rorient);
	!wait_human(ID).

@cd[max_attempts(15)]+!check_dist(ID, HTF, Point, Dist, Bool) : true <- 
	tf.is_dist_human2point_sup(HTF, Point, Dist, Result);
	if(.substring(Result, Bool)){
		.wait(200);
		!check_dist(ID, HTF, Point, Dist, Bool);
	}.
	
-!check_dist(ID, HTF, Point, Dist, Bool) : not adjust(_) <- 
	?human_first(Side);
	!repeat_move(ID, HTF, Point, Bool, Side).
	
-!check_dist(ID, HTF, Rposit, Dist, Bool) : adjust(Side) <- 
	!repeat_move(ID, HTF, Rposit, Bool, Side).	
	
@rm[max_attempts(2)]+!repeat_move(ID, HTF, Rposit, Dist, Bool, Side) : true <-
	!speak(ID, step_more, Side);
	jia.reset_att_counter(check_dist);
	!check_dist(ID, HTF, Rposit, Dist, Bool).
	
-!repeat_move(ID, HTF, Rposit, Dist) : true <- 
	!speak(ID, cannot_move); 
	!log_failure(ID, repeat_move, cannot_move, _);
	.drop_intention(be_at_good_pos(ID)).
	
-!repeat_move(ID, HTF, Rposit, Dist) : adjust(_) <- 
	!log_failure(ID, repeat_move, adjust, _);
	if(jia.believes(dir_to_point(_))){
		?dir_to_point(D);
		.concat("human_", Human, HTF);
		if(not jia.can_be_visible(HTF, D)){
			-dir_to_point(D);
		}
	}
	if(jia.believes(target_to_point(_))){
		?target_to_point(T);
		.concat("human_", Human, HTF);
		if(not jia.can_be_visible(HTF, T)){
			-target_to_point(T);
		}
	}
	-adjust(_);
	.drop_intention(be_at_good_pos(ID)).

-!be_at_good_pos(ID)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)] : true <-
	if(not .substring(robot_pose, Code)){
//		!drop_current_task(ID, be_at_good_pos, Failure, Code);
		!log_failure(ID, be_at_good_pos, Failure, Code);
	}.
 
@wh[max_attempts(3)]+!wait_human(ID) : true <- 
	?task(ID, guiding, Human, _);
	?robot_pose(Rframe,Rposit, Rorient);
	?human_pose(Hframe,Hposit,_);
	.concat("human_", Human, HTF);
	if(tf.get_transform(map, HTF, Point,_)){
		.nth(2, Point, Z);
		if(Z > 1.8){
			Zbis=1.8;
		}else{
			Zbis=Z;
		}	
		jia.replace(2, Hposit, Zbis, Pointf);
		jia.publish_marker(Hframe, Pointf, blue);
		-look_at(look);
		look_at(Hframe,Pointf,true);
		.wait(isPerceiving(_),4000);
	//	.wait(isPerceiving(Human),4000);
		-~here(Human);
		.wait(look_at(look),20000);
		look_at_events(human_perceived);
		!check_pos(ID, Human);
	}else{
		!log_failure(ID, wait_human, no_transform, tf.get_transform(map, HTF, Point,_));
	}.
	
@cp[max_attempts(2)]+!check_pos(Human): true <-
	if(jia.believes(dir_to_point(_))){
		?dir_to_point(D);
		.concat("human_", Human, HTF);
		can_be_visible(HTF, D);
	}
	if(jia.believes(target_to_point(_))){
		?target_to_point(T);
		.concat("human_", Human, HTF);
		can_be_visible(HTF, T);
	}.

-!check_pos(ID, Human)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : not ajust(_) <- 
	if(.substring(not_visible,Error)){
		.concat("human_", Human, HTF);
		?human_pose(Hframe,Hposit,_);
		tf.is_dist_human2point_sup(HTF, Hposit, 0.5, Result);
		if(.substring(Result, true)){
			!adjust_human_pos(ID, Human);
		}
	}else{
		!log_failure(ID, check_pos, Failure, Code);
	}.
	
+!adjust_human_pos(ID, Human) : true <-
	if(tf.h_step_r_or_l(Human, Pose, Side)){
		+adjust(Side);
		!speak(ID, step, Side);
		.concat("human_", Human, HTF);
		!check_dist(ID, HTF, Pose, 0.5, true);
		jia.reset_att_counter(check_dist);
		-adjust(Side);
	}.

-!adjust_human_pos(ID, Human)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	-adjust(_);
	!log_failure(ID, check_pos, Failure, Code).
	
-!wait_human(ID)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	?task(ID, guiding, Human, _);
	// TODO meme erreur que pour isPerceiving, ne permet pas de differencier les deux
	if(.substring(wait_timeout, Error) & .substring(isPerceiving, Code)){
		look_at_events(stop_look_at);
		+~here(Human);
		!speak(ID, come);
		!wait_human(ID);
	}elif(.substring(wait_timeout, Error) & .substring(look_at, Code)){
		look_at_events(stop_look_at);
		!log_failure(ID, look_at, not_received, look_at(look));
		!check_pos(ID, Human);
	}elif(.substring(max_attempts,Error)){
		-look_at(look);
		look_at_events(stop_look_at);
		!speak(ID,cannot_find); 
		!drop_current_task(ID, wait_human, max_attempts, "wait too long");
	}else{
//		!drop_current_task(ID, wait_human, Failure, Code);
		!log_failure(ID, wait_human, Failure, Code);
	}.
	
/*******  show landmarks **********/

//^!point_look_at(ID, _)[state(started)] <- jia.suspend_all(monitoring(_)). 
//^!point_look_at(ID, _)[state(resumed)] <- jia.suspend_all(monitoring(_)).
//^!point_look_at(ID, _)[state(suspended)[reason(R)]] <- 
//	if(.substring(suspended,R)){
//		jia.suspend_all(monitoring(_));
//	}.
//^!point_look_at(ID)[state(finished)] <- jia.suspend_all(monitoring(_)).

@sl[max_attempts(3)]+!show_landmarks(ID) : true <- 
	?task(ID, guiding, Human, _);
	!show_target(ID);
	!show_direction(ID); 
	.wait(800);
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
		!drop_current_task(ID, show_landmarks, max_attempts, multiple_wrong_answers);
	}else{
		!log_failure(ID, show_landmarks, Failure, Code);
//		!drop_current_task(ID, show_landmarks, Failure, Code);
	}.


+!show_target(ID) : true <-
	?target_place(TargetPlace);
	// if cannot transform, leaves the plan
	tf.can_transform(map, TargetPlace);
	!point_look_at(ID, TargetPlace);
	jia.reset_att_counter(point_look_at).
	

-!show_target(ID)[Failure, code(Code),code_line(_),code_src(_),error(_),error_msg(_)] : true <-
	if(not .substring(can_transform, Code)){
		!log_failure(ID, show_target, Failure, Code);
	}else{
		?target_place(TargetPlace);
		!verbalization(ID, TargetPlace);
	}.
	
+!show_direction(ID) : true <-
	?direction(D);
	tf.can_transform(map, D);
	!point_look_at(ID, D);
	jia.reset_att_counter(point_look_at).

-!show_direction(ID)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	?task(ID, guiding, Human, _);
	if(.substring(can_transform, Code)){
		?direction(D);
		!verbalization(ID, D);
	}elif(not .substring(test_goal_failed, Error)){
		!log_failure(ID, show_direction, Failure, Code);
	}.

landmark_to_see(Ld) :- (target_to_point(T) & T == Ld) | (dir_to_point(D) & D == Ld).

// TODO handle timeout point_at
@pl_l[max_attempts(3), atomic_r]+!point_look_at(ID, Ld) : landmark_to_see(Ld) <-
	?task(ID, guiding, Human, _);
	+should_check_target_seen(Human,Ld);
	point_at(Ld,false,true);
	jia.time(T);
	.print(T);
	.wait(point_at(point),30000);
	-point_at(point);
	!verbalization(ID, Ld);
	.wait(point_at(finished),30000);
	-point_at(finished);
	-should_check_target_seen(Human,Ld);
	?(canSee(Ld)[source(Human)] | hasSeen(Ld)[source(Human)]);
	?verba_name(Ld,Verba);
	!speak(ID, tell_seen(Verba)).


@pl_nl[max_attempts(3), atomic_r]+!point_look_at(ID, Ld) : not landmark_to_see(Ld) <-
	?task(ID, guiding, Human, _);
	point_at(Ld,false,true);
	.wait(point_at(point),30000);
	-point_at(point);
	!verbalization(ID, Ld).


-!point_look_at(ID, Ld)[Failure, code(Code),code_line(_),code_src(_),error(Error),error_msg(_)] : true <-
	?task(ID, guiding, Human, _);
	if(.substring(test_goal_failed, Error)){
		!ask_seen(ID,Ld);
	}elif(.substring(Error,max_attempts)){
		!speak(ID,pl_sorry); 
		jia.reset_att_counter(point_look_at);
	}elif(.substring(point_at,Code)){
		jia.time(T2);
		.print(T2);
		!log_failure(ID, point_look_at, point_at(point), not_received);
	}else{
		!log_failure(ID, point_look_at, Failure, Code);
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
  	!log_failure(ID, ask_seen, Failure, Code).

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

  	