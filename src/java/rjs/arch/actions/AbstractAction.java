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
	
	protected static AbstractRosNode rosnode;
	protected String actionName;

	public AbstractAction(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		this.rosAgArch = rosAgArch;
		this.actionExec = actionExec;
		actionName = actionExec.getActionTerm().getFunctor();
	}
	
	protected String removeQuotes(String string) {
		return string.replaceAll("^\"|\"$", "");
	}
	
	protected ArrayList<String> removeQuotes(List<Term> terms) {
		ArrayList<String> params = new ArrayList<String>();
		for (Term term : terms) {
			params.add(term.toString().replaceAll("^\"|\"$", ""));
		}
		return params;
	}

	
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

}
