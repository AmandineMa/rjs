package utils;

import java.util.Iterator;

import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;

public class QoI {

	
	
	public static double logaFormula(double value, double threshold, double parameter) {
		return 1 - Math.exp(-Math.log(2) * Math.pow((value / threshold), parameter));
	}
	
	public static double normaFormulaMinus1To1(double value, double min, double max) {
		return 2 * (value - min) / (max - min) - 1;
	}
	
	public static double normaFormul0To1(double value, double min, double max) {
		return (value - min) / (max - min);
	}
	
	public static double normaFormulaMinus1To0(double value, double min, double max) {
		return (value - max) / (max - min);
	}

}
