!start.

+!start : true <-
	rjs.jia.log_beliefs;
	+guiding(human1, burger_king);
	.wait(500);
	+guiding(human2, burger_king);
	.wait(500);
	+guiding(human1, atm);
	.wait(500);
	+guiding(human3, toilets);
	rjs.jia.more_recent_bel(guiding(_,_), X);
	.print(X);
	?guiding(A,B);
	.print(guiding(A,B));
	rjs.jia.more_recent_bel(guiding(human1,_), W);
	.print(X);
	?guiding(human1,C);
	.print(guiding(human1,C));
	-guiding(human3, toilets);
	rjs.jia.more_recent_bel(guiding(_,_), Y);
	.print(Y);
	-guiding(human2, burger_king);
	rjs.jia.more_recent_bel(guiding(_,_), Z);
	.print(Z).