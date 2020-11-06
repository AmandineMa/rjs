package rjs.jia;

import java.util.Iterator;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;

/**
  <p>Internal action: <b><code>.delete if it is exactly equal</code></b>.

  <p>Description: delete elements of strings or lists.

*/
@SuppressWarnings("serial")
public class delete_from_list extends DefaultInternalAction {

    @Override public int getMinArgs() {
        return 3;
    }
    @Override public int getMaxArgs() {
        return 3;
    }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        ListTerm l = (ListTerm)args[1];
        Iterator<Term> it = l.iterator();
        if(args[0].isNumeric()) {
        	 NumberTermImpl s = ((NumberTermImpl) args[0]);
             while(it.hasNext()) {
             	Term t = it.next();
             	if(((NumberTermImpl) t).equals(s)) {
             		l.remove(t);
             	}
             }
        }else if(args[0].isString()) {
	        String s = ((StringTermImpl) args[0]).toString();
	        while(it.hasNext()) {
	        	Term t = it.next();
	        	if(((StringTermImpl) t).toString().equals(s)) {
	        		l.remove(t);
	        	}
	        }
        }
        return un.unifies(args[2], l);
    }
}

