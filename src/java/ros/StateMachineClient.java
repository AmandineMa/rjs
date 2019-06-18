package ros;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.Message;
import org.ros.message.Duration;
import org.ros.message.MessageListener;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Subscriber;

import resource_management_msgs.StateMachinesCancel;
import resource_management_msgs.StateMachinesCancelRequest;
import resource_management_msgs.StateMachinesCancelResponse;
import resource_management_msgs.StateMachinesStatus;

public class StateMachineClient<MessageRequest, MessageResponse extends SMCMessageResponse> extends AbstractNodeMain{
	private Logger logger = Logger.getLogger(StateMachineClient.class.getName());
	
	private ConnectedNode nh_;
	private String name_;
	private int id_;
	private boolean synchronised_;


	private String register_topic_name_;
	private String cancel_topic_name_;
	private String status_topic_name_;
	private Subscriber<StateMachinesStatus> status_subscriber_;
	ServiceClient<MessageRequest, MessageResponse> send_client_;
	ServiceClient<StateMachinesCancelRequest, StateMachinesCancelResponse> cancel_client_;
	
	private Boolean cancellation_ack_ = null;
	
//	private Supplier<stateMachineState_t> status_callback_;

	private stateMachineState_t state_;

	private class stateMachineState_t
	{

		String state_name_;
		String state_event_;

		@SuppressWarnings("unused")
		stateMachineState_t() {}
		@SuppressWarnings("unused")
		stateMachineState_t(String state_name, String state_event){
			state_name_ = state_name;
			state_event_ = state_event;
		}


		public String toString(){
			return state_name_ + " : " + state_event_;
		}


	}

	public GraphName getDefaultNodeName() {
		return GraphName.of("state_machine_client");
	}
	
	StateMachineClient(String name, boolean synchronised){
		name_ = "/" + name;
		id_ = -1;
		synchronised_ = synchronised;
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.nh_ = connectedNode;
		
		while(nh_ == null) {
			sleep(100);
		}
		
		register_topic_name_ = synchronised_ ? name_ + "/state_machines_register__" : name_ + "/state_machines_register";
		cancel_topic_name_ = synchronised_ ? name_ + "/state_machine_cancel__" : name_ + "/state_machine_cancel";
		status_topic_name_ = synchronised_ ? name_ + "/state_machine_status__" : name_ + "/state_machine_status";
		
		try {
			send_client_ = nh_.newServiceClient(register_topic_name_, get_srv_type(register_topic_name_));
		} catch (ServiceNotFoundException e) {
			logger.severe("Service not found exception : "+e.getMessage());
			throw new RosRuntimeException(e);
		}
		
		try {
			cancel_client_ = nh_.newServiceClient(cancel_topic_name_, StateMachinesCancel._TYPE);
		} catch (ServiceNotFoundException e) {
			logger.severe("Service not found exception : "+e.getMessage());
			throw new RosRuntimeException(e);
		}
		
		status_subscriber_ = nh_.newSubscriber(status_topic_name_, StateMachinesStatus._TYPE);
		status_subscriber_.addMessageListener(new MessageListener<StateMachinesStatus>() {

			@Override
			public void onNewMessage(StateMachinesStatus msg) {
				if((int)msg.getId() == id_){
					state_.state_name_ = msg.getStateName();
					state_.state_event_ = msg.getStateEvent();
//					if(status_callback_ != null)
//						status_callback_(state_);
				}
			}
		});
		
		
	}
	
	
//	public void registerStatusCallback(Supplier<stateMachineState_t> status_callback) {
//		status_callback_ = status_callback;
//	}


	public void send(MessageRequest req){
		send_client_.call(req, new ServiceResponseListener<MessageResponse>() {

			@Override
			public void onFailure(RemoteException arg0) {
				throw new RosRuntimeException(arg0);
			}

			@Override
			public void onSuccess(MessageResponse arg0) {
				id_ = arg0.getId();
				state_.state_name_ = "_";
			}
		});
	}

	//TODO remove duplicate code with RosNode
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
	
	public boolean waitForResult(){  
	  boolean end = false;

	  while(end == false){
	    if(state_.state_name_ == ""){
	      end = true;
	      continue;
	    }
	  }
	  return end;
	}
	
	public boolean waitForResult(Duration timeout){  
		boolean end = false;
		Time strat = nh_.getCurrentTime();
		
		while(end == false & (nh_.getCurrentTime().subtract(strat)).compareTo(timeout) < 0){
			if(state_.state_name_ == ""){
				end = true;
				continue;
			}
		}
		return end;
	}
	
	public boolean cancel(){
		cancellation_ack_ = null;
		if(id_ == -1)
			return false;

		StateMachinesCancelRequest srv = cancel_client_.newMessage();
		srv.setId(id_);

		cancel_client_.call(srv, new ServiceResponseListener<StateMachinesCancelResponse>() {

			@Override
			public void onFailure(RemoteException arg0) {
				throw new RosRuntimeException(arg0);
			}

			@Override
			public void onSuccess(StateMachinesCancelResponse arg0) {
				cancellation_ack_ = arg0.getAck();
			}

		});
		
		while(cancellation_ack_ == null) {
			sleep(200);
		}
		
		return cancellation_ack_;
	}
	
	void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException ex) {
		}
	}

}
