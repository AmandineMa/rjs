package utils;

public class SimpleFact {
	
	String predicate;
	String object;
	
	public SimpleFact(String predicate,String object) {
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


//	public String getSubject() {
//		return subject;
//	}
//
//
//	public void setSubject(String subject) {
//		this.subject = subject;
//	}
	
	public boolean equals(Object o) { 
		  
        // If the object is compared with itself then return true   
        if (o == this) { 
            return true; 
        } 
  
        /* Check if o is an instance of SimpleFact or not 
          "null instanceof [type]" also returns false */
        if (!(o instanceof SimpleFact)) { 
            return false; 
        } 
          
        // typecast o to SimpleFact so that we can compare data members  
        SimpleFact sf = (SimpleFact) o; 
          
        // Compare the data members and return accordingly  
        return predicate.equals(sf.predicate)
                && object.equals(sf.object); 
    } 

}
