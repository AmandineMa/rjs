package rjs.jia;

import java.util.Iterator;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Term;

@SuppressWarnings("serial")
public class believes extends DefaultInternalAction {

	@Override public int getMinArgs() {
        return 1;
    }
    @Override public int getMaxArgs() {
        return 1;
    }

    @Override protected void checkArguments(Term[] args) throws JasonException {
        super.checkArguments(args); // check number of arguments
        if (!(args[0] instanceof LogicalFormula))
            throw JasonException.createWrongArgument(this,"first argument must be a formula");
    }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);

        LogicalFormula logExpr = (LogicalFormula)args[0];
        Iterator<Unifier> iu = logExpr.logicalConsequence(ts.getAg(), un);
        if (iu != null && iu.hasNext()) {
        	return true;
        }else {
        	return false;
        }
    }
}
