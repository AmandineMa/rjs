package tf;
// Internal action code for project supervisor

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;
import org.ros.rosjava.tf.Transform;
import org.ros.rosjava.tf.TransformTree;

import arch.ROSAgArch;
import geometry_msgs.Pose;
import jason.asSemantics.*;
import jason.asSyntax.*;
import msg_srv_impl.PoseCustom;

public class is_dist_human2point_sup extends DefaultInternalAction {

	  @Override
	    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
	    	String human = args[0].toString();
	    	human = human.replaceAll("^\"|\"$", "");
	    	Iterator<Term> values_it =  ((ListTermImpl) args[1].iterator();
			List<Double> point_values = new ArrayList<>();
			while(values_it.hasNext()) {
				point_values.add(((NumberTermImpl)values_it.next()).solve());
			}
			TransformTree tfTree = ((ROSAgArch) ts.getUserAgArch()).getTfTree();
			Transform human_pose_now = tfTree.lookupMostRecent("map", human);
			double h_dist_to_new_pose = Math.hypot(human_pose_now.translation.x - robot_pose.getPosition().getX(), 
												   human_pose_now.translation.y - robot_pose.getPosition().getY());
			NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
			MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();
			Point point = messageFactory.newFromType(Pose._TYPE);
			PoseCustom pose = new PoseCustom(pose_values);
	    	TransformTree tfTree = ((ROSAgArch) ts.getUserAgArch()).getTfTree();
	    	Transform transform;
	    	if(tfTree.canTransform(frame1, frame2)) {
		    	try {
		    		
		    		transform = tfTree.lookupMostRecent(frame1, frame2);
		    		String translation = transform.translation.toString();
		    		translation = translation.replaceAll("\\(", "[").replaceAll("\\)", "]");
		    		ListTerm listterm_trans = ListTermImpl.parseList(translation);
		    		String rotation = transform.rotation.toString();
		    		rotation = rotation.replaceAll("\\(", "[").replaceAll("\\)", "]");
		    		ListTerm listterm_rot = ListTermImpl.parseList(rotation);
		    		boolean ok = un.unifies(args[2], listterm_trans);
		    		if(ok)
		    			un.unifies(args[3], listterm_rot);
		    		return ok;
		    	}
		    	catch (Exception e) {
		    		return false;
		    	}
	    	}else {
	    		return false;
	    	}

	    }

}
