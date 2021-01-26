package rjs.arch.actions.ros;

import java.util.HashMap;
import java.util.Map.Entry;

import jason.asSemantics.ActionExec;
import jason.asSyntax.Atom;
import rjs.arch.actions.AbstractAction;
import rjs.arch.agarch.AbstractROSAgArch;

public class InitSub extends AbstractAction {

	public InitSub(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}

	@Override
	public void execute() {
		HashMap<String, Boolean> subStatus = getRosNode().createSubscribers();
		actionExec.setResult(true);
		for(Entry<String, Boolean> entry : subStatus.entrySet()) {
			if(entry.getValue()) {
				rosAgArch.addBelief("connectedTopic("+entry.getKey()+")");
				rosAgArch.removeBelief("~connectedTopic("+entry.getKey()+")");
			}else {
				rosAgArch.addBelief("~connectedTopic("+entry.getKey()+")");
				actionExec.setResult(false);
				actionExec.setFailureReason(new Atom("topic_not_connected"), "Some services are not connected");
			}
		}

	}

}
