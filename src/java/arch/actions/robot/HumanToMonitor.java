package arch.actions.robot;

import org.ros.node.topic.Publisher;

import arch.actions.AbstractAction;
import arch.agarch.AbstractROSAgArch;
import jason.asSemantics.ActionExec;

public class HumanToMonitor extends AbstractAction {
	
	private Publisher<std_msgs.String> humanToMonitorPub; 

	public HumanToMonitor(ActionExec actionExec, AbstractROSAgArch rosAgArch, Publisher<std_msgs.String> humanToMonitorPub) {
		super(actionExec, rosAgArch);
		this.humanToMonitorPub = humanToMonitorPub;
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

	public void setHumanToMonitorPub(Publisher<std_msgs.String> humanToMonitorPub) {
		this.humanToMonitorPub = humanToMonitorPub;
	}
	

}
