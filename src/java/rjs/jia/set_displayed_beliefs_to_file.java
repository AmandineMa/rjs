// Internal action code for project supervisor

package rjs.jia;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import jason.runtime.MASConsoleGUI;

public class set_displayed_beliefs_to_file extends DefaultInternalAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	if(MASConsoleGUI.hasConsole())
    		MASConsoleGUI.get().setSaveBeliefsToFile(true);
    	return true;
    	
    }

}
