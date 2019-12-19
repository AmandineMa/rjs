// Internal action code for project supervisor

package jia;

import arch.agarch.AbstractROSAgArch;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ObjectTermImpl;
import jason.asSyntax.Term;

/**
	<p>Internal action: <b><code>supervisor.time(Time)</code></b>.
  	<p>Description: gets the current time of the supervisor in milliseconds
  	<p>Parameters:<ul>
  		<li>+ Time (number): the time in milliseconds </li>
  	</ul>

	@author amdia
*/

@SuppressWarnings("serial")
public class time extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	double time_now = ((AbstractROSAgArch) ts.getUserAgArch()).getRosTimeSeconds();
    	return un.unifies(args[0], new ObjectTermImpl(time_now));
    	
    }
}
