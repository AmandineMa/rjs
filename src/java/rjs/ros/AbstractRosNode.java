package rjs.ros;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.Message;
import org.ros.master.client.MasterStateClient;
import org.ros.master.client.TopicSystemState;
import org.ros.message.MessageFactory;
import org.ros.message.MessageListener;
import org.ros.message.Time;
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
	
	protected String name;
	protected ConnectedNode connectedNode;
	protected NodeConfiguration nodeConfiguration;
	protected MessageFactory messageFactory;
	protected TransformListener tfl;
	protected ParameterTree parameters;
	protected MasterStateClient msc;
	protected HashMap<String, HashMap<String, String>> servicesMap = null;
	protected HashMap<String, ServiceClient<Message, Message>> serviceClients;
	protected HashMap<String, HashMap<String, String>> topicsMap = null;
	protected HashMap<String, Subscriber<Message>> subscribers;
	protected HashMap<String, Publisher<Message>> publishers;

	public AbstractRosNode(String name) {
		nodeConfiguration = NodeConfiguration.newPrivate();
		messageFactory = nodeConfiguration.getTopicMessageFactory();
		this.name = name;
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
		subscribers = new HashMap<String, Subscriber<Message>>();
		publishers = new HashMap<String, Publisher<Message>>();
		setServicesMap();
		setTopicsMap();
		createPublishers();
	}
	
	@SuppressWarnings("unchecked")
	public <T> void callAsyncService(Object service, ServiceResponseListener<T> srl, Message params) {
		if(service instanceof String)
			serviceClients.get(service).call(params, (ServiceResponseListener<Message>) srl);
		else if(service instanceof ServiceClient<?,?>)
			((ServiceClient<Message, Message>) service).call(params, (ServiceResponseListener<Message>) srl);
	}
	
	public <T> T callSyncService(Object service, Message params) {
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
		HashMap<String, Boolean> serviceStatus = new HashMap<String, Boolean>();
		if(servicesMap != null) {
			for (Entry<String, HashMap<String, String>> entry : servicesMap.entrySet()) {
				String key = entry.getKey();
				if(!serviceClients.containsKey(key)) {
					URI ls = msc.lookupService(servicesMap.get(key).get("name"));
					if(ls.toString().isEmpty()){
						serviceStatus.put(key, false);
					}else {
						createServiceClient(key);
						serviceStatus.put(key, true);
					}
				}
			}
		}
		return serviceStatus;
	}


	private void createServiceClient(String key) {
		String srv_name = servicesMap.get(key).get("name");
		try {
			serviceClients.put(key, connectedNode.newServiceClient(srv_name, servicesMap.get(key).get("type")));
		} catch (ServiceNotFoundException e) {
			logger.severe("Service not found exception : " + e.getMessage());
			throw new RosRuntimeException(e);
		} catch (Exception e) {
			logger.info(Tools.getStackTrace(e));
		}
	}
	
	public HashMap<String, Boolean> createSubscribers() {
		HashMap<String, Boolean> subStatus = new HashMap<String, Boolean>();
		if(topicsMap != null) {
			for (Entry<String, HashMap<String, String>> entry : topicsMap.entrySet()) {
				String key = entry.getKey();
				if(topicsMap.get(key).get("function").equals("sub") && !subscribers.containsKey(key)) {
//					if(!isTopicPublished(topicsMap.get(key).get("name"))){
//						subStatus.put(key, false);
//					}else {
						subscribers.put(key, connectedNode.newSubscriber(topicsMap.get(key).get("name"), topicsMap.get(key).get("type")));
						subStatus.put(key, true);
//					}
				}
			}
		}
		return subStatus;
	}
	
	public void createPublishers() {
		if(topicsMap != null) {
			for (Entry<String, HashMap<String, String>> entry : topicsMap.entrySet()) {
				String key = entry.getKey();
				if(topicsMap.get(key).get("function").equals("pub")) {
					publishers.put(key, connectedNode.newPublisher(topicsMap.get(key).get("name"), topicsMap.get(key).get("type")));
				}
			}
		}
	}
	
	private final boolean isTopicPublished(final String topicName) {
        boolean result = false;
        if (topicName != null) {
            for (final TopicSystemState topicSystemState : msc.getSystemState().getTopics()) {
                if (topicSystemState != null
                        && topicName.equals(topicSystemState.getTopicName())
                        && topicSystemState.getPublishers() != null
                        && !topicSystemState.getPublishers().isEmpty()) {
                    result = true;
                    break;
                }

            }
        }
        return result;
    }
	
	@SuppressWarnings("unchecked")
	public <T> void setSubListener(String subName, MessageListener<T> listener) {
		if(subscribers.get(subName) != null)
			subscribers.get(subName).addMessageListener((MessageListener<Message>) listener, 10);
	}
	
	public void publish(String pubName, Message message) {
		publishers.get(pubName).publish(message);
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
		header.setStamp(Time.fromMillis(0));
		return point;
	}
	
	public PointStamped buildPointStamped(String frame, ListTermImpl point_term) {
		MessageFactory messageFactory = connectedNode.getTopicMessageFactory();
		PointStamped point_stamped = buildPointStamped(frame);

		Point point = messageFactory.newFromType(Point._TYPE);
		point.setX(((NumberTermImpl) point_term.get(0)).solve());
		point.setY(((NumberTermImpl) point_term.get(1)).solve());
		point.setZ(((NumberTermImpl) point_term.get(2)).solve());
		point_stamped.setPoint(point);
		
		return point_stamped;
	}

	public PointStamped buildPointStamped(ActionExec action, String frame) {
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
	
	protected abstract void setServicesMap();
	
	protected abstract void setTopicsMap();
	
	public HashMap<String, HashMap<String, String>> getTopicsMap() {
		return topicsMap;
	}
	
}