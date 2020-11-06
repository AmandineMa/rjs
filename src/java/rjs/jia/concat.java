package rjs.jia;

import java.util.Iterator;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.InternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Term;

/**
  <p>Internal action: <b><code>.concat</code></b>.

  <p>Description: concatenates list of lists in one list

*/

public class concat extends DefaultInternalAction {

    private static InternalAction singleton = null;
    public static InternalAction create() {
        if (singleton == null)
            singleton = new concat();
        return singleton;
    }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {

        if (args[0].isList() & args.length == 2) {
            if (!args[args.length-1].isVar() && !args[args.length-1].isList()) {
                throw new JasonException("Last argument of concat '"+args[args.length-1]+"'is not a list nor a variable.");
            }
            ListTerm result = new ListTermImpl();
            Iterator<Term> list = ((ListTerm)args[0].clone()).iterator();
            
            while(list.hasNext()) {
            	result.concat((ListTerm)list.next());
            }
            return un.unifies(result, args[args.length-1]);


        } else {
        	throw new JasonException("The first argument should be a list");
        }
    }
}
