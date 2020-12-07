package rjs.ros;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.Message;
import org.ros.master.client.MasterStateClient;
import org.ros.message.MessageFactory;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.rosjava.tf.TransformTree;
import org.ros.rosjava.tf.pubsub.TransformListener;

import geometry_msgs.Point;
import geometry_msgs.PointStamped;
import jason.asSemantics.ActionExec;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.NumberTermImpl;
import rjs.utils.Tools;
import std_msgs.Header;

/***
 * ROS node to be used by Jason
 * 
 * @author Google Code
 * @version 1.0
 * @since 2014-06-02
 *
 */
public abstract class AbstractRosNode extends AbstractNodeMain {
	protected Logger logger = Logger.getLogger(AbstractRosNode.class.getName());
	
	protected ConnectedNode connectedNode;
	protected NodeConfiguration nodeConfiguration;
	protected MessageFactory messageFactory;
	protected TransformListener tfl;
	protected ParameterTree parameters;
	protected MasterStateClient msc;
	protected HashMap<String, HashMap<String, String>> servicesMap;
	protected HashMap<String, ServiceClient<Message, Message>> serviceClients;

	public AbstractRosNode(String name) {
		nodeConfiguration = NodeConfiguration.newPrivate();
		messageFactory = nodeConfiguration.getTopicMessageFactory();
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
	}
	
	public void init() {
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

	@SuppressWarnings("unchecked")
	public <T> void callAsyncService(String serviceName, ServiceResponseListener<T> srl, Message params) {
			serviceClients.get(serviceName).call(params, (ServiceResponseListener<Message>) srl);
	}

	public <T> T callSyncService(String service, Message params) {
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

	public HashMap<String, Boolean> initServiceClients() {
		HashMap<String, Boolean> services_status = new HashMap<String, Boolean>();
		if(servicesMap != null) {
			for (Entry<String, HashMap<String, String>> entry : servicesMap.entrySet()) {
				services_status.put(entry.getKey(), createServiceClient(entry.getKey()));
			}
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
	
	public <T> T newServiceRequestFromType(String type) {
		return connectedNode.getServiceRequestMessageFactory().newFromType(type);
	}

	public PointStamped buildPointStamped(String frame) {
		PointStamped point = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.PointStamped._TYPE);
		Header header = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);
		header.setFrameId(frame);
		point.setHeader(header);
		return point;
	}

	public PointStamped build_point_stamped(ActionExec action, String frame) {
		MessageFactory messageFactory = connectedNode.getTopicMessageFactory();
		PointStamped point_stamped = buildPointStamped(frame);

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
	
}