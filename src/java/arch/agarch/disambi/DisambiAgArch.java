package arch.agarch.disambi;

import arch.actions.Action;
import arch.actions.ActionFactoryDisambi;
import arch.agarch.AbstractROSAgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Message;
import jason.asSyntax.Atom;
import utils.Tools;

public class DisambiAgArch extends AbstractROSAgArch {

	public DisambiAgArch() {
		super();
	}
	
	public class AgRunnable implements Runnable {
		
		private ActionExec action;
		private DisambiAgArch rosAgArch;
		
		public AgRunnable(DisambiAgArch rosAgArch, ActionExec action) {
			this.action = action;
			this.rosAgArch = rosAgArch;
		}
		
		@Override
		public void run() {
			if(actionFactory == null && rosnode != null)
				actionFactory = new ActionFactoryDisambi(DisambiAgArch.this);
			String action_name = action.getActionTerm().getFunctor();
			Message msg = new Message("tell", getAgName(), "supervisor", "action_started(" + action_name + ")");
			try {
				sendMsg(msg);
			} catch (Exception e) {
				Tools.getStackTrace(e);
			}
			Action actionExecutable = ActionFactoryDisambi.createAction(action, rosAgArch);
			if(action != null) {
				actionExecutable.execute();
				if(actionExecutable.isSync())
					actionExecuted(action);
			} else {
				action.setResult(false);
				action.setFailureReason(new Atom("act_not_found"), "no action " + action_name + " is implemented");
				actionExecuted(action);
			}
		}
	}
	
	@Override
	public void act(final ActionExec action) {
		executor.execute(new AgRunnable(this, action));
	}
	
	

}
