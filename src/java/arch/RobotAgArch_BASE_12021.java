package arch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;

import actionlib_msgs.GoalStatus;
import deictic_gestures_msgs.CanLookAtResponse;
import deictic_gestures_msgs.CanPointAtResponse;
import deictic_gestures_msgs.LookAtResponse;
import deictic_gestures_msgs.PointAtResponse;
import dialogue_as.dialogue_actionActionFeedback;
import dialogue_as.dialogue_actionActionResult;
import geometry_msgs.Point;
import geometry_msgs.PointStamped;
import geometry_msgs.PoseStamped;
import jason.RevisionFailedException;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Message;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;
import move_base_msgs.MoveBaseActionFeedback;
import move_base_msgs.MoveBaseActionResult;
import msg_srv_impl.PoseCustom;
import msg_srv_impl.RouteImpl;
import msg_srv_impl.SemanticRouteResponseImpl;
import nao_interaction_msgs.SayResponse;
import ontologenius_msgs.OntologeniusServiceResponse;
import perspectives_msgs.HasMeshResponse;
import pointing_planner.PointingActionFeedback;
import pointing_planner.PointingActionResult;
import pointing_planner.PointingPlannerResponse;
import pointing_planner.VisibilityScoreResponse;
import route_verbalization_msgs.VerbalizeRegionRouteResponse;
import rpn_recipe_planner_msgs.SuperQueryResponse;
import semantic_route_description_msgs.Route;
import semantic_route_description_msgs.SemanticRouteResponse;
import speech_wrapper_msgs.SpeakToResponse;
import std_msgs.Header;
import utils.Code;



public class RobotAgArch extends ROSAgArch {

	@SuppressWarnings("unused")
	private Logger logger = Logger.getLogger(RobotAgArch.class.getName());
	private String running_task_name;
	private String current_human;
	private int max_attempt = 1;

	@Override
	public void act(final ActionExec action) {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				String action_name = action.getActionTerm().getFunctor();
				Message msg = new Message("tell", getAgName(), "supervisor", "action_started("+action_name+")");
				String task_id = action.getIntention().getBottom().getTrigger().getLiteral().getTerm(0).toString();
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
							String route_list = route.getRoute().stream()
									  .map(s -> "\"" + s + "\"")
									  .collect(Collectors.joining(", "));
							getTS().getAg().addBel(Literal.parseLiteral("route(["+route_list+"])["+task_id+"]"));
							getTS().getAg().addBel(Literal.parseLiteral("target_place(\""+route.getGoal()+"\")["+task_id+"]"));
							action.setResult(true);
							actionExecuted(action);

						} catch (RevisionFailedException e) {
							e.printStackTrace();
						}
					}

				} else if(action_name.equals("get_onto_individual_info")) {
					String param = action.getActionTerm().getTerm(0).toString();
					String individual_o =  action.getActionTerm().getTerm(1).toString();
					String individual = individual_o.replaceAll("^\"|\"$", "");
					String belief_name = action.getActionTerm().getTerm(2).toString();
					m_rosnode.call_onto_individual_srv(param, individual);
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
									getTS().getAg().addBel(Literal.parseLiteral(belief_name+"("+individual_o+","+places_list.get(0)+")["+task_id+"]"));
							}else {
								if(belief_name.equals("shop_names")||belief_name.equals("shop_name"))
									getTS().getAg().addBel(Literal.parseLiteral(belief_name+"("+places_list+")"));
								else
									getTS().getAg().addBel(Literal.parseLiteral(belief_name+"("+individual_o+","+places_list+")["+task_id+"]"));
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
				} else if(action_name.equals("get_placements")) {
					// to remove the extra ""
					ArrayList<String> params = new ArrayList<String>();
					for(Term term : action.getActionTerm().getTerms()) {
						params.add(term.toString().replaceAll("^\"|\"$", ""));
					}

					String target = params.get(0);
					String direction = params.get(1);
					String human = params.get(2);
					int tar_is_dir = Integer.parseInt(params.get(3));
					
					m_rosnode.call_svp_planner_srv(target, direction, human);
					PointingPlannerResponse placements_result;
					do {
						placements_result = m_rosnode.getPlacements_resp();
						sleep(100);
					}while(placements_result == null);
					try {
						if(!placements_result.getPointedLandmarks().isEmpty()) {
							PoseCustom robot_pose = new PoseCustom(placements_result.getRobotPose().getPose());
							String r_frame = placements_result.getRobotPose().getHeader().getFrameId();
							PoseCustom human_pose = new PoseCustom(placements_result.getHumanPose().getPose());
							String h_frame = placements_result.getHumanPose().getHeader().getFrameId();
							getTS().getAg().addBel(Literal.parseLiteral("robot_pose("+r_frame+","+robot_pose.toString()+")["+task_id+"]"));
							getTS().getAg().addBel(Literal.parseLiteral("human_pose("+h_frame+","+human_pose.toString()+")["+task_id+"]"));
							int nb_ld_to_point = placements_result.getPointedLandmarks().size();
							for(int i = 0; i < nb_ld_to_point; i++) {
								String ld = placements_result.getPointedLandmarks().get(i);
								if(tar_is_dir == 0) {
									if(ld.equals(target)) {
										getTS().getAg().addBel(Literal.parseLiteral("target_to_point(\""+ld+"\")["+task_id+"]"));
									}else if(ld.equals(direction)) {
										getTS().getAg().addBel(Literal.parseLiteral("dir_to_point(\""+ld+"\")["+task_id+"]"));
									}
								}else {
									if(ld.equals(target)) {
										getTS().getAg().addBel(Literal.parseLiteral("dir_to_point(\""+ld+"\")["+task_id+"]"));
									}
								}
							}
							getTS().getAg().addBel(Literal.parseLiteral("ld_to_point["+task_id+"]"));
						}else {
							getTS().getAg().addBel(Literal.parseLiteral("~ld_to_point["+task_id+"]"));
						}
					}catch (RevisionFailedException e) {
						e.printStackTrace();
					}
					action.setResult(true);
					actionExecuted(action);

//					if(m_rosnode.call_svp_planner(target, direction, human, max_attempt)) {
//						PointingActionResult placements_result;
//						PointingActionFeedback placements_fb;
//						PointingActionFeedback placements_fb_prev = null;
//						do {
//							placements_result = m_rosnode.get_placements_result();
//							placements_fb = m_rosnode.getPlacements_fb();
//							if(placements_fb != null & placements_fb != placements_fb_prev) {
//								try {
//									getTS().getAg().addBel(Literal.parseLiteral("fb(svp_planner, "+placements_fb.getFeedback().getState()+")["+task_id+"]"));
//								} catch (RevisionFailedException e) {
//									e.printStackTrace();
//								}
//								placements_fb_prev = placements_fb;
//							}
//							sleep(200);
//						}while(placements_result == null);

//						if(placements_result.getStatus().getStatus() == GoalStatus.SUCCEEDED) {
//							try {
//								PoseCustom robot_pose = new PoseCustom(placements_result.getResult().getRobotPose().getPose());
//								String r_frame = placements_result.getResult().getRobotPose().getHeader().getFrameId();
//								PoseCustom human_pose = new PoseCustom(placements_result.getResult().getHumanPose().getPose());
//								String h_frame = placements_result.getResult().getHumanPose().getHeader().getFrameId();
//								getTS().getAg().addBel(Literal.parseLiteral("robot_pose("+r_frame+","+robot_pose.toString()+")["+task_id+"]"));
//								getTS().getAg().addBel(Literal.parseLiteral("human_pose("+h_frame+","+human_pose.toString()+")["+task_id+"]"));
//								int nb_ld_to_point = placements_result.getResult().getPointedLandmarks().size();
//								boolean at_least_one = false;
//								for(int i = 0; i < nb_ld_to_point; i++) {
//									String ld = placements_result.getResult().getPointedLandmarks().get(i);
//									if(tar_is_dir == 0) {
//										if(ld.equals(target)) {
//											getTS().getAg().addBel(Literal.parseLiteral("target_to_point(\""+ld+"\")["+task_id+"]"));
//											at_least_one = true;
//										}else if(ld.equals(direction)) {
//											getTS().getAg().addBel(Literal.parseLiteral("dir_to_point(\""+ld+"\")["+task_id+"]"));
//											at_least_one = true;
//										}
//									}else {
//										if(ld.equals(target)) {
//											getTS().getAg().addBel(Literal.parseLiteral("dir_to_point(\""+ld+"\")["+task_id+"]"));
//											at_least_one = true;
//										}
//									}
//								}
//								if(!at_least_one) {
//									getTS().getAg().addBel(Literal.parseLiteral("~ld_to_point[\""+task_id+"\"]"));
//								}else {
//									getTS().getAg().addBel(Literal.parseLiteral("ld_to_point[\""+task_id+"\"]"));
//								}
//							} catch (RevisionFailedException e) {
//								e.printStackTrace();
//							}
//							action.setResult(true);
//							actionExecuted(action);
//						}else {
//							action.setResult(false);
//							action.setFailureReason(new Atom("svp_failure"), "SVP planner goal status :"+placements_result.getStatus().getStatus());
//							actionExecuted(action);
//						}
//					}else {
//						action.setResult(false);
//						action.setFailureReason(new Atom("svp_srv_not_found"), "SVP planner action server not found");
//						actionExecuted(action);
//					}

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
							getTS().getAg().addBel(Literal.parseLiteral("canBeVisibleFor(\""+place+"\","+human+")["+task_id+"]"));
						} catch (RevisionFailedException e) {
							e.printStackTrace();
						}
					}else {
						try {
							getTS().getAg().addBel(Literal.parseLiteral("~canBeVisibleFor(\""+place+"\","+human+")["+task_id+"]"));
						} catch (RevisionFailedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						action.setResult(false);
						action.setFailureReason(new Atom("not_visible"), place+" is not visible");
					}
					actionExecuted(action);
				} else if(action_name.equals("point_at")) {
					// to remove the extra ""
					String frame = action.getActionTerm().getTerm(0).toString();
					frame = frame.replaceAll("^\"|\"$", "");
					boolean with_head = Boolean.parseBoolean(action.getActionTerm().getTerm(1).toString());
					boolean with_base = Boolean.parseBoolean(action.getActionTerm().getTerm(2).toString());
					m_rosnode.call_point_at_srv(frame, with_head, with_base);
					PointAtResponse point_at_resp;
					do {
						point_at_resp = m_rosnode.getPoint_at_resp();
						sleep(100);
					}while(point_at_resp == null);
					if(point_at_resp.getSuccess()) {
						action.setResult(true);
					}else {
						action.setResult(false);
						action.setFailureReason(new Atom("point_at_failed"), "the pointing failed for "+frame);
					}
					actionExecuted(action);
				} else if(action_name.equals("look_at")) {
					// to remove the extra ""
					String frame = action.getActionTerm().getTerm(0).toString();
					frame = frame.replaceAll("^\"|\"$", "");
					boolean with_base = Boolean.parseBoolean(action.getActionTerm().getTerm(1).toString());
					NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
					MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();
					PointStamped point_stamped = messageFactory.newFromType(PointStamped._TYPE);
					if(action.getActionTerm().getArity() != 1) {
						ListTermImpl point_term =  ((ListTermImpl) action.getActionTerm().getTerm(1));
						Point point = messageFactory.newFromType(Point._TYPE);
						point.setX(((NumberTermImpl)point_term.get(0)).solve());
						point.setY(((NumberTermImpl)point_term.get(1)).solve());
						point.setZ(((NumberTermImpl)point_term.get(2)).solve());
						point_stamped.setPoint(point);
					}
					Header header = messageFactory.newFromType(std_msgs.Header._TYPE);
					header.setFrameId(frame);
					point_stamped.setHeader(header);
					m_rosnode.call_look_at_srv(point_stamped, with_base);

					LookAtResponse look_at_resp;
					do {
						look_at_resp = m_rosnode.getLook_at_resp();
						sleep(100);
					}while(look_at_resp == null);
					if(look_at_resp.getSuccess()) {
						action.setResult(true);
					}else {
						action.setResult(false);
						action.setFailureReason(new Atom("look_at_failed"), "the look at failed for "+frame);
					}
					actionExecuted(action);
					//				} else if(action_name.equals("can_point_at")) {
					//					// to remove the extra ""
//					String place = action.getActionTerm().getTerm(0).toString();
//					place = place.replaceAll("^\"|\"$", "");
//					m_rosnode.call_can_point_at_srv(place);
//					CanPointAtResponse can_point_at_resp;
//					do {
//						can_point_at_resp = m_rosnode.getCan_point_at_resp();
//						sleep(100);
//					}while(can_point_at_resp == null);
//					if(can_point_at_resp.getSuccess()) {
//						action.setResult(true);
//					}else {
//						action.setResult(false);
//						action.setFailureReason(new Atom("cannot_point"), "pointing not possible for "+place);
//					}
//					actionExecuted(action);
//				} else if(action_name.equals("can_look_at")) {
//						// to remove the extra ""
//						String frame = action.getActionTerm().getTerm(0).toString();
//						frame = frame.replaceAll("^\"|\"$", "");
//						NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
//						MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();
//						PointStamped point_stamped = messageFactory.newFromType(PointStamped._TYPE);
//					if(action.getActionTerm().getArity() != 1) {
//						ListTermImpl point_term =  ((ListTermImpl) action.getActionTerm().getTerm(1));
//						Point point = messageFactory.newFromType(Point._TYPE);
//						point.setX(((NumberTermImpl)point_term.get(0)).solve());
//						point.setY(((NumberTermImpl)point_term.get(1)).solve());
//						point.setZ(((NumberTermImpl)point_term.get(2)).solve());
//						point_stamped.setPoint(point);
//					}
//					Header header = messageFactory.newFromType(std_msgs.Header._TYPE);
//					header.setFrameId(frame);
//					point_stamped.setHeader(header);
//					m_rosnode.call_can_look_at_srv(point_stamped);
//					
//					CanLookAtResponse can_look_at_resp;
//					do {
//						can_look_at_resp = m_rosnode.getCan_look_at_resp();
//						sleep(100);
//					}while(can_look_at_resp == null);
//					if(can_look_at_resp.getSuccess()) {
//						action.setResult(true);
//					}else {
//						action.setResult(false);
//						action.setFailureReason(new Atom("cannot_look_at"), "looking not possible for"+frame);
//					}
//					actionExecuted(action);
				}else if(action_name.equals("text2speech")) {
					// to remove the extra ""
					String human = action.getActionTerm().getTerm(0).toString();
					human = human.replaceAll("^\"|\"$", "");
					Literal bel = (Literal) action.getActionTerm().getTerm(1);
					String text = "";
					String bel_functor = bel.getFunctor();
					String bel_arg = null;
					if(bel.getTerms().size()==1) {
						bel_arg = bel.getTerms().get(0).toString();
						bel_arg = bel_arg.replaceAll("^\"|\"$", "").replaceAll("\\[", "").replaceAll("\\]",""); 
					}
					boolean hwu_dial = m_rosnode.getParameters().getBoolean("guiding/dialogue/hwu");

					switch(bel_functor) {
					case "thinking": text = new String("Wait, I'm thinking"); break;
					case "list_places": text = new String("There are "+bel_arg+". Which one do you want to go to ?"); break;
					case "closest": text = new String("The closest ones are "+bel_arg+". Which one do you want to go to ?"); break;
					case "where_are_u": text = new String("Where are you ? I cannot see you"); break;
					case "found_again": text = new String("Ok I can see you again"); break;
					case "cannot_find": text = new String("I cannot find you, sorry"); break;
					case "ask_stairs": text = new String("Are you able to climb stairs ?"); break;
					case "ask_escalator": text = new String("Can you take the escalator ?"); break;
					case "able_to_see": text = new String("I think that you're seeing the place right now, good"); break;
					case "route_verbalization" : text = bel_arg; break;
					case "route_verbalization_n_vis" : text = "in this direction, "+bel_arg; break;
					case "no_place" : text = new String("The place you asked for does not exist. Do you want to go somewhere else ?"); break;
					case "come" : text = new String("Please, come in front of me"); break;
					case "move_again" : text = new String("I am sorry, we are going to move again"); break;
					case "ask_explain_again" : text = new String("Should I explain you again ?"); break;
					case "cannot_show" : text = new String("I am sorry, I cannot show you. I hope you will find your way"); break;
					case "cannot_tell_seen" : text = new String("Have you seen "+bel_arg+" ?. I'm not able to tell."); break;
					case "ask_show_again" : text = new String("Should I show you again ?"); break;
					case "sl_sorry" : text = new String("I am sorry if you did not understand. I won't explain one more time."); break;
					case "pl_sorry" : text = new String("I am sorry if you did not see. I won't show you again."); break;
					case "max_sorry" : text = new String("I am sorry, I give up, you asked me too many times something that I don't know."); break;
					case "tell_seen" : text = new String("I can tell that you've seen "+bel_arg); break;
					case "visible_target" : text = new String("Look, "+bel_arg+" is there"); break;
					case "not_visible_target" : text = new String("Look, "+bel_arg+" is in this direction"); break;
					case "hope_find_way" : text = new String("I hope you will find your way"); break;
					case "ask_understand" : text = new String("Did you understand ?"); break;
					case "happy_end" : text = new String("I am happy that I was able to help you."); break;
					case "retire" : 
						if(bel_arg == null)
							throw new IllegalArgumentException("retire speech should have an argument");
						switch(bel_arg) {
						case "unknown_words" : text = new String("You didn't told me a place where I can guide you. "); break;
						}
						if(!text.isEmpty()) {
							text = text + new String("Let's play now!"); break;
						}else {
							text = new String("Let's play now!"); break;
						}
					case "failure" : text = new String("My component for "+bel_arg+" has crashed"); break;
					default : action.setResult(false); action.setFailureReason(new Atom("unknown_string"), "no speech to say"); actionExecuted(action); return;
					}
					if(hwu_dial & !text.isEmpty()) {
						if(text.contains("?")) {
							if(bel_arg != null) {
								m_rosnode.call_dialogue_query_srv(bel_functor, bel_arg);
							}else {
								m_rosnode.call_dialogue_query_srv(bel_functor);
							}
							SuperQueryResponse dial_resp;
							do {
								dial_resp = m_rosnode.getD_query_resp();
								sleep(100);
							}while(dial_resp == null);
							String resp;
							if(dial_resp.getResult().equals("true")) {
								resp = "yes";
							}else {
								resp = "no";
							}
							try {
								getTS().getAg().addBel(Literal.parseLiteral("listen_result("+bel_functor+","+resp+")["+task_id+"]"));
							} catch (RevisionFailedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}else {
							if(bel_arg != null) {
								m_rosnode.call_dialogue_inform_srv(bel_functor, bel_arg);
							}else {
								m_rosnode.call_dialogue_inform_srv(bel_functor);
							}
						}
						action.setResult(true);
					}else {
						m_rosnode.call_speak_to_srv(human, text);
						SayResponse speak_to_resp;
						do {
							speak_to_resp = m_rosnode.getSpeak_to_resp();
							sleep(100);
						}while(speak_to_resp == null);
//						if(speak_to_resp.getSuccess()) {
//							action.setResult(true);
//						}else {
//							action.setResult(false);
//							action.setFailureReason(new Atom("speak_to_failed"), "the speech service failed");
//						}
						action.setResult(true);
					}
					actionExecuted(action);
				} else if(action_name.equals("get_route_verbalization")) {
					// to remove the extra ""
					@SuppressWarnings("unchecked")
					List<Term> route_temp = (List<Term>) action.getActionTerm().getTerm(0);
					List<String> route = new ArrayList<>();
					for(Term t : route_temp) {
						route.add(t.toString().replaceAll("^\"|\"$", ""));
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
							getTS().getAg().addBel(Literal.parseLiteral("verbalization(\""+verba+"\")["+task_id+"]"));
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
					boolean hwu_dial = m_rosnode.getParameters().getBoolean("guiding/dialogue/hwu");
					if(!hwu_dial) {
						String question = action.getActionTerm().getTerm(0).toString();
						ArrayList<String> words = new ArrayList<String>();
						for(Term term : (ListTermImpl)action.getActionTerm().getTerms().get(1)) {
							words.add(term.toString().replaceAll("^\"|\"$", ""));
						}
						if(m_rosnode.call_dialogue_as(words, max_attempt)) {
							try {
								getTS().getAg().addBel(Literal.parseLiteral("listening["+task_id+"]"));
							} catch (RevisionFailedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							dialogue_actionActionResult listening_result;
							dialogue_actionActionFeedback listening_fb;
							dialogue_actionActionFeedback listening_fb_prev = null;
							int count = 0;
							do {
								listening_result = m_rosnode.getListening_result();
								listening_fb = m_rosnode.getListening_fb();
								if(listening_fb != null & listening_fb != listening_fb_prev) {
									try {
										getTS().getAg().abolish(Literal.parseLiteral("not_exp_ans(_)"), new Unifier());
										getTS().getAg().addBel(Literal.parseLiteral("not_exp_ans("+Integer.toString(count)+")["+task_id+"]"));
									} catch (RevisionFailedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									count += 1;
									listening_fb_prev = listening_fb;
								}
								sleep(200);
							}while(listening_result == null);
							byte status = listening_result.getStatus().getStatus();
							if(listening_result.getStatus().getStatus() == GoalStatus.SUCCEEDED) {						
								try {
									getTS().getAg().addBel(Literal.parseLiteral("listen_result("+question+",\""+listening_result.getResult().getSubject()+"\")["+task_id+"]"));
								} catch (RevisionFailedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								action.setResult(true);
							}else {
								action.setResult(false);
								action.setFailureReason(new Atom("dialogue_as_failed"), "");
								if(listening_result.getStatus().getStatus() == GoalStatus.PREEMPTED)
									logger.info("dialogue_as preempted");
							}
						}else {
							action.setResult(false);
							action.setFailureReason(new Atom("dialogue_as_not_found"), "");
						}
						try {
							getTS().getAg().abolish(Literal.parseLiteral("listening"), new Unifier());
						} catch (RevisionFailedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}else {
						action.setResult(true);
					}
					actionExecuted(action);
				} else if(action_name.equals("stop_listen")) {
					
					m_rosnode.cancel_goal_dialogue();
					action.setResult(true);
					actionExecuted(action);
				} else if(action_name.equals("move_to")) {
					String frame = action.getActionTerm().getTerm(0).toString();
					Iterator<Term> action_term_it =  ((ListTermImpl) action.getActionTerm().getTerm(1)).iterator();
					List<Double> pose_values = new ArrayList<>();
					while(action_term_it.hasNext()) {
						pose_values.add(((NumberTermImpl)action_term_it.next()).solve());
					}
					action_term_it =  ((ListTermImpl) action.getActionTerm().getTerm(2)).iterator();
					while(action_term_it.hasNext()) {
						pose_values.add(((NumberTermImpl)action_term_it.next()).solve());
					}
					PoseCustom pose = new PoseCustom(pose_values);
					NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
					MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();
					PoseStamped pose_stamped = messageFactory.newFromType(PoseStamped._TYPE);
					Header header = messageFactory.newFromType(std_msgs.Header._TYPE);
					header.setFrameId(frame);
					pose_stamped.setHeader(header);
					pose_stamped.setPose(pose.getPose());
					if(m_rosnode.call_move_to_as(pose_stamped, max_attempt)) {
						MoveBaseActionResult move_to_result;
						MoveBaseActionFeedback move_to_fb;
						do {
							move_to_result = m_rosnode.getMove_to_result();
							move_to_fb = m_rosnode.getMove_to_fb();
							if(move_to_fb != null) {
								try {
									getTS().getAg().addBel(Literal.parseLiteral("fb(move_to, "+move_to_fb.getFeedback().getBasePosition().getPose().getPosition().getX()+","+
											move_to_fb.getFeedback().getBasePosition().getPose().getPosition().getY()+","+
											move_to_fb.getFeedback().getBasePosition().getPose().getPosition().getZ()+")["+task_id+"]"));
								} catch (RevisionFailedException e) {
									e.printStackTrace();
								}
							}
							sleep(200);
						}while(move_to_result == null);
						if(move_to_result.getStatus().getStatus() == GoalStatus.SUCCEEDED) {
							try {
								getTS().getAg().addBel(Literal.parseLiteral("move_goal_reached"));
							} catch (RevisionFailedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							action.setResult(true);
						}else {
							action.setResult(false);
							action.setFailureReason(new Atom("move_to_failed"), "");
						}
					}else {
						action.setResult(false);
						action.setFailureReason(new Atom("move_to_as_not_found"), "");
					}
					actionExecuted(action);
				}else if(action_name.equals("get_hatp_plan")){
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
											+task.getName()+","+agents+","+parameters+","+task.getCost()+")["+task_id+"]"));
								}else {
									getTS().getAg().addBel(Literal.parseLiteral("task("+task.getId()+","+task.getType()+","
											+task.getName()+","+agents+","+task.getCost()+")["+task_id+"]"));
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
								getTS().getAg().addBel(Literal.parseLiteral(belief+"["+task_id+"]"));
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
	
	public RouteImpl[] select_2_best_routes(List<SemanticRouteResponse> routes_resp_list) {
		RouteImpl[] best_routes = new RouteImpl[2];
		float min_cost1 = Float.MAX_VALUE;
		float min_cost2 = Float.MAX_VALUE;
		int list_size = routes_resp_list.size();
		if (list_size > 2) {
			for (SemanticRouteResponse route_resp : routes_resp_list) {
				List<Route> routes = route_resp.getRoutes();
				float[] costs = route_resp.getCosts();
				List<String> goals = route_resp.getGoals();

				for (int i = 0; i < routes.size(); i++) {
					if (costs[i] < min_cost1) {
						min_cost2 = min_cost1;
						best_routes[0].setRoute(routes.get(i).getRoute());
						best_routes[0].setGoal(goals.get(i));
						min_cost1 = costs[i];
					}else if (costs[i] < min_cost2) {
						min_cost2 = costs[i];
						best_routes[1].setRoute(routes.get(i).getRoute());
						best_routes[1].setGoal(goals.get(i));
					}
				}
			}
		}else {
			return null;
		}

		return best_routes;

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


