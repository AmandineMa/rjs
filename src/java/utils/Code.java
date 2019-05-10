package utils;

public enum Code{
	 OK(0), ERROR(1);

	 private Code(int code){
	    this.code = code;
	  }

	  private int code;

	  public int getCode(){
	    return this.code;
	  }

	}
