package supervisor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import actionlib_msgs.GoalStatus;
import deictic_gestures_msgs.PointAtResponse;
import dialogue_as.dialogue_actionActionFeedback;
import dialogue_as.dialogue_actionActionResult;
import geometry_msgs.Pose;
import jason.RevisionFailedException;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Message;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;
import msg_srv_impl.RouteImpl;
import msg_srv_impl.SemanticRouteResponseImpl;
import ontologenius_msgs.OntologeniusServiceResponse;
import perspectives_msgs.HasMeshResponse;
import pointing_planner_msgs.PointingActionFeedback;
import pointing_planner_msgs.PointingActionResult;
import pointing_planner_msgs.VisibilityScoreResponse;
import route_verbalization_msgs.VerbalizeRegionRouteResponse;
import semantic_route_description_msgs.Route;
import semantic_route_description_msgs.SemanticRouteResponse;
import speech_wrapper_msgs.SpeakToResponse;



public class RobotAgArch extends ROSAgArch {

	@SuppressWarnings("unused")
	private Logger logger = Logger.getLogger(RobotAgArch.class.getName());
	private String running_task_name;
	private String current_human;

	@Override
	public void act(final ActionExec action) {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				String action_name = action.getActionTerm().getFunctor();
				Message msg = new Message("tell", getAgName(), "supervisor", "action_started("+action_name+")");

				try {
					sendMsg(msg);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				//TODO check number of terms for each action
				if(action_name.equals("set_task_infos")) {
					running_task_name = action.getActionTerm().getTerm(0).toString();
					current_human = action.getActionTerm().getTerm(1).toString();
					action.setResult(true);
					actionExecuted(action);
				}else if(action_name.equals("compute_route")) {
					// to remove the extra ""
					String from = action.getActionTerm().getTerm(0).toString();
					from = from.replaceAll("^\"|\"$", "");


					ListTerm to_list;
					Term to = (Term) action.getActionTerm().getTerm(1);
					// if there is multiple places (toilet and atm cases)
					if(to.isList()) {
						to_list = (ListTerm) to;
					}
					// if there is only one place, we convert it to a list with one element for convenience
					else {
						to_list = new ListTermImpl();
						to_list.add(to);
					}
					List<SemanticRouteResponse> routes = new ArrayList<SemanticRouteResponse>();
					boolean at_least_one_ok = false;
					// we get all the possible routes for the different places 
					// (we will be able then to choose between the best toilet or atm to go)
					for (Term t: to_list) {
						// call the service to compute route
						m_rosnode.call_get_route_srv(from, 
								((StringTermImpl)t).getString(),
								action.getActionTerm().getTerm(2).toString(), 
								Boolean.parseBoolean(action.getActionTerm().getTerm(3).toString()));

						SemanticRouteResponseImpl resp = new SemanticRouteResponseImpl();
						// we wait the result return from the service
						do {
							resp = m_rosnode.get_get_route_resp();
							if (resp != null) {
								routes.add(resp);
							}
							sleep(100);
						}while(resp == null);
						if(resp.getCode() == Code.ERROR.getCode()) {
							action.setResult(false);
							action.setFailureReason(new Atom("route_not_found"), "No route has been found");

						}else {
							at_least_one_ok = true;
						}
					}        
					if(!at_least_one_ok) {
						actionExecuted(action);
					}else {
						RouteImpl route = select_best_route(routes);

						try {
							getTS().getAg().addBel(Literal.parseLiteral("route("+route.getRoute()+")["+running_task_name+","+current_human+"]"));
							getTS().getAg().addBel(Literal.parseLiteral("target_place("+route.getGoal()+")["+running_task_name+","+current_human+"]"));
							action.setResult(true);
							actionExecuted(action);

						} catch (RevisionFailedException e) {
							e.printStackTrace();
						}
					}

				} else if(action_name.equals("get_onto_individual_info")) {
					String param = action.getActionTerm().getTerm(0).toString();
					String individual =  action.getActionTerm().getTerm(1).toString();
					individual = individual.replaceAll("^\"|\"$", "");
					String belief_name = action.getActionTerm().getTerm(2).toString();
					m_rosnode.call_onto_indivual_srv(param, individual);
					OntologeniusServiceResponse places;
					do {
						places = m_rosnode.get_onto_individual_resp();
						sleep(100);
					}while(places == null);
					if(places.getCode() == Code.OK.getCode()) {
						try {
							ListTermImpl places_list = new ListTermImpl();
							for(String place : places.getValues()) {
								places_list.add(new StringTermImpl(place));
							}
							if(places_list.size() == 1) {
								if(belief_name.equals("shop_names")||belief_name.equals("shop_name"))
									getTS().getAg().addBel(Literal.parseLiteral(belief_name+"("+places_list.get(0)+")"));
								else
									getTS().getAg().addBel(Literal.parseLiteral(belief_name+"("+places_list.get(0)+")["+running_task_name+","+current_human+"]"));
							}else {
								if(belief_name.equals("shop_names")||belief_name.equals("shop_name"))
									getTS().getAg().addBel(Literal.parseLiteral(belief_name+"("+places_list+")"));
								else
									getTS().getAg().addBel(Literal.parseLiteral(belief_name+"("+places_list+")["+running_task_name+","+current_human+"]"));
							}
							action.setResult(true);
							actionExecuted(action);
						} catch (RevisionFailedException e) {
							e.printStackTrace();
						}
					}else {
						action.setResult(false);
						action.setFailureReason(new Atom("individual_not_found"), individual+" has been not been found in the ontology with the param "+param);
						actionExecuted(action);
					} 
				} else if(action_name.equals("get_human_abilities")) { //TODO to proper implement
					try {
						getTS().getAg().addBel(Literal.parseLiteral("persona_asked(old)["+running_task_name+","+current_human+"]"));
					} catch (RevisionFailedException e) {
						e.printStackTrace();
					}
					action.setResult(true);
					actionExecuted(action);
				} else if(action_name.equals("get_placements")) {
					// to remove the extra ""
					ArrayList<String> params = new ArrayList<String>();
					for(Term term : action.getActionTerm().getTerms()) {
						params.add(term.toString().replaceAll("^\"|\"$", ""));
					}

					String target = params.get(0);
					String direction = params.get(1);
					String human = params.get(2);

					if(m_rosnode.call_svp_planner(target, direction, human)) {
						PointingActionResult placements_result;
						PointingActionFeedback placements_fb;
						do {
							placements_result = m_rosnode.get_placements_result();
							placements_fb = m_rosnode.getPlacements_fb();
							if(placements_fb != null) {
								try {
									getTS().getAg().addBel(Literal.parseLiteral("fb(svp_planner, "+placements_fb.getFeedback().getState()+")["+running_task_name+","+current_human+"]"));
								} catch (RevisionFailedException e) {
									e.printStackTrace();
								}
							}
							sleep(200);
						}while(placements_result == null);

						if(placements_result.getStatus().getStatus() == GoalStatus.SUCCEEDED) {
							try {
								Pose robot_pose = placements_result.getResult().getRobotPose().getPose();
								Pose human_pose = placements_result.getResult().getHumanPose().getPose();
								getTS().getAg().addBel(Literal.parseLiteral("robot_pos("+robot_pose.getPosition().getX()+","
										+robot_pose.getPosition().getY()+","
										+robot_pose.getPosition().getZ()+")["+running_task_name+","+current_human+"]"));
								getTS().getAg().addBel(Literal.parseLiteral("human_pos("+human_pose.getPosition().getX()+","
										+human_pose.getPosition().getY()+","
										+human_pose.getPosition().getZ()+")["+running_task_name+","+current_human+"]"));
								int nb_ld_to_point = placements_result.getResult().getPointedLandmarks().size();
								if(nb_ld_to_point == 0) {
									getTS().getAg().addBel(Literal.parseLiteral("ld_to_point(None)["+running_task_name+","+current_human+"]"));
								} else if(nb_ld_to_point==1) {
									getTS().getAg().addBel(Literal.parseLiteral("ld_to_point("+placements_result.getResult().getPointedLandmarks().get(0)+")["+running_task_name+","+current_human+"]"));
								} else if(nb_ld_to_point==2){
									getTS().getAg().addBel(Literal.parseLiteral("ld_to_point("+placements_result.getResult().getPointedLandmarks().get(0)+","
											+placements_result.getResult().getPointedLandmarks().get(1)+")["+running_task_name+","+current_human+"]"));
								}
							} catch (RevisionFailedException e) {
								e.printStackTrace();
							}
							action.setResult(true);
							actionExecuted(action);
						}else {
							action.setResult(false);
							action.setFailureReason(new Atom("svp_failure"), "SVP planner goal status :"+placements_result.getStatus().getStatus());
							actionExecuted(action);
						}
					}else {
						action.setResult(false);
						action.setFailureReason(new Atom("svp_srv_not_found"), "SVP planner action server not found");
						actionExecuted(action);
					}

				}else if(action_name.equals("has_mesh")) {
					// to remove the extra ""
					String param = action.getActionTerm().getTerm(0).toString();
					param = param.replaceAll("^\"|\"$", "");

					m_rosnode.call_has_mesh_srv("base", param);
					HasMeshResponse has_mesh;
					do {
						has_mesh = m_rosnode.getHas_mesh_resp();
						sleep(100);
					}while(has_mesh == null);
					if(has_mesh.getHasMesh()) {
						action.setResult(true);
					}else {
						action.setResult(false);
						if(!has_mesh.getHasMesh()) {
							action.setFailureReason(new Atom("has_no_mesh"), param+" does not have a mesh");
						}else {
							action.setFailureReason(new Atom("srv_has_mesh_failed"), "has_mesh service failed");
						}
					}
					actionExecuted(action);
				} else if(action_name.equals("can_be_visible")) {
					// to remove the extra ""
					String human = action.getActionTerm().getTerm(0).toString();
					human = human.replaceAll("^\"|\"$", "");
					String place = action.getActionTerm().getTerm(1).toString();
					place = place.replaceAll("^\"|\"$", "");
					m_rosnode.call_visibility_score_srv(human, place);
					VisibilityScoreResponse vis_resp;
					do {
						vis_resp = m_rosnode.getVisibility_score_resp();
						sleep(100);
					}while(vis_resp == null);
					if(vis_resp.getIsVisible()) {
						action.setResult(true);
						try {
							getTS().getAg().addBel(Literal.parseLiteral("canBeVisibleFor("+place+","+human+")["+running_task_name+","+current_human+"]"));
						} catch (RevisionFailedException e) {
							e.printStackTrace();
						}
					}else {
						action.setResult(false);
						action.setFailureReason(new Atom("not_visible"), place+" is not visible");
					}
					actionExecuted(action);
				} else if(action_name.equals("point_at")) {
					// to remove the extra ""
					String place = action.getActionTerm().getTerm(0).toString();
					place = place.replaceAll("^\"|\"$", "");
					m_rosnode.call_point_at_srv(place);
					PointAtResponse point_at_resp;
					do {
						point_at_resp = m_rosnode.getPoint_at_resp();
						sleep(100);
					}while(point_at_resp == null);
					if(point_at_resp.getSuccess()) {
						action.setResult(true);
					}else {
						action.setResult(false);
						action.setFailureReason(new Atom("point_at_failed"), "the pointing failed for "+place);
					}
					actionExecuted(action);
				} else if(action_name.equals("text2speech")) {
					// to remove the extra ""
					String human = action.getActionTerm().getTerm(0).toString();
					human = human.replaceAll("^\"|\"$", "");
					Literal bel = (Literal) action.getActionTerm().getTerm(1);
					String text;
					String bel_functor = bel.getFunctor();
					switch(bel_functor) {
					case "should_look_place": text = new String("Look "+bel.getTerm(0)+" over there"); break;
					case "should_look_orientation": text = new String("You should look a bit more at the "+bel.getTerm(0)); break;
					case "able_to_see": text = new String("I note that you must be looking at the place right now"); break;
					case "route_verbalization" : text = bel.getTerm(0).toString().replaceAll("^\"|\"$", ""); break;
					case "no_place" : text = new String("The place you asked for does not exist"); break;
					default : action.setResult(false); action.setFailureReason(new Atom("unknown_string"), "no speech to say"); actionExecuted(action); return;
					}
					m_rosnode.call_speak_to_srv(human, text);
					SpeakToResponse speak_to_resp;
					do {
						speak_to_resp = m_rosnode.getSpeak_to_resp();
						sleep(100);
					}while(speak_to_resp == null);
					if(speak_to_resp.getSuccess()) {
						action.setResult(true);
					}else {
						action.setResult(false);
						action.setFailureReason(new Atom("speak_to_failed"), "the speech service failed");
					}
					actionExecuted(action);
				} else if(action_name.equals("get_route_verbalization")) {
					// to remove the extra ""
					@SuppressWarnings("unchecked")
					List<Term> route_temp = (List<Term>) action.getActionTerm().getTerm(0);
					List<String> route = new ArrayList<>();
					for(Term t : route_temp) {
						route.add(t.toString());
					}
					String robot_place = action.getActionTerm().getTerm(1).toString();
					robot_place = robot_place.replaceAll("^\"|\"$", "");
					String place = action.getActionTerm().getTerm(2).toString();
					place = place.replaceAll("^\"|\"$", "");
					m_rosnode.call_route_verbalization_srv(route, robot_place, place);
					VerbalizeRegionRouteResponse verba_resp;
					do {
						verba_resp = m_rosnode.getVerbalization_resp();
						sleep(100);
					}while(verba_resp == null);
					String verba = new String(verba_resp.getRegionRoute());
					if(verba_resp.getSuccess() & verba != "") {
						action.setResult(true);
						try {
							getTS().getAg().addBel(Literal.parseLiteral("verbalization(\""+verba+"\")["+running_task_name+","+current_human+"]"));
						} catch (RevisionFailedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}else {
						action.setResult(false);
						action.setFailureReason(new Atom("route_verba_failed"), "the route verbalization service failed");
					}
					actionExecuted(action);
				} else if(action_name.equals("listen")) {
					ArrayList<String> words = new ArrayList<String>();
					for(Term term : (ListTermImpl)action.getActionTerm().getTerms().get(0)) {
						words.add(term.toString().replaceAll("^\"|\"$", ""));
					}
					if(m_rosnode.call_dialogue_as(words)) {
						dialogue_actionActionResult listening_result;
						dialogue_actionActionFeedback listening_fb;
						dialogue_actionActionFeedback listening_fb_prev = null;

						do {
							listening_result = m_rosnode.getListening_result();
							listening_fb = m_rosnode.getListening_fb();
							if(listening_fb != null & listening_fb != listening_fb_prev) {
								logger.info("not expected answer");
								listening_fb_prev = listening_fb;
							}
							sleep(200);
						}while(listening_result == null);
						if(listening_result.getStatus().getStatus() == GoalStatus.SUCCEEDED) {
							logger.info("human said :"+listening_result.getResult().getSubject());
							action.setResult(true);
						}else {
							action.setResult(false);
							action.setFailureReason(new Atom("dialogue_as_failed"), "");
							if(listening_result.getStatus().getStatus() == GoalStatus.PREEMPTED)
								logger.info("dialogue_as preempted");
						}
					}
					actionExecuted(action);
				} else if(action_name.equals("get_hatp_plan")){
					String task_name = action.getActionTerm().getTerm(0).toString();
					task_name = task_name.replaceAll("^\"|\"$", "");
					if(action.getActionTerm().getArity() > 1) {
						@SuppressWarnings("unchecked")
						List<Term> parameters_temp = (List<Term>) action.getActionTerm().getTerm(1);
						List<String> parameters = new ArrayList<>();
						for(Term t : parameters_temp) {
							parameters.add(t.toString());
						}
						m_rosnode.call_hatp_planner(task_name, "plan", parameters);
					}else {
						m_rosnode.call_hatp_planner(task_name, "plan");
					}
					hatp_msgs.Plan plan;
					do {
						plan = m_rosnode.getHatp_planner_resp();
						sleep(100);
					} while(plan == null);
					if(plan.getReport().equals("OK")) {
						action.setResult(true);
						try {
							for(hatp_msgs.Task task : plan.getTasks()) {
								String agents = array_2_str_array(task.getAgents());
								if(!task.getParameters().isEmpty()) {
									String parameters = array_2_str_array(task.getParameters());
									getTS().getAg().addBel(Literal.parseLiteral("task("+task.getId()+","+task.getType()+","
											+task.getName()+","+agents+","+parameters+","+task.getCost()+")["+running_task_name+","+current_human+"]"));
								}else {
									getTS().getAg().addBel(Literal.parseLiteral("task("+task.getId()+","+task.getType()+","
											+task.getName()+","+agents+","+task.getCost()+")["+running_task_name+","+current_human+"]"));
								}					
							}
							for(hatp_msgs.StreamNode stream : plan.getStreams()) {
								String belief = "stream("+stream.getTaskId();
								if(stream.getPredecessors().length != 0) {
									String pred = array_2_str_array(stream.getPredecessors());
									belief = belief+","+pred;
								}
								if(stream.getSuccessors().length != 0) {
									String succ = array_2_str_array(stream.getSuccessors());
									belief = belief+","+succ;
								}
								belief = belief+")";
								getTS().getAg().addBel(Literal.parseLiteral(belief+"["+running_task_name+","+current_human+"]"));
							}
						} catch (RevisionFailedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}else {
						action.setResult(false);
						action.setFailureReason(new Atom("no_plan_found"), "hatp planner could not find any feasible plan");
					}
					actionExecuted(action);
				} 
				else {
					action.setResult(false);
					action.setFailureReason(new Atom("act_not_found"), "no action "+action_name+" is implemented");
					actionExecuted(action);
				}

			}
		});


	}

