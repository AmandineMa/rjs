package rjs.utils;

public class QoI {

	
	
	public static double logaFormula0To1(double value, double threshold, double parameter) {
		return 1 - Math.exp(-Math.log(2) * Math.pow((value / threshold), parameter));
	}
	
	public static double logaFormulaMinus1To1(double value, double threshold, double parameter) {
		return 1 - 2 * Math.exp(-Math.log(2) * Math.pow((value / threshold), parameter));
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
