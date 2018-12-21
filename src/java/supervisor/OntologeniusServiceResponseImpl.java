package supervisor;

import java.util.ArrayList;
import java.util.List;

import org.ros.internal.message.RawMessage;

import ontologenius_msgs.OntologeniusServiceResponse;

public class OntologeniusServiceResponseImpl implements OntologeniusServiceResponse {
	
	private List<String> values;
	private short code;
	

	public OntologeniusServiceResponseImpl() {
		// TODO Auto-generated constructor stub
	}

	public RawMessage toRawMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	public short getCode() {
		return code;
	}

	public List<String> getValues() {
		return values;
	}

	public void setCode(short arg0) {
		code = arg0;
	}

	public void setValues(List<String> arg0) {
		values = new ArrayList<String>(arg0);
	}

}
