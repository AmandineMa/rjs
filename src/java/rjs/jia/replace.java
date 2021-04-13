// Internal action code for project supervisor

package rjs.jia;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Term;

public class replace extends DefaultInternalAction {

    /**
	 Set an element in a numbered list.
	 jia.replace(index, list, element, new list)
	
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	// TODO check args types and implement for other element than double
    	 ListTerm result = (ListTerm)args[1].clone();
    	 int index = (int)((NumberTerm)args[0]).solve();
    	 NumberTerm element = (NumberTerm)args[2];
    	 result.set(index, element);
        return un.unifies(result, args[3]);
    }
}
