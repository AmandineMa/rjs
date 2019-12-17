package arch.actions.robot;

import org.ros.node.topic.Publisher;

import arch.ROSAgArch;
import arch.actions.AbstractAction;
import jason.asSemantics.ActionExec;

public class HumanToMonitor extends AbstractAction {
	
	private static Publisher<std_msgs.String> humanToMonitorPub; 

	public HumanToMonitor(ActionExec actionExec, ROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		if(humanToMonitorPub == null) {
			humanToMonitorPub = createPublisher("guiding/topics/human_to_monitor");
		}
		setSync(true);
	}

	@Override
	public void execute() {
		String param = removeQuotes(actionExec.getActionTerm().getTerm(0).toString());
		std_msgs.String str = humanToMonitorPub.newMessage();
		str.setData(param);
		humanToMonitorPub.publish(str);
		actionExec.setResult(true);
	}

}
