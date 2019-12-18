package arch.actions.robot;

import org.ros.node.topic.Publisher;

import arch.ROSAgArch;
import arch.actions.AbstractAction;
import jason.asSemantics.ActionExec;

public class PubLookAtEvents extends AbstractAction {
	
	private Publisher<std_msgs.String> lookAtEventsPub; 

	public PubLookAtEvents(ActionExec actionExec, ROSAgArch rosAgArch, Publisher<std_msgs.String> lookAtEventsPub) {
		super(actionExec, rosAgArch);
		this.lookAtEventsPub = lookAtEventsPub;
		setSync(true);
	}

	@Override
	public void execute() {
		String event = removeQuotes(actionExec.getActionTerm().getTerm(0).toString());
		std_msgs.String str = lookAtEventsPub.newMessage();
		str.setData(event);
		lookAtEventsPub.publish(str);
		actionExec.setResult(true);
	}

	public void setLookAtEventsPub(Publisher<std_msgs.String> lookAtEventsPub) {
		this.lookAtEventsPub = lookAtEventsPub;
	}
	
}
