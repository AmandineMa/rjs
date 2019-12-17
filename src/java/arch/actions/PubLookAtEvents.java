package arch.actions;

import org.ros.node.topic.Publisher;

import arch.ROSAgArch;
import jason.asSemantics.ActionExec;

public class PubLookAtEvents extends AbstractAction {
	
	private static Publisher<std_msgs.String> lookAtEventsPub; 

	public PubLookAtEvents(ActionExec actionExec, ROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		if(lookAtEventsPub == null) {
			lookAtEventsPub = createPublisher("guiding/topics/look_at_events");
		}
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

}
