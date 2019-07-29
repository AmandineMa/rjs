package ros;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ros.exception.ParameterNotFoundException;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.Message;
import org.ros.master.client.MasterStateClient;
import org.ros.message.Duration;
import org.ros.message.MessageFactory;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava.tf.Transform;
import org.ros.rosjava.tf.TransformTree;
import org.ros.rosjava.tf.pubsub.TransformListener;
import com.github.rosjava_actionlib.ActionClient;
import com.github.rosjava_actionlib.ActionClientListener;
import com.github.rosjava_actionlib.ActionServer;
import com.github.rosjava_actionlib.ActionServerListener;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import actionlib_msgs.GoalID;
import actionlib_msgs.GoalStatusArray;
import arch.ROSAgArch;
import dialogue_as.dialogue_actionActionFeedback;
import dialogue_as.dialogue_actionActionGoal;
import dialogue_as.dialogue_actionActionResult;
import dialogue_as.dialogue_actionGoal;
import geometry_msgs.Point;
import geometry_msgs.PointStamped;
import geometry_msgs.PoseStamped;
import guiding_as_msgs.taskActionFeedback;
import guiding_as_msgs.taskActionGoal;
import guiding_as_msgs.taskActionResult;
import jason.asSemantics.ActionExec;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.NumberTermImpl;
import move_base_msgs.MoveBaseActionFeedback;
import move_base_msgs.MoveBaseActionGoal;
import move_base_msgs.MoveBaseActionResult;
import move_base_msgs.MoveBaseGoal;
import pepper_base_manager_msgs.StateMachineStatePrioritizedAngle;
import pepper_resources_synchronizer_msgs.MetaStateMachineRegisterResponse;
import pepper_resources_synchronizer_msgs.SubStateMachine_pepper_base_manager_msgs;
import perspectives_msgs.Fact;
import perspectives_msgs.FactArrayStamped;
import perspectives_msgs.GetNameResponse;
import pointing_planner.PointingActionFeedback;
import pointing_planner.PointingActionGoal;
import pointing_planner.PointingActionResult;
import pointing_planner.PointingGoal;
import resource_management_msgs.EndCondition;
import resource_management_msgs.MessagePriority;
import resource_management_msgs.StateMachineStateHeader;
import resource_management_msgs.StateMachineTransition;
import resource_synchronizer_msgs.MetaStateMachineHeader;
import resource_synchronizer_msgs.SubStateMachineHeader;
import std_msgs.Header;
import utils.Quaternion;
import utils.SimpleFact;
import utils.Tools;

/***
 * ROS node to be used by Jason
 * 
 * @author Google Code
 * @version 1.0
 * @since 2014-06-02
 *
 */
public class RosNode extends AbstractNodeMain {
	@SuppressWarnings("unused")
	private String name;
	private Logger logger = Logger.getLogger(RosNode.class.getName());

	private ConnectedNode connectedNode;
	private TransformListener tfl;
	private ParameterTree parameters;
	private MasterStateClient msc;
	HashMap<String, HashMap<String, String>> services_map;
	private HashMap<String, ServiceClient<Message, Message>> service_clients;
	private HashMap<String, String> service_types;
	private ActionServer<taskActionGoal, taskActionFeedback, taskActionResult> guiding_as;
	private taskActionGoal new_guiding_goal = null;
	private Stack<taskActionGoal> stack_guiding_goals;
	private ActionClient<PointingActionGoal, PointingActionFeedback, PointingActionResult> get_placements_ac;
	private ActionClient<dialogue_actionActionGoal, dialogue_actionActionFeedback, dialogue_actionActionResult> get_human_answer_ac;
	private ActionClient<MoveBaseActionGoal, MoveBaseActionFeedback, MoveBaseActionResult> move_to_ac;
	private Subscriber<perspectives_msgs.FactArrayStamped> facts_sub;
	private PointingActionResult placements_result;
	private PointingActionFeedback placements_fb;
	private dialogue_actionActionResult listening_result;
	private dialogue_actionActionFeedback listening_fb;
	private dialogue_actionActionGoal listen_goal_msg;
	private MoveBaseActionResult move_to_result;
	private MoveBaseActionFeedback move_to_fb;
	private Publisher<MoveBaseActionGoal> move_to_goal_pub;
	private Subscriber<MoveBaseActionResult> move_to_result_sub;
	private Publisher<visualization_msgs.Marker> marker_pub;
	private Publisher<std_msgs.Int32> person_of_interest_pub;
	private Subscriber<guiding_as_msgs.Task> goal_listener;

