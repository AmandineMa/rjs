package rjs.arch.actions.ros;

import java.util.HashMap;
import java.util.Map.Entry;

import jason.asSemantics.ActionExec;
import jason.asSyntax.Atom;
import rjs.arch.actions.AbstractAction;
import rjs.arch.agarch.AbstractROSAgArch;

public class InitServices extends AbstractAction {

	public InitServices(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}

	@Override
	public void execute() {
		HashMap<String, Boolean> services_status = rosnode.initServiceClients();
		actionExec.setResult(true);
		for(Entry<String, Boolean> entry : services_status.entrySet()) {
			if(entry.getValue()) {
				rosAgArch.addBelief("connected_srv("+entry.getKey()+")");
			}else {
				rosAgArch.addBelief("~connected_srv("+entry.getKey()+")");
				actionExec.setResult(false);
				actionExec.setFailureReason(new Atom("srv_not_connected"), "Some services are not connected");
			}
		}

	}

}
