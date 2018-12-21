// Internal action code for project supervisor

package supervisor;

import jason.*;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class time extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	TimeBB bb = (TimeBB) ts.getAg().getBB();
    	long start_time = bb.getStartTime();
    	long time_now = System.currentTimeMillis() - start_time;
    	return un.unifies(args[0], new ObjectTermImpl(time_now));
    	
    }
}
