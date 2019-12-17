package arch.actions.robot;

import arch.ROSAgArch;
import arch.actions.AbstractAction;
import jason.asSemantics.ActionExec;

public class Engage extends AbstractAction {

	public Engage(ActionExec actionExec, ROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}

	@Override
	public void execute() {
		String human_id = actionExec.getActionTerm().getTerm(0).toString();
		rosnode.callEngageAS(human_id);
		actionExec.setResult(true);
	}

}
