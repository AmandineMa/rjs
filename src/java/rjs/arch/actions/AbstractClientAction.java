package rjs.arch.actions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.ros.internal.message.Message;

import com.github.rosjava_actionlib.ActionClientListener;

import actionlib_msgs.GoalID;
import actionlib_msgs.GoalStatus;
import actionlib_msgs.GoalStatusArray;
import jason.asSemantics.ActionExec;
import rjs.arch.actions.ros.RjsActionClient;
import rjs.arch.agarch.AbstractROSAgArch;
import rjs.utils.Tools;

public abstract class AbstractClientAction<T_ACTION_GOAL extends Message, T_ACTION_FEEDBACK extends Message, T_ACTION_RESULT extends Message> extends AbstractAction implements ActionClientListener<T_ACTION_FEEDBACK, T_ACTION_RESULT> {

	protected RjsActionClient<T_ACTION_GOAL, T_ACTION_FEEDBACK, T_ACTION_RESULT> actionClient;
	private GoalID goalID;
	
	public AbstractClientAction(ActionExec actionExec, AbstractROSAgArch rosAgArch, RjsActionClient<T_ACTION_GOAL, T_ACTION_FEEDBACK, T_ACTION_RESULT> actionClient) {
		super(actionExec, rosAgArch);
		this.actionClient = actionClient;
	}

	@Override
	public void execute() {
		actionClient.addListener(this);
		boolean serverStarted = actionClient.checkServerConnected(1);
		if(serverStarted) {
			sendGoal(computeGoal());
		}else {
			actionExec.setResult(false);
		}

	}
	
	public abstract T_ACTION_GOAL computeGoal();
	
	public void sendGoal(T_ACTION_GOAL actionGoal) {
		goalID = actionClient.sendGoal(actionGoal);
	}


	@Override
	public void resultReceived(T_ACTION_RESULT result) {
		Method getStatusMethod;
		boolean resultSuccess = false;
		try {
			getStatusMethod = result.getClass().getMethod("getStatus");
			getStatusMethod.setAccessible(true);
			GoalStatus goalStatus = (GoalStatus) getStatusMethod.invoke(result);
			if(goalStatus.getGoalId().equals(goalID)) {
				if(goalStatus.getStatus()==actionlib_msgs.GoalStatus.SUCCEEDED) {
					resultSuccess = true;
					setResultSucceeded(result);
				}else {
					String fieldName = RjsActionClient.getGoalStatus(goalStatus);
					
					if(fieldName.isEmpty()) {
						logger.info("result with goal "+goalStatus.getGoalId().getId()+" not achieved: "+goalStatus.getStatus());
					} else {
						logger.info("result with goal "+goalStatus.getGoalId().getId()+" not achieved: "+fieldName);
					}
				}
				actionClient.removeListener(this);
				actionExec.setResult(resultSuccess);
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			Tools.getStackTrace(e);
			actionClient.removeListener(this);
			actionExec.setResult(resultSuccess);
		}
		
	}

	
	public abstract void setResultSucceeded(T_ACTION_RESULT result);

}
