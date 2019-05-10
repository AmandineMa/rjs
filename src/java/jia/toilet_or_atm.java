// Internal action code for project supervisor

package jia;

//import java.util.logging.Logger;

import jason.asSemantics.*;
import jason.asSyntax.*;

public class toilet_or_atm extends DefaultInternalAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
//	private Logger logger = Logger.getLogger(toilet_or_atm.class.getName());
	
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	String param = args[0].toString();
		param = param.replaceAll("^\"|\"$", "");
        if(param.equals("atm")) {
        	return un.unifies(args[1], new StringTermImpl("atm"));
        }else if(param.equals("toilets")) {
        	return un.unifies(args[1], new StringTermImpl("toilets"));
        }
        else {
        	return false;
        }
    }
}
