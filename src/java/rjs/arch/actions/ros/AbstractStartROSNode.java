package rjs.arch.actions.ros;

import jason.asSemantics.ActionExec;
import rjs.arch.actions.AbstractAction;
import rjs.arch.agarch.AbstractROSAgArch;
import rjs.utils.Tools;

public abstract class AbstractStartROSNode extends AbstractAction {
	
//	private RosNodeGuiding rosnode;

	public AbstractStartROSNode(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}

	
	protected void configRosnode() {
		rosAgArch.getNodeMainExecutor().execute(rosnode, rosAgArch.getNodeConfiguration());
		while(rosnode.getConnectedNode() == null) {
			Tools.sleep(100);
		}
		rosnode.init();
		AbstractROSAgArch.setRosnode(rosnode);
		try {
			rosAgArch.setActionFactoryRosVariables();
		} catch (Exception e) {
			logger.info(Tools.getStackTrace(e));
		}
		actionExec.setResult(true);
	}

}
