// Internal action code for project supervisor

package jia;

import java.util.List;

import arch.ROSAgArch;

//import java.util.logging.Logger;

import jason.asSemantics.*;
import jason.asSyntax.*;
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
		ROSAgArch.getM_rosnode().call_onto_class_srv(action, param);
		
		OntologeniusServiceResponse places;
		do {
			places = ROSAgArch.getM_rosnode().getOnto_class_resp();
			sleep(100);
		}while(places == null);
		short c = places.getCode() ;
		List<String> l = places.getValues();
		if(places.getCode() == Code.OK.getCode() & !places.getValues().isEmpty()) {
        	return un.unifies(args[2], new StringTermImpl(places.getValues().get(0)));
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
