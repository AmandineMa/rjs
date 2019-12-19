package arch.actions.ros;

import java.util.ArrayList;
import java.util.List;

import org.ros.helpers.ParameterLoaderNode;

import arch.actions.AbstractAction;
import arch.agarch.AbstractROSAgArch;
import arch.agarch.guiding.SupervisorAgArch;
import jason.asSemantics.ActionExec;

public class StartParameterLoaderNode extends AbstractAction {

	public StartParameterLoaderNode(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}

	@Override
	public void execute() {
		String file = removeQuotes(actionExec.getActionTerm().getTerm(0).toString());
		List<ParameterLoaderNode.Resource> resourceList = new ArrayList<ParameterLoaderNode.Resource>() {{
			add(new ParameterLoaderNode.Resource(SupervisorAgArch.class.getResourceAsStream(file), ""));
		}}; 
		rosAgArch.setParameterLoaderNode(new ParameterLoaderNode(resourceList));
		rosAgArch.getNodeMainExecutor().execute(rosAgArch.getParameterLoaderNode(), rosAgArch.getNodeConfiguration());
		actionExec.setResult(true);
	}

}
