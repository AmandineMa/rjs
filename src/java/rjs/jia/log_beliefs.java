// Internal action code for project supervisor

package rjs.jia;

import java.util.logging.Logger;

//import java.util.logging.Logger;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import jason.runtime.MASConsoleGUI;

public class log_beliefs extends DefaultInternalAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Logger logger = Logger.getLogger(log_beliefs.class.getName());
	
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	
    	logger.info("Setting "+ts.getAgArch().getAgName()+" for Beliefs");
    	if(MASConsoleGUI.hasConsole())
    		MASConsoleGUI.get().addBeliefAgent(ts.getAg());
    	return true;
    	
    }

}
