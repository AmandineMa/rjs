package rjs.arch.actions.ros;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.ros.internal.message.Message;
import org.ros.message.Duration;
import org.ros.node.ConnectedNode;

import com.github.rosjava_actionlib.ActionClient;
import com.github.rosjava_actionlib.ActionClientListener;

import actionlib_msgs.GoalID;
import actionlib_msgs.GoalStatus;
import rjs.utils.Tools;

public class RjsActionClient<T_ACTION_GOAL extends Message, T_ACTION_FEEDBACK extends Message, T_ACTION_RESULT extends Message> {

	protected Logger logger = Logger.getLogger(this.getClass().getName());	
	private ActionClient<T_ACTION_GOAL, T_ACTION_FEEDBACK, T_ACTION_RESULT> actionClient;

	public RjsActionClient(ConnectedNode connectedNode, String topic, String typeActionGoal, String typeActionFeedback, String typeActionResult) {
		actionClient  
		= new ActionClient<T_ACTION_GOAL, T_ACTION_FEEDBACK, T_ACTION_RESULT>(connectedNode, topic, typeActionGoal,typeActionFeedback,typeActionResult);
	}

	public boolean checkServerConnected(double seconds) {
		Duration serverTimeout = new Duration(seconds);
		boolean serverStarted = actionClient.waitForActionServerToStart(serverTimeout);
		if (serverStarted) {
			logger.info("Dialogue server started.\n");
		} else {
			logger.severe("No dialogue server found for "+ serverTimeout.totalNsecs() / 1e9 + " seconds!");
		}
		return serverStarted;
	}

	public T_ACTION_GOAL newGoalMessage() {
		return actionClient.newGoalMessage();
	} 
	

	public GoalID sendGoal(T_ACTION_GOAL actionGoal) {
		logger.info("Sending goal");
		actionClient.sendGoal(actionGoal);
		
		Method gidMethod;
		try {
			gidMethod = actionGoal.getClass().getMethod("getGoalId");
			gidMethod.setAccessible(true);
			GoalID gid = (GoalID) gidMethod.invoke(actionGoal);
			logger.info("Sent goal with ID: " + gid.getId());
			return gid;
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			Tools.getStackTrace(e);
		}
		return null;
	}
	
	public void addListener(ActionClientListener<T_ACTION_FEEDBACK, T_ACTION_RESULT> actionListener) {
		actionClient.addListener(actionListener);
	}
	
	public void removeListener(ActionClientListener<T_ACTION_FEEDBACK, T_ACTION_RESULT> actionListener) {
		actionClient.removeListener(actionListener);
	}
	
	public static String getGoalStatus(GoalStatus goalStatus) {
		String fieldName = "";
		for(Field f : goalStatus.getClass().getFields()) {
			try {
				if(f.get(goalStatus) instanceof java.lang.Byte && (byte)f.get(goalStatus) == goalStatus.getStatus()) {
					fieldName = f.getName();
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return fieldName;
	}

}
