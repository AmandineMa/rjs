package supervisor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import org.ros.node.ConnectedNode;
import org.ros.rosjava.tf.TransformTree;

import jason.architecture.MindInspectorAgArch;
import jason.asSyntax.Literal;
import supervisor.RosNode;
import supervisor.SimpleFact;

public class ROSAgArch extends MindInspectorAgArch {
	
	static protected RosNode m_rosnode;
	
	private int percept_id = -1;
	@SuppressWarnings("unused")
	protected Logger logger = Logger.getLogger(ROSAgArch.class.getName());
	
	
	@Override
	public void init() {
		setupMindInspector("gui(cycle,html,history)");    
	} 
	
	
	@Override
	public Collection<Literal> perceive() {
		Collection<Literal> l = new ArrayList<Literal>();
		if(m_rosnode != null) {
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
		}
		return l;
		
	}
	
	public ConnectedNode getConnectedNode() {
		return m_rosnode.getConnectedNode();
	}
	
	public TransformTree getTfTree() {
		return m_rosnode.getTfTree();
	}
	
	void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException ex) {
		}
	}

}
