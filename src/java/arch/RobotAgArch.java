package arch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.ros.exception.RemoteException;
import org.ros.message.MessageListener;
import org.ros.node.NodeConfiguration;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Publisher;
import org.ros.rosjava.tf.Transform;
import org.ros.rosjava.tf.TransformFactory;
import org.ros.rosjava.tf.TransformTree;
import org.ros.rosjava_geometry.Vector3;

import actionlib_msgs.GoalStatus;
import agent.TimeBB;
import arch.actions.Action;
import arch.actions.ActionFactory;
import deictic_gestures.LookAtResponse;
import deictic_gestures.LookAtStatus;
import deictic_gestures.PointAtResponse;
import deictic_gestures.PointAtStatus;
import dialogue_as.dialogue_actionActionFeedback;
import dialogue_as.dialogue_actionActionResult;
import geometry_msgs.PoseStamped;
import hatp_msgs.PlanningRequestResponse;
import jason.RevisionFailedException;
import jason.architecture.AgArch;
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
import jason.bb.BeliefBase;
import move_base_msgs.MoveBaseActionFeedback;
import move_base_msgs.MoveBaseActionResult;
import msg_srv_impl.PoseCustom;
import msg_srv_impl.RouteImpl;
import msg_srv_impl.SemanticRouteResponseImpl;
import nao_interaction_msgs.SayResponse;
import ontologenius_msgs.OntologeniusService;
import ontologenius_msgs.OntologeniusServiceResponse;
import pepper_resources_synchronizer_msgs.MetaStateMachineRegisterResponse;
import perspectives_msgs.HasMeshResponse;
import pointing_planner.PointingPlannerResponse;
import pointing_planner.VisibilityScoreResponse;
import route_verbalization_msgs.VerbalizeRegionRouteResponse;
import rpn_recipe_planner_msgs.SupervisionServerInformActionResult;
import rpn_recipe_planner_msgs.SupervisionServerQueryActionResult;
import semantic_route_description_msgs.Route;
import semantic_route_description_msgs.SemanticRouteResponse;
import std_msgs.Header;
import std_srvs.EmptyResponse;
import std_srvs.SetBoolResponse;
import utils.Code;
import utils.QoI;
import utils.Quaternion;
import utils.Tools;

public class RobotAgArch extends ROSAgArch {

	@SuppressWarnings("unused")
	protected Logger logger = Logger.getLogger(RobotAgArch.class.getName());

	protected Publisher<std_msgs.String> human_to_monitor;
	private Publisher<std_msgs.String> look_at_events_pub;

	std_msgs.Header her;
	AgArch interact_arch = new InteractAgArch();
	double startTimeOngoingAction = -1;
	double startTimeOngoingStep = -1;
	HashMap<String,Double> actionsThreshold = new HashMap<String,Double>() {{
        put("speak", 10000.0);
        put("question", 15000.0);
        put("robot_move", 20000.0);
        put("come_closer", 30000.0);
        put("step", 20000.0);
    }};
	HashMap<String,Double> stepsThreshold = new HashMap<String,Double>() {{
        put("goal_nego", 30000.0);
        put("person_abilities", 15000.0);
        put("agents_at_right_place", 30000.0);
        put("target_explanation", 20000.0);
        put("direction_explanation", 35000.0);
        put("landmark_seen", 20000.0);
    }};
	
	String onGoingAction = "";
	String onGoingStep = "";
	String speak;
	HashMap<String, UpdateTimeBB> actionsQoI = new HashMap<String, UpdateTimeBB>();
	HashMap<String, UpdateTimeBB> tasksQoI = new HashMap<String, UpdateTimeBB>();
	String taskId = "";
	boolean inQuestion = false;
	int humanAnswer = 0;
	double onTimeTaskExecution = 1;
	double onTimeTaskExecutionPrev = 1;
	double distToGoal = 0;
	double decreasingSpeed = 2;
	float step = 0;
	ArrayList<Double> currentTaskActQoI = new ArrayList<Double>();
	private final ReadWriteLock startActionLock = new ReentrantReadWriteLock();
	
	double steps = 2;
	double taskQoIAverage = 0;
	double nbTaskQoI = 0;
	boolean firstTimeInTask = true;
	double monitorTimeAnswering = 0;
	boolean wasComingCloser = false;
	boolean wasStepping = false;
	boolean wasMoving = false;
	boolean newStep = false;
	boolean startAction = false;

	@Override
	public void init() {

		MessageListener<PointAtStatus> ml_point_at = new MessageListener<PointAtStatus>() {
			public void onNewMessage(PointAtStatus status) {
				try {
					switch (status.getStatus()) {
					case 0:
						getTS().getAg().addBel(Literal.parseLiteral("point_at(idle)"));
						break;
					case 1:
						getTS().getAg().addBel(Literal.parseLiteral("point_at(rotate)"));
						break;
					case 2:
						getTS().getAg().addBel(Literal.parseLiteral("point_at(point)"));
						break;
					case 3:
						getTS().getAg().addBel(Literal.parseLiteral("point_at(finished)"));
						break;
					}
				} catch (RevisionFailedException e) {
					Tools.getStackTrace(e);
				}

			}
		};
		rosnode.addListener("guiding/topics/point_at_status", PointAtStatus._TYPE, ml_point_at);

		MessageListener<LookAtStatus> ml_look_at = new MessageListener<LookAtStatus>()  {
			public void onNewMessage(LookAtStatus status) {
				try {
					switch (status.getStatus()) {
					case 0:
						getTS().getAg().addBel(Literal.parseLiteral("look_at(idle)"));
						break;
					case 1:
						getTS().getAg().addBel(Literal.parseLiteral("look_at(rotate)"));
						break;
					case 2:
						getTS().getAg().addBel(Literal.parseLiteral("look_at(look)"));
						break;
					case 3:
						getTS().getAg().addBel(Literal.parseLiteral("look_at(finished)"));
						break;
					}
				} catch (RevisionFailedException e) {
					Tools.getStackTrace(e);
				}

			}
		};
		rosnode.addListener("guiding/topics/look_at_status", LookAtStatus._TYPE, ml_look_at);

		human_to_monitor = rosnode.getConnectedNode().newPublisher(
				rosnode.getParameters().getString("guiding/topics/human_to_monitor"), std_msgs.String._TYPE);

		look_at_events_pub = rosnode.getConnectedNode().newPublisher(
				rosnode.getParameters().getString("/guiding/topics/look_at_events"), std_msgs.String._TYPE);

		super.init();
	}

	@Override
	public void reasoningCycleStarting() {
		Literal onGoingTask = findBel("started");
		if(!onGoingStep.isEmpty() && onGoingTask != null) {
			if(firstTimeInTask) {
//				display.insert_discontinuity("task", getRosTimeMilliSeconds());
				firstTimeInTask = false;
				startTimeOngoingStep = ((NumberTermImpl)((Literal) onGoingTask.getAnnots("add_time").get(0)).getTerm(0)).solve();
			}
			
			Literal attentive_ratio = null;
			Literal action_expectation = null;
			Literal action_efficiency = null;
			ArrayList<Literal> list = new ArrayList<Literal>();

			String id = onGoingTask.getAnnots().get(0).toString();

			if(actionsQoI.get(id) == null)
				actionsQoI.put(id, new UpdateTimeBB());
			if(tasksQoI.get(id) == null)
				tasksQoI.put(id, new UpdateTimeBB());

			// attentive ratio
			// inform & questions
			startActionLock.readLock().lock();
			if(startTimeOngoingAction != -1.0) {
				double ar = ((InteractAgArch) interact_arch).attentive_ratio(startTimeOngoingAction, getRosTimeMilliSeconds());
				startActionLock.readLock().unlock();
				attentive_ratio = literal("attentive_ratio",onGoingAction,QoI.normaFormulaMinus1To1(ar, 0, 1));
//				logger.info(attentive_ratio.toString());
				actionsQoI.get(id).add(attentive_ratio);
				list.add(attentive_ratio);
			} else {
				startActionLock.readLock().unlock();
			}
			
			// action expectations
			// questions
			if((inQuestion || humanAnswer != 0) && !onGoingAction.equals("")) {
				if(humanAnswer == 0)
					monitorTimeAnswering = Math.max(-Math.max(getRosTimeMilliSeconds() - startTimeOngoingAction - actionsThreshold.get(onGoingAction), 0)
																/ (actionsThreshold.get(onGoingAction) * decreasingSpeed) + 1 , -1);	
				else if(attentive_ratio == null)  {
					attentive_ratio = findBel(Literal.parseLiteral("attentive_ratio("+onGoingAction+",_)"), this.actionsQoI.get(id));
					actionsQoI.get(id).add(attentive_ratio);
					list.add(attentive_ratio);
				}
					
				action_expectation = literal("action_expectation", onGoingAction,monitorTimeAnswering);
//				logger.info(action_expectation.toString());
				actionsQoI.get(id).add(action_expectation);
				list.add(action_expectation);
			} else {

				// attentive ratio & action expectations & action efficiency
				// come closer
				Literal closer = findBel("adjust");
				if(humanAnswer == 0){
					if(closer != null) {
						onGoingAction = "come_closer";
						// attentive ratio
						double startTime = ((NumberTermImpl) closer.getAnnot("add_time").getTerm(0)).solve();
						double ar = ((InteractAgArch) interact_arch).attentive_ratio(startTime, getRosTimeMilliSeconds());
						attentive_ratio = literal("attentive_ratio","come_closer",QoI.normaFormulaMinus1To1(ar, 0, 1));
						list.add(attentive_ratio);
						actionsQoI.get(id).add(attentive_ratio);
//						logger.info(attentive_ratio.toString());
						// action expectations
						Literal distToGoal = findBel("dist_to_goal(_,_,_)");
						if(distToGoal != null) {
//							logger.info(distToGoal.toString());
							Transform human_pose_now = getTfTree().lookupMostRecent("map", distToGoal.getTerm(0).toString().replaceAll("^\"|\"$", ""));
							ArrayList<Double> init_pose = Tools.listTermNumbers_to_list((ListTermImpl) distToGoal.getTerm(1));
//							logger.info("init pose :"+init_pose);
							if(human_pose_now != null) {
								double h_dist_to_init_pose = Math.hypot(human_pose_now.translation.x - init_pose.get(0), 
										human_pose_now.translation.y - init_pose.get(1));
//								logger.info("dist from initial pose :"+h_dist_to_init_pose);
								ArrayList<Double> goal = Tools.listTermNumbers_to_list((ListTermImpl) distToGoal.getTerm(2));
								double init_dist_to_goal = Math.hypot(init_pose.get(0) - goal.get(0), 
										init_pose.get(1) - goal.get(1));
//								logger.info("initial dist to goal "+init_dist_to_goal);
								double h_dist_to_goal = Math.hypot(human_pose_now.translation.x - goal.get(0), 
										human_pose_now.translation.y - goal.get(1));
//								logger.info("dist to goal "+h_dist_to_goal);
								double ratio = h_dist_to_init_pose/init_dist_to_goal;
								if(h_dist_to_goal > init_dist_to_goal)
									ratio = -1 * ratio;
								action_expectation = literal("action_expectation","come_closer",ratio);
								actionsQoI.get(id).add(action_expectation);
								list.add(action_expectation);
//								logger.info(action_expectation.toString());
							}
						}

						// action efficiency
						Literal said = findBel("said(closer,_)");
						double c = 0;
						if(said != null) {
							c = ((NumberTermImpl) said.getTerm(1)).solve();
						}
						action_efficiency = literal("action_efficiency","come_closer",QoI.logaFormula(c, 2, 2) * (-1));
						actionsQoI.get(id).add(action_efficiency);
						list.add(action_efficiency);
//						logger.info(action_efficiency.toString());
						if(!wasComingCloser)
							startAction = true;
						wasComingCloser = true;
					} else if(wasComingCloser) {
//						display.insert_discontinuity("action", getRosTimeMilliSeconds());
						wasComingCloser = false;
					}

					// step on side
					Literal step = findBel("step");
					if(step != null) {
						
						onGoingAction = "step";
						// attentive ratio
						double startTime = ((NumberTermImpl) step.getAnnot("add_time").getTerm(0)).solve();
						double ar = ((InteractAgArch) interact_arch).attentive_ratio(startTime, getRosTimeMilliSeconds());
						attentive_ratio = literal("attentive_ratio","step",QoI.normaFormulaMinus1To1(ar, 0, 1));
						actionsQoI.get(id).add(attentive_ratio);
						list.add(attentive_ratio);

						// action expectations
						Literal distToGoal = findBel("dist_to_goal(_,_,_)");
						if(distToGoal != null) {
							Transform human_pose_now = getTfTree().lookupMostRecent("map", distToGoal.getTerm(0).toString().replaceAll("^\"|\"$", ""));
							ArrayList<Double> init_pose = Tools.listTermNumbers_to_list((ListTermImpl) distToGoal.getTerm(1));
							ArrayList<Double> robot_place = Tools.listTermNumbers_to_list((ListTermImpl) distToGoal.getTerm(2));

							double h_init_pose_dist_to_robot_place = Math.hypot(robot_place.get(0) - init_pose.get(0), 
									robot_place.get(1) - init_pose.get(1));

							double dist_threshold = rosnode.getParameters().getDouble("guiding/tuning_param/human_move_first_dist_th");
							double h_dist_to_robot_place = Math.hypot(human_pose_now.translation.x - robot_place.get(0), 
									human_pose_now.translation.y - robot_place.get(1));
							double value;
							if(h_dist_to_robot_place > h_init_pose_dist_to_robot_place) {
								value = QoI.normaFormul0To1(h_dist_to_robot_place, h_init_pose_dist_to_robot_place, dist_threshold);
							} else {
								value = QoI.normaFormulaMinus1To0(h_dist_to_robot_place, 0, h_init_pose_dist_to_robot_place);
							}
							action_expectation = literal("action_expectation","step",value);
							actionsQoI.get(id).add(action_expectation);	
							list.add(action_expectation);
							if(!wasStepping)
								startAction = true;
							wasStepping = true;
						}

						// action efficiency
						Literal said = findBel("said(step(_),_)");
						double c = 0;
						if(said != null) {
							c = ((NumberTermImpl) said.getTerm(1)).solve();
						}
						action_efficiency = literal("action_efficiency","step",QoI.logaFormula(c, 2, 2) * (-1));
						actionsQoI.get(id).add(action_efficiency);
						list.add(action_efficiency);
					}  else if(wasStepping) {
//						display.insert_discontinuity("action", getRosTimeMilliSeconds());
						wasStepping = false;
					}
					
					// robot moves
					Literal move = findBel("move(started)");
					Literal move_over = findBel("move(over)");
					if(move != null && move_over == null) {
						onGoingAction = "robot_move";
//						logger.info("move(started) found");
						// action expectations
						Literal distToGoal = findBel("dist_to_goal(_,_,_)");
						if(distToGoal != null) {
//							logger.info("dist_to_goal found");
							Literal robot_move_l = findBel("robot_move(_,_,_)");
							ArrayList<Double> robot_pose = null;
							if(robot_move_l != null) {
//								logger.info("robot_move found");
								robot_pose = Tools.listTermNumbers_to_list((ListTermImpl) robot_move_l.getTerm(1));
							}
							if(robot_pose != null) {
								TransformTree tfTree = getTfTree();
								Transform robot_pose_now;
								robot_pose_now = tfTree.lookupMostRecent("map", "base_footprint");
								if(robot_pose_now != null) {
									
									ArrayList<Double> init_pose = Tools.listTermNumbers_to_list((ListTermImpl) distToGoal.getTerm(1));

									double r_dist_to_init_pose = Math.hypot(robot_pose_now.translation.x - init_pose.get(0), 
											robot_pose_now.translation.y - init_pose.get(1));

									ArrayList<Double> goal = Tools.listTermNumbers_to_list((ListTermImpl) distToGoal.getTerm(2));
									double init_dist_to_goal = Math.hypot(init_pose.get(0) - goal.get(0), 
											init_pose.get(1) - goal.get(1));
									double r_dist_to_goal = Math.hypot(robot_pose_now.translation.x - goal.get(0), 
											robot_pose_now.translation.y - goal.get(1));
									double ratio = r_dist_to_init_pose/init_dist_to_goal;
									if(r_dist_to_goal > init_dist_to_goal)
										ratio = -1 * ratio;
									action_expectation = literal("action_expectation","robot_move",ratio);
									actionsQoI.get(id).add(action_expectation);
									list.add(action_expectation);
//									logger.info(action_expectation.toString());
									if(!wasMoving)
										startAction = true;
									wasMoving = true;
								}
							}
							
						}
						
					} else if(wasMoving) {
//						display.insert_discontinuity("action", getRosTimeMilliSeconds());
						wasMoving = false;
					}
				}
			}
			double sum = 0;
			for(Literal l : list) {
				sum += ((NumberTermImpl) l.getTerm(1)).solve();
			}
			double actionsQoIAverage = 0;
			Literal la = null;
			if(!list.isEmpty()) {
				double QoI = sum/list.size();
				this.actionsQoI.get(id).add(literal("qoi",onGoingAction, QoI));
				currentTaskActQoI.add(QoI);
				la = findBel(Literal.parseLiteral("qoi(_,_)"), this.actionsQoI.get(id));
			}
			actionsQoIAverage = currentTaskActQoI.stream().mapToDouble(val -> val).average().orElse(0.0);
			if(!currentTaskActQoI.isEmpty()) {
				tasksQoI.get(id).add(literal("actionsQoI",id,actionsQoIAverage));
//				logger.info("actions qoi average :"+actionsQoIAverage);
			}
			// QoI task - distance to goal and task evolution
			
			distToGoal = step/steps;
			
			onTimeTaskExecution = Math.max(-Math.max(getRosTimeMilliSeconds() - startTimeOngoingStep - stepsThreshold.get(onGoingStep), 0)/(stepsThreshold.get(onGoingStep) * decreasingSpeed) + onTimeTaskExecutionPrev , -1);
			
			tasksQoI.get(id).add(literal("var_DtG",id,distToGoal));
			tasksQoI.get(id).add(literal("taskExecutionEvolution",id,onTimeTaskExecution));
//			logger.info("dist to goal :"+distToGoal);
//			logger.info("taskExecutionEvolution :"+onTimeTaskExecution);
			tasksQoI.get(id).add(literal("qoi",id,(actionsQoIAverage+distToGoal+onTimeTaskExecution)/3.0));
				
			Literal lt = findBel(Literal.parseLiteral("qoi(_,_)"), this.tasksQoI.get(id));
//			logger.info(lt.toString());
//			display.update(null,lt, la );
			if(newStep) {
				newStep = false;
//				display.add_label(onGoingStep, lt);
			}
			if(humanAnswer != 0) {
				humanAnswer = 0;
//				display.insert_discontinuity("action", getRosTimeMilliSeconds());
			}
			if(startAction && la != null) {
				startAction = false;
//				logger.info("start action to false");
//				display.add_label(onGoingAction, la);
			}
//			logger.info(tasksQoI.get(id).toString());
//			logger.info(this.actionsQoI.get(id).toString());
			nbTaskQoI++;
			taskQoIAverage = taskQoIAverage + ( ((NumberTermImpl) lt.getTerm(1)).solve() - taskQoIAverage) / nbTaskQoI;
			Message msg = new Message("tell", getAgName(), "interac", "qoi("+id+","+Double.toString(taskQoIAverage)+")");
			try {
				sendMsg(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}



		super.reasoningCycleStarting();
	}
	
	
	public class AgRunnable implements Runnable {
		
		private ActionExec action;
		private ROSAgArch rosAgArch;
		
		public AgRunnable(ROSAgArch rosAgArch, ActionExec action) {
			this.action = action;
			this.rosAgArch = rosAgArch;
		}
		
		@Override
		public void run() {
			String action_name = action.getActionTerm().getFunctor();
			Message msg = new Message("tell", getAgName(), "supervisor", "action_started(" + action_name + ")");
			String tmp_task_id = "";
			if (action.getIntention().getBottom().getTrigger().getLiteral().getTerms() != null)
				tmp_task_id = action.getIntention().getBottom().getTrigger().getLiteral().getTerm(0).toString();
			taskID = tmp_task_id;
			try {
				sendMsg(msg);
			} catch (Exception e) {
				Tools.getStackTrace(e);
			}
			Action actionExecutable = ActionFactory.createAction(action, rosAgArch);
			actionExecutable.execute();
			
			if(!actionExecutable.isAsync())
				actionExecuted(action);
		}
	}


	@Override
	public void act(final ActionExec action) {
		executor.execute(new AgRunnable(this, action));
				
/*				
				
				
				new Runnable() {

			@Override
			public void run() {
				
				String action_name = action.getActionTerm().getFunctor();
//				logger.info("ONGOING ACTION :"+action_name);
				Message msg = new Message("tell", getAgName(), "supervisor", "action_started(" + action_name + ")");
				String tmp_task_id = "";
				if (action.getIntention().getBottom().getTrigger().getLiteral().getTerms() != null)
					tmp_task_id = action.getIntention().getBottom().getTrigger().getLiteral().getTerm(0).toString();
				taskID = tmp_task_id;
				try {
					sendMsg(msg);
				} catch (Exception e) {
					Tools.getStackTrace(e);
				}
				Action actionExecutable = ActionFactory.createAction(action_name, action, getROSAgArch());
				actionExecutable.execute();
				actionExecuted(action);
				
				// TODO check number of terms for each action
				if (action_name.equals("compute_route")) {
					// to remove the extra ""
					String from = action.getActionTerm().getTerm(0).toString();
					from = from.replaceAll("^\"|\"$", "");

					ListTerm to_list;
					Term to = (Term) action.getActionTerm().getTerm(1);
					// if there is multiple places (toilet and atm cases)
					if (to.isList()) {
						to_list = (ListTerm) to;
					}
					// if there is only one place, we convert it to a list with one element for
					// convenience
					else {
						to_list = new ListTermImpl();
						to_list.add(to);
					}
					List<SemanticRouteResponse> routes = new ArrayList<SemanticRouteResponse>();
					boolean at_least_one_ok = false;
					// we get all the possible routes for the different places
					// (we will be able then to choose between the best toilet or atm to go)
					for (Term t : to_list) {
						Map<String, Object> parameters = new HashMap<String, Object>();
						parameters.put("from", from);
						parameters.put("to", ((StringTermImpl) t).getString());
						parameters.put("persona", action.getActionTerm().getTerm(2).toString());
						parameters.put("signpost", Boolean.parseBoolean(action.getActionTerm().getTerm(3).toString()));
						// call the service to compute route
						SemanticRouteResponse resp = rosnode.callSyncService("get_route", parameters);
						if(resp != null) {
							SemanticRouteResponseImpl get_route_resp = new SemanticRouteResponseImpl(resp.getCosts(),
									resp.getGoals(), resp.getRoutes());
							if (resp.getRoutes().isEmpty()) {
								get_route_resp.setCode(Code.ERROR.getCode());
							} else {
								get_route_resp.setCode(Code.OK.getCode());
							}
							routes.add(get_route_resp);
							if (get_route_resp.getCode() == Code.ERROR.getCode()) {
								action.setResult(false);
								action.setFailureReason(new Atom("route_not_found"), "No route has been found");

							} else {
								at_least_one_ok = true;
							}
						}
					}
					if (!at_least_one_ok) {
						actionExecuted(action);
					} else {
						RouteImpl route = select_best_route(routes);
						if(route != null) {
							try {
								logger.info(route.getRoute().toString());
								String route_list = route.getRoute().stream().map(s -> "\"" + s + "\"")
										.collect(Collectors.joining(", "));
								getTS().getAg()
								.addBel(Literal.parseLiteral("route([" + route_list + "])[" + task_id + "]"));
								getTS().getAg().addBel(
										Literal.parseLiteral("target_place(\"" + route.getGoal() + "\")[" + task_id + "]"));
								action.setResult(true);
								actionExecuted(action);

							} catch (RevisionFailedException e) {
								Tools.getStackTrace(e);
							}
						}else {
							action.setResult(false);
							action.setFailureReason(new Atom("route_not_found_wo_stairs"), "No route has been found without stairs");
							actionExecuted(action);
						}
					}

				} else if (action_name.equals("human_to_monitor")) {
					String param = action.getActionTerm().getTerm(0).toString();
					param = param.replaceAll("^\"|\"$", "");
					std_msgs.String str = human_to_monitor.newMessage();
					str.setData(param);
					human_to_monitor.publish(str);
					action.setResult(true);
					actionExecuted(action);
				}  else if (action_name.equals("pause_asr_and_display_processing")) {
					ServiceResponseListener<std_srvs.EmptyResponse> respListener = new ServiceResponseListener<std_srvs.EmptyResponse>() {

						@Override
						public void onFailure(RemoteException e) {
							//							handleFailure(action, action_name, e);
							action.setResult(true);
							actionExecuted(action);
						}

						@Override
						public void onSuccess(EmptyResponse arg0) {
							action.setResult(true);
							actionExecuted(action);
						}
					};
					rosnode.callAsyncService("pause_asr", respListener, null);
					rosnode.callAsyncService("web_view_start_processing", respListener, null);
					action.setResult(true);
					actionExecuted(action);
				} else if (action_name.equals("get_onto_individual_info")) {
					String param = action.getActionTerm().getTerm(0).toString();
					String individual_o = action.getActionTerm().getTerm(1).toString();
					String individual = individual_o.replaceAll("^\"|\"$", "");
					String belief_name = action.getActionTerm().getTerm(2).toString();

					ServiceResponseListener<OntologeniusServiceResponse> respListener = new ServiceResponseListener<OntologeniusServiceResponse>() {
						public void onFailure(RemoteException e) {
							handleFailure(action, action_name, e);
						}

						public void onSuccess(OntologeniusServiceResponse onto_individual_resp) {
							OntologeniusServiceResponse places = rosnode
									.newServiceResponseFromType(OntologeniusService._TYPE);
							if (onto_individual_resp.getValues().isEmpty()) {
								places.setCode((short) Code.ERROR.getCode());
							} else {
								places.setCode((short) Code.OK.getCode());
								places.setValues(onto_individual_resp.getValues());
							}

							if (places.getCode() == Code.OK.getCode()) {
								try {
									ListTermImpl places_list = new ListTermImpl();
									for (String place : places.getValues()) {
										places_list.add(new StringTermImpl(place));
									}
									if (places_list.size() == 1) {
										if (belief_name.equals("shop_names") || belief_name.equals("shop_name"))
											getTS().getAg().addBel(
													Literal.parseLiteral(belief_name + "(" + places_list.get(0) + ")"));
										else
											getTS().getAg().addBel(Literal.parseLiteral(belief_name + "(" + individual_o
													+ "," + places_list.get(0) + ")[" + task_id + "]"));
									} else {
										if (belief_name.equals("shop_names") || belief_name.equals("shop_name"))
											getTS().getAg().addBel(
													Literal.parseLiteral(belief_name + "(" + places_list + ")"));
										else
											getTS().getAg().addBel(Literal.parseLiteral(belief_name + "(" + individual_o
													+ "," + places_list + ")[" + task_id + "]"));
									}
									action.setResult(true);
									actionExecuted(action);
								} catch (RevisionFailedException e) {
									Tools.getStackTrace(e);
								}
							} else {
								action.setResult(false);
								action.setFailureReason(new Atom("individual_not_found"), individual
										+ " has been not been found in the ontology with the param " + param);
								actionExecuted(action);
							}
						}
					};

					Map<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("action", param);
					parameters.put("param", individual);
					rosnode.callAsyncService("get_individual_info", respListener, parameters);

				} else if (action_name.equals("get_placements")) {
					// to remove the extra ""
					ArrayList<String> params = new ArrayList<String>();
					for (Term term : action.getActionTerm().getTerms()) {
						params.add(term.toString().replaceAll("^\"|\"$", ""));
					}

					String target = params.get(0);
					String direction = params.get(1);
					String human = params.get(2);
					int tar_is_dir = Integer.parseInt(params.get(3));

					ServiceResponseListener<PointingPlannerResponse> respListener = new ServiceResponseListener<PointingPlannerResponse>() {

						public void onFailure(RemoteException e) {
							nodeConfiguration = NodeConfiguration.newPrivate();
							messageFactory = nodeConfiguration.getTopicMessageFactory();
							PointingPlannerResponse placements_resp = messageFactory
									.newFromType(PointingPlannerResponse._TYPE);
							placements_resp.setPointedLandmarks(new ArrayList<String>());
							pointPlan(placements_resp, task_id, human, tar_is_dir, target, direction, action);
						}

						public void onSuccess(PointingPlannerResponse placements_resp) {
							pointPlan(placements_resp, task_id, human, tar_is_dir, target, direction, action);
						}

					};

					Map<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("targetlandmark", target);
					parameters.put("directionlandmark", direction);
					parameters.put("human", human);
					rosnode.callAsyncService("pointing_planner", respListener, parameters);

				} else if (action_name.equals("has_mesh")) {
					// to remove the extra ""
					final String param = action.getActionTerm().getTerm(0).toString().replaceAll("^\"|\"$", "");

					ServiceResponseListener<HasMeshResponse> respListener = new ServiceResponseListener<HasMeshResponse>() {
						public void onFailure(RemoteException e) {
							handleFailure(action, action_name, e);
						}

						public void onSuccess(HasMeshResponse has_mesh) {
							action.setResult(has_mesh.getHasMesh());
							if (!has_mesh.getHasMesh()) {
								if (has_mesh.getSuccess()) {
									action.setFailureReason(new Atom("has_no_mesh"), param + " does not have a mesh");
								} else {
									action.setFailureReason(new Atom("srv_has_mesh_failed"), "has_mesh service failed");
								}
							}
							actionExecuted(action);
						}
					};

					Map<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("world", "base");
					parameters.put("name", param);
					rosnode.callAsyncService("has_mesh", respListener, parameters);

				} else if (action_name.equals("can_be_visible")) {
					// to remove the extra ""
					final String human = action.getActionTerm().getTerm(0).toString().replaceAll("^\"|\"$", "");
					final String place = action.getActionTerm().getTerm(1).toString().replaceAll("^\"|\"$", "");

					ServiceResponseListener<VisibilityScoreResponse> respListener = new ServiceResponseListener<VisibilityScoreResponse>() {
						public void onFailure(RemoteException e) {
							handleFailure(action, action_name, e);
						}

						public void onSuccess(VisibilityScoreResponse vis_resp) {
							if (vis_resp.getIsVisible()) {
								action.setResult(true);
								try {
									getTS().getAg().addBel(Literal.parseLiteral(
											"canBeVisibleFor(\"" + place + "\"," + human + ")[" + task_id + "]"));
								} catch (RevisionFailedException e) {
									Tools.getStackTrace(e);
								}
							} else {
								try {
									getTS().getAg().addBel(Literal.parseLiteral(
											"~canBeVisibleFor(\"" + place + "\"," + human + ")[" + task_id + "]"));
								} catch (RevisionFailedException e) {
									// TODO Auto-generated catch block
									Tools.getStackTrace(e);
								}
								action.setResult(false);
								action.setFailureReason(new Atom("not_visible"), place + " is not visible");
							}
							actionExecuted(action);
						}
					};

					Map<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("agentname", human);
					parameters.put("targetname", place);
					rosnode.callAsyncService("is_visible", respListener, parameters);

				} else if (action_name.equals("point_at")) {
					// to remove the extra ""
					final String frame = action.getActionTerm().getTerm(0).toString().replaceAll("^\"|\"$", "");
					boolean with_head = Boolean.parseBoolean(action.getActionTerm().getTerm(1).toString());
					boolean with_base = Boolean.parseBoolean(action.getActionTerm().getTerm(2).toString());

					ServiceResponseListener<PointAtResponse> respListenerP = new ServiceResponseListener<PointAtResponse>() {
						public void onFailure(RemoteException e) {
							handleFailure(action, action_name, e);
						}

						public void onSuccess(PointAtResponse response) {
							action.setResult(response.getSuccess());
							if (!action.getResult())
								action.setFailureReason(new Atom("point_at_failed"),
										"the pointing failed for " + frame);
							actionExecuted(action);
						}
					};
					Map<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("point", rosnode.build_point_stamped(frame));
					parameters.put("withhead", with_head);
					parameters.put("withbase", with_base);

					rosnode.callAsyncService("point_at", respListenerP, parameters);

				} else if(action_name.equals("enable_animated_speech")){
					boolean b = Boolean.parseBoolean(action.getActionTerm().getTerm(0).toString());
					ServiceResponseListener<SetBoolResponse> respListenerA = new ServiceResponseListener<SetBoolResponse>() {
						public void onFailure(RemoteException e) {
							//							handleFailure(action, action_name, e);
						}

						public void onSuccess(SetBoolResponse response) {
							action.setResult(response.getSuccess());
							if (!action.getResult())
								action.setFailureReason(new Atom("ena_animated_speech_failed"), "ena/disable animated say failed: "+response.getMessage());
							actionExecuted(action);	
						}
					};
					Map<String, Object> parametersA = new HashMap<String, Object>();
					parametersA.put("data", b);

					rosnode.callAsyncService("enable_animated_speech", respListenerA, parametersA);
					action.setResult(true);
					actionExecuted(action);	


				} else if (action_name.equals("face_human")) {
					String id = action_name;
					// to remove the extra ""
					String frame = action.getActionTerm().getTerm(0).toString();
					frame = frame.replaceAll("^\"|\"$", "");

					TransformTree tfTree = getTfTree();
					Transform transform;
					String frame1 = "base_link";
					if (tfTree.canTransform(frame1, frame)) {
						transform = tfTree.lookupMostRecent(frame1, frame);
						if(transform != null) {
							float d = (float) Math.atan2(transform.translation.y, transform.translation.x);

							Map<String, Object> parameters = new HashMap<String, Object>();
							parameters.put("statemachinepepperbasemanager", rosnode.build_state_machine_pepper_base_manager(id, (float) d));
							parameters.put("header", rosnode.build_meta_header());

							MetaStateMachineRegisterResponse face_resp = rosnode.callSyncService("pepper_synchro", parameters);

							//							sleep(5000);
							//							Map<String, Object> params = new HashMap<String, Object>();
							//							params.put("posturename", "StandInit");
							//							params.put("speed", (float) 0.8);
							//							GoToPostureResponse go_to_posture_resp = rosnode.callSyncService("stand_pose",
							//									params);
							//							action.setResult(go_to_posture_resp != null);
							action.setResult(true);
							if(face_resp == null) {
								action.setFailureReason(new Atom("cannot_face_human"), "Service Failure, face human failed for " + frame);
								action.setResult(false);
							} 
						}else {
							action.setResult(false);
							action.setFailureReason(new Atom("cannot_face_human"), "Null transform, face human failed for " + frame);
						}

					} else {
						action.setResult(false);
						action.setFailureReason(new Atom("cannot_face_human"), "Cannot Transform, face human failed for " + frame);
					}
					actionExecuted(action);

				} else if (action_name.equals("rotate")) {
					String id = action_name;
					// to remove the extra ""
					ListTerm quaternion = (ListTerm) action.getActionTerm().getTerm(0);

					Quaternion q = new Quaternion(((NumberTermImpl) quaternion.get(0)).solve(),
							((NumberTermImpl) quaternion.get(1)).solve(), ((NumberTermImpl) quaternion.get(2)).solve(),
							((NumberTermImpl) quaternion.get(3)).solve());
					double d = q.getYaw();

					Map<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("statemachinepepperbasemanager", rosnode.build_state_machine_pepper_base_manager(id, (float) d));
					parameters.put("header", rosnode.build_meta_header());

					MetaStateMachineRegisterResponse response = rosnode.callSyncService("pepper_synchro", parameters);

					action.setResult(response != null);
					if(response == null) {
						action.setFailureReason(new Atom("cannot_rotate"), "rotation failed");
					}
					actionExecuted(action);


				} else if (action_name.equals("look_at")) {
					// to remove the extra ""
					final String frame = action.getActionTerm().getTerm(0).toString().replaceAll("^\"|\"$", "");
					boolean with_base = Boolean.parseBoolean(action.getActionTerm().getTerm(2).toString());
					ServiceResponseListener<LookAtResponse> respListener = new ServiceResponseListener<LookAtResponse>() {
						public void onFailure(RemoteException e) {
							handleFailure(action, action_name, e);
						}

						public void onSuccess(LookAtResponse look_at_resp) {
							action.setResult(look_at_resp.getSuccess());
							if (!look_at_resp.getSuccess()) {
								action.setFailureReason(new Atom("look_at_failed"), "the look at failed for " + frame);
							}
							actionExecuted(action);
						}
					};
					Map<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("point", rosnode.build_point_stamped(action, frame));
					parameters.put("withbase", with_base);
					rosnode.callAsyncService("look_at", respListener, parameters);

				} else if (action_name.equals("text2speech")) {
					Literal bel = (Literal) action.getActionTerm().getTerm(0);
					text2speech(bel, task_id, action);
				} else if (action_name.equals("get_route_verbalization")) {
					// to remove the extra ""
					@SuppressWarnings("unchecked")
					List<Term> route_temp = (List<Term>) action.getActionTerm().getTerm(0);
					List<String> route = new ArrayList<>();
					for (Term t : route_temp) {
						route.add(t.toString().replaceAll("^\"|\"$", ""));
					}
					String robot_place = action.getActionTerm().getTerm(1).toString();
					robot_place = robot_place.replaceAll("^\"|\"$", "");
					String place = action.getActionTerm().getTerm(2).toString();
					place = place.replaceAll("^\"|\"$", "");

					ServiceResponseListener<VerbalizeRegionRouteResponse> respListener = new ServiceResponseListener<VerbalizeRegionRouteResponse>() {
						public void onFailure(RemoteException e) {
							handleFailure(action, action_name, e);
						}

						public void onSuccess(VerbalizeRegionRouteResponse verba_resp) {
							String verba = new String(verba_resp.getRegionRoute());
							if (verba_resp.getSuccess() & verba != "") {
								action.setResult(true);
								try {
									getTS().getAg().addBel(
											Literal.parseLiteral("verbalization(\"" + verba + "\")[" + task_id + "]"));
								} catch (RevisionFailedException e) {
									// TODO Auto-generated catch block
									Tools.getStackTrace(e);
								}
							} else {
								action.setResult(false);
								action.setFailureReason(new Atom("route_verba_failed"),
										"the route verbalization service failed");
							}
							actionExecuted(action);
						}
					};

					Map<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("route", route);
					parameters.put("startplace", robot_place);
					parameters.put("goalshop", place);
					rosnode.callAsyncService("route_verbalization", respListener, parameters);

				} else if (action_name.equals("listen")) {
					boolean hwu_dial = rosnode.getParameters().getBoolean("guiding/dialogue/hwu");
					if (!hwu_dial) {
						inQuestion = true;
						String question = action.getActionTerm().getTerm(0).toString();
						ArrayList<String> words = new ArrayList<String>();
						if(action.getActionTerm().getTerms().get(1).isList()) {
							for (Term term : (ListTermImpl) action.getActionTerm().getTerms().get(1)) {
								words.add(term.toString().replaceAll("^\"|\"$", ""));
							}
						}else {
							words.add(action.getActionTerm().getTerms().get(1).toString().replaceAll("^\"|\"$", ""));
						}
						rosnode.call_dialogue_as(words);
						try {
							getTS().getAg().addBel(Literal.parseLiteral("listening[" + task_id + "]"));
						} catch (RevisionFailedException e) {
							// TODO Auto-generated catch block
							Tools.getStackTrace(e);
						}
						dialogue_actionActionResult listening_result;
						dialogue_actionActionFeedback listening_fb;
						dialogue_actionActionFeedback listening_fb_prev = null;
						int count = 0;
						do {
							listening_result = rosnode.getListening_result();
							listening_fb = rosnode.getListening_fb();
							if (listening_fb != null & listening_fb != listening_fb_prev) {
								try {
									getTS().getAg().abolish(Literal.parseLiteral("not_exp_ans(_)"), new Unifier());
									getTS().getAg().addBel(Literal.parseLiteral(
											"not_exp_ans(" + Integer.toString(count) + ")[" + task_id + "]"));
								} catch (RevisionFailedException e) {
									// TODO Auto-generated catch block
									Tools.getStackTrace(e);
								}
								count += 1;
								listening_fb_prev = listening_fb;
							}
							sleep(200);
						} while (listening_result == null || listening_result.getStatus().getStatus() != GoalStatus.SUCCEEDED);
						byte status = listening_result.getStatus().getStatus();
						try {
							getTS().getAg().addBel(Literal.parseLiteral("listen_result(" + question + ",\""
									+ listening_result.getResult().getSubject() + "\")[" + task_id + "]"));
						} catch (RevisionFailedException e) {
							// TODO Auto-generated catch block
							Tools.getStackTrace(e);
						}
						action.setResult(true);
						try {
							getTS().getAg().abolish(Literal.parseLiteral("listening"), new Unifier());
						} catch (RevisionFailedException e) {
							// TODO Auto-generated catch block
							Tools.getStackTrace(e);
						}
					} else {
						action.setResult(true);
					}
					actionExecuted(action);
				} else if (action_name.equals("look_at_events")) {
					String event = action.getActionTerm().getTerm(0).toString();
					event = event.replaceAll("^\"|\"$", "");
					std_msgs.String str = look_at_events_pub.newMessage();
					str.setData(event);
					look_at_events_pub.publish(str);
					action.setResult(true);
					actionExecuted(action);
				} else if (action_name.equals("move_to")) {
					String frame = action.getActionTerm().getTerm(0).toString();
					Iterator<Term> action_term_it = ((ListTermImpl) action.getActionTerm().getTerm(1)).iterator();
					List<Double> pose_values = new ArrayList<>();
					while (action_term_it.hasNext()) {
						pose_values.add(((NumberTermImpl) action_term_it.next()).solve());
					}
					action_term_it = ((ListTermImpl) action.getActionTerm().getTerm(2)).iterator();
					while (action_term_it.hasNext()) {
						pose_values.add(((NumberTermImpl) action_term_it.next()).solve());
					}
					PoseCustom pose = new PoseCustom(pose_values);
					nodeConfiguration = NodeConfiguration.newPrivate();
					messageFactory = nodeConfiguration.getTopicMessageFactory();
					PoseStamped pose_stamped = messageFactory.newFromType(PoseStamped._TYPE);
					Header header = messageFactory.newFromType(std_msgs.Header._TYPE);
					header.setFrameId(frame);
					pose_stamped.setHeader(header);
					pose_stamped.setPose(pose.getPose());
					rosnode.call_move_to_as(pose_stamped);
					MoveBaseActionResult move_to_result;
					MoveBaseActionFeedback move_to_fb;
					do {
						move_to_result = rosnode.getMove_to_result();
						//							move_to_fb = rosnode.getMove_to_fb();
						//							if(move_to_fb != null) {
						//								try {
						//									getTS().getAg().addBel(Literal.parseLiteral("fb(move_to, "+move_to_fb.getFeedback().getBasePosition().getPose().getPosition().getX()+","+
						//											move_to_fb.getFeedback().getBasePosition().getPose().getPosition().getY()+","+
						//											move_to_fb.getFeedback().getBasePosition().getPose().getPosition().getZ()+")["+task_id+"]"));
						//								} catch (RevisionFailedException e) {
						//									Tools.getStackTrace(e);
						//								}
						//							}
						sleep(200);
					} while (move_to_result == null);
					if (move_to_result.getStatus().getStatus() == GoalStatus.SUCCEEDED) {
						try {
							getTS().getAg().addBel(Literal.parseLiteral("move_goal_reached"));
						} catch (RevisionFailedException e) {
							// TODO Auto-generated catch block
							Tools.getStackTrace(e);
						}
						action.setResult(true);
					} else {
						action.setResult(false);
						action.setFailureReason(new Atom("move_to_failed"), "");
					}
					actionExecuted(action);
				} else if (action_name.equals("get_hatp_plan")) {
					String task_name = action.getActionTerm().getTerm(0).toString();
					task_name = task_name.replaceAll("^\"|\"$", "");
					ServiceResponseListener<PlanningRequestResponse> respListener = new ServiceResponseListener<PlanningRequestResponse>() {
						public void onFailure(RemoteException e) {
							handleFailure(action, action_name, e);
						}

						public void onSuccess(PlanningRequestResponse resp) {
							hatp_msgs.Plan plan = resp.getSolution();
							if (plan.getReport().equals("OK")) {
								action.setResult(true);
								try {
									for (hatp_msgs.Task task : plan.getTasks()) {
										String agents = Tools.array_2_str_array(task.getAgents());
										if (!task.getParameters().isEmpty()) {
											String parameters = Tools.array_2_str_array(task.getParameters());
											getTS().getAg().addBel(Literal.parseLiteral("task(" + task.getId() + ","
													+ task.getType() + "," + task.getName() + "," + agents + ","
													+ parameters + "," + task.getCost() + ")[" + task_id + "]"));
										} else {
											getTS().getAg()
											.addBel(Literal.parseLiteral("task(" + task.getId() + ","
													+ task.getType() + "," + task.getName() + "," + agents + ","
													+ task.getCost() + ")[" + task_id + "]"));
										}
									}
									for (hatp_msgs.StreamNode stream : plan.getStreams()) {
										String belief = "stream(" + stream.getTaskId();
										if (stream.getPredecessors().length != 0) {
											String pred = Tools.array_2_str_array(stream.getPredecessors());
											belief = belief + "," + pred;
										}
										if (stream.getSuccessors().length != 0) {
											String succ = Tools.array_2_str_array(stream.getSuccessors());
											belief = belief + "," + succ;
										}
										belief = belief + ")";
										getTS().getAg().addBel(Literal.parseLiteral(belief + "[" + task_id + "]"));
									}
								} catch (RevisionFailedException e) {
									// TODO Auto-generated catch block
									Tools.getStackTrace(e);
								}
							} else {
								action.setResult(false);
								action.setFailureReason(new Atom("no_plan_found"),
										"hatp planner could not find any feasible plan");
							}
							actionExecuted(action);
						}
					};

					List<String> params = new ArrayList<>();
					if (action.getActionTerm().getArity() > 1) {
						@SuppressWarnings("unchecked")
						List<Term> parameters_temp = (List<Term>) action.getActionTerm().getTerm(1);
						for (Term t : parameters_temp) {
							params.add(t.toString());
						}
					}

					Map<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("request", rosnode.build_hatp_request(task_name, "plan", params));
					rosnode.callAsyncService("hatp_planner", respListener, parameters);
				} else {
					action.setResult(false);
					action.setFailureReason(new Atom("act_not_found"), "no action " + action_name + " is implemented");
					actionExecuted(action);
				}

			}
		});*/

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
			if(best_route.getRoute() == null) {
				best_route = null;
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
					} else if (costs[i] < min_cost2) {
						min_cost2 = costs[i];
						best_routes[1].setRoute(routes.get(i).getRoute());
						best_routes[1].setGoal(goals.get(i));
					}
				}
			}
		} else {
			return null;
		}

