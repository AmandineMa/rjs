package rjs.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ros.message.Time;

import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.SetTerm;
import jason.asSyntax.SetTermImpl;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;
import jason.asSyntax.UnnamedVar;

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

	public static String arrayToStringArray(List<String> array) {
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

	public static String arrayToStringArray(int[] array) {
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
	
	public static ListTerm arrayToListTerm(List<String> array) {
		ListTerm list = new ListTermImpl();
		for(String str : array) {
			list.add(new StringTermImpl(str));
		}
		return list;
	}
	
	public static SetTerm arrayToSetTerm(List<String> array) {
		SetTerm list = new SetTermImpl();
		for(String str : array) {
			list.add(new Atom(str));
		}
		return list;
	}
	
	public static ArrayList<Double> listTermNumbers_to_list(ListTermImpl lti){
		ArrayList<Double> values = new ArrayList<>();
		Iterator<Term> values_it =  lti.iterator();
		while(values_it.hasNext()) {
			values.add(((NumberTermImpl)values_it.next()).solve());
		}
		return values;
	}
	
	public static List<String> listTermStringTolist(ListTermImpl lti){
		ArrayList<String> values = new ArrayList<>();
		Iterator<Term> values_it =  lti.iterator();
		while(values_it.hasNext()) {
			values.add((values_it.next()).toString());
		}
		return values;
	}
	
	public static Literal stringFunctorAndTermsToBelLiteral(String functor, List<Object> terms) {
		String bel = functor + "(";
		Iterator<Object> it = terms.iterator();
		Object term;
		boolean first = true;
		while(it.hasNext()) {
			
			term = it.next();
			if(term.equals("_"))
				term = new UnnamedVar();
			else if(term instanceof String && !((String)term).startsWith("["))
				term = new StringTermImpl((String)term);
			
			if(first)
				bel = bel + term;
			else
				bel = bel + "," + term;
			first = false;
		}
		bel = bel + ")";
		return Literal.parseLiteral(bel);
	}
	
	public static void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException ex) {
		}
	}
	
	public static String removeQuotes(String string) {
		return string.replaceAll("^\"|\"$", "");
	}
	
	public static ArrayList<String> removeQuotes(List<Term> terms) {
		ArrayList<String> params = new ArrayList<String>();
		for (Term term : terms) {
			params.add(term.toString().replaceAll("^\"|\"$", ""));
		}
		return params;
	}
	
	public static Time rosTimeFromMSec(Double ms) {
		int nsec = (int) (ms % 1000) * 1000000;
		int sec = (int) (ms / 1000);
		return new Time(sec,nsec);
	}

}
