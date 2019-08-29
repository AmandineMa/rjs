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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.ros.exception.ParameterNotFoundException;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.Message;
import org.ros.internal.node.response.StatusCode;
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
import org.ros.rosjava.tf.TransformTree;
import org.ros.rosjava.tf.pubsub.TransformListener;

import com.github.rosjava_actionlib.ActionServer;
import com.github.rosjava_actionlib.ActionServerListener;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import actionlib_msgs.GoalID;
import actionlib_msgs.GoalStatus;
import arch.ROSAgArch;
import dialogue_arbiter.DialogueArbiterActionGoal;
import dialogue_arbiter.DialogueArbiterGoal;
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
import guiding_as_msgs.taskResult;
import jason.asSemantics.ActionExec;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.NumberTermImpl;
import move_base_msgs.MoveBaseActionFeedback;
import move_base_msgs.MoveBaseActionGoal;
import move_base_msgs.MoveBaseActionResult;
import move_base_msgs.MoveBaseGoal;
import pepper_base_manager_msgs.StateMachineStatePrioritizedAngle;
import pepper_resources_synchronizer_msgs.SubStateMachine_pepper_base_manager_msgs;
import perspectives_msgs.Fact;
import perspectives_msgs.FactArrayStamped;
import perspectives_msgs.GetNameResponse;
import pointing_planner.PointingActionFeedback;
import pointing_planner.PointingActionResult;
import resource_management_msgs.EndCondition;
import resource_management_msgs.MessagePriority;
import resource_management_msgs.StateMachineStateHeader;
import resource_management_msgs.StateMachineTransition;
import resource_synchronizer_msgs.MetaStateMachineHeader;
import resource_synchronizer_msgs.SubStateMachineHeader;
import std_msgs.Header;
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
	private Logger logger = Logger.getLogger(RosNode.class.getName());

	private ConnectedNode connectedNode;
	private NodeConfiguration nodeConfiguration;
	private MessageFactory messageFactory;
	private TransformListener tfl;
	private ParameterTree parameters;
	private MasterStateClient msc;
	HashMap<String, HashMap<String, String>> services_map;
	private HashMap<String, ServiceClient<Message, Message>> service_clients;
	private ActionServer<taskActionGoal, taskActionFeedback, taskActionResult> guiding_as;
	private taskActionGoal new_guiding_goal = null;
	private Stack<taskActionGoal> stack_guiding_goals;
	private Subscriber<perspectives_msgs.FactArrayStamped> facts_sub;
	private PointingActionResult placements_result;
	private PointingActionFeedback placements_fb;
	private dialogue_actionActionResult listening_result;
	private dialogue_actionActionFeedback listening_fb;
	private MoveBaseActionResult move_to_result;
	private MoveBaseActionFeedback move_to_fb;
	private Publisher<MoveBaseActionGoal> move_to_goal_pub;
	private Publisher<dialogue_actionActionGoal> dialogue_pub;
	private Publisher<GoalID> dialogue_cancel_pub;
	private Publisher<DialogueArbiterActionGoal> engage_pub;
	private Publisher<visualization_msgs.Marker> marker_pub;
	private Publisher<std_msgs.Int32> person_of_interest_pub;
	private Multimap<String, SimpleFact> perceptions = Multimaps
			.synchronizedMultimap(ArrayListMultimap.<String, SimpleFact>create());
	private volatile int percept_id = 0;

	public RosNode(String name) {
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
			nodeConfiguration = NodeConfiguration.newPrivate();
			messageFactory = nodeConfiguration.getTopicMessageFactory();
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
			services_map = null;
			if(parameters.has("/guiding/services_immo"))
				services_map = (HashMap<String, HashMap<String, String>>) parameters.getMap("/guiding/services_immo");
			
			if(parameters.has("/guiding/services_moving") && !parameters.getBoolean("/guiding/immo")) {
				HashMap<String, HashMap<String, String>> map = (HashMap<String, HashMap<String, String>>) parameters.getMap("/guiding/services_moving");
				services_map.putAll(map);
			}
			
			if(parameters.has("/guiding/services_hwu") && parameters.getBoolean("/guiding/dialogue/hwu")) {
				HashMap<String, HashMap<String, String>> map = (HashMap<String, HashMap<String, String>>) parameters.getMap("/guiding/services_hwu");
				services_map.putAll(map);
			}
			
			if(parameters.has("/guiding/services_wo_hwu") && !parameters.getBoolean("/guiding/dialogue/hwu")) {
				HashMap<String, HashMap<String, String>> map = (HashMap<String, HashMap<String, String>>) parameters.getMap("/guiding/services_wo_hwu");
				services_map.putAll(map);
			}
			
			stack_guiding_goals = new Stack<taskActionGoal>();
			marker_pub = ROSAgArch.getM_rosnode().getConnectedNode().newPublisher("/pp_debug",
					visualization_msgs.Marker._TYPE);
			person_of_interest_pub = ROSAgArch.getM_rosnode().getConnectedNode()
					.newPublisher(parameters.getString("/guiding/topics/person_of_interest"), std_msgs.Int32._TYPE);
			guiding_as = new ActionServer<>(connectedNode, "/guiding_task", taskActionGoal._TYPE,
					taskActionFeedback._TYPE, taskActionResult._TYPE);

			if(parameters.has("/guiding/action_servers/move_to")) {
				move_to_goal_pub = connectedNode.newPublisher(
						parameters.getString("/guiding/action_servers/move_to") + "/goal", MoveBaseActionGoal._TYPE);
				
				MessageListener<MoveBaseActionResult> ml_move = new MessageListener<MoveBaseActionResult>() {
	
					@Override
					public void onNewMessage(MoveBaseActionResult result) {
						move_to_result = result;
					}
				};
				addListenerResult("/guiding/action_servers/move_to", MoveBaseActionResult._TYPE, ml_move);
			}
			
			if(parameters.has("/guiding/action_servers/dialogue") && !parameters.getBoolean("guiding/dialogue/hwu")) {
				dialogue_pub = connectedNode.newPublisher(
						parameters.getString("/guiding/action_servers/dialogue") + "/goal", dialogue_actionActionGoal._TYPE);
				dialogue_cancel_pub = connectedNode.newPublisher(
						parameters.getString("/guiding/action_servers/dialogue") + "/cancel", GoalID._TYPE);
				
				MessageListener<dialogue_actionActionResult> ml_dialogue = new MessageListener<dialogue_actionActionResult>() {
	
					@Override
					public void onNewMessage(dialogue_actionActionResult result) {
						listening_result = result;
						if(result.getStatus().getStatus()==actionlib_msgs.GoalStatus.SUCCEEDED) {
							logger.info("result succeeded :"+result.getResult().getSubject());
						}	
					}
				};
				addListenerResult("/guiding/action_servers/dialogue", dialogue_actionActionResult._TYPE, ml_dialogue);
				MessageListener<dialogue_actionActionFeedback> ml_dialogue_fb = new MessageListener<dialogue_actionActionFeedback>() {
					@Override
					public void onNewMessage(dialogue_actionActionFeedback fb) {
						listening_fb = fb;
					}
				};
				addListenerFb("/guiding/action_servers/dialogue", dialogue_actionActionFeedback._TYPE, ml_dialogue_fb);
			}
			
			if(parameters.has("/guiding/action_servers/engage")) {
				engage_pub = connectedNode.newPublisher(
						parameters.getString("/guiding/action_servers/engage") + "/goal", DialogueArbiterActionGoal._TYPE);
			}
			

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
								if (!subject.startsWith("\""))
									subject = "\"" + subject + "\"";			
								perceptions.put(subject, simple_fact);
							}
						}
					}
					percept_id = facts.getHeader().getSeq();
				}
			}, 10);

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
			try {
				Method m = ServiceResponseListener.class.getMethod("onFailure", RemoteException.class);
				m.invoke(srl, new RemoteException(StatusCode.ERROR, "Service (" + serviceName + ") not declared in yaml or type not filled"));
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
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
//				logger.info("message return: " + response.toString());
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

	@SuppressWarnings("unchecked")
	public <T> void call_service(String serviceName, String className, ServiceResponseListener<T> srl,
			Map<String, Object> params) {
//		logger.info("Calling service (" + serviceName + ") with class: " + className);
		Message msg = service_clients.get(serviceName).newMessage();

		List<Method> setMethods = new ArrayList<Method>();
		try {
			Class<?> c = Class.forName(className);
			Method[] methods = c.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++) {
//				logger.info("Current method: " + methods[i].getName());
				if ("set".equalsIgnoreCase(methods[i].getName().substring(0, 3))) {
//					logger.info("Is a set method");
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
					setM.getParameterTypes();

					String paramName = setM.getName().substring(3).toLowerCase();
					if (params.containsKey(paramName)) {
//						logger.info("Setting " + paramName + " with value: " + params.get(paramName));
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

//		logger.info("Calling CALL method");
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
			taskActionResult result = messageFactory.newFromType(taskActionResult._TYPE);
			taskResult r = messageFactory.newFromType(taskResult._TYPE);
			GoalID g_id = messageFactory.newFromType(GoalID._TYPE);
			g_id.setId(id);
			GoalStatus status = messageFactory.newFromType(GoalStatus._TYPE);
			status.setGoalId(g_id);
			if (success.equals("succeeded")) {
				guiding_as.setSucceed(id);
				status.setStatus(GoalStatus.SUCCEEDED);
				r.setSuccess(true);
			}else if(success.equals("preempted")) {
				guiding_as.setPreempt(id);
				status.setStatus(GoalStatus.PREEMPTED);
				r.setSuccess(true);
			}else {
				guiding_as.setAbort(id);
				status.setStatus(GoalStatus.ABORTED);
				r.setSuccess(false);
			}
			result.setResult(r);
			result.setStatus(status);
			guiding_as.sendResult(result);
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

	public MetaStateMachineHeader build_meta_header() {
		nodeConfiguration = NodeConfiguration.newPrivate();
		messageFactory = nodeConfiguration.getTopicMessageFactory();

		MessagePriority priority = messageFactory.newFromType(MessagePriority._TYPE);
		priority.setValue(MessagePriority.URGENT);

		MetaStateMachineHeader metaheader = messageFactory.newFromType(MetaStateMachineHeader._TYPE);
		metaheader.setBeginDeadLine(connectedNode.getCurrentTime().add(new Duration(5)));
		metaheader.setTimeout(new Duration(-1));
		metaheader.setPriority(priority);

		return metaheader;
	}

	public SubStateMachine_pepper_base_manager_msgs build_state_machine_pepper_base_manager(String id, float d) {
		nodeConfiguration = NodeConfiguration.newPrivate();
		messageFactory = nodeConfiguration.getTopicMessageFactory();

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
		subsmheader.setInitialState(id);
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

	public void call_move_to_as(PoseStamped pose) {
		move_to_fb = null;
		move_to_result = null;
		MoveBaseActionGoal goal_msg;
		goal_msg = move_to_goal_pub.newMessage();

		nodeConfiguration = NodeConfiguration.newPrivate();
		messageFactory = nodeConfiguration.getTopicMessageFactory();
		MoveBaseGoal move_to_goal = messageFactory.newFromType(MoveBaseGoal._TYPE);

		move_to_goal.setTargetPose(pose);
		goal_msg.setGoal(move_to_goal);

		move_to_goal_pub.publish(goal_msg);
	}
	
	public void call_engage_as(String human_id) {
		DialogueArbiterActionGoal goal_msg;
		goal_msg = engage_pub.newMessage();
		
		nodeConfiguration = NodeConfiguration.newPrivate();
		messageFactory = nodeConfiguration.getTopicMessageFactory();
		DialogueArbiterGoal engage_goal = messageFactory.newFromType(DialogueArbiterGoal._TYPE);
		
		engage_goal.setId(UUID.randomUUID().toString());
		engage_goal.setParams("{\"person_frame\": \"human_0\"}");
		goal_msg.setGoal(engage_goal);
		
		engage_pub.publish(goal_msg);
	}

	public void call_dialogue_as(List<String> subjects) {
		call_dialogue_as(subjects, new ArrayList<String>());
	}
	
	public void call_dialogue_as(List<String> subjects, List<String> verbs) {
		listening_fb = null;
		listening_result = null;
		nodeConfiguration = NodeConfiguration.newPrivate();
		messageFactory = nodeConfiguration.getTopicMessageFactory();
		dialogue_actionActionGoal listen_goal_msg = messageFactory.newFromType(dialogue_actionActionGoal._TYPE);
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
		GoalID goalID = messageFactory.newFromType(GoalID._TYPE);
		goalID.setId("");
		dialogue_cancel_pub.publish(goalID);
		sleep(200);
		dialogue_pub.publish(listen_goal_msg);
		logger.info("dialogue_as listening");
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
		sub.addMessageListener(ml, 10);
	}
	
	public <T> void addListenerResult(String action_server, String type, MessageListener<T> ml) {
		Subscriber<T> sub = getConnectedNode().newSubscriber(getParameters().getString(action_server) + "/result", type);
		sub.addMessageListener(ml, 10);
	}
	
	public <T> void addListenerFb(String action_server, String type, MessageListener<T> ml) {
		Subscriber<T> sub = getConnectedNode().newSubscriber(getParameters().getString(action_server) + "/feedback", type);
		sub.addMessageListener(ml, 10);
	}
	
	void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException ex) {
		}
	}
	
}