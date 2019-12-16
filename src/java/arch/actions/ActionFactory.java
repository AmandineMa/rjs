package arch.actions;

import arch.ROSAgArch;
import jason.asSemantics.ActionExec;

public class ActionFactory {
	
	public static Action createAction(ActionExec actionExec, ROSAgArch rosAgArch) {
		String actionName = actionExec.getActionTerm().getFunctor();
		Action action = null;
		
		switch(actionName) {
			case "human_to_monitor" :
				action = new HumanToMonitor(actionExec, rosAgArch);
				break;
			case "compute_route" :
				action = new ComputeRoute(actionExec, rosAgArch);
				break;
			case "pause_asr_and_display_processing" :
				action = new PauseASR(actionExec, rosAgArch);
				break;
			case "get_onto_individual_info" :
				action = new GetOntoIndividualInfo(actionExec, rosAgArch);
				break;
			default:
		}
			
		return action;
	}

}
