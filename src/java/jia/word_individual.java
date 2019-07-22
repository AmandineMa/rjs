// Internal action code for project supervisor

package jia;

import java.util.HashMap;
import java.util.Map;

import arch.ROSAgArch;

//import java.util.logging.Logger;

import jason.asSemantics.*;
import jason.asSyntax.*;
import ontologenius_msgs.OntologeniusServiceResponse;
import utils.Code;

public class word_individual extends DefaultInternalAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
		String action = args[0].toString();
		action = action.replaceAll("^\"|\"$", "");
    	String param = args[1].toString();
		param = param.replaceAll("^\"|\"$", "");
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("action", action);
		parameters.put("param", param);
		OntologeniusServiceResponse places = ROSAgArch.getM_rosnode().callSyncService("get_individual_info", parameters);
		
		if(places.getCode() == Code.OK.getCode() & !places.getValues().isEmpty()) {
			if(places.getValues().size() == 1) {
				return un.unifies(args[2], new StringTermImpl(places.getValues().get(0)));
			}else {
				ListTermImpl places_list = new ListTermImpl();
				for(String place : places.getValues()) {
					places_list.add(new StringTermImpl(place));
				}
	        	return un.unifies(args[2], places_list);
			}
        }else {
        	return false;
        }
    }
    
    void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException ex) {
		}
	}

}
