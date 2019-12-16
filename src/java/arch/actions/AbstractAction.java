package arch.actions;

import java.util.logging.Logger;

import org.ros.exception.RosRuntimeException;
import org.ros.node.topic.Publisher;

import arch.ROSAgArch;
import jason.asSemantics.ActionExec;
import jason.asSyntax.Atom;
import ros.RosNode;
import utils.Tools;

public abstract class AbstractAction implements Action {
	
	protected Logger logger = Logger.getLogger(this.getClass().getName());
	protected boolean async = false;
	protected ActionExec actionExec;
	protected ROSAgArch rosAgArch;
	
	protected RosNode rosnode = ROSAgArch.getM_rosnode();
	protected String actionName;

	public AbstractAction(ActionExec actionExec, ROSAgArch rosAgArch) {
		this.rosAgArch = rosAgArch;
		this.actionExec = actionExec;
		actionName = actionExec.getActionTerm().getFunctor();
	}
	
	protected Publisher<std_msgs.String> createPublisher(String topic) {
		String param = rosnode.getParameters().getString(topic);
		return rosAgArch.getConnectedNode().newPublisher(param, std_msgs.String._TYPE);
	}
	
	protected String removeQuotes(String string) {
		return string.replaceAll("^\"|\"$", "");
	}
	
	public void handleFailure(ActionExec action, String srv_name, RuntimeException e) {
		RosRuntimeException RRE = new RosRuntimeException(e);
		logger.info(Tools.getStackTrace(RRE));

		action.setResult(false);
		action.setFailureReason(new Atom(srv_name+ "_ros_failure"), srv_name+" service failed");
		rosAgArch.actionExecuted(action);
	}
	
	public boolean isAsync() {
		return async;
	}
	
	protected void setAsync(boolean as) {
		async = as;
	}

}
