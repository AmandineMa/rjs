package supervisor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ros.exception.ParameterNotFoundException;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.Message;
import org.ros.master.client.MasterStateClient;
import org.ros.message.Duration;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava.tf.TransformTree;
import org.ros.rosjava.tf.pubsub.TransformListener;

import com.github.rosjava_actionlib.ActionClient;
import com.github.rosjava_actionlib.ActionClientListener;
import com.github.rosjava_actionlib.ClientState;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import actionlib_msgs.GoalStatus;
import actionlib_msgs.GoalStatusArray;
import deictic_gestures_msgs.PointAtRequest;
import deictic_gestures_msgs.PointAtResponse;
import geometry_msgs.PointStamped;
import msg_srv_impl.SemanticRouteResponseImpl;
import ontologenius_msgs.OntologeniusService;
import ontologenius_msgs.OntologeniusServiceRequest;
import ontologenius_msgs.OntologeniusServiceResponse;
import perspectives_msgs.Fact;
import perspectives_msgs.FactArrayStamped;
import perspectives_msgs.HasMeshRequest;
import perspectives_msgs.HasMeshResponse;
import pointing_planner_msgs.PointingActionFeedback;
import pointing_planner_msgs.PointingActionGoal;
import pointing_planner_msgs.PointingActionResult;
import pointing_planner_msgs.PointingGoal;
import pointing_planner_msgs.VisibilityScoreRequest;
import pointing_planner_msgs.VisibilityScoreResponse;
import route_verbalization_msgs.VerbalizeRegionRouteRequest;
import route_verbalization_msgs.VerbalizeRegionRouteResponse;
import semantic_route_description_msgs.SemanticRouteRequest;
import semantic_route_description_msgs.SemanticRouteResponse;
import speech_wrapper_msgs.SpeakToRequest;
import speech_wrapper_msgs.SpeakToResponse;
import std_msgs.Header;

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
	private ActionClient<PointingActionGoal, PointingActionFeedback, PointingActionResult> get_placements_ac;
	private Subscriber<perspectives_msgs.FactArrayStamped> facts_sub;
	private OntologeniusServiceResponse onto_individual_resp;
	private SemanticRouteResponseImpl get_route_resp;
	private HasMeshResponse has_mesh_resp;
	private VisibilityScoreResponse visibility_score_resp;
	private SpeakToResponse speak_to_resp;
	private PointAtResponse point_at_resp;
	private VerbalizeRegionRouteResponse verbalization_resp;
	private PointingActionResult placements_result;
	private PointingActionFeedback placements_fb;

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
				uri = new URI("http://localhost:11311");
			} catch (URISyntaxException e) {
				logger.info("Wrong URI syntax :"+e.getMessage());
			}
			msc = new MasterStateClient(connectedNode, uri);
			service_clients = new HashMap<String, ServiceClient<Message, Message>>();
			service_types = new HashMap <String, String>();
			services_map = (HashMap<String, String>) parameters.getMap("/guiding/services");
			
			get_placements_ac = new ActionClient<PointingActionGoal, PointingActionFeedback, PointingActionResult>
			(connectedNode, parameters.getString("/guiding/action_servers/pointing_planner"), PointingActionGoal._TYPE, PointingActionFeedback._TYPE, PointingActionResult._TYPE);
			// too many useless loginfo in the class ActionClient (modified ActionClient by amdia to add the setLogLevel method)
			get_placements_ac.setLogLevel(Level.OFF);
			
			get_placements_ac.attachListener(new ActionClientListener<PointingActionFeedback, PointingActionResult>() {
	
				@Override
				public void feedbackReceived(PointingActionFeedback fb) {
					placements_fb = fb;
				}
	
				@Override
				public void resultReceived(PointingActionResult result) {
					placements_result = result;
				}
	
				@Override
				public void statusReceived(GoalStatusArray arg0) {}
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
								simple_fact = new SimpleFact(predicate,subject);
								perceptions.put(object, simple_fact);	
							}else {
								if(!object.isEmpty()) {
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
					if(!type.contains("msgs")) {
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

	public void call_onto_indivual_srv(String action, String param) {
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
				throw new RosRuntimeException(e);
			}

			public void onSuccess(Message response) {
				visibility_score_resp = (VisibilityScoreResponse) response;
			}
			
		});
	}
	
	public void call_point_at_srv(String frame) {
		point_at_resp = null;
		final PointAtRequest request = (PointAtRequest) service_clients.get("point_at").newMessage();
		PointStamped point = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.PointStamped._TYPE);
		Header header = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);
		header.setFrameId(frame);
		point.setHeader(header);
		request.setPoint(point);
		service_clients.get("point_at").call(request, new ServiceResponseListener<Message>() {

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			public void onSuccess(Message response) {
				point_at_resp = (PointAtResponse) response;
			}
			
		});
	}
	
	public void call_speak_to_srv(String look_at, String text) {
		speak_to_resp = null;
		final SpeakToRequest request = (SpeakToRequest) service_clients.get("speak_to").newMessage();
		PointStamped point = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.PointStamped._TYPE);
		Header header = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);
		header.setFrameId(look_at);
		point.setHeader(header);
		request.setLookAt(point);
		request.setText(text);
		service_clients.get("speak_to").call(request, new ServiceResponseListener<Message>() {

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			public void onSuccess(Message response) {
				speak_to_resp = (SpeakToResponse) response;
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

	public boolean call_svp_planner(String target_ld, String direction_ld, String human) {
		placements_result = null;
		placements_fb = null;
		boolean serverStarted = get_placements_ac.waitForActionServerToStart(new Duration(10));
		// TODO send info to supervisor agent
		if (serverStarted) {
			logger.info("Action server started.\n");
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
			logger.info("No actionlib svp server found ");
			return false;
		}
	}
	

	public OntologeniusServiceResponse get_onto_individual_resp() {
		return onto_individual_resp;
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

	public HasMeshResponse getHas_mesh_resp() {
		return has_mesh_resp;
	}

	public VisibilityScoreResponse getVisibility_score_resp() {
		return visibility_score_resp;
	}
	

	public SpeakToResponse getSpeak_to_resp() {
		return speak_to_resp;
	}

	public PointAtResponse getPoint_at_resp() {
		return point_at_resp;
	}

	public VerbalizeRegionRouteResponse getVerbalization_resp() {
		return verbalization_resp;
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

	void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException ex) {
		}
	}


}