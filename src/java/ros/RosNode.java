package ros;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
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
import org.yaml.snakeyaml.DumperOptions;

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
import deictic_gestures.CanLookAtRequest;
import deictic_gestures.CanLookAtResponse;
import deictic_gestures.CanPointAt;
import deictic_gestures.CanPointAtRequest;
import deictic_gestures.CanPointAtResponse;
import deictic_gestures.LookAtRequest;
import deictic_gestures.LookAtResponse;
import deictic_gestures.LookAtStatus;
import deictic_gestures.PointAtRequest;
import deictic_gestures.PointAtResponse;
import deictic_gestures.PointAtStatus;
import dialogue_as.dialogue_actionActionFeedback;
import dialogue_as.dialogue_actionActionGoal;
import dialogue_as.dialogue_actionActionResult;
import dialogue_as.dialogue_actionGoal;
import geometry_msgs.Point;
import geometry_msgs.PointStamped;
import geometry_msgs.Pose;
import geometry_msgs.PoseStamped;
import guiding_as_msgs.taskActionFeedback;
import guiding_as_msgs.taskActionGoal;
import guiding_as_msgs.taskActionResult;
import hatp_msgs.PlanningRequestRequest;
import hatp_msgs.PlanningRequestResponse;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.StringTermImpl;
import move_base_msgs.MoveBaseActionFeedback;
import move_base_msgs.MoveBaseActionGoal;
import move_base_msgs.MoveBaseActionResult;
import move_base_msgs.MoveBaseGoal;
import msg_srv_impl.SemanticRouteResponseImpl;
import nao_interaction_msgs.SayRequest;
import nao_interaction_msgs.SayResponse;
import ontologenius_msgs.OntologeniusService;
import ontologenius_msgs.OntologeniusServiceRequest;
import ontologenius_msgs.OntologeniusServiceResponse;
import pepper_base_manager_msgs.StateMachine;
import pepper_base_manager_msgs.StateMachineStatePrioritizedAngle;
import pepper_base_manager_msgs.StateMachineStatePrioritizedTwist;
import pepper_resources_synchronizer_msgs.MetaStateMachineRegisterRequest;
import pepper_resources_synchronizer_msgs.MetaStateMachineRegisterResponse;
import pepper_resources_synchronizer_msgs.SubStateMachine_pepper_base_manager_msgs;
import perspectives_msgs.Fact;
import perspectives_msgs.FactArrayStamped;
import perspectives_msgs.HasMeshRequest;
import perspectives_msgs.HasMeshResponse;
import pointing_planner.PointingActionFeedback;
import pointing_planner.PointingActionGoal;
import pointing_planner.PointingActionResult;
import pointing_planner.PointingGoal;
import pointing_planner.PointingPlannerRequest;
import pointing_planner.PointingPlannerResponse;
import pointing_planner.VisibilityScoreRequest;
import pointing_planner.VisibilityScoreResponse;
import resource_management_msgs.EndCondition;
import resource_management_msgs.MessagePriority;
import resource_management_msgs.StateMachineStateHeader;
import resource_management_msgs.StateMachineTransition;
import resource_synchronizer_msgs.MetaStateMachineHeader;
import resource_synchronizer_msgs.SubStateMachineHeader;
import route_verbalization_msgs.VerbalizeRegionRouteRequest;
import route_verbalization_msgs.VerbalizeRegionRouteResponse;
import rpn_recipe_planner_msgs.SuperInformRequest;
import rpn_recipe_planner_msgs.SuperInformResponse;
import rpn_recipe_planner_msgs.SuperQueryRequest;
import rpn_recipe_planner_msgs.SuperQueryResponse;
import semantic_route_description_msgs.SemanticRouteRequest;
import semantic_route_description_msgs.SemanticRouteResponse;
import speech_wrapper_msgs.SpeakToRequest;
import speech_wrapper_msgs.SpeakToResponse;
import std_msgs.Header;
import tf.get_transform;
import utils.Code;
import utils.SimpleFact;

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
	HashMap<String, String> services_map;
	private HashMap <String, ServiceClient<Message, Message>> service_clients;
	private HashMap <String, String> service_types;
	private ActionServer<taskActionGoal, taskActionFeedback, taskActionResult> guiding_as;
	private taskActionGoal new_guiding_goal = null;
	private Stack<taskActionGoal> stack_guiding_goals;
	private ActionClient<PointingActionGoal, PointingActionFeedback, PointingActionResult> get_placements_ac;
	private ActionClient<dialogue_actionActionGoal, dialogue_actionActionFeedback, dialogue_actionActionResult> get_human_answer_ac;
	private ActionClient<MoveBaseActionGoal, MoveBaseActionFeedback, MoveBaseActionResult> move_to_ac;
	private Subscriber<perspectives_msgs.FactArrayStamped> facts_sub;
	private OntologeniusServiceResponse onto_individual_resp;
	private OntologeniusServiceResponse onto_class_resp;
	private SemanticRouteResponseImpl get_route_resp;
	private HasMeshResponse has_mesh_resp;
	private VisibilityScoreResponse visibility_score_resp;
//	private SpeakToResponse speak_to_resp;
	private SayResponse speak_to_resp;
	private PointAtResponse point_at_resp;
	private LookAtResponse look_at_resp;
	private CanPointAtResponse can_point_at_resp;
	private CanLookAtResponse can_look_at_resp;
	private VerbalizeRegionRouteResponse verbalization_resp;
	private MetaStateMachineRegisterResponse face_human_resp;
	private SuperInformResponse d_inform_resp;
	private SuperQueryResponse d_query_resp;
	private PointingPlannerResponse placements_resp;
	private hatp_msgs.Plan hatp_planner_resp;
	private PointingActionResult placements_result;
	private PointingActionFeedback placements_fb;
	private dialogue_actionActionResult listening_result;
	private dialogue_actionActionFeedback listening_fb; 
	private dialogue_actionActionGoal listen_goal_msg;
	private MoveBaseActionResult move_to_result;
	private MoveBaseActionFeedback move_to_fb;
	private Publisher<visualization_msgs.Marker> marker_pub;
	private Publisher<std_msgs.Int32> person_of_interest_pub;

	private Multimap<String, SimpleFact> perceptions = Multimaps.synchronizedMultimap(ArrayListMultimap.<String, SimpleFact>create());
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
				logger.info("Wrong URI syntax :"+e.getMessage());
			}
			msc = new MasterStateClient(connectedNode, uri);
			service_clients = new HashMap<String, ServiceClient<Message, Message>>();
			service_types = new HashMap <String, String>();
			services_map = (HashMap<String, String>) parameters.getMap("/guiding/services");
			stack_guiding_goals = new Stack<taskActionGoal>();
			marker_pub = ROSAgArch.getM_rosnode().getConnectedNode().newPublisher("/look_at", visualization_msgs.Marker._TYPE);
			person_of_interest_pub = ROSAgArch.getM_rosnode().getConnectedNode().newPublisher(parameters.getString("/guiding/topics/person_of_interest"), std_msgs.Int32._TYPE);
			guiding_as = new ActionServer<>(connectedNode, "/guiding_task", taskActionGoal._TYPE, taskActionFeedback._TYPE, taskActionResult._TYPE);
			guiding_as.attachListener(new ActionServerListener<taskActionGoal>() {

				@Override
				public void goalReceived(taskActionGoal goal) {}

				@Override
				public void cancelReceived(GoalID id) {
					new_guiding_goal = null;
				}

				@Override
				public boolean acceptGoal(taskActionGoal goal) {
					stack_guiding_goals.push(goal);
					return true;
				}
			});
			
			
			get_human_answer_ac = new ActionClient<dialogue_actionActionGoal, dialogue_actionActionFeedback, dialogue_actionActionResult>(
					connectedNode, parameters.getString("/guiding/action_servers/dialogue"), dialogue_actionActionGoal._TYPE, dialogue_actionActionFeedback._TYPE, dialogue_actionActionResult._TYPE);
			
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
			
			move_to_ac = new ActionClient<MoveBaseActionGoal, MoveBaseActionFeedback, MoveBaseActionResult>(
					connectedNode, parameters.getString("/guiding/action_servers/move_to"), MoveBaseActionGoal._TYPE, MoveBaseActionFeedback._TYPE, MoveBaseActionResult._TYPE);
			
			move_to_ac.setLogLevel(Level.OFF);
			
			move_to_ac.attachListener(new ActionClientListener<MoveBaseActionFeedback, MoveBaseActionResult>() {

				@Override
				public void feedbackReceived(MoveBaseActionFeedback fb) {
					move_to_fb = fb;
					
				}

				@Override
				public void resultReceived(MoveBaseActionResult result) {
					move_to_result = result;
					
				}

				@Override
				public void statusReceived(GoalStatusArray arg0) {
				}
			});
			
			
			facts_sub = connectedNode.newSubscriber(parameters.getString("/guiding/topics/current_facts"), perspectives_msgs.FactArrayStamped._TYPE);
	
			facts_sub.addMessageListener(new MessageListener<perspectives_msgs.FactArrayStamped>() {
				
	
				public void onNewMessage(FactArrayStamped facts) {
					synchronized (perceptions) {
						perceptions.clear();
						for(Fact fact : facts.getFacts()) {
							SimpleFact simple_fact;
							String predicate = fact.getPredicate();
							String subject = fact.getSubjectName();
							String object = fact.getObjectName();
							if(predicate.equals("isVisibleBy")) {
								predicate = "canSee";
								if(!subject.startsWith("\""))
									subject = "\""+subject+"\"";
								simple_fact = new SimpleFact(predicate,subject);
								perceptions.put(object, simple_fact);	
							}else {
								if(!object.isEmpty()) {
									if(!object.startsWith("\""))
										object = "\""+object+"\"";
									simple_fact = new SimpleFact(predicate,object);
								}else {
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
			logger.severe("Parameter not found exception : "+e.getMessage());
			throw new RosRuntimeException(e);
		}
	}
	
	public HashMap<String, Boolean> init_service_clients() {
		HashMap<String, Boolean> services_status = new HashMap<String, Boolean>();		
		
		for(Map.Entry<String, String> entry : services_map.entrySet()) {
			services_status.put(entry.getKey(), create_service_client(entry.getKey(), entry.getValue()));
		}
		return services_status;
	}
	
	public HashMap<String, Boolean> retry_init_service_clients(Set<String> clients_to_init){
		HashMap<String, Boolean> services_status = new HashMap<String, Boolean>();
		
		for(String client : clients_to_init) {
			services_status.put(client, create_service_client(client, services_map.get(client)));
		}
		return services_status;
	}
	
	private boolean create_service_client(String key, String srv_name) {
		boolean status = false;
		URI ls = msc.lookupService(srv_name);
		if (ls.toString().isEmpty()) {
			service_clients.put(key, null);
			status = false;
		}else {
	        ServiceClient<Message, Message> serv_client;
			try {
				logger.info("connect to "+srv_name);
				service_types.put(key, get_srv_type(srv_name));
				serv_client = connectedNode.newServiceClient(srv_name, service_types.get(key));
				service_clients.put(key, serv_client);	
				status = true;
			}catch (ServiceNotFoundException e) {
				logger.severe("Service not found exception : "+e.getMessage());
				throw new RosRuntimeException(e);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return status;
	}
	
	
	private String get_srv_type(String srv_name) {
		ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", ". ./env; rosservice info "+srv_name);
		builder.redirectErrorStream(true);
		String type = null;
		try {
			Process p = builder.start();
			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;

			while (true) {
				line = r.readLine();
				if (line == null) { break; }
				String[] tokens = line.split(" ");
				if(tokens[0].contains("Type")) {
					type = tokens[1];
					if(!type.contains("msgs") && !srv_name.contains("pointing_planner") && !srv_name.contains("look_at") && !srv_name.contains("point_at")) {
						String[] tokens_type = type.split("/");
						type = tokens_type[0]+"_msgs/"+tokens_type[1];
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return type;
	}

	public void call_onto_individual_srv(String action, String param) {
		onto_individual_resp = null;
		final OntologeniusServiceRequest request = (OntologeniusServiceRequest) service_clients.get("get_individual_info").newMessage();
		request.setAction(action);
		request.setParam(param);
		service_clients.get("get_individual_info").call(request, new ServiceResponseListener<Message>() {

			@Override
			public void onSuccess(Message response) {
				onto_individual_resp = connectedNode.getServiceResponseMessageFactory().newFromType(OntologeniusService._TYPE);
				if(((OntologeniusServiceResponse) response).getValues().isEmpty()) {
					onto_individual_resp.setCode((short) Code.ERROR.getCode());
				}else {
					onto_individual_resp.setCode((short) Code.OK.getCode());
					onto_individual_resp.setValues(((OntologeniusServiceResponse) response).getValues());
				}
				
			}

			@Override
			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
				
			}
		});
	}
	
	public void call_onto_class_srv(String action, String param) {
		onto_class_resp = null;
		final OntologeniusServiceRequest request = (OntologeniusServiceRequest) service_clients.get("get_class_info").newMessage();
		request.setAction(action);
		request.setParam(param);
		service_clients.get("get_class_info").call(request, new ServiceResponseListener<Message>() {

			@Override
			public void onSuccess(Message response) {
				onto_class_resp = connectedNode.getServiceResponseMessageFactory().newFromType(OntologeniusService._TYPE);
				if(((OntologeniusServiceResponse) response).getValues().isEmpty()) {
					onto_class_resp.setCode((short) Code.ERROR.getCode());
				}else {
					onto_class_resp.setCode((short) Code.OK.getCode());
					onto_class_resp.setValues(((OntologeniusServiceResponse) response).getValues());
				}
				
			}

			@Override
			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
				
			}
		});
	}

	public void call_get_route_srv(String from, String to, String persona, boolean signpost) {
		get_route_resp = null;
		final SemanticRouteRequest request = (SemanticRouteRequest) service_clients.get("get_route").newMessage();
		request.setFrom(from);
		request.setTo(to);
		request.setPersona(persona);
		request.setSignpost(signpost);
		service_clients.get("get_route").call(request, new ServiceResponseListener<Message>() {
			public void onSuccess(Message response) {
				get_route_resp = new SemanticRouteResponseImpl(
						((SemanticRouteResponse) response).getCosts(), 
						((SemanticRouteResponse) response).getGoals(), 
						((SemanticRouteResponse) response).getRoutes());
				if(((SemanticRouteResponse) response).getRoutes().isEmpty()) {
					get_route_resp.setCode(Code.ERROR.getCode());
				}else {
					get_route_resp.setCode(Code.OK.getCode());
				}
			}

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}
		});
	}
	

	public void call_has_mesh_srv(String world, String frame) {
		has_mesh_resp = null;
		final HasMeshRequest request = (HasMeshRequest) service_clients.get("has_mesh").newMessage();
		request.setName(frame);
		request.setWorld(world);
		service_clients.get("has_mesh").call(request, new ServiceResponseListener<Message>() {

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			public void onSuccess(Message response) {
				has_mesh_resp = (HasMeshResponse) response;
			}
			
		});
	}
	
	public void call_visibility_score_srv(String agent, String frame) {
		visibility_score_resp = null;
		final VisibilityScoreRequest request = (VisibilityScoreRequest) service_clients.get("is_visible").newMessage();
		request.setAgentName(agent);
		request.setTargetName(frame);
		service_clients.get("is_visible").call(request, new ServiceResponseListener<Message>() {

			public void onFailure(RemoteException e) {
				logger.info(e.toString());
				throw new RosRuntimeException(e);
			}

			public void onSuccess(Message response) {
				visibility_score_resp = (VisibilityScoreResponse) response;
			}
			
		});
	}
	
	public void call_point_at_srv(String frame, boolean with_head, boolean with_base) {
		point_at_resp = null;
		final PointAtRequest request = (PointAtRequest) service_clients.get("point_at").newMessage();
		PointStamped point = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.PointStamped._TYPE);
		Header header = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);
		header.setFrameId(frame);
		point.setHeader(header);
		request.setPoint(point);
		request.setWithBase(with_base);
		request.setWithHead(with_head);
		service_clients.get("point_at").call(request, new ServiceResponseListener<Message>() {

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			public void onSuccess(Message response) {
				point_at_resp = (PointAtResponse) response;
			}
			
		});
	}
	
	public void call_look_at_srv(PointStamped point_stamped, boolean with_base) {
		look_at_resp = null;
		final LookAtRequest request = (LookAtRequest) service_clients.get("look_at").newMessage();
		request.setPoint(point_stamped);
		request.setWithBase(with_base);
		service_clients.get("look_at").call(request, new ServiceResponseListener<Message>() {

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			public void onSuccess(Message response) {
				look_at_resp = (LookAtResponse) response;
			}
			
		});
	}
	
	public void call_can_point_at_srv(String frame) {
		can_point_at_resp = null;
		final CanPointAtRequest request = (CanPointAtRequest) service_clients.get("can_point_at").newMessage();
		PointStamped point = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.PointStamped._TYPE);
		Header header = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);
		header.setFrameId(frame);
		point.setHeader(header);
		request.setPoint(point);
		service_clients.get("can_point_at").call(request, new ServiceResponseListener<Message>() {

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			public void onSuccess(Message response) {
				can_point_at_resp = (CanPointAtResponse) response;
			}
			
		});
	}
	
	public void call_can_look_at_srv(PointStamped point_stamped) {
		can_look_at_resp = null;
		final CanLookAtRequest request = (CanLookAtRequest) service_clients.get("can_look_at").newMessage();
		request.setPoint(point_stamped);
		service_clients.get("can_look_at").call(request, new ServiceResponseListener<Message>() {

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			public void onSuccess(Message response) {
				can_look_at_resp = (CanLookAtResponse) response;
			}
			
		});
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
	
	public void call_speak_to_srv(String look_at, String text) {
		speak_to_resp = null;
		final SayRequest request = (SayRequest) service_clients.get("speak_to").newMessage();
//		PointStamped point = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.PointStamped._TYPE);
//		Header header = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);
//		header.setFrameId(look_at);
//		point.setHeader(header);
//		request.setLookAt(point);
		request.setText(text);
		service_clients.get("speak_to").call(request, new ServiceResponseListener<Message>() {

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			public void onSuccess(Message response) {
				speak_to_resp = (SayResponse) response;
			}
			
		});
	}
	
	public void call_route_verbalization_srv(List<String> route, String start_place, String goal_shop) {
		verbalization_resp = null;
		final VerbalizeRegionRouteRequest request = (VerbalizeRegionRouteRequest) service_clients.get("route_verbalization").newMessage();
		request.setRoute(route);
		request.setStartPlace(start_place);
		request.setGoalShop(goal_shop);
		service_clients.get("route_verbalization").call(request, new ServiceResponseListener<Message>() {

			@Override
			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			@Override
			public void onSuccess(Message response) {
				verbalization_resp = (VerbalizeRegionRouteResponse) response;
			}
		});
	}
	
	public void call_dialogue_inform_srv(String sentence_code) {
		call_dialogue_inform_srv(sentence_code,  "");
	}
	
	public void call_dialogue_inform_srv(String sentence_code, String param) {
		d_inform_resp = null;
		final SuperInformRequest request = (SuperInformRequest) service_clients.get("dialogue_inform").newMessage();
		request.setStatus(sentence_code);
		request.setStatus(param);
		service_clients.get("dialogue_inform").call(request, new ServiceResponseListener<Message>() {

			@Override
			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			@Override
			public void onSuccess(Message response) {
				// empty
				d_inform_resp = (SuperInformResponse) response;
			}
		});
	}
	
	public void call_dialogue_query_srv(String sentence_code) {
		call_dialogue_query_srv(sentence_code,  "");
	}
	
	public void call_dialogue_query_srv(String sentence_code, String param) {
		d_query_resp = null;
		final SuperQueryRequest request = (SuperQueryRequest) service_clients.get("dialogue_query").newMessage();
		request.setStatus(sentence_code);
		request.setStatus(param);
		service_clients.get("dialogue_query").call(request, new ServiceResponseListener<Message>() {

			@Override
			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			@Override
			public void onSuccess(Message response) {
				d_query_resp = (SuperQueryResponse) response;
			}
		});
	}
	
	public void call_svp_planner_srv(String target_ld, String direction_ld, String human) {
		placements_resp = null;
		final PointingPlannerRequest request = (PointingPlannerRequest) service_clients.get("pointing_planner").newMessage();
		request.setTargetLandmark(target_ld);
		request.setDirectionLandmark(direction_ld);
		request.setHuman(human);
		service_clients.get("pointing_planner").call(request, new ServiceResponseListener<Message>() {

			@Override
			public void onFailure(RemoteException arg0) {
				placements_resp.setPointedLandmarks(new ArrayList<String>());
			}

			@Override
			public void onSuccess(Message response) {
				placements_resp = (PointingPlannerResponse) response;
				
			}
		});
	}
	
	public boolean call_pepper_synchro_srv(String action, String human) {
		face_human_resp = null;
		final MetaStateMachineRegisterRequest request = (MetaStateMachineRegisterRequest) service_clients.get("pepper_synchro").newMessage();
		
		if(action.equals("face_human")) {
			TransformTree tfTree = getTfTree();
			Transform transform;
			String frame1 = "base_link";
			String frame2 = human;
			frame2 = frame2.replaceAll("^\"|\"$", "");
			if(tfTree.canTransform(frame1, frame2)) {
				try {

					transform = tfTree.lookupMostRecent(frame1, frame2);
					float d = (float) Math.atan2(transform.translation.y, transform.translation.z);
					NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
					MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();
					SubStateMachine_pepper_base_manager_msgs substatemachine = messageFactory.newFromType(SubStateMachine_pepper_base_manager_msgs._TYPE);
					pepper_base_manager_msgs.StateMachine statemachine = messageFactory.newFromType(pepper_base_manager_msgs.StateMachine._TYPE);
					StateMachineStatePrioritizedAngle state = messageFactory.newFromType(StateMachineStatePrioritizedAngle._TYPE);
					StateMachineStateHeader stateheader = messageFactory.newFromType(StateMachineStateHeader._TYPE);
					StateMachineTransition smtransition = messageFactory.newFromType(StateMachineTransition._TYPE);
					EndCondition end_condition = messageFactory.newFromType(EndCondition._TYPE);
					SubStateMachineHeader subsmheader = messageFactory.newFromType(SubStateMachineHeader._TYPE);
					MetaStateMachineHeader metaheader = messageFactory.newFromType(MetaStateMachineHeader._TYPE);
					MessagePriority priority = messageFactory.newFromType(MessagePriority._TYPE);
					state.setData(d);
					stateheader.setId("rotate");
					end_condition.setDuration(new Duration(-1));
					end_condition.setTimeout(new Duration(-1));
					ArrayList<String> a = new ArrayList<String>();
					a.add("__done__");
					end_condition.setRegexEndCondition(a);
					smtransition.setEndCondition(end_condition);
					smtransition.setNextState("end");
					stateheader.setTransitions(Arrays.asList(smtransition));
					state.setHeader(stateheader);
					statemachine.setStatesPrioritizedAngle(Arrays.asList(state));
					subsmheader.setInitialState("rotate");
					subsmheader.setBeginDeadLine(connectedNode.getCurrentTime().add(new Duration(5)));
					subsmheader.setTimeout(new Duration(-1));
					substatemachine.setHeader(subsmheader);
					substatemachine.setStateMachine(statemachine);
					request.setStateMachinePepperBaseManager(substatemachine);
					metaheader.setBeginDeadLine(connectedNode.getCurrentTime().add(new Duration(5)));
					metaheader.setTimeout(new Duration(-1));
					priority.setValue(MessagePriority.URGENT);
					metaheader.setPriority(priority);
					request.setHeader(metaheader);
					service_clients.get("pepper_synchro").call(request, new ServiceResponseListener<Message>() {

						@Override
						public void onFailure(RemoteException arg0) {
							face_human_resp.setError("fail to rotate");
						}

						@Override
						public void onSuccess(Message response) {
							face_human_resp = (MetaStateMachineRegisterResponse) response;

						}
					});
				}
				catch (Exception e) {
					return false;
				}
			}else {
				return false;
			}
		}
		return true;
	}
	
	public void call_hatp_planner(String task, String type) {
		call_hatp_planner(task, type, new ArrayList<String>());
	}
	
	public void call_hatp_planner(String task, String type, List<String> parameters) {
		hatp_planner_resp = null;
		final PlanningRequestRequest request = (PlanningRequestRequest) service_clients.get("hatp_planner").newMessage();
		hatp_msgs.Request r_request = connectedNode.getTopicMessageFactory().newFromType(hatp_msgs.Request._TYPE);
		r_request.setTask(task);
		r_request.setType(type);
		if(!parameters.isEmpty()) {
			r_request.setParameters(parameters);
		}
		request.setRequest(r_request);
		service_clients.get("hatp_planner").call(request, new ServiceResponseListener<Message>() {

			@Override
			public void onSuccess(Message response) {
				PlanningRequestResponse resp = (PlanningRequestResponse) response;
				hatp_planner_resp = resp.getSolution();
			}

			@Override
			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}
		});
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
		}while(!server_started & count < max_attempt);
		return false;
	}
	
	
	
	
	public boolean call_move_to_as(PoseStamped pose, int max_attempt) {
		move_to_fb = null;
		move_to_result = null;
		logger.info("wait to start move_to");
		boolean server_started;
		int count = 0;
		do {
			server_started = move_to_ac.waitForActionServerToStart(new Duration(3));
			if(server_started) {
				logger.info("move to as started");
				
				MoveBaseActionGoal goal_msg;
				goal_msg = move_to_ac.newGoalMessage();
				
				NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
				MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();
				MoveBaseGoal move_to_goal = messageFactory.newFromType(MoveBaseGoal._TYPE);;
				move_to_goal.setTargetPose(pose);
				goal_msg.setGoal(move_to_goal);
				move_to_ac.sendGoal(goal_msg);
				return true;
			}else {
				logger.info("No actionlib move_to server found ");
				count += 1;
			}
		}while(!server_started & count < max_attempt);
		return false;	
			
	}
	public boolean call_dialogue_as(List<String> subjects, int max_attempt) {
		return call_dialogue_as(subjects, new ArrayList<String>(), max_attempt);
	}
	
	public boolean call_dialogue_as(List<String> subjects, List<String> verbs, int max_attempt) {
		listening_fb = null;
		listening_result =  null;
		logger.info("wait to start dialogue_as");
		boolean server_started;
		int count = 0;
		do {
			server_started = get_human_answer_ac.waitForActionServerToStart(new Duration(3));
			if(server_started) {
				logger.info("dialogue started");
				
				listen_goal_msg = get_human_answer_ac.newGoalMessage();
				dialogue_actionGoal listen_goal = listen_goal_msg.getGoal();
				if(!subjects.isEmpty()) {
					listen_goal.setSubjects(subjects);
				}else {
					listen_goal.setEnableOnlyVerb(true);
				}
				if(!verbs.isEmpty()) {
					listen_goal.setVerbs(verbs);
				}else {
					listen_goal.setEnableOnlySubject(true);
				}
				listen_goal_msg.setGoal(listen_goal);
				
				get_human_answer_ac.sendGoal(listen_goal_msg);
	//			logger.info(listen_goal_msg.getGoalId().toString());
				logger.info(listen_goal_msg.getGoalId().getId());
				logger.info(listen_goal_msg.getGoalId().getStamp().toString());
				return true;
			}else {
				count += 1;
				logger.info("No actionlib dialogue server found ");
			}
		}while(!server_started & count < max_attempt);
		return false;	
	}
	

	
	public void cancel_goal_dialogue() {
		if(get_human_answer_ac != null & listen_goal_msg != null)
			get_human_answer_ac.sendCancel(listen_goal_msg.getGoalId());
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


	public OntologeniusServiceResponse get_onto_individual_resp() {
		return onto_individual_resp;
	}

	public OntologeniusServiceResponse getOnto_class_resp() {
		return onto_class_resp;
	}

	public SemanticRouteResponseImpl get_get_route_resp() {
		return get_route_resp;
	}


	public PointingActionResult get_placements_result() {
		return placements_result;
	}
	
	public PointingActionFeedback getPlacements_fb() {
		return placements_fb;
	}
	
	public PointingPlannerResponse getPlacements_resp() {
		return placements_resp;
	}

	

	public MetaStateMachineRegisterResponse getFace_human_resp() {
		return face_human_resp;
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

	public HasMeshResponse getHas_mesh_resp() {
		return has_mesh_resp;
	}

	public VisibilityScoreResponse getVisibility_score_resp() {
		return visibility_score_resp;
	}

//	public SpeakToResponse getSpeak_to_resp() {
//		return speak_to_resp;
//	}
	
	public SayResponse getSpeak_to_resp() {
		return speak_to_resp;
	}

	public SuperInformResponse getD_inform_resp() {
		return d_inform_resp;
	}

	public SuperQueryResponse getD_query_resp() {
		return d_query_resp;
	}

	public PointAtResponse getPoint_at_resp() {
		return point_at_resp;
	}

	public LookAtResponse getLook_at_resp() {
		return look_at_resp;
	}

	public CanPointAtResponse getCan_point_at_resp() {
		return can_point_at_resp;
	}

	public CanLookAtResponse getCan_look_at_resp() {
		return can_look_at_resp;
	}

	public VerbalizeRegionRouteResponse getVerbalization_resp() {
		return verbalization_resp;
	}

	public hatp_msgs.Plan getHatp_planner_resp() {
		return hatp_planner_resp;
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

	void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException ex) {
		}
	}


}