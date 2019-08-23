package tf;
// Internal action code for project supervisor

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.vecmath.Vector3d;

import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;
import org.ros.rosjava.tf.Transform;
import org.ros.rosjava.tf.TransformTree;

import arch.ROSAgArch;
import arch.RobotAgArch;
import geometry_msgs.Pose;
import jason.asSemantics.*;
import jason.asSyntax.*;
import msg_srv_impl.PoseCustom;

public class is_dist_human2point_sup extends DefaultInternalAction {
	
	private Logger logger = Logger.getLogger(is_dist_human2point_sup.class.getName());

	  @Override
	    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
	    	String human = args[0].toString();
	    	human = human.replaceAll("^\"|\"$", "");
	    	Atom is_dist_sup = new Atom(Literal.parseLiteral("no_transform"));
	    	Iterator<Term> values_it =  ((ListTermImpl) args[1]).iterator();
			List<Double> point_values = new ArrayList<>();
			while(values_it.hasNext()) {
				point_values.add(((NumberTermImpl)values_it.next()).solve());
			}
			TransformTree tfTree = ((ROSAgArch) ts.getUserAgArch()).getTfTree();
			Transform human_pose_now = tfTree.lookupMostRecent("map", human);
			
			
			
			if(human_pose_now != null) {
				double h_dist_to_new_pose = Math.hypot(human_pose_now.translation.x - point_values.get(0), 
													   human_pose_now.translation.y - point_values.get(1));
				logger.info("dist from wanted pos :"+h_dist_to_new_pose);
				is_dist_sup =  h_dist_to_new_pose > ((NumberTermImpl) args[2]).solve() ? new Atom(Literal.parseLiteral("true")) :  new Atom(Literal.parseLiteral("false"));
			}
			return un.unifies(args[3], is_dist_sup);
	    }

}
