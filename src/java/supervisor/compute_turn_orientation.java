// Internal action code for project supervisor
package supervisor;

import jason.asSemantics.*;
import jason.asSyntax.*;

import javax.vecmath.Quat4d;

import org.ros.rosjava.tf.Transform;
import org.ros.rosjava.tf.TransformTree;

@SuppressWarnings("serial")
public class compute_turn_orientation extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	String frame1 = args[0].toString();
    	frame1 = frame1.replaceAll("^\"|\"$", "");
    	String frame2 = args[1].toString();
    	frame2 = frame2.replaceAll("^\"|\"$", "");
    	TransformTree tfTree = ((ROSAgArch) ts.getUserAgArch()).getTfTree();
    	Transform transform;
    	try {
    		transform = tfTree.lookupMostRecent(frame1, frame2);
    		Quat4d quat = transform.rotation;
    		quat.normalize();
    		EulerAngle eulerAngle = toEulerAngle(quat);
    		if(eulerAngle.yaw > 0) {
    			return un.unifies(args[args.length - 1], new ObjectTermImpl("turn_left"));
    		}else{
    			return un.unifies(args[args.length - 1], new ObjectTermImpl("turn_right"));
    		}
    	}
    	catch (Exception e) {
    		return un.unifies(args[args.length - 1], Literal.LFalse);
    	}

    }

    public EulerAngle toEulerAngle(Quat4d q1) {
    	double test = q1.getX()*q1.getY() + q1.getZ()*q1.getW();
    	double yaw;
    	double pitch;
    	double roll;
    	EulerAngle eulerAngle;
    	if (test > 0.499) { // singularity at north pole
    		yaw = 2 * Math.atan2(q1.getX(),q1.getW());
    		pitch = Math.PI/2;
    		roll = 0;
    		eulerAngle = new EulerAngle(roll, pitch,yaw);
    		return eulerAngle;
    	}
    	if (test < -0.499) { // singularity at south pole
    		yaw = -2 * Math.atan2(q1.getX(),q1.getW());
    		pitch = - Math.PI/2;
    		roll = 0;
    		eulerAngle = new EulerAngle(roll, pitch,yaw);
    		return eulerAngle;
    	}
        double sqx = q1.getX()*q1.getX();
        double sqy = q1.getY()*q1.getY();
        double sqz = q1.getZ()*q1.getZ();
        yaw = Math.atan2(2*q1.getY()*q1.getW()-2*q1.getX()*q1.getZ() , 1 - 2*sqy - 2*sqz);
    	pitch = Math.asin(2*test);
    	roll = Math.atan2(2*q1.getX()*q1.getW()-2*q1.getY()*q1.getZ() , 1 - 2*sqx - 2*sqz);
    	eulerAngle = new EulerAngle(roll, pitch,yaw);
		return eulerAngle;
    }
    
    class EulerAngle{
  	
    	double roll;
    	double pitch;
    	double yaw;
    	
		public EulerAngle(double roll, double pitch, double yaw) {
			this.yaw = yaw;
			this.pitch = pitch;
			this.roll = roll;
		}
		
    }
}