	private String array_2_str_array(List<String> array) {
		String str_array = new String();
		for(String str : array) {
			if(str_array.isEmpty()) {
				str_array = str;
			}else {
				str_array = str_array+","+str;
			}
		}
		str_array = "["+str_array+"]";
		return str_array;
	}

	private String array_2_str_array(int[] array) {
		String str_array = new String();
		for(int i : array) {
			if(str_array.isEmpty()) {
				str_array = String.valueOf(i);
			}else {
				str_array = str_array+","+String.valueOf(i);
			}
		}
		str_array = "["+str_array+"]";
		return str_array;
	}

	public RouteImpl select_best_route(List<SemanticRouteResponse> routes_resp_list) {
		RouteImpl best_route = new RouteImpl();
		float min_cost = Float.MAX_VALUE;
		if (routes_resp_list.size() > 0) {
			for (SemanticRouteResponse route_resp : routes_resp_list) {
				List<Route> routes = route_resp.getRoutes();
				float[] costs = route_resp.getCosts();
				List<String> goals = route_resp.getGoals();

				for (int i = 0; i < routes.size(); i++) {
					if (costs[i] < min_cost) {
						best_route.setRoute(routes.get(i).getRoute());
						best_route.setGoal(goals.get(i));
						min_cost = costs[i];
					}
				}
			}
		}

		return best_route;

	}





	@Override
	public void actionExecuted(ActionExec act) {
		String action_name = act.getActionTerm().getFunctor();
		Message msg;
		if(act.getResult()) {
			msg = new Message("tell", getAgName(), "supervisor", "action_over("+action_name+")");
		}else {
			//    		msg = new Message("tell", getAgName(), "supervisor", "action_failed("+action_name+","+new StringTermImpl(act.getFailureReason().toString())+")");
			msg = new Message("tell", getAgName(), "supervisor", "action_failed("+action_name+","+act.getFailureReason().toString()+")");
		}
		try {
			sendMsg(msg);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		super.actionExecuted(act);
	}



};


