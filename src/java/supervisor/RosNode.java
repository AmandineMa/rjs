package supervisor;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.message.Duration;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
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

import msg_srv_impl.*;

import actionlib_msgs.GoalStatusArray;
import deictic_gestures_msgs.PointAt;
import deictic_gestures_msgs.PointAtRequest;
import deictic_gestures_msgs.PointAtResponse;
import geometry_msgs.PointStamped;
import ontologenius_msgs.OntologeniusService;
import ontologenius_msgs.OntologeniusServiceRequest;
import ontologenius_msgs.OntologeniusServiceResponse;
import perspectives_msgs.Fact;
import perspectives_msgs.FactArrayStamped;
import perspectives_msgs.HasMesh;
import perspectives_msgs.HasMeshRequest;
import perspectives_msgs.HasMeshResponse;
import pointing_planner_msgs.PointingActionFeedback;
import pointing_planner_msgs.PointingActionGoal;
import pointing_planner_msgs.PointingActionResult;
import pointing_planner_msgs.PointingGoal;
import pointing_planner_msgs.VisibilityScore;
import pointing_planner_msgs.VisibilityScoreRequest;
import pointing_planner_msgs.VisibilityScoreResponse;
import route_verbalization.VerbalizeRegionRoute;
import route_verbalization.VerbalizeRegionRouteRequest;
import route_verbalization.VerbalizeRegionRouteResponse;
import semantic_route_description_msgs.SemanticRoute;
import semantic_route_description_msgs.SemanticRouteRequest;
import semantic_route_description_msgs.SemanticRouteResponse;
import speech_wrapper_msgs.SpeakTo;
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
	private ServiceClient<OntologeniusServiceRequest, OntologeniusServiceResponse> onto_individual_c;
	private ServiceClient<SemanticRouteRequest, SemanticRouteResponse> get_route_c;
	private ServiceClient<HasMeshRequest, HasMeshResponse> has_mesh_c;
	private ServiceClient<VisibilityScoreRequest, VisibilityScoreResponse> visibility_score_c;
	private ServiceClient<PointAtRequest, PointAtResponse> point_at_c;
	private ServiceClient<SpeakToRequest, SpeakToResponse> speak_to_c;
	private ServiceClient<VerbalizeRegionRouteRequest, VerbalizeRegionRouteResponse> route_verbalization_c;
	private ActionClient<PointingActionGoal, PointingActionFeedback, PointingActionResult> get_placements_ac;
	private Subscriber<perspectives_msgs.FactArrayStamped> facts_sub;
	private OntologeniusServiceResponse onto_individual_resp;
	private SemanticRouteResponseImpl get_route_resp;
	private HasMeshResponse has_mesh_resp;
	private VisibilityScoreResponse visibility_score_resp;
	private SpeakToResponse speak_to_resp;
	private PointAtResponse point_at_resp;
	private VerbalizeRegionRouteResponse verbalization_resp;
	private PointingActionResult get_placements_result;

	private Multimap<String, SimpleFact> perceptions = Multimaps.synchronizedMultimap(ArrayListMultimap.<String, SimpleFact>create());
	private volatile int percept_id = 0;


	public RosNode(String name) {
		this.name = name;
	}

	public GraphName getDefaultNodeName() {
		return GraphName.of("clients_node");
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		try {
			this.connectedNode = connectedNode;
			//TODO handle srv not started
			tfl = new TransformListener(connectedNode);
			
			onto_individual_c = connectedNode.newServiceClient("/ontologenius/individual",OntologeniusService._TYPE);

			get_route_c = connectedNode.newServiceClient("/semantic_route_description/get_route", SemanticRoute._TYPE);
			
			visibility_score_c = connectedNode.newServiceClient("/pointing_planner/visibility_score", VisibilityScore._TYPE);
			
			speak_to_c = connectedNode.newServiceClient("/speech_wrapper/speak_to", SpeakTo._TYPE);
			
			has_mesh_c = connectedNode.newServiceClient("/uwds_ros_bridge/has_mesh", HasMesh._TYPE);
			
			point_at_c = connectedNode.newServiceClient("/deictic_gestures/point_at", PointAt._TYPE);
			
			route_verbalization_c = connectedNode.newServiceClient("/route_verbalization/verbalizePlace", VerbalizeRegionRoute._TYPE);

			get_placements_ac = new ActionClient<PointingActionGoal, PointingActionFeedback, PointingActionResult>
			(connectedNode, "/pointing_planner/PointingPlanner", PointingActionGoal._TYPE, PointingActionFeedback._TYPE, PointingActionResult._TYPE);
			// too many useless loginfo in the class ActionClient (modified ActionClient by amdia to add the setLogLevel method)
			get_placements_ac.setLogLevel(Level.OFF);
			
			facts_sub = connectedNode.newSubscriber("/base/current_facts", perspectives_msgs.FactArrayStamped._TYPE);

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
			

			ActionClientListener<PointingActionFeedback, PointingActionResult> client_listener 
			= new ActionClientListener<PointingActionFeedback, PointingActionResult>(){

				public void feedbackReceived(PointingActionFeedback arg0) {}

				public void resultReceived(PointingActionResult result) {
					get_placements_result = result;
				}

				public void statusReceived(GoalStatusArray arg0) {}


			};
			get_placements_ac.attachListener(client_listener);
		} catch (ServiceNotFoundException e) {
			throw new RosRuntimeException(e);
		}
	}

	public void call_onto_indivual_srv(String action, String param) {
		onto_individual_resp = null;
		final OntologeniusServiceRequest request = onto_individual_c.newMessage();
		request.setAction(action);
		request.setParam(param);
		onto_individual_c.call(request, new ServiceResponseListener<OntologeniusServiceResponse>() {
			public void onSuccess(OntologeniusServiceResponse response) {
				onto_individual_resp = connectedNode.getServiceResponseMessageFactory().newFromType(OntologeniusService._TYPE);
				if(response.getValues().isEmpty()) {
					onto_individual_resp.setCode((short) Code.ERROR.getCode());
				}else {
					onto_individual_resp.setCode((short) Code.OK.getCode());
					onto_individual_resp.setValues(response.getValues());
				}
			}

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}
		});
	}

	public void call_get_route_srv(String from, String to, String persona, boolean signpost) {
		get_route_resp = null;
		final SemanticRouteRequest request = get_route_c.newMessage();
		request.setFrom(from);
		request.setTo(to);
		request.setPersona(persona);
		request.setSignpost(signpost);
		get_route_c.call(request, new ServiceResponseListener<SemanticRouteResponse>() {
			public void onSuccess(SemanticRouteResponse response) {
				get_route_resp = new SemanticRouteResponseImpl(response.getCosts(), response.getGoals(), response.getRoutes());
				if(response.getRoutes().isEmpty()) {
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
		final HasMeshRequest request = has_mesh_c.newMessage();
		request.setName(frame);
		request.setWorld(world);
		has_mesh_c.call(request, new ServiceResponseListener<HasMeshResponse>() {

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			public void onSuccess(HasMeshResponse response) {
				has_mesh_resp = response;
			}
			
		});
	}
	
	public void call_visibility_score_srv(String agent, String frame) {
		visibility_score_resp = null;
		final VisibilityScoreRequest request = visibility_score_c.newMessage();
		request.setAgentName(agent);
		request.setTargetName(frame);
		visibility_score_c.call(request, new ServiceResponseListener<VisibilityScoreResponse>() {

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			public void onSuccess(VisibilityScoreResponse response) {
				visibility_score_resp = response;
			}
			
		});
	}
	
	public void call_point_at_srv(String frame) {
		point_at_resp = null;
		final PointAtRequest request = point_at_c.newMessage();
		PointStamped point = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.PointStamped._TYPE);
		Header header = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);
		header.setFrameId(frame);
		point.setHeader(header);
		request.setPoint(point);
		point_at_c.call(request, new ServiceResponseListener<PointAtResponse>() {

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			public void onSuccess(PointAtResponse response) {
				point_at_resp = response;
			}
			
		});
	}
	
	public void call_speak_to_srv(String look_at, String text) {
		speak_to_resp = null;
		final SpeakToRequest request = speak_to_c.newMessage();
		PointStamped point = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.PointStamped._TYPE);
		Header header = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);
		header.setFrameId(look_at);
		point.setHeader(header);
		request.setLookAt(point);
		request.setText(text);
		speak_to_c.call(request, new ServiceResponseListener<SpeakToResponse>() {

			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			public void onSuccess(SpeakToResponse response) {
				speak_to_resp = response;
			}
			
		});
	}
	
	public void call_route_verbalization_srv(List<String> route, String start_place, String goal_shop) {
		verbalization_resp = null;
		final VerbalizeRegionRouteRequest request = route_verbalization_c.newMessage();
		request.setRoute(route);
		request.setStartPlace(start_place);
		request.setGoalShop(goal_shop);
		route_verbalization_c.call(request, new ServiceResponseListener<VerbalizeRegionRouteResponse>() {

			@Override
			public void onFailure(RemoteException e) {
				throw new RosRuntimeException(e);
			}

			@Override
			public void onSuccess(VerbalizeRegionRouteResponse response) {
				verbalization_resp = response;
			}
		});
	}

	public void call_svp_planner(String target_ld, String direction_ld, String human) {
		get_placements_result = null;
		boolean serverStarted = get_placements_ac.waitForActionServerToStart(new Duration(10));
		// TODO send info to supervisor agent
		if (serverStarted) {
			logger.info("Action server started.\n");
		} else {
			logger.info("No actionlib svp server found ");
		}
		PointingActionGoal goal_msg;
		goal_msg = get_placements_ac.newGoalMessage();
		PointingGoal svp_goal = goal_msg.getGoal();
		svp_goal.setHuman(human);
		svp_goal.setDirectionLandmark(direction_ld);
		svp_goal.setTargetLandmark(target_ld);
		goal_msg.setGoal(svp_goal);
		get_placements_ac.sendGoal(goal_msg);
		while(get_placements_ac.getGoalState() != ClientState.DONE) {
			sleep(100);
		}
	}
	

	public OntologeniusServiceResponse get_onto_individual_resp() {
		return onto_individual_resp;
	}

	public SemanticRouteResponseImpl get_get_route_resp() {
		return get_route_resp;
	}


	public PointingActionResult get_get_placements_result() {
		return get_placements_result;
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