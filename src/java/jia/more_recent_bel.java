// Internal action code for project supervisor

package jia;

import java.util.Iterator;
import java.util.logging.Logger;

import agent.TimeBB;
import jason.asSemantics.*;
import jason.asSyntax.*;
import ros.RosNode;

@SuppressWarnings("serial")
public class more_recent_bel extends DefaultInternalAction {
	
	@SuppressWarnings("unused")
	private Logger logger = Logger.getLogger(RosNode.class.getName());

	@Override public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        TimeBB bb = (TimeBB) ts.getAg().getBB();
        Literal bel = (Literal)args[0];
        Literal more_recent_bel = null;
    	int more_recent_time = 0; 
        synchronized (bb.getLock()) {
            Iterator<Literal> il = bb.getCandidateBeliefs(bel, un);
            if (il != null) {
            	
                while (il.hasNext()) {
                    Literal inBB = il.next();   
                    if (!inBB.isRule()) {
                        // need to clone unifier since it is changed in previous iteration
                        if (un.clone().unifiesNoUndo(bel, inBB)) {
                        	Literal l = (Literal) inBB.getAnnots("add_time").get(0);
                        	int time = (int) ((NumberTermImpl)l.getTerm(0)).solve();
                        	if(time > more_recent_time) {
                            	more_recent_time = time;
                            	more_recent_bel = inBB;
                        	}
                        }
                    }
                }
            }
        }
        if(more_recent_bel != null) {
        	return un.unifies(args[1], more_recent_bel);
        }
        return true;
    }
}
