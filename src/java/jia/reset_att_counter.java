// Internal action code for project supervisor

package jia;

import agent.LimitedAgent;
import jason.*;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class reset_att_counter extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	if(args.length == 1)
    		((LimitedAgent) ts.getAg()).removeCounter(args[0].toString());
    	else if(args.length == 0)
    		((LimitedAgent) ts.getAg()).removeCounters();
    	else
    		return false;
    	return true;
    }
}
