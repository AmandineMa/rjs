package rjs.utils;

import jason.asSyntax.Literal;

public class Predicate {
	
	public String property;
	public String subject;
	public String object;
	
	public Predicate(String property, String subject, String object) {
		this.property = property;
		this.subject = subject;
		this.object = object;
	}
	
	public Predicate(Literal predicate) {
			property = predicate.getFunctor();
    		subject = predicate.getTerm(0) == null ? "bool#true" : Tools.removeQuotes(predicate.getTerm(0).toString());
    		object = predicate.getTerm(1) == null ? "bool#true" : Tools.removeQuotes(predicate.getTerm(1).toString());
	}
	
	public Predicate(String property) {
		this.property = property;
		this.subject = "bool#true";
		this.object = "bool#true";
	}
	
	public Predicate(String property, String subject) {
		this.property = property;
		this.subject = subject;
		this.object = "bool#true";
	}

	@Override
	public String toString() {
		return property+"("+subject+","+object+")";
	}
	

//	public String getProperty() {
//		return property;
//	}
//	public void setProperty(String property) {
//		this.property = property;
//	}
//	
//	public String getSubject() {
//		return subject;
//	}
//
//	public void setSubject(String subject) {
//		this.subject = subject;
//	}
//
//	public String getObject() {
//		return object;
//	}
//	public void setObject(String object) {
//		this.object = object;
//	}
	
}