		return best_routes;

	}

	public void pointPlan(PointingPlannerResponse placements_result, String task_id, String human, int tar_is_dir, String target, String direction, ActionExec action ) {
		try {
			if (placements_result != null && !placements_result.getPointedLandmarks().isEmpty()) {
				PoseCustom robot_pose = new PoseCustom(placements_result.getRobotPose().getPose());
				String r_frame = placements_result.getRobotPose().getHeader().getFrameId();
				PoseCustom human_pose = new PoseCustom(placements_result.getHumanPose().getPose());
				String h_frame = placements_result.getHumanPose().getHeader().getFrameId();
				TransformTree tfTree = getTfTree();
				Transform robot_pose_now;
				robot_pose_now = tfTree.lookupMostRecent("map", "base_footprint");
				if(robot_pose_now != null) {
					double r_dist_to_new_pose = Math.hypot(
							robot_pose_now.translation.x - robot_pose.getPosition().getX(),
							robot_pose_now.translation.y - robot_pose.getPosition().getY());
					logger.info("robot dist to new pose :"+r_dist_to_new_pose);
					if (r_dist_to_new_pose > rosnode.getParameters()
							.getDouble("guiding/tuning_param/robot_should_move_dist_th")) {

						getTS().getAg().addBel(Literal.parseLiteral("robot_move(" + r_frame + ","
								+ robot_pose.toString() + ")[" + task_id + "]"));

						Transform human_pose_now = tfTree.lookupMostRecent("map", human);
						if(human_pose_now != null) {
							double h_dist_to_new_pose = Math.hypot(
									human_pose_now.translation.x - robot_pose.getPosition().getX(),
									human_pose_now.translation.y - robot_pose.getPosition().getY());

							logger.info("human pose now :"+human_pose_now);
							logger.info("dist future robot pose and human now :"+h_dist_to_new_pose);

							if (h_dist_to_new_pose < rosnode.getParameters()
									.getDouble("guiding/tuning_param/human_move_first_dist_th")) {

								String side;
								geometry_msgs.Vector3 vector_msg = messageFactory
										.newFromType(geometry_msgs.Vector3._TYPE);
								// isLeft from robot view then it is right from human view
								side = Tools.isLeft(TransformFactory.vector2msg(robot_pose_now.translation),
										TransformFactory.vector2msg(human_pose_now.translation),
										Vector3.fromPointMessage(human_pose.getPosition())
										.toVector3Message(vector_msg)) ? "right" : "left";
								getTS().getAg().addBel(
										Literal.parseLiteral("human_first(" + side + ")[" + task_id + "]"));

							}
						}
					}else {

						getTS().getAg().addBel(Literal.parseLiteral("robot_turn(" + r_frame + ", ["
								+ robot_pose_now.translation.x + ","+ robot_pose_now.translation.y + "," + robot_pose_now.translation.z+ "], ["  
								+ robot_pose.getOrientation().getX()+ "," + robot_pose.getOrientation().getY()+ ","
								+ robot_pose.getOrientation().getZ()+ "," + robot_pose.getOrientation().getW()+ "])[" + task_id + "]"));
					}
				}else {
					logger.info("robot pose now = null");
					getTS().getAg().addBel(Literal.parseLiteral("robot_move(" + r_frame + ","
							+ robot_pose.toString() + ")[" + task_id + "]"));
				}
				getTS().getAg().addBel(Literal.parseLiteral("robot_pose(" + r_frame + ","
						+ robot_pose.toString() + ")[" + task_id + "]"));
				getTS().getAg().addBel(Literal.parseLiteral("human_pose(" + h_frame + ","
						+ human_pose.toString() + ")[" + task_id + "]"));
				int nb_ld_to_point = placements_result.getPointedLandmarks().size();
				for (int i = 0; i < nb_ld_to_point; i++) {
					String ld = placements_result.getPointedLandmarks().get(i);
					if (tar_is_dir == 0) {
						if (ld.equals(target)) {
							getTS().getAg().addBel(Literal.parseLiteral(
									"target_to_point(\"" + ld + "\")[" + task_id + "]"));
						} else if (ld.equals(direction)) {
							getTS().getAg().addBel(Literal
									.parseLiteral("dir_to_point(\"" + ld + "\")[" + task_id + "]"));
						}
					} else {
						if (ld.equals(target)) {
							getTS().getAg().addBel(Literal
									.parseLiteral("dir_to_point(\"" + ld + "\")[" + task_id + "]"));
						}
					}
				}
				getTS().getAg().addBel(Literal.parseLiteral("ld_to_point[" + task_id + "]"));
			} else {
				getTS().getAg().addBel(Literal.parseLiteral("~ld_to_point[" + task_id + "]"));
			}
		} catch (RevisionFailedException e) {
			Tools.getStackTrace(e);
		}
		action.setResult(true);
		actionExecuted(action);
	}

	public void text2speech(Literal bel, String task_id, ActionExec action) {
//		logger.info("SPEAK");
		// to remove the extra ""
		String text = "";
		String bel_functor = bel.getFunctor();
		String bel_arg = null;
		boolean hwu_dial = rosnode.getParameters().getBoolean("guiding/dialogue/hwu");
		if (bel.getTerms().size() == 1) {
			bel_arg = bel.getTerms().get(0).toString();
			if (hwu_dial)
				bel_arg = bel_arg.replaceAll("^\"|\"$", "");
			else
				bel_arg = bel_arg.replaceAll("^\"|\"$", "").replaceAll("\\[", "").replaceAll("\\]", "");
		}

		switch (bel_functor) {
		case "hello":
			text = new String("Hello ! Nice to meet you");
			break;
		case "goodbye":
			text = new String("Goodbye");
			break;
		case "localising":
			text = new String("I am going to my charging station now.");
			break;
		case "closer":
			text = new String("Can you come closer, please");
			break;
		case "move_closer":
			text = new String("Can you take another step forward, please");
			break;
		case "thinking":
			text = new String("Wait, I'm thinking");
			break;
		case "list_places":
			text = new String("There are " + bel_arg + ". Which one do you want to go to ?");
			break;
		case "where_are_u":
			text = new String("Where are you, I cannot see you");
			break;
		case "found_again":
			text = new String("Ok I can see you again");
			break;
		case "cannot_find":
			text = new String("I cannot find you, sorry");
			break;
		case "ask_stairs":
			text = new String("It is upstairs. Are you able to climb stairs ?");
			break;
		case "ask_escalator":
			text = new String("Can you take the escalator ?");
			break;
		case "stairs":
			text = new String("I'm sorry, no route exists without stairs to go there");
			break;
		case "no_way":
			text = new String("I'm sorry, I cannot find a way to go there");
			break;
		case "able_to_see":
			text = new String("I think that you're seeing the place right now, good");
			break;
		case "explain_route":
			text = new String("Now, I'm going to explain to you the route");
			break;
		case "route_verbalization":
			text = bel_arg;
			break;
		case "route_verbalization_n_vis":
			text = "in this direction, " + bel_arg;
			break;
		case "no_place":
			text = new String("The place you asked for does not exist");
			break;
		case "get_attention":
			text = new String("Hey are you listening");
			break;
		case "continue_anyway":
			text = new String("Ok, I'll continue anyway");
			break;
		case "going_to_move":
			text = new String("I'm going to move so I can show you");
			break;
		case "step":
			text = new String("Can you make a few steps on your " + bel_arg + ", please");
			if(bel_arg.equals("left")) {
				bel_functor = "step_left";
			}else {
				bel_functor = "step_right";
			}
			bel_arg = null;
			break;
		case "step_more":
			text = new String("Can you move a bit more on your " + bel_arg + ", please");
			if(bel_arg.equals("left")) {
				bel_functor = "step_more_left";
			}else {
				bel_functor = "step__more_right";
			}
			bel_arg = null;
			break;
		case "cannot_move":
			text = new String("I'm sorry I cannot move, I'll try my best to show you from there");
			break;
		case "come":
			text = new String("Please, come in front of me");
			break;
		case "move_again":
			text = new String("I am sorry, we are going to move again");
			break;
		case "ask_explain_again":
			text = new String("Should I explain you again ?");
			break;
		case "cannot_show":
			text = new String("I am sorry, I cannot show you. I hope you will find your way");
			break;
		case "cannot_tell_seen":
			text = new String("Have you seen " + bel_arg + " ?");
			break;
		case "ask_show_again":
			text = new String("Should I show you again ?");
			break;
		case "sl_sorry":
			text = new String("I am sorry if you did not understand. I won't explain one more time.");
			break;
		case "pl_sorry":
			text = new String("I am sorry if you did not see. I won't show you again.");
			break;
		case "max_sorry":
			text = new String(
					"I am sorry, I give up, you asked me too many times something that I don't know.");
			break;
		case "tell_seen":
			text = new String("I can tell that you've seen " + bel_arg);
			break;
		case "visible_target":
			text = new String("Look, " + bel_arg + " is there");
			break;
		case "not_visible_target":
			text = new String(bel_arg + " is not visible from here but it is in this direction");
			break;
		case "hope_find_way":
			text = new String("I hope you will find your way");
			break;
		case "ask_understand":
			text = new String("Did you understand ?");
			break;
		case "generic":
			text = new String("Do you mean " + bel_arg + "?");
			break;
		case "happy_end":
			text = new String("I am happy that I was able to help you.");
			break;
		case "retire":
			if (bel_arg == null)
				throw new IllegalArgumentException("retire speech should have an argument");
			switch (bel_arg) {
			case "unknown_words":
				text = new String("You didn't told me a place where I can guide you. ");
				break;
			}
			if (!text.isEmpty()) {
				text = text + new String("Let's play now!");
				break;
			} else {
				text = new String("Let's play now!");
				break;
			}
		case "failed":
			if(bel_arg != null)
				text = new String(bel_arg);
			else
				text = "failed";
			break;
		case "succeeded":
			text = new String("succeeded");
			break;
		default:
			action.setResult(false);
			action.setFailureReason(new Atom("unknown_string"), "no speech to say");
			actionExecuted(action);
			return;
		}
		if (hwu_dial & !text.isEmpty()) {
			boolean result = true;
			if(!bel_functor.equals("where_are_u") && !bel_functor.equals("cannot_find") && !bel_functor.equals("going_to_move")
					&& !bel_functor.contains("step") && !bel_functor.contains("closer") && !bel_functor.contains("come")) {
				start_action(text);
				speak = bel.toString();
			}
			if (text.contains("?")) {
				inQuestion = true;
				humanAnswer = 0;
				if(!bel_functor.equals("list_places")) {
					rosnode.call_dialogue_as_query("clarification." + bel_functor, bel_arg);
				} else {
					rosnode.call_dialogue_as_query("disambiguation", bel_arg);
				}
				SupervisionServerQueryActionResult listening_result = wait_dialogue_as_answer(bel_functor, task_id);
				inQuestion = false;
				if(listening_result == null) {
					humanAnswer = -1;
					result = false;
					action.setFailureReason(new Atom("dialogue_no_return"), "dialogue did not returned");
				} else if(listening_result.getStatus().getStatus() == GoalStatus.PREEMPTED) {
					humanAnswer = -1;
					result = false;
					action.setFailureReason(new Atom("dialogue_preempted"), "dialogue preempted");
				}else if(listening_result.getStatus().getStatus() == GoalStatus.SUCCEEDED) {
					humanAnswer = 1;
					result = true;
				}else {
					humanAnswer = -1;
					result = false;
					action.setFailureReason(new Atom("dialogue_failure"), "dialogue failure");
				}
				try {
					getTS().getAg().abolish(Literal.parseLiteral("listening"), new Unifier());
				} catch (RevisionFailedException e) {
					Tools.getStackTrace(e);
				}

			} else {
				String sentence_code = bel_functor;
				if(!bel_functor.equals("failed") && !bel_functor.equals("succeeded"))
					sentence_code = "verbalisation." + bel_functor;
				logger.info(sentence_code);
				rosnode.call_dialogue_as_inform(sentence_code, bel_arg);
				SupervisionServerInformActionResult listening_result;
				do {
					listening_result = rosnode.getListening_result_inform();
					sleep(200);
				} while (listening_result == null || listening_result.getStatus().getStatus() != GoalStatus.SUCCEEDED);
				if(listening_result.getStatus().getStatus() == GoalStatus.PREEMPTED) {
					result = false;
					action.setFailureReason(new Atom("dialogue_preempted"), "dialogue preempted");
				}else if(listening_result.getStatus().getStatus() == GoalStatus.SUCCEEDED) {
					result = true;
				}else {
					result = false;
					action.setFailureReason(new Atom("dialogue_failure"), "dialogue failure");
				}	
			}
			pause_asr();
			display_processing();
			action.setResult(result);
			if(!bel_functor.equals("where_are_u") && !bel_functor.equals("cannot_find") && !bel_functor.equals("going_to_move")
					&& !bel_functor.contains("step") && !bel_functor.contains("closer") && !bel_functor.contains("come")) {
				if(humanAnswer != 0) {
					startActionLock.writeLock().lock();
					startTimeOngoingAction = -1;
					startActionLock.writeLock().unlock();
				} else
					end_action();
			}
		} else {
//			if(!bel_functor.equals("where_are_u") && !bel_functor.equals("cannot_find") && !bel_functor.equals("going_to_move")
//					&& !bel_functor.contains("step") && !bel_functor.contains("closer")) {
//				startTimeOngoingAction = getRosTimeMilliSeconds();
//				speak = bel.toString();
//			}
			if (!text.equals("succeeded")) {
				Map<String, Object> parameters = new HashMap<String, Object>();
				parameters.put("text", text);
				SayResponse speak_to_resp = rosnode.callSyncService("speak_to", parameters);
				action.setResult(speak_to_resp != null);
			}else {
				action.setResult(true);
			}
			end_action();
		}
//		logger.info("END SPEAK");
		actionExecuted(action);
	}

	public SupervisionServerQueryActionResult wait_dialogue_as_answer(String s, String task_id) {
		try {
			getTS().getAg().addBel(Literal.parseLiteral("listening[" + task_id + "]"));
		} catch (RevisionFailedException e) {
			Tools.getStackTrace(e);
		}
		SupervisionServerQueryActionResult listening_result;
		double start = getRosTimeMilliSeconds();
		do {
			listening_result = rosnode.getListening_result_query();
			sleep(200);
		} while (listening_result == null && getRosTimeSeconds() - start < 15);
		if(listening_result == null) {
			try {
				getTS().getAg().addBel(Literal.parseLiteral("preempted("+task_id+")"));
			} catch (RevisionFailedException e) {
				e.printStackTrace();
			}
		} else {
			try {
				String resp;
				if (listening_result.getResult().getResult().equals("true")) {
					resp = "yes";
				} else if (listening_result.getResult().getResult().equals("false")) {
					resp = "no";
				} else {
					resp = listening_result.getResult().getResult();
				}
				if(listening_result.getStatus().getStatus() == GoalStatus.SUCCEEDED) {
					getTS().getAg().addBel(Literal.parseLiteral("listen_result(" + s + ",\""
							+ resp + "\")[" + task_id + "]"));
				}
			} catch (RevisionFailedException e) {
				Tools.getStackTrace(e);
			}
		}
		return listening_result;

	}


	@Override
	public void actionExecuted(ActionExec act) {
		String action_name = act.getActionTerm().getFunctor();
		Message msg;
		if (act.getResult()) {
			msg = new Message("tell", getAgName(), "supervisor", "action_over(" + action_name + ")");
		} else {
			// msg = new Message("tell", getAgName(), "supervisor",
			// "action_failed("+action_name+","+new
			// StringTermImpl(act.getFailureReason().toString())+")");
			if (act.getFailureReason() != null)
				msg = new Message("tell", getAgName(), "supervisor",
						"action_failed(" + action_name + "," + act.getFailureReason().toString() + ")");
			else
				msg = new Message("tell", getAgName(), "supervisor", "action_failed(" + action_name + ")");
		}
		try {
			sendMsg(msg);
		} catch (Exception e) {
			Tools.getStackTrace(e);
		}
		super.actionExecuted(act);
	}

	public void reinit_steps_number() {
		distToGoal = 0;
		steps = 2;
	}

	public void increment_steps_number() {
		steps += 1;
	}

	public void reinit_step() {
		step = 0;
	}

	public void increment_step() {
		step += 1;
	}
	
	public void set_on_going_step(String step) {
		onGoingStep = step;
//		display.setOngoingStep(step);
//		logger.info("------------------------------------------"+step+"-----------------------------");
//		logger.info("step "+this.step+" over "+steps);
		newStep = true;
		onTimeTaskExecutionPrev = onTimeTaskExecution;
		startTimeOngoingStep = getRosTimeMilliSeconds();
	}
	
	public double task_achievement() {
		return step/steps;
	}
	
	public void reinit_qoi_variables() {
//		display.insert_discontinuity("task", getRosTimeMilliSeconds());
		currentTaskActQoI.clear();
		firstTimeInTask = true;
		onGoingStep = "";
		onGoingAction = "";
		startActionLock.writeLock().lock();
		startTimeOngoingAction = -1;
		startActionLock.writeLock().unlock();
		wasStepping = false;
		wasMoving = false;
		wasComingCloser = false;
		onTimeTaskExecution = 1;
		onTimeTaskExecutionPrev = 1;
	}
	
	public void start_action(String text) {
		if(text.contains("?"))
			onGoingAction = "question";
		else
			onGoingAction = "speak";
		startTimeOngoingAction = getRosTimeMilliSeconds();
//		logger.info("start action to true with "+text);
		startAction = true;
	}
	
	public void end_action() {
		startActionLock.writeLock().lock();
		startTimeOngoingAction = -1;
		startActionLock.writeLock().unlock();
//		logger.info("insert discontinuity");
//		display.insert_discontinuity("action", getRosTimeMilliSeconds());
	}
	
	public BeliefBase getTaskBB(String id) {
		return tasksQoI.get(id);
	}
	
	public BeliefBase getActionBB(String id) {
		return actionsQoI.get(id);
	}
	
	private void pause_asr() {
		ServiceResponseListener<std_srvs.EmptyResponse> respListener = new ServiceResponseListener<std_srvs.EmptyResponse>() {

			@Override
			public void onFailure(RemoteException e) {
			}

			@Override
			public void onSuccess(EmptyResponse arg0) {
			}
		};
		rosnode.callAsyncService("pause_asr", respListener, null);
	}
	
	private void display_processing() {
		ServiceResponseListener<std_srvs.EmptyResponse> respListener = new ServiceResponseListener<std_srvs.EmptyResponse>() {

			@Override
			public void onFailure(RemoteException e) {
			}

			@Override
			public void onSuccess(EmptyResponse arg0) {
			}
		};
		rosnode.callAsyncService("web_view_start_processing", respListener, null);
	}


};