	private Multimap<String, SimpleFact> perceptions = Multimaps
			.synchronizedMultimap(ArrayListMultimap.<String, SimpleFact>create());
	private volatile int percept_id = 0;

	public RosNode(String name) {
		this.name = name;
	}

	public GraphName getDefaultNodeName() {
		return GraphName.of("supervisor_clients");
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
	}

	@SuppressWarnings("unchecked")
	public void init() {
		try {
			tfl = new TransformListener(connectedNode);
			parameters = connectedNode.getParameterTree();
			URI uri = null;
			try {
				uri = new URI(System.getenv("ROS_MASTER_URI"));
			} catch (URISyntaxException e) {
				logger.info("Wrong URI syntax :" + e.getMessage());
			}
			msc = new MasterStateClient(connectedNode, uri);
			service_clients = new HashMap<String, ServiceClient<Message, Message>>();
			services_map = (HashMap<String, HashMap<String, String>>) parameters.getMap("/guiding/services");
			stack_guiding_goals = new Stack<taskActionGoal>();
			marker_pub = ROSAgArch.getM_rosnode().getConnectedNode().newPublisher("/pp_debug",
					visualization_msgs.Marker._TYPE);
			person_of_interest_pub = ROSAgArch.getM_rosnode().getConnectedNode()
					.newPublisher(parameters.getString("/guiding/topics/person_of_interest"), std_msgs.Int32._TYPE);
			guiding_as = new ActionServer<>(connectedNode, "/guiding_task", taskActionGoal._TYPE,
					taskActionFeedback._TYPE, taskActionResult._TYPE);

			get_human_answer_ac = new ActionClient<dialogue_actionActionGoal, dialogue_actionActionFeedback, dialogue_actionActionResult>(
					connectedNode, parameters.getString("/guiding/action_servers/dialogue"),
					dialogue_actionActionGoal._TYPE, dialogue_actionActionFeedback._TYPE,
					dialogue_actionActionResult._TYPE);

			get_human_answer_ac.setLogLevel(Level.OFF);
			
			get_human_answer_ac.attachListener(new ActionClientListener<dialogue_actionActionFeedback, dialogue_actionActionResult>() {

				@Override
				public void feedbackReceived(dialogue_actionActionFeedback fb) {
					listening_fb = fb;
				}

				@Override
				public void resultReceived(dialogue_actionActionResult result) {
					listening_result = result;
					if(result.getStatus().getStatus()==actionlib_msgs.GoalStatus.SUCCEEDED) {
						logger.info("result succeeded :"+result.getResult().getSubject());
					}
					if(result.getStatus().getStatus()==actionlib_msgs.GoalStatus.PREEMPTED) {
						logger.info("result preempted");
					}
					
					
				}

				@Override
				public void statusReceived(GoalStatusArray arg0) {}
			});

//			move_to_ac = new ActionClient<MoveBaseActionGoal, MoveBaseActionFeedback, MoveBaseActionResult>(
//					connectedNode, parameters.getString("/guiding/action_servers/move_to"), MoveBaseActionGoal._TYPE, MoveBaseActionFeedback._TYPE, MoveBaseActionResult._TYPE);
//			
//			move_to_ac.setLogLevel(Level.OFF);
//			
//			move_to_ac.attachListener(new ActionClientListener<MoveBaseActionFeedback, MoveBaseActionResult>() {
//
//				@Override
//				public void feedbackReceived(MoveBaseActionFeedback fb) {
//					move_to_fb = fb;
//					
//				}
//
//				@Override
//				public void resultReceived(MoveBaseActionResult result) {
//					move_to_result = result;
//					
//				}
//
//				@Override
//				public void statusReceived(GoalStatusArray arg0) {
//				}
//			});

			move_to_goal_pub = connectedNode.newPublisher(
					parameters.getString("/guiding/action_servers/move_to") + "/goal", MoveBaseActionGoal._TYPE);
			move_to_result_sub = connectedNode.newSubscriber(
					parameters.getString("/guiding/action_servers/move_to") + "/result", MoveBaseActionResult._TYPE);
			move_to_result_sub.addMessageListener(new MessageListener<MoveBaseActionResult>() {

				@Override
				public void onNewMessage(MoveBaseActionResult result) {
					move_to_result = result;
				}
			});

			facts_sub = connectedNode.newSubscriber(parameters.getString("/guiding/topics/current_facts"),
					perspectives_msgs.FactArrayStamped._TYPE);

			facts_sub.addMessageListener(new MessageListener<perspectives_msgs.FactArrayStamped>() {

				public void onNewMessage(FactArrayStamped facts) {
					synchronized (perceptions) {
						perceptions.clear();
						for (Fact fact : facts.getFacts()) {
							final SimpleFact simple_fact;
							String predicate = fact.getPredicate();
							String subject = fact.getSubjectName();
							String object = fact.getObjectName();
							if (predicate.equals("isVisibleBy")) {
								predicate = "canSee";
								if (subject.startsWith("\""))
									subject = subject.replaceAll("^\"|\"$", "");
								if (service_clients.get("get_uwds_name") != null) {
									Map<String, Object> parameters = new HashMap<String, Object>();
									parameters.put("id", subject);
									parameters.put("world", "robot/merged_visibilities");
									GetNameResponse uwds_name_resp = callSyncService("get_uwds_name", parameters);
									if(uwds_name_resp != null) {
										subject = uwds_name_resp.getName();
										if (!subject.startsWith("\""))
											subject = "\"" + subject + "\"";
										simple_fact = new SimpleFact(predicate, subject);
										perceptions.put(object, simple_fact);
									}
								}

							} else {
								if (!object.isEmpty()) {
									if (!object.startsWith("\""))
										object = "\"" + object + "\"";
									simple_fact = new SimpleFact(predicate, object);
								} else {
									simple_fact = new SimpleFact(predicate);
								}
								perceptions.put(subject, simple_fact);
							}
						}
					}
					percept_id = facts.getHeader().getSeq();
				}
			});

		} catch (ParameterNotFoundException e) {
			logger.severe("Parameter not found exception : " + e.getMessage());
			throw new RosRuntimeException(e);
		}
	}


