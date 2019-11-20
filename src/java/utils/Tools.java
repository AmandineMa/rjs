package utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;

public class Tools {

	public static String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}
	
	public static boolean isLeft(geometry_msgs.Vector3 A, geometry_msgs.Vector3 B, geometry_msgs.Vector3 C) {
		return ((B.getX()- A.getX())*(C.getY()- A.getY())-(B.getY() - A.getY())*(C.getX()-A.getX()))>0;
	}

	public static String array_2_str_array(List<String> array) {
		String str_array = new String();
		for(String str : array) {
			if(str_array.isEmpty()) {
				str_array = str;
			}else {
				str_array = str_array+","+str;
			}
		}
		str_array = "["+str_array+"]";
		return str_array;
	}

	public static String array_2_str_array(int[] array) {
		String str_array = new String();
		for(int i : array) {
			if(str_array.isEmpty()) {
				str_array = String.valueOf(i);
			}else {
				str_array = str_array+","+String.valueOf(i);
			}
		}
		str_array = "["+str_array+"]";
		return str_array;
	}
	
	public static ArrayList<Double> listTermNumbers_to_list(ListTermImpl lti){
		ArrayList<Double> values = new ArrayList<>();
		Iterator<Term> values_it =  lti.iterator();
		while(values_it.hasNext()) {
			values.add(((NumberTermImpl)values_it.next()).solve());
		}
		return values;
	}
	
	public static void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException ex) {
		}
	}

}
