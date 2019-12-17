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
			case "get_placements" :
				action = new GetPlacements(actionExec, rosAgArch);
				break;
			case "has_mesh" :
				action = new HasMesh(actionExec, rosAgArch);
				break;
			case "can_be_visible" :
				action = new CanBeVisible(actionExec, rosAgArch);
				break;
			case "point_at" :
				action = new PointAt(actionExec, rosAgArch);
				break;
			case "enable_animated_speech" :
				action = new EnableAnimatedSpeech(actionExec, rosAgArch);
				break;
			case "face_human" :
				action = new FaceHuman(actionExec, rosAgArch);
				break;
			case "rotate" :
				action = new Rotate(actionExec, rosAgArch);
				break;
			case "look_at" :
				action = new LookAt(actionExec, rosAgArch);
				break;
			case "text2speech":
				action = new TextToSpeech(actionExec, rosAgArch);
				break;
			case "listen":
				action = new Listen(actionExec, rosAgArch);
				break;
			case "get_route_verbalization":
				action = new GetRouteVerba(actionExec, rosAgArch);
				break;
			case "look_at_events":
				action = new PubLookAtEvents(actionExec, rosAgArch);
				break;
			case "move_to":
				action = new MoveTo(actionExec, rosAgArch);
				break;
			default:
				break;
		}
			
		return action;
	}

}
