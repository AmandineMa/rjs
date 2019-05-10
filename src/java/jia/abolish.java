// Internal action code for project supervisor

package jia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import agent.TimeBB;
import jason.asSemantics.*;
import jason.asSyntax.*;
import ros.RosNode;

/**
	<p>Internal action: <b><code>supervisor.abolish(b(_),Time)</code></b>.
	<p>Description: removes all the beliefs that match b and that have been added before the given time
	<p>Parameters:<ul>
		<li>+ b(_) (literal) : belief to delete from the BB </li>
		<li>+ Time (number): the time in milliseconds </li>
	</ul>

	@author amdia
*/

@SuppressWarnings("serial")
public class abolish extends DefaultInternalAction {
	
	@SuppressWarnings("unused")
	private Logger logger = Logger.getLogger(RosNode.class.getName());

	@Override public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        List<Literal> toDel = new ArrayList<Literal>();
        TimeBB bb = (TimeBB) ts.getAg().getBB();
        Literal bel = (Literal)args[0];
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
                        	if(time < Integer.parseInt(args[1].toString())) {
                            	toDel.add(inBB);
                        	}
                        }
                    }
                }
            }
            for (Literal l: toDel) {
            	ts.getAg().delBel(l);
            }
     
        }
        return true;
    }

}
