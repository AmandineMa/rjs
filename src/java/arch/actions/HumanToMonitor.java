package arch.actions;

import org.ros.node.topic.Publisher;

import arch.ROSAgArch;
import jason.asSemantics.ActionExec;
import ros.RosNode;

public class HumanToMonitor extends AbstractAction {
	
	private static Publisher<std_msgs.String> humanToMonitorPub; 

	public HumanToMonitor(ActionExec actionExec, ROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		if(humanToMonitorPub == null) {
			humanToMonitorPub = createPublisher("guiding/topics/human_to_monitor");
		}
	}

	@Override
	public void execute() {
		String param = actionExec.getActionTerm().getTerm(0).toString();
		param = removeQuotes(param);
		std_msgs.String str = humanToMonitorPub.newMessage();
		str.setData(param);
		humanToMonitorPub.publish(str);
		actionExec.setResult(true);
	}

}
