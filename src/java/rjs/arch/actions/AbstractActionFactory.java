package rjs.arch.actions;

import java.util.logging.Logger;

import org.ros.internal.message.Message;
import org.ros.message.Duration;
import org.ros.node.topic.Publisher;

import com.github.rosjava_actionlib.ActionClient;

import rjs.arch.agarch.AbstractROSAgArch;
import rjs.ros.AbstractRosNode;
import rjs.utils.Tools;

public abstract class AbstractActionFactory implements ActionFactory {
	
	protected Logger logger = Logger.getLogger(this.getClass().getName());
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
		
		ActionClient<T_ACTION_GOAL, T_ACTION_FEEDBACK, T_ACTION_RESULT> actionClient  
			= new ActionClient<T_ACTION_GOAL, T_ACTION_FEEDBACK, T_ACTION_RESULT>(rosnode.getConnectedNode(), rosnode.getParameters().getString(topic), typeActionGoal,typeActionFeedback,typeActionResult);
		
		Duration serverTimeout = new Duration(20);
        boolean serverStarted;
        
        logger.info("\nWaiting for action server to start...");
        serverStarted = actionClient.waitForActionServerToStart(new Duration(20));
        if (serverStarted) {
        	logger.info("Action server started.\n");
        } else {
        	logger.info("No actionlib server found after waiting for " + serverTimeout.totalNsecs() / 1e9 + " seconds!");
        }
		return actionClient;
	}
	
	


}
