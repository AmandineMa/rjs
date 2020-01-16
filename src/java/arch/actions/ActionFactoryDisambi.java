package arch.actions;

import org.ros.node.topic.Publisher;

import arch.actions.robot.Say;
import arch.actions.robot.internal.Disambiguate;
import arch.actions.robot.internal.GetSparqlVerba;
import arch.agarch.AbstractROSAgArch;
import jason.asSemantics.ActionExec;

public class ActionFactoryDisambi extends AbstractActionFactory {
	
	private static Publisher<std_msgs.String> sayPub;
	
	public ActionFactoryDisambi(AbstractROSAgArch rosAgArch) {
		super(rosAgArch);
		sayPub = createPublisher("disambi/topics/say");
	}
	
	public static Action createAction(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		String actionName = actionExec.getActionTerm().getFunctor();
		Action action = null;
		
		switch(actionName) {
			case "disambiguate":
				action = new Disambiguate(actionExec, rosAgArch);
				break;
			case "sparql_verbalization":
				action = new GetSparqlVerba(actionExec, rosAgArch);
				break;
			case "say":
				action = new Say(actionExec, rosAgArch, sayPub);
				break;
			default:
				break;
		}
			
		return action;
	}
	
}
