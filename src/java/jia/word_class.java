// Internal action code for project supervisor

package jia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import arch.ROSAgArch;

//import java.util.logging.Logger;

import jason.asSemantics.*;
import jason.asSyntax.*;
import ontologenius_msgs.OntologeniusService;
import ontologenius_msgs.OntologeniusServiceResponse;
import utils.Code;

public class word_class extends DefaultInternalAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	String action = args[0].toString();
    	String param = args[1].toString();
		param = param.replaceAll("^\"|\"$", "");
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("action", action);
		parameters.put("param", param);
		OntologeniusServiceResponse onto_class_resp = ROSAgArch.getM_rosnode().callSyncService("get_class_info", parameters);
		
		boolean result = false;
		if(onto_class_resp != null) {
			OntologeniusServiceResponse places = ROSAgArch.getM_rosnode().newServiceResponseFromType(OntologeniusService._TYPE);
			if(((OntologeniusServiceResponse) onto_class_resp).getValues().isEmpty()) {
				places.setCode((short) Code.ERROR.getCode());
			}else {
				places.setCode((short) Code.OK.getCode());
				places.setValues(((OntologeniusServiceResponse) onto_class_resp).getValues());
			}
			
			if(places.getCode() == Code.OK.getCode() & !places.getValues().isEmpty()) {
				result = un.unifies(args[2], new StringTermImpl(places.getValues().get(0)));
	        }
		}
		return result;
    }
}
