package tf;

import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Vector3;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;

public class quat_face_human extends DefaultInternalAction {

	  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
	    	ListTerm robot_pose = (ListTerm) args[0];
	    	ListTerm human_pose = (ListTerm) args[1];
			float angle = (float) Math.atan2(((NumberTermImpl) human_pose.get(1)).solve() - ((NumberTermImpl) robot_pose.get(1)).solve(), 
											((NumberTermImpl) human_pose.get(0)).solve() - ((NumberTermImpl) robot_pose.get(0)).solve());
			Quaternion q = Quaternion.fromAxisAngle(new Vector3(0,0,1), angle);
			ListTerm listterm_q = new ListTermImpl();
			listterm_q.add(new NumberTermImpl(q.getX()));
			listterm_q.add(new NumberTermImpl(q.getY()));
			listterm_q.add(new NumberTermImpl(q.getZ()));
			listterm_q.add(new NumberTermImpl(q.getW()));
			return un.unifies(args[2], listterm_q);
	    	

	    }

}