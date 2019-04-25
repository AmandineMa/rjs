package supervisor;

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

import com.google.common.collect.Lists;

import jason.RevisionFailedException;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Term;

public class SupervisorAgArch extends ROSAgArch {
	private NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
	private NodeConfiguration nodeConfiguration;
	private ParameterLoaderNode parameterLoaderNode;
	
	public void init() {
		super.init();
		List<String> emptyArgv = Lists.newArrayList("EmptyList");
		CommandLineLoader loader = new CommandLineLoader(emptyArgv);
		nodeConfiguration = loader.build();	
	}
	
	@Override
    public void act(final ActionExec action) {
		executor.execute(new Runnable() {
			
			@Override
			public void run() {
				String action_name = action.getActionTerm().getFunctor();
				if(action_name.equals("startParameterLoaderNode")){
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
				}
				
			}
		});
		
	}
	

}
