package rjs.agent;

import jason.asSemantics.*;
import jason.asSyntax.*;
import jason.bb.*;
import rjs.utils.Tools;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Logger;

import org.ros.internal.loader.CommandLineLoader;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import com.google.common.collect.Lists;

public class TimeBB extends DefaultBeliefBase {
	
	// time in milliseconds

	private static double start;
	private static boolean start_initialized = false;
	private static ConnectedNode connected_node;
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(TimeBB.class.getSimpleName());

	@Override
	public void init(Agent ag, String[] args) {
		if(!start_initialized) {
			
			NodeConfiguration nodeConfiguration;
			NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
			if(System.getenv("ROS_MASTER_URI") != null && System.getenv("ROS_IP") != null /*&& !System.getenv("ROS_IP").equals("127.0.0.1")*/) {
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
				AbstractNodeMain node = new AbstractNodeMain() {	
					
					@Override
					public GraphName getDefaultNodeName() {
						return GraphName.of("supervisor_time");
					}
					@Override
					public void onStart(final ConnectedNode connectedNode) {
					connected_node = connectedNode;
					}
				};
				nodeMainExecutor.execute(node, nodeConfiguration);
				while(connected_node == null) {
					Tools.sleep(100);
				}
//				start =  connected_node.getCurrentTime().toSeconds() * 1000;
				start_initialized = true;
			}else {
				if(System.getenv("ROS_MASTER_URI") == null)
					logger.info("ROS_MASTER_URI has not been set");
				if(System.getenv("ROS_IP") == null)
					logger.info("ROS_IP has not been set");
				else if (System.getenv("ROS_IP").equals("127.0.0.1"))
					logger.info("ROS_IP should not be localhost");
			}
		}
		super.init(ag,args);
	}

	@Override
	public boolean add(Literal bel) {
		annote_time(bel);
		return super.add(bel, false);
	}

	@Override
	public boolean add(int index, Literal bel) {
		annote_time(bel);
		return super.add(bel, index != 0);
	}

	@Override
	protected boolean add(Literal bel, boolean addInEnd) {
		annote_time(bel);
		return super.add(bel, addInEnd);
	}

	protected void annote_time(Literal bel) {
		if (! hasTimeAnnot(bel)) {
			Structure time = new Structure("add_time");
//			double pass = connected_node.getCurrentTime().toSeconds() - start;
//			time.addTerm(new NumberTermImpl(pass));
			Double t = connected_node.getCurrentTime().toSeconds() * 1000;
			time.addTerm(new NumberTermImpl(t.longValue()));
			bel.addAnnot(time);
		}
	}

	private boolean hasTimeAnnot(Literal bel) {
		Literal belInBB = contains(bel);
		if (belInBB != null)
			for (Term a : belInBB.getAnnots())
				if (a.isStructure())
					if (((Structure)a).getFunctor().equals("add_time"))
						return true;
		return false;
	}
	
	public double getStartTime() {
		return start;
	}
}