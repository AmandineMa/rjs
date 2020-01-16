package arch.actions.ros.disambi;

import com.github.rosjava_actionlib.GoalIDGenerator;

import arch.actions.AbstractAction;
import arch.agarch.AbstractROSAgArch;
import jason.asSemantics.ActionExec;
import ros.RosNodeDisambi;
import utils.Tools;

public class StartROSNodeDisambi extends AbstractAction {
	
//	private RosNodeGuiding rosnode;

	public StartROSNodeDisambi(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}

	@Override
	public void execute() {
		rosnode = new RosNodeDisambi("supervisor");
		rosAgArch.getNodeMainExecutor().execute(rosnode, rosAgArch.getNodeConfiguration());
		while(rosnode.getConnectedNode() == null) {
			Tools.sleep(100);
		}
		rosnode.init();
		AbstractROSAgArch.setRosnode(rosnode);
		rosAgArch.setGoalIDGenerator(new GoalIDGenerator(rosnode.getConnectedNode()));
		actionExec.setResult(true);
	}

}
