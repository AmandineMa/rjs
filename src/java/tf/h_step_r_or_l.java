package tf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;
import org.ros.rosjava.tf.Transform;
import org.ros.rosjava.tf.TransformFactory;
import org.ros.rosjava.tf.TransformTree;
import org.ros.rosjava_geometry.Vector3;

import arch.agarch.AbstractROSAgArch;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;
import utils.Tools;

public class h_step_r_or_l extends DefaultInternalAction {
	
	  @Override
	    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
	    	String human = args[0].toString();
	    	human = human.replaceAll("^\"|\"$", "");
	    	List<Double> point_values = new ArrayList<>();
	    	Iterator<Term> values_it =  ((ListTermImpl) args[1]).iterator();
			while(values_it.hasNext()) {
				point_values.add(((NumberTermImpl)values_it.next()).solve());
			}
	    	TransformTree tfTree = ((AbstractROSAgArch) ts.getUserAgArch()).getTfTree();
			Transform human_pose_now = tfTree.lookupMostRecent("map", human);
			Transform robot_pose_now = tfTree.lookupMostRecent("map", "base_footprint");
			if(human_pose_now != null && robot_pose_now != null) {
				String side;
				NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
				MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();
	
				geometry_msgs.Vector3 vector_msg = messageFactory.newFromType(geometry_msgs.Vector3._TYPE);
	
				vector_msg.setX(((NumberTermImpl) ((ListTermImpl) args[1]).get(0)).solve());
				vector_msg.setY(((NumberTermImpl) ((ListTermImpl) args[1]).get(1)).solve());
				vector_msg.setZ(((NumberTermImpl) ((ListTermImpl) args[1]).get(2)).solve());
				// isLeft from robot view then it is right from human view
				side = Tools.isLeft(TransformFactory.vector2msg(robot_pose_now.translation),
								    TransformFactory.vector2msg(human_pose_now.translation),
								    vector_msg) ? "right" : "left";
				
		    	return un.unifies(args[2], new Atom(Literal.parseLiteral(side)));
			}else {
				return false;
			}

	    }

}