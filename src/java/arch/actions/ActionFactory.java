package arch.actions;

import arch.ROSAgArch;
import arch.actions.robot.EnableAnimatedSpeech;
import arch.actions.robot.Engage;
import arch.actions.robot.FaceHuman;
import arch.actions.robot.HumanToMonitor;
import arch.actions.robot.Listen;
import arch.actions.robot.Localise;
import arch.actions.robot.LookAt;
import arch.actions.robot.MoveTo;
import arch.actions.robot.PauseASR;
import arch.actions.robot.PointAt;
import arch.actions.robot.PubLookAtEvents;
import arch.actions.robot.ReinitLoca;
import arch.actions.robot.Rotate;
import arch.actions.robot.TerminateInteraction;
import arch.actions.robot.TextToSpeech;
import arch.actions.robot.internal.CanBeVisible;
import arch.actions.robot.internal.ComputeRoute;
import arch.actions.robot.internal.GetOntoIndividualInfo;
import arch.actions.robot.internal.GetPlacements;
import arch.actions.robot.internal.GetRouteVerba;
import arch.actions.robot.internal.HasMesh;
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
			case "engage":
				action = new Engage(actionExec, rosAgArch);
				break;
			case "terminate_interaction":
				action = new TerminateInteraction(actionExec, rosAgArch);
				break;
			case "localise":
				action = new Localise(actionExec, rosAgArch);
				break;
			case "reinit_loca":
				action = new ReinitLoca(actionExec, rosAgArch);
				break;
			default:
				break;
		}
			
		return action;
	}

}
