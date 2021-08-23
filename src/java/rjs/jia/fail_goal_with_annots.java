package rjs.jia;

import java.util.List;

import jason.asSemantics.Event;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;
import jason.stdlib.fail_goal;

public class fail_goal_with_annots extends fail_goal {
	
	List<Term> annots;
	
    @Override public int getMinArgs() {
        return 2;
    }
    @Override public int getMaxArgs() {
        return 2;
    }
	
	@Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        annots = (ListTerm) args[1];
        drop(ts, (Literal)args[0], un);
        return true;
    }

    @Override
    protected void addAnnotsToFailEvent(Event failEvent) {
    	failEvent.getTrigger().getLiteral().addAnnots(annots);
    }

}
