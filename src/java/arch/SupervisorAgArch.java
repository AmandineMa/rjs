package arch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.ros.helpers.ParameterLoaderNode;
import org.ros.internal.loader.CommandLineLoader;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import com.github.rosjava_actionlib.ActionServerListener;
import com.github.rosjava_actionlib.GoalIDGenerator;
import com.google.common.collect.Lists;

import actionlib_msgs.GoalID;
import guiding_as_msgs.taskActionGoal;
import jason.RevisionFailedException;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Term;
import ros.RosNode;

public class SupervisorAgArch extends ROSAgArch {
	private NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
	private NodeConfiguration nodeConfiguration;
	private ParameterLoaderNode parameterLoaderNode;
	private GoalIDGenerator goalIDGenerator;
	
	@Override
    public void act(final ActionExec action) {
		executor.execute(new Runnable() {
			
			@Override
			public void run() {
				String action_name = action.getActionTerm().getFunctor();
				if(action_name.equals("configureNode")){
					if(System.getenv("ROS_MASTER_URI") != null && System.getenv("ROS_IP") != null && !System.getenv("ROS_IP").equals("127.0.0.1")) {
						List<String> emptyArgv = Lists.newArrayList("EmptyList");
						CommandLineLoader loader = new CommandLineLoader(emptyArgv);
						URI masterUri = null;
						nodeConfiguration = loader.build();	
						try {
							masterUri = new URI(System.getenv("ROS_MASTER_URI"));			
						} catch (URISyntaxException e) {
							logger.info("Wrong URI syntax :" + e.getMessage());
						} 
						nodeConfiguration.setMasterUri(masterUri);
						action.setResult(true);
					}else {
						action.setResult(false);
						if(System.getenv("ROS_MASTER_URI") == null)
							logger.info("ROS_MASTER_URI has not been set");
						if(System.getenv("ROS_IP") == null)
							logger.info("ROS_IP has not been set");
						else if (System.getenv("ROS_IP").equals("127.0.0.1"))
							logger.info("ROS_IP should not be localhost");
					}
					actionExecuted(action);
				}else if(action_name.equals("startParameterLoaderNode")){
					@SuppressWarnings("serial")
					List<ParameterLoaderNode.Resource> resourceList = new ArrayList<ParameterLoaderNode.Resource>() {{
						add(new ParameterLoaderNode.Resource(getClass().getResourceAsStream("/guiding.yaml"), ""));
					}}; 
					parameterLoaderNode = new ParameterLoaderNode(resourceList);
					nodeMainExecutor.execute(parameterLoaderNode, nodeConfiguration);
					action.setResult(true);
		        	actionExecuted(action);
		        	
				}else if(action_name.equals("startROSNode")){
					m_rosnode = new RosNode("node_test");
					nodeMainExecutor.execute(m_rosnode, nodeConfiguration);
					while(m_rosnode.getConnectedNode() == null) {
						sleep(100);
					}
					m_rosnode.init();
					goalIDGenerator = new GoalIDGenerator(getConnectedNode());
					action.setResult(true);
		        	actionExecuted(action);
				}else if(action_name.equals("initServices")){
					HashMap<String, Boolean> services_status = m_rosnode.init_service_clients();
					action.setResult(true);
					for(Entry<String, Boolean> entry : services_status.entrySet()) {
						try {
							if(entry.getValue()) {
								getTS().getAg().addBel(Literal.parseLiteral("connected_srv("+entry.getKey()+")"));
							}else {
								getTS().getAg().addBel(Literal.parseLiteral("~connected_srv("+entry.getKey()+")"));
								action.setResult(false);
								action.setFailureReason(new Atom("srv_not_connected"), "Some services are not connected");
							}
						} catch (RevisionFailedException e) {
							logger.info("Belief could not be added to the belief base :"+e.getMessage());
						}
					}
					actionExecuted(action);
					
				}else if(action_name.equals("initGuidingAs")) {
					ActionServerListener<taskActionGoal> listener = new ActionServerListener<taskActionGoal>() {

						@Override
						public void goalReceived(taskActionGoal goal) {
							GoalID goalID = goal.getGoalId();
							if(goal.getGoalId().getId().isEmpty()) {
								goalIDGenerator.generateID(goalID);
							}
						}

						@Override
						public void cancelReceived(GoalID id) {
							try {
								getTS().getAg().addBel(Literal.parseLiteral("cancel_goal(\""+id.getId()+"\")"));
							} catch (RevisionFailedException e) {
								e.printStackTrace();
							}
						}

						@Override
						public boolean acceptGoal(taskActionGoal goal) {
							String person = "\""+goal.getGoal().getPersonFrame()+"\"";
							if(m_rosnode.getParameters().getBoolean("guiding/dialogue/hwu"))
								person = person.replaceAll("human-", "");
							try {
								getTS().getAg().addBel(Literal.parseLiteral("guiding_goal(\""+goal.getGoalId().getId()+"\","+person+",\""+goal.getGoal().getPlaceFrame()+"\")"));
							} catch (RevisionFailedException e) {
								e.printStackTrace();
							}
							return true;
						}
					};
					m_rosnode.set_guiding_as_listener(listener);
					action.setResult(true);
					actionExecuted(action);
				}else if(action_name.equals("retryInitServices")){
					action.setResult(true);
					LogicalFormula logExpr = Literal.parseLiteral("~connected_srv(X)");
					Iterator<Unifier> iu = logExpr.logicalConsequence(getTS().getAg(), new Unifier());
					Set<String> list = new HashSet<String>();
					Term var = Literal.parseLiteral("X");
			        while (iu.hasNext()) {
			        	Term term = var.capply(iu.next());
			        	list.add(term.toString());
			        }
					HashMap<String, Boolean> services_status = m_rosnode.retry_init_service_clients(list);
					for(Entry<String, Boolean> entry : services_status.entrySet()) {
						try {
							if(entry.getValue()) {
								getTS().getAg().addBel(Literal.parseLiteral("connected_srv("+entry.getKey()+")"));
								getTS().getAg().delBel(Literal.parseLiteral("~connected_srv("+entry.getKey()+")"));
							}else {
								getTS().getAg().addBel(Literal.parseLiteral("~connected_srv("+entry.getKey()+")"));
								action.setResult(false);
								action.setFailureReason(new Atom("srv_not_connected"), "Some services are not connected");
							}
						} catch (RevisionFailedException e) {
							logger.info("Belief could not be added to the belief base :"+e.getMessage());
						}
					}
					actionExecuted(action);
				}else if(action_name.equals("set_guiding_result")){
					String success = action.getActionTerm().getTerm(0).toString();
					success = success.replaceAll("^\"|\"$", "");
					String id = action.getActionTerm().getTerm(1).toString();
					id = id.replaceAll("^\"|\"$", "");
					m_rosnode.set_task_result(success, id);
					logger.info("goal result : "+success);
					action.setResult(true);
					actionExecuted(action);
				}
				
			}
		});
		
	}
	

}
