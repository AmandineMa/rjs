!start.

+!start : true <-
	+guiding(human1, burger_king);
	.wait(500);
	+guiding(human2, burger_king);
	.wait(500);
	+guiding(human1, atm);
	.wait(500);
	+guiding(human3, toilets);
	rjs.jia.more_recent_bel(guiding(_,_), X);
	.print(X);
	-guiding(human3, toilets);
	rjs.jia.more_recent_bel(guiding(_,_), Y);
	.print(Y);
	-guiding(human2, burger_king);
	rjs.jia.more_recent_bel(guiding(_,_), Z);
	.print(Z).