package rjs.arch.actions;

import org.ros.node.topic.Publisher;

import rjs.arch.agarch.AbstractROSAgArch;
import rjs.ros.AbstractRosNode;
import rjs.utils.Tools;

public abstract class AbstractActionFactory implements ActionFactory {
	
	
	protected AbstractRosNode rosnode;

	public void setRosVariables() {
		rosnode = AbstractROSAgArch.getRosnode();
	}
	
	protected Publisher<std_msgs.String> createPublisher(String topic) {
		String param = rosnode.getParameters().getString(topic);
		Publisher<std_msgs.String> pub = rosnode.getConnectedNode().newPublisher(param, std_msgs.String._TYPE);
		Tools.sleep(400);
		return pub;
	}


}
