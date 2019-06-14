// Internal action code for project supervisor

package jia;

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
		ROSAgArch.getM_rosnode().call_onto_individual_srv(action, param);
		
		OntologeniusServiceResponse places;
		do {
			places = ROSAgArch.getM_rosnode().get_onto_individual_resp();
			sleep(100);
		}while(places == null);
		
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
