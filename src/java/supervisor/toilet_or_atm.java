// Internal action code for project supervisor

package supervisor;

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
        if(args[0].toString().equals("atm")) {
        	return un.unifies(args[1], new ObjectTermImpl("atm"));
        }else if(args[0].toString().equals("toilets")) {
        	return un.unifies(args[1], new ObjectTermImpl("toilets"));
        }
        else {
        	return false;
        }
    }
}
