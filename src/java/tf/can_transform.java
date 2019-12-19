package tf;

import org.ros.rosjava.tf.TransformTree;

import arch.agarch.AbstractROSAgArch;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;

public class can_transform extends DefaultInternalAction {

	  @Override
	    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
	    	String frame1 = args[0].toString();
	    	frame1 = frame1.replaceAll("^\"|\"$", "");
	    	String frame2 = args[1].toString();
	    	frame2 = frame2.replaceAll("^\"|\"$", "");
	    	TransformTree tfTree = ((AbstractROSAgArch) ts.getUserAgArch()).getTfTree();
	    	if(tfTree.canTransform(frame1, frame2)) {
	    		return true;
	    	}else {
	    		return false;
	    	}

	    }

}