package arch.actions.robot;

import java.util.HashMap;
import java.util.Map;

import arch.actions.AbstractAction;
import arch.agarch.AbstractROSAgArch;
import jason.asSemantics.ActionExec;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import pepper_resources_synchronizer_msgs.MetaStateMachineRegisterResponse;
import ros.RosNodeGuiding;
import utils.Quaternion;

public class Rotate extends AbstractAction {
	
	public Rotate(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}

	@Override
	public void execute() {
		ListTerm quaternion = (ListTerm) actionExec.getActionTerm().getTerm(0);

		Quaternion q = Quaternion.create(quaternion);
		double d = q.getYaw();

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("statemachinepepperbasemanager", ((RosNodeGuiding) rosnode).buildStateMachinePepperBaseManager(actionName, (float) d));
		parameters.put("header", ((RosNodeGuiding) rosnode).buildMetaHeader());

		MetaStateMachineRegisterResponse response = rosnode.callSyncService("pepper_synchro", parameters);

		actionExec.setResult(response != null);
		if(response == null) {
			actionExec.setFailureReason(new Atom("cannot_rotate"), "rotation failed");
		}
	}
}
