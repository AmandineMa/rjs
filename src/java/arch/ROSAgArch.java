package arch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.ros.node.ConnectedNode;
import org.ros.rosjava.tf.TransformTree;

import com.google.common.collect.Multimap;

import jason.RevisionFailedException;
import jason.architecture.AgArch;
import jason.architecture.MindInspectorAgArch;
import jason.asSyntax.Literal;
import ros.RosNode;
import utils.SimpleFact;

public class ROSAgArch extends MindInspectorAgArch {
	
	static protected RosNode m_rosnode;
	
	protected ExecutorService executor;
	
	protected int percept_id = -1;

	protected Logger logger = Logger.getLogger(ROSAgArch.class.getName());
	
	
	public ROSAgArch() {
		super();
		executor = Executors.newFixedThreadPool(4);
	}
	

	@Override
	public void stop() {
		executor.shutdownNow();
		super.stop();
	}


	@Override
	public void init() {
//		setupMindInspector("gui(cycle,html,history)");    
	} 
	
	
	@Override
	public Collection<Literal> perceive() {
		Collection<Literal> l = new ArrayList<Literal>();
		if(m_rosnode != null) {
//			if(percept_id != m_rosnode.getPercept_id()) {
				Multimap<String,SimpleFact> mm = m_rosnode.getPerceptions();
				synchronized (mm) {
					String agent_name = "";
					if(mm.containsKey(getAgName()))
						agent_name = getAgName();
					if(mm.containsKey("\""+getAgName()+"\""));
						agent_name = "\""+getAgName()+"\"";
					Collection<SimpleFact> perceptions = new ArrayList<SimpleFact>(mm.get(agent_name));
					if(perceptions != null) {
						for(SimpleFact percept : perceptions) {
							if(percept.getObject().isEmpty()) {
								l.add(Literal.parseLiteral(percept.getPredicate()));
							}else {
								l.add(Literal.parseLiteral(percept.getPredicate()+"("+percept.getObject()+")"));
							}
						}
					}
				}
				List<String> change_id = m_rosnode.getChange_id();
				if(change_id != null && !change_id.isEmpty()) {
					synchronized (change_id) {
						if(getAgName().equals("robot")) {
							try {
								getTS().getAg().addBel(Literal.parseLiteral("change_id("+change_id.get(0)+","+change_id.get(1)+")"));
								change_id.clear();
							} catch (RevisionFailedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
//				percept_id = m_rosnode.getPercept_id();
//			}
		}
		return l;
		
	}
	
	public ConnectedNode getConnectedNode() {
		return m_rosnode.getConnectedNode();
	}
	
	
	public static RosNode getM_rosnode() {
		return m_rosnode;
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
