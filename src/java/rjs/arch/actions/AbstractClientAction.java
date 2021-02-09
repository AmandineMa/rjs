package rjs.arch.actions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.ros.internal.message.Message;

import com.github.rosjava_actionlib.ActionClientListener;

import actionlib_msgs.GoalID;
import actionlib_msgs.GoalStatus;
import jason.asSemantics.ActionExec;
import jason.asSyntax.Literal;
import rjs.arch.actions.ros.RjsActionClient;
import rjs.arch.agarch.AbstractROSAgArch;
import rjs.utils.Tools;

public abstract class AbstractClientAction<T_ACTION_GOAL extends Message, T_ACTION_FEEDBACK extends Message, T_ACTION_RESULT extends Message> extends AbstractAction implements ActionClientListener<T_ACTION_FEEDBACK, T_ACTION_RESULT> {

	protected RjsActionClient<T_ACTION_GOAL, T_ACTION_FEEDBACK, T_ACTION_RESULT> rjsActionClient;
	private GoalID goalID;
	
	public AbstractClientAction(ActionExec actionExec, AbstractROSAgArch rosAgArch, RjsActionClient<T_ACTION_GOAL, T_ACTION_FEEDBACK, T_ACTION_RESULT> actionClient) {
		super(actionExec, rosAgArch);
		this.rjsActionClient = actionClient;
	}

	@Override
	public void execute() {
		rjsActionClient.addListener(this);
		boolean serverStarted = rjsActionClient.checkServerConnected(10);
		if(serverStarted) {
			sendGoal(computeGoal());
		}else {
			setJasonActionResult(false);
		}

	}
	
	public abstract T_ACTION_GOAL computeGoal();
	
	public void sendGoal(T_ACTION_GOAL actionGoal) {
		goalID = rjsActionClient.sendGoal(actionGoal);
		addGoalStatusInBB();
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
					setResultAborted(result);
					if(fieldName.isEmpty()) {
						logger.info("result with goal "+goalStatus.getGoalId().getId()+" not achieved: "+goalStatus.getStatus());
					} else {
						logger.info("result with goal "+goalStatus.getGoalId().getId()+" not achieved: "+fieldName);
					}
				}
				removeGoalStatusInBB();
				rjsActionClient.removeListener(this);
				setJasonActionResult(resultSuccess);
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			Tools.getStackTrace(e);
			rjsActionClient.removeListener(this);
			setJasonActionResult(resultSuccess);
		}
		
	}
	
	public T_ACTION_GOAL newGoalMessage() {
		return rjsActionClient.newGoalMessage();
	}

	
	protected abstract void setResultSucceeded(T_ACTION_RESULT result);
	
	protected abstract void setResultAborted(T_ACTION_RESULT result);
	
	protected void addGoalStatusInBB() {
		if(actionTerms != null)
			rosAgArch.addBelief(actionName, Arrays.asList("actionStarted",actionTerms));
		else
			rosAgArch.addBelief(actionName, Arrays.asList("actionStarted"));
	}
	
	protected void removeGoalStatusInBB() {
		rosAgArch.removeBelief(actionName, Arrays.asList("actionFeedback",Literal.parseLiteral("_")));
		if(actionTerms != null) {
			rosAgArch.removeBelief(actionName, Arrays.asList("actionStarted",actionTerms));
			rosAgArch.addBelief(actionName, Arrays.asList("actionOver",actionTerms));
		}else {
			rosAgArch.removeBelief(actionName, Arrays.asList("actionStarted"));
			rosAgArch.addBelief(actionName, Arrays.asList("actionOver"));
		}
	}
	
	private void setJasonActionResult(boolean resultSuccess) {
		actionExec.setResult(resultSuccess);
		rosAgArch.actionExecuted(actionExec);
	}

}