	public <T> void callAsyncService(String serviceName, ServiceResponseListener<T> srl, Map<String, Object> params) {
		HashMap<String, String> mapInfoService = services_map.get(serviceName);

		if (mapInfoService != null && mapInfoService.containsKey("type")) {
			String type = mapInfoService.get("type");
			type = type.replace('/', '.') + "Request";
			call_service(serviceName, type, srl, params);
		} else {
			logger.info("Service (" + serviceName + ") not declared in yaml or type not filled");
		}
	}

	public <T> T callSyncService(String service, Map<String, Object> params) {
		CompletableFuture<T> future = new CompletableFuture<T>();

		ServiceResponseListener<T> srl = new ServiceResponseListener<T>() {

			@Override
			public void onFailure(RemoteException e) {
				future.complete(null);
				RosRuntimeException RRE = new RosRuntimeException(e);
				logger.info(Tools.getStackTrace((Exception) RRE));
			}

			@Override
			public void onSuccess(T response) {
				logger.info("message return: " + response.toString());
				future.complete(response);
			}
		};

		callAsyncService(service, srl, params);

		T response = null;
		try {
			response = future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return response;
	}

	public <T> void call_service(String serviceName, String className, ServiceResponseListener<T> srl,
			Map<String, Object> params) {
		logger.info("Calling service (" + serviceName + ") with class: " + className);
		Message msg = service_clients.get(serviceName).newMessage();

		List<Method> setMethods = new ArrayList<Method>();
		try {
			Class<?> c = Class.forName(className);
			Method[] methods = c.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++) {
				logger.info("Current method: " + methods[i].getName());
				if ("set".equalsIgnoreCase(methods[i].getName().substring(0, 3))) {
					logger.info("Is a set method");
					setMethods.add(methods[i]);
				}
			}
		} catch (ClassNotFoundException | IllegalArgumentException e) {
			e.printStackTrace();
		}

		for (String name : params.keySet()) {
			boolean noMethod = true;
			for (Method curMethod : setMethods) {
				if (curMethod.getName().substring(3).toLowerCase().equalsIgnoreCase(name))
					noMethod = false;
			}

			if (noMethod) {
				logger.info("Warning: Parameter " + name + " doesn't have a corresponding setter in " + className);
				logger.info("List of setters for this class: ");
				for (Method curMethod : setMethods) {
					logger.info("  -  " + curMethod.getName());
				}
			}
		}

		for (int i = 0; i < setMethods.size(); i++) {
			try {
				Method setM = setMethods.get(i);
				if (setM.getParameterTypes().length == 1) {
					// Needed to check types ?
					@SuppressWarnings("unused")
					Class<?> parmType = setM.getParameterTypes()[0];

					String paramName = setM.getName().substring(3).toLowerCase();
					if (params.containsKey(paramName)) {
						logger.info("Setting " + paramName + " with value: " + params.get(paramName));
						setM.invoke(msg, params.get(paramName));
					} else {
						logger.info("No value defined for parameter: " + paramName);
					}

				} else {
					logger.info("Error: incorrect number of parameters: " + setM.getParameterTypes().length);
				}

			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		logger.info("Calling CALL method");
		service_clients.get(serviceName).call(msg, (ServiceResponseListener<Message>) srl);
	}

/*	public <T> void call_service(String serviceName, String className, RosCallback<T> rcb, Map<String, Object> params) {
		ServiceResponseListener<T> respList = getResponseListener(rcb);
		call_service(serviceName, className, respList, params);
	}

	public <T> ServiceResponseListener<T> getResponseListener(RosCallback<T> rcb) {
		return new ServiceResponseListener<T>() {

			public void onFailure(RemoteException e) {
				RosRuntimeException RRE = new RosRuntimeException(e);
				logger.info(Tools.getStackTrace((Exception) RRE));
				throw RRE;
			}

			public void onSuccess(T response) {
				logger.info("Response received on ServiceResponseListener");
				rcb.callback(response);
			}

		};
	}*/

	public void set_task_result(String success, String id) {
		if (guiding_as != null) {
			if (success.equals("succeeded"))
				guiding_as.setSucceed(id);
			else
				guiding_as.setAbort(id);
		} else {
			logger.info("guiding as null");
		}
	}

	public HashMap<String, Boolean> init_service_clients() {
		HashMap<String, Boolean> services_status = new HashMap<String, Boolean>();

		for (Entry<String, HashMap<String, String>> entry : services_map.entrySet()) {
			services_status.put(entry.getKey(), create_service_client(entry.getKey()));
		}
		return services_status;
	}

	public HashMap<String, Boolean> retry_init_service_clients(Set<String> clients_to_init) {
		HashMap<String, Boolean> services_status = new HashMap<String, Boolean>();

		for (String client : clients_to_init) {
			services_status.put(client, create_service_client(client));
		}
		return services_status;
	}

	private boolean create_service_client(String key) {
		boolean status = false;
		String srv_name = services_map.get(key).get("name");
		URI ls = msc.lookupService(srv_name);
		if (ls.toString().isEmpty()) {
			service_clients.put(key, null);
			status = false;
		} else {
			ServiceClient<Message, Message> serv_client;
			try {
				logger.info("connect to " + srv_name);
				serv_client = connectedNode.newServiceClient(srv_name, services_map.get(key).get("type"));
				service_clients.put(key, serv_client);
				status = true;
			} catch (ServiceNotFoundException e) {
				logger.severe("Service not found exception : " + e.getMessage());
				throw new RosRuntimeException(e);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return status;
	}

	public <T> T newServiceResponseFromType(String type) {
		return connectedNode.getServiceResponseMessageFactory().newFromType(type);
	}

	public PointStamped build_point_stamped(String frame) {
		PointStamped point = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.PointStamped._TYPE);
		Header header = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);
		header.setFrameId(frame);
		point.setHeader(header);
		return point;
	}

	public PointStamped build_point_stamped(ActionExec action, String frame) {
		MessageFactory messageFactory = connectedNode.getTopicMessageFactory();
		PointStamped point_stamped = build_point_stamped(frame);

		if (action.getActionTerm().getArity() != 1) {
			ListTermImpl point_term = ((ListTermImpl) action.getActionTerm().getTerm(1));
			Point point = messageFactory.newFromType(Point._TYPE);
			point.setX(((NumberTermImpl) point_term.get(0)).solve());
			point.setY(((NumberTermImpl) point_term.get(1)).solve());
			point.setZ(((NumberTermImpl) point_term.get(2)).solve());
			point_stamped.setPoint(point);
		}

		return point_stamped;
	}

//	public void call_speak_to_srv(String look_at, String text) {
//		speak_to_resp = null;
//		final SpeakToRequest request = (SpeakToRequest) service_clients.get("speak_to").newMessage();
//		PointStamped point = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.PointStamped._TYPE);
//		Header header = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);
//		header.setFrameId(look_at);
//		point.setHeader(header);
//		request.setLookAt(point);
//		request.setText(text);
//		service_clients.get("speak_to").call(request, new ServiceResponseListener<Message>() {
//
//			public void onFailure(RemoteException e) {
//				throw new RosRuntimeException(e);
//			}
//
//			public void onSuccess(Message response) {
//				speak_to_resp = (SpeakToResponse) response;
//			}
//			
//		});
//	}

	public MetaStateMachineHeader build_meta_header() {
		NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
		MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();

		MessagePriority priority = messageFactory.newFromType(MessagePriority._TYPE);
		priority.setValue(MessagePriority.URGENT);

		MetaStateMachineHeader metaheader = messageFactory.newFromType(MetaStateMachineHeader._TYPE);
		metaheader.setBeginDeadLine(connectedNode.getCurrentTime().add(new Duration(5)));
		metaheader.setTimeout(new Duration(-1));
		metaheader.setPriority(priority);

		return metaheader;
	}

	public SubStateMachine_pepper_base_manager_msgs build_state_machine_pepper_base_manager(String id, float d) {
		NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
		MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();

		// Build End Condition: __done__
		EndCondition end_condition = messageFactory.newFromType(EndCondition._TYPE);
		end_condition.setDuration(new Duration(-1));
		end_condition.setTimeout(new Duration(-1));
		ArrayList<String> a = new ArrayList<String>();
		a.add("__done__");
		end_condition.setRegexEndCondition(a);

		// Set State Machine Transition from End Condition
		StateMachineTransition smtransition = messageFactory.newFromType(StateMachineTransition._TYPE);
		smtransition.setEndCondition(end_condition);
		smtransition.setNextState("end");

		// Set State Header from Transition
		StateMachineStateHeader stateheader = messageFactory.newFromType(StateMachineStateHeader._TYPE);
		stateheader.setId(id);
		stateheader.setTransitions(Arrays.asList(smtransition));

		// Creating corresponding state
		StateMachineStatePrioritizedAngle state = messageFactory.newFromType(StateMachineStatePrioritizedAngle._TYPE);
		state.setData(d);
		state.setHeader(stateheader);

		pepper_base_manager_msgs.StateMachine statemachine = messageFactory
				.newFromType(pepper_base_manager_msgs.StateMachine._TYPE);
		statemachine.setStatesPrioritizedAngle(Arrays.asList(state));

		SubStateMachineHeader subsmheader = messageFactory.newFromType(SubStateMachineHeader._TYPE);
		subsmheader.setInitialState("rotate");
		subsmheader.setBeginDeadLine(connectedNode.getCurrentTime().add(new Duration(5)));
		subsmheader.setTimeout(new Duration(-1));

		SubStateMachine_pepper_base_manager_msgs substatemachine = messageFactory
				.newFromType(SubStateMachine_pepper_base_manager_msgs._TYPE);
		substatemachine.setHeader(subsmheader);
		substatemachine.setStateMachine(statemachine);
		return substatemachine;
	}

	public hatp_msgs.Request build_hatp_request(String task, String type, List<String> parameters) {
		hatp_msgs.Request r_request = connectedNode.getTopicMessageFactory().newFromType(hatp_msgs.Request._TYPE);
		r_request.setTask(task);
		r_request.setType(type);
		if (parameters != null && !parameters.isEmpty()) {
			r_request.setParameters(parameters);
		}
		return r_request;
	}

	public boolean call_svp_planner(String target_ld, String direction_ld, String human, int max_attempt) {
		placements_result = null;
		placements_fb = null;
		boolean server_started;
		int count = 0;
		logger.info("wait to start svp planner");
		do {
			server_started = get_placements_ac.waitForActionServerToStart(new Duration(3));
			// TODO send info to supervisor agent
			if (server_started) {
				logger.info("SVP Planner action server started.\n");
				PointingActionGoal goal_msg;
				goal_msg = get_placements_ac.newGoalMessage();
				PointingGoal svp_goal = goal_msg.getGoal();
				svp_goal.setHuman(human);
				svp_goal.setDirectionLandmark(direction_ld);
				svp_goal.setTargetLandmark(target_ld);
				goal_msg.setGoal(svp_goal);
				get_placements_ac.sendGoal(goal_msg);
				return true;
			} else {
				count += 1;
				logger.info("No actionlib svp server found ");
			}
		} while (!server_started & count < max_attempt);
		return false;
	}

//	public boolean call_move_to_as(PoseStamped pose, int max_attempt) {
//		move_to_fb = null;
//		move_to_result = null;
//		logger.info("wait to start move_to");
//		boolean server_started;
//		int count = 0;
//		do {
//			server_started = move_to_ac.waitForActionServerToStart(new Duration(3));
//			if(server_started) {
//				logger.info("move to as started");
//				
//				MoveBaseActionGoal goal_msg;
//				goal_msg = move_to_ac.newGoalMessage();
//				
//				NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
//				MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();
//				MoveBaseGoal move_to_goal = messageFactory.newFromType(MoveBaseGoal._TYPE);;
//				move_to_goal.setTargetPose(pose);
//				goal_msg.setGoal(move_to_goal);
//				move_to_ac.sendGoal(goal_msg);
//				return true;
//			}else {
//				logger.info("No actionlib move_to server found ");
//				count += 1;
//			}
//		}while(!server_started & count < max_attempt);
//		return false;	
//			
//	}

	public boolean call_move_to_as(PoseStamped pose, int max_attempt) {
		move_to_fb = null;
		move_to_result = null;
		MoveBaseActionGoal goal_msg;
		goal_msg = move_to_goal_pub.newMessage();

		NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
		MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();
		MoveBaseGoal move_to_goal = messageFactory.newFromType(MoveBaseGoal._TYPE);

		move_to_goal.setTargetPose(pose);
		goal_msg.setGoal(move_to_goal);

		move_to_goal_pub.publish(goal_msg);
		return true;

	}

	public boolean call_dialogue_as(List<String> subjects, int max_attempt) {
		return call_dialogue_as(subjects, new ArrayList<String>(), max_attempt);
	}

	public boolean call_dialogue_as(List<String> subjects, List<String> verbs, int max_attempt) {
		listening_fb = null;
		listening_result = null;
		logger.info("wait to start dialogue_as");
		boolean server_started;
		int count = 0;
		do {
			server_started = get_human_answer_ac.waitForActionServerToStart(new Duration(3));
			if (server_started) {
				logger.info("dialogue started");

				listen_goal_msg = get_human_answer_ac.newGoalMessage();
				dialogue_actionGoal listen_goal = listen_goal_msg.getGoal();
				if (!subjects.isEmpty()) {
					listen_goal.setSubjects(subjects);
				} else {
					listen_goal.setEnableOnlyVerb(true);
				}
				if (!verbs.isEmpty()) {
					listen_goal.setVerbs(verbs);
				} else {
					listen_goal.setEnableOnlySubject(true);
				}
				listen_goal_msg.setGoal(listen_goal);

				get_human_answer_ac.sendGoal(listen_goal_msg);
				// logger.info(listen_goal_msg.getGoalId().toString());
				logger.info(listen_goal_msg.getGoalId().getId());
				logger.info(listen_goal_msg.getGoalId().getStamp().toString());
				return true;
			} else {
				count += 1;
				logger.info("No actionlib dialogue server found ");
			}
		} while (!server_started & count < max_attempt);
		return false;
	}

	public void cancel_goal_dialogue() {
		if (get_human_answer_ac != null & listen_goal_msg != null)
			get_human_answer_ac.sendCancel(listen_goal_msg.getGoalId());
	}
	
	public void set_guiding_as_listener(ActionServerListener<taskActionGoal> listener) {
		guiding_as.attachListener(listener);
	}

	public taskActionGoal getNew_guiding_goal() {
		return new_guiding_goal;
	}

	public void setNew_guiding_goal(taskActionGoal current_guiding_goal) {
		this.new_guiding_goal = current_guiding_goal;
	}

	public Stack<taskActionGoal> getStack_guiding_goals() {
		return stack_guiding_goals;
	}

	public PointingActionResult get_placements_result() {
		return placements_result;
	}

	public PointingActionFeedback getPlacements_fb() {
		return placements_fb;
	}

	public dialogue_actionActionResult getListening_result() {
		return listening_result;
	}

	public dialogue_actionActionFeedback getListening_fb() {
		return listening_fb;
	}

	public MoveBaseActionResult getMove_to_result() {
		return move_to_result;
	}

	public MoveBaseActionFeedback getMove_to_fb() {
		return move_to_fb;
	}

	public Multimap<String, SimpleFact> getPerceptions() {
		return perceptions;
	}

	public int getPercept_id() {
		return percept_id;
	}

	public void setPerceptions(Multimap<String, SimpleFact> perceptions) {
		this.perceptions = perceptions;
	}

	public ConnectedNode getConnectedNode() {
		return connectedNode;
	}

	public TransformTree getTfTree() {
		return tfl.getTree();
	}

	public ParameterTree getParameters() {
		return parameters;
	}

	public Publisher<std_msgs.Int32> getPerson_of_interest_pub() {
		return person_of_interest_pub;
	}

	public Publisher<visualization_msgs.Marker> getMarker_pub() {
		return marker_pub;
	}


	public <T> void addListener(String topic, String type, MessageListener<T> ml) {
		Subscriber<T> sub = getConnectedNode().newSubscriber(getParameters().getString(topic), type);
		sub.addMessageListener(ml);
	}
}