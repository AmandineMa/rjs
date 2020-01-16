package ros;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.Message;
import org.ros.internal.node.response.StatusCode;
import org.ros.master.client.MasterStateClient;
import org.ros.message.MessageFactory;
import org.ros.message.MessageListener;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava.tf.TransformTree;
import org.ros.rosjava.tf.pubsub.TransformListener;

import com.github.rosjava_actionlib.GoalIDGenerator;

import geometry_msgs.Point;
import geometry_msgs.PointStamped;
import jason.asSemantics.ActionExec;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.NumberTermImpl;
import std_msgs.Header;
import utils.Tools;

/***
 * ROS node to be used by Jason
 * 
 * @author Google Code
 * @version 1.0
 * @since 2014-06-02
 *
 */
public abstract class RosNode extends AbstractNodeMain {
	private Logger logger = Logger.getLogger(RosNode.class.getName());
	
	protected GoalIDGenerator goalIDGenerator;
	protected ConnectedNode connectedNode;
	protected NodeConfiguration nodeConfiguration;
	protected MessageFactory messageFactory;
	protected TransformListener tfl;
	protected ParameterTree parameters;
	protected MasterStateClient msc;
	HashMap<String, HashMap<String, String>> servicesMap;
	protected HashMap<String, ServiceClient<Message, Message>> serviceClients;

	public RosNode(String name) {
		nodeConfiguration = NodeConfiguration.newPrivate();
		messageFactory = nodeConfiguration.getTopicMessageFactory();
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
	}
	
	public void init() {
		goalIDGenerator = new GoalIDGenerator(getConnectedNode());
		tfl = new TransformListener(connectedNode);
		parameters = connectedNode.getParameterTree();
		URI uri = null;
		try {
			uri = new URI(System.getenv("ROS_MASTER_URI"));
		} catch (URISyntaxException e) {
			logger.info("Wrong URI syntax :" + e.getMessage());
		}
		msc = new MasterStateClient(connectedNode, uri);
		serviceClients = new HashMap<String, ServiceClient<Message, Message>>();
		servicesMap = null;
	}

	public <T> void callAsyncService(String serviceName, ServiceResponseListener<T> srl, Map<String, Object> params) {
		HashMap<String, String> mapInfoService = servicesMap.get(serviceName);

		if (mapInfoService != null && mapInfoService.containsKey("type")) {
			String type = mapInfoService.get("type");
			type = type.replace('/', '.') + "Request";
			callService(serviceName, type, srl, params);
		} else {
			logger.info("Service (" + serviceName + ") not declared in yaml or type not filled");
			srl.onFailure(new RemoteException(StatusCode.ERROR, "Service (" + serviceName + ") not declared in yaml or type not filled"));
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
			Tools.getStackTrace(e);
		}
		return response;
	}

	@SuppressWarnings("unchecked")
	public <T> void callService(String serviceName, String className, ServiceResponseListener<T> srl,
			Map<String, Object> params) {
//		logger.info("Calling service (" + serviceName + ") with class: " + className);
		Message msg = serviceClients.get(serviceName).newMessage();

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
			Tools.getStackTrace(e);
		}

		if(params != null) {
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
					Tools.getStackTrace(e);
				}
			}
		}

//		logger.info("Calling CALL method");
		serviceClients.get(serviceName).call(msg, (ServiceResponseListener<Message>) srl);
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

	public HashMap<String, Boolean> initServiceClients() {
		HashMap<String, Boolean> services_status = new HashMap<String, Boolean>();

		for (Entry<String, HashMap<String, String>> entry : servicesMap.entrySet()) {
			services_status.put(entry.getKey(), createServiceClient(entry.getKey()));
		}
		return services_status;
	}

	public HashMap<String, Boolean> retryInitServiceClients(Set<String> clients_to_init) {
		HashMap<String, Boolean> services_status = new HashMap<String, Boolean>();

		for (String client : clients_to_init) {
			services_status.put(client, createServiceClient(client));
		}
		return services_status;
	}

	private boolean createServiceClient(String key) {
		boolean status = false;
		String srv_name = servicesMap.get(key).get("name");
		URI ls = msc.lookupService(srv_name);
		if (ls.toString().isEmpty()) {
			serviceClients.put(key, null);
			status = false;
		} else {
			ServiceClient<Message, Message> serv_client;
			try {
				logger.info("connect to " + srv_name);
				serv_client = connectedNode.newServiceClient(srv_name, servicesMap.get(key).get("type"));
				serviceClients.put(key, serv_client);
				status = true;
			} catch (ServiceNotFoundException e) {
				logger.severe("Service not found exception : " + e.getMessage());
				throw new RosRuntimeException(e);
			} catch (Exception e) {
				logger.info(Tools.getStackTrace(e));
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

	public ConnectedNode getConnectedNode() {
		return connectedNode;
	}

	public TransformTree getTfTree() {
		return tfl.getTree();
	}

	public ParameterTree getParameters() {
		return parameters;
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
	
	
}