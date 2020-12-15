package rjs.arch.actions.ros;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ros.helpers.ParameterLoaderNode;

import jason.asSemantics.ActionExec;
import jason.asSyntax.Term;
import rjs.arch.actions.AbstractAction;
import rjs.arch.agarch.AbstractROSAgArch;

public class StartParameterLoaderNode extends AbstractAction {

	public StartParameterLoaderNode(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}

	@Override
	public void execute() {
		Iterator<Term> it = actionExec.getActionTerm().getTerms().iterator();
		List<ParameterLoaderNode.Resource> resourceList = new ArrayList<ParameterLoaderNode.Resource>();
		while(it.hasNext()) {
			String file = removeQuotes(it.next().toString());
			resourceList.add(new ParameterLoaderNode.Resource(AbstractROSAgArch.class.getResourceAsStream(file), ""));
		}
		rosAgArch.setParameterLoaderNode(new ParameterLoaderNode(resourceList));
		rosAgArch.getNodeMainExecutor().execute(rosAgArch.getParameterLoaderNode(), rosAgArch.getNodeConfiguration());
		actionExec.setResult(true);
		//TODO: investigate on how to active actionExecuted
//		rosAgArch.actionExecuted(actionExec);
	}

}
