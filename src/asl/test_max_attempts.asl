//!start.
//!test(["Pouet", "piop"]).
//!test(["Pouet", "piop"]).
//!test(["Pouet", "piop"]).
//!test2(0).
!start.
//!test(a).
//!test(a).
//!test(a).
//!test(a).
//!test(b).
//!test(b).
//!test(b).
//!test(b).
//!test(["Pouet"]).
//!test(["Pouet"]).
//!test(["Pouet"]).
//!test(["Pouet"]).
//!test(["Pouet", "piop"]).
//!test(["Pouet", "piop"]).
//!test(["Pouet", "piop"]).
//!test(["Pouet", "piop"]).
//!test2.

^!start[state(S)] : S == started | S == resumed <- 
	+monitoring.
	
^!start[state(S)] : S == finished | S == failed <- 
	-monitoring.

+!start : true <-
	!test(a);
	!test(a);
	!test(a);
	!test3;
	!test3;
	rjs.jia.reset_att_counter(test3);
	!test3;
	!test(a).
	
@test3[max_exec(2)] +!test3 : true <- .print(test3).
 -!test3 : true <- .print(coucou3).
@test[max_exec(3)] +!test(Pipo) : true <- if(.substring(Pipo,b)){.print(hey);} if(.substring(Pipo,a)){.print(bouh);}.
//if(.substring(Pipo,b)){.print("----------------hey-------------------");} if(.substring(Pipo,a)){.print("****************bouh*************");}.
-!test(Pipo)[_, error(ErrorId), error_msg(_), code(_), code_src(_), code_line(_)] : true <- .print(ErrorId).
//+!bouh : true <- .wait(1000); !hey.
//@label_test +!hey : true <- .wait(3000); .print(bouh).

@t[max_attempts(4)]+!test2(X): true <- 
	?b(Y); 
	Z = Y + 1; 
	-+b(Z); 
	.print("TEEEEEST"); 
	if(Z==3){
		.fail;
	}
	
	!test2(0).

//-!test2(X) : true <- .print(fail).
//code(.fail),code_line(74),code_src("supervisor.asl"),error(ia_failed),error_msg(""),source(self)
-!test2(X)[_, error(ErrorId), error_msg(_), code(_), code_src(_), code_line(_)] : true <- 
	if(.substring(ErrorId,max_attempts)){	
		.print("MAAAAX");
	}else{
		.print("FAAAAIL");
		!test2(0);
	}.
	