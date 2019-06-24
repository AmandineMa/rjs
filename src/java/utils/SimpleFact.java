package utils;

public class SimpleFact {
	
	String predicate;
	String subject;
	String object;
	
	public SimpleFact(String predicate, String subject, String object) {
		this.predicate = predicate;
		this.subject = subject;
		this.object = object;
	}
	
	
	public SimpleFact() {
		// TODO Auto-generated constructor stub
	}


	public SimpleFact(String predicate, String subject) {
		this.predicate = predicate;
		this.subject = subject;
		this.object = "";
	}


	public String getPredicate() {
		return predicate;
	}
	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}
	public String getObject() {
		return object;
	}
	public void setObject(String object) {
		this.object = object;
	}


	public String getSubject() {
		return subject;
	}


	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	

}
