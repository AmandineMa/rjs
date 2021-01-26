package rjs.arch.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.ros.exception.RosRuntimeException;

import jason.asSemantics.ActionExec;
import jason.asSyntax.Atom;
import jason.asSyntax.Term;
import rjs.arch.agarch.AbstractROSAgArch;
import rjs.ros.AbstractRosNode;
import rjs.utils.Tools;

public abstract class AbstractAction implements Action {
	
	protected Logger logger = Logger.getLogger(this.getClass().getName());
	protected boolean sync = false;
	protected ActionExec actionExec;
	protected AbstractROSAgArch rosAgArch;
	
	protected String actionName;
	protected List<Term> actionTerms;

	public AbstractAction(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		this.rosAgArch = rosAgArch;
		this.actionExec = actionExec;
		actionName = actionExec.getActionTerm().getFunctor();
		actionTerms = actionExec.getActionTerm().getTerms();
	}
	
//	protected List<Term> getActionTerms() {
//		return actionExec.getActionTerm().getTerms();
//	}

	
	public void handleFailure(ActionExec action, String srv_name, RuntimeException e) {
		RosRuntimeException RRE = new RosRuntimeException(e);
		logger.info(Tools.getStackTrace(RRE));

		action.setResult(false);
		action.setFailureReason(new Atom(srv_name+ "_ros_failure"), srv_name+" service failed");
		rosAgArch.actionExecuted(action);
	}
	
	public boolean isSync() {
		return sync;
	}
	
	protected void setSync(boolean as) {
		sync = as;
	}
	
	protected void setActionExecuted(boolean res) {
		actionExec.setResult(res);
		rosAgArch.actionExecuted(actionExec);
	}
	
	protected AbstractRosNode getRosNode() {
		return AbstractROSAgArch.getRosnode();
	}

}
