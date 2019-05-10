// Internal action code for project supervisor

package jia;

import agent.TimeBB;
import jason.asSemantics.*;
import jason.asSyntax.*;

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
    	TimeBB bb = (TimeBB) ts.getAg().getBB();
    	long start_time = bb.getStartTime();
    	long time_now = System.currentTimeMillis() - start_time;
    	return un.unifies(args[0], new ObjectTermImpl(time_now));
    	
    }
}
