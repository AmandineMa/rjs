// Internal action code for project supervisor

package rjs.jia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;
import jason.bb.BeliefBase;
import rjs.ros.AbstractRosNode;

/**
	<p>Internal action: <b><code>rjs.abolish_all_except_ground</code></b>.
	<p>Description: removes all the beliefs except the ones with the annotation "ground"

	@author amdia
*/

@SuppressWarnings("serial")
public class abolish_all_except_ground extends DefaultInternalAction {
	
	@Override public int getMinArgs() {
        return 0;
    }
    @Override public int getMaxArgs() {
        return 0;
    }

	@SuppressWarnings("unused")
	private Logger logger = Logger.getLogger(AbstractRosNode.class.getName());

	@Override public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        List<Literal> toDel = new ArrayList<Literal>();
        BeliefBase bb = ts.getAg().getBB();
        synchronized (bb.getLock()) {
            Iterator<Literal> il = bb.iterator();
            if (il != null) {
                while (il.hasNext()) {
                    Literal inBB = il.next();
                    if (!inBB.isRule()) {
                    	ListTerm l = inBB.getAnnots("ground");
                    	if(l.isEmpty()) {
                        	toDel.add(inBB);
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
