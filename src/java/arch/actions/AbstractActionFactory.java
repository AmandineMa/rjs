package arch.actions;

import org.ros.node.topic.Publisher;

import arch.agarch.AbstractROSAgArch;
import ros.RosNode;
import utils.Tools;

public class AbstractActionFactory {
	
	protected AbstractROSAgArch rosAgArch;
	
	protected RosNode rosnode;

	public AbstractActionFactory(AbstractROSAgArch rosAgArch) {
		this.rosAgArch = rosAgArch;
		rosnode = AbstractROSAgArch.getRosnode();
	}
	
	protected Publisher<std_msgs.String> createPublisher(String topic) {
		String param = rosnode.getParameters().getString(topic);
		Publisher<std_msgs.String> pub = rosAgArch.getConnectedNode().newPublisher(param, std_msgs.String._TYPE);
		Tools.sleep(400);
		return pub;
	}

}
