package rjs.utils;

public class SimpleFact {
	
	String predicate;
	String object;
	
	public SimpleFact(String predicate, String object) {
		this.predicate = predicate;
		this.object = object;
	}
	
	
	public SimpleFact() {
		// TODO Auto-generated constructor stub
	}


	public SimpleFact(String predicate) {
		this.predicate = predicate;
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
	
	

}
