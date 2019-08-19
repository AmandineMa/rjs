package tf;
// Internal action code for project supervisor

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.vecmath.Vector3d;

import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;
import org.ros.rosjava.tf.Transform;
import org.ros.rosjava.tf.TransformTree;

import arch.ROSAgArch;
import geometry_msgs.Pose;
import jason.asSemantics.*;
import jason.asSyntax.*;
import msg_srv_impl.PoseCustom;

public class is_dist_human2robot_sup extends DefaultInternalAction {

	  @Override
	    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
	    	String human = args[0].toString();
	    	human = human.replaceAll("^\"|\"$", "");
			TransformTree tfTree = ((ROSAgArch) ts.getUserAgArch()).getTfTree();
			Transform transform = tfTree.lookupMostRecent("base_footprint", human);
			double h_dist_to_r = Math.hypot(transform.translation.x, transform.translation.y);
			
			boolean is_dist_sup = h_dist_to_r > ((NumberTermImpl) args[1]).solve() ? true : false;
			return is_dist_sup;

	    }

}
