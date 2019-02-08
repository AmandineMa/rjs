package supervisor;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import org.ros.node.ConnectedNode;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.rosjava.tf.TransformTree;

import jason.architecture.MindInspectorAgArch;
import jason.asSyntax.Literal;
import supervisor.RosNode;
import supervisor.SimpleFact;

public class ROSAgArch extends MindInspectorAgArch {
	
	static RosNode                      	   m_rosnode;
	private static NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
	private static NodeConfiguration nodeConfiguration;
	static URI masteruri;
	private int percept_id = -1;
	@SuppressWarnings("unused")
	private Logger logger = Logger.getLogger(ROSAgArch.class.getName());
	
	static {
		masteruri = URI.create("http://140.93.7.251:11311");
		nodeConfiguration = NodeConfiguration.newPublic("140.93.7.251", masteruri);
		m_rosnode = new RosNode("node_test");
		nodeMainExecutor.execute(m_rosnode, nodeConfiguration);
	}
	
	@Override
	public void init() {
		setupMindInspector("gui(cycle,html,history)");    
	} 
	
	
	@Override
	public Collection<Literal> perceive() {
//		logger.info(getTS().getAg().getBB().toString());
		Collection<Literal> l = new ArrayList<Literal>();
		if(percept_id != m_rosnode.getPercept_id()) {
			Collection<SimpleFact> perceptions = new ArrayList<SimpleFact>(m_rosnode.getPerceptions().get(getAgName()));
			if(perceptions != null) {
				for(SimpleFact percept : perceptions) {
					if(percept.getObject().isEmpty()) {
						l.add(Literal.parseLiteral(percept.getPredicate()));
					}else {
						l.add(Literal.parseLiteral(percept.getPredicate()+"("+percept.getObject()+")"));
					}
				}
			}
			percept_id = m_rosnode.getPercept_id();
		}
		return l;
	}
	
	public ConnectedNode getConnectedNode() {
		return m_rosnode.getConnectedNode();
	}
	
	public TransformTree getTfTree() {
		return m_rosnode.getTfTree();
	}

}
