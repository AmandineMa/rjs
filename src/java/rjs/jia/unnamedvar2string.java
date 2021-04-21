
package rjs.jia;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.InternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;

/**
 Based on .term2string
*/

@SuppressWarnings("serial")
public class unnamedvar2string extends DefaultInternalAction {

    private static InternalAction singleton = null;
    public static InternalAction create() {
        if (singleton == null)
            singleton = new unnamedvar2string();
        return singleton;
    }

    @Override public int getMinArgs() {
        return 2;
    }
    @Override public int getMaxArgs() {
        return 2;
    }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);

        if (args[0].isVar() && args[1].isVar()) {
            return un.unifies(new StringTermImpl(un.getFirstValue((VarTerm)args[0]).toString()), args[1]);
        }

        throw new JasonException("invalid case of unnamedvar2string");
    }
}

