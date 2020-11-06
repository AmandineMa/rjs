package rjs.arch.actions;

import java.util.logging.Level;

import org.ros.internal.message.Message;
import org.ros.node.topic.Publisher;

import com.github.rosjava_actionlib.ActionClient;

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
		Tools.sleep(100);
		return pub;
	}
	
	protected <T_ACTION_GOAL extends Message, T_ACTION_FEEDBACK extends Message, T_ACTION_RESULT extends Message> 
			ActionClient<T_ACTION_GOAL,T_ACTION_FEEDBACK,T_ACTION_RESULT> 
			createActionClient(String topic, String typeActionGoal, String typeActionFeedback, String typeActionResult) {
		
		ActionClient<T_ACTION_GOAL, T_ACTION_FEEDBACK, T_ACTION_RESULT> actionClientDialogue  
			= new ActionClient<T_ACTION_GOAL, T_ACTION_FEEDBACK, T_ACTION_RESULT>(rosnode.getConnectedNode(), rosnode.getParameters().getString(topic), typeActionGoal,typeActionFeedback,typeActionResult);
		actionClientDialogue.setLogLevel(Level.SEVERE);
		Tools.sleep(100);
		return actionClientDialogue;
	}
	
	


}
