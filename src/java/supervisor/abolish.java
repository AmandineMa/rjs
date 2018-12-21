// Internal action code for project supervisor

package supervisor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import jason.*;
import jason.asSemantics.*;
import jason.asSyntax.*;
import jason.bb.BeliefBase;


public class abolish extends DefaultInternalAction {
	
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
                        	int time = Integer.parseInt(inBB.getAnnot("add_time").getTerm(0).toString());
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
