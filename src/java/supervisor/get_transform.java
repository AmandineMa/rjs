package supervisor;
// Internal action code for project supervisor

import org.ros.rosjava.tf.Transform;
import org.ros.rosjava.tf.TransformTree;

import jason.asSemantics.*;
import jason.asSyntax.*;
import supervisor.ROSAgArch;

public class get_transform extends DefaultInternalAction {

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

	    }

}
