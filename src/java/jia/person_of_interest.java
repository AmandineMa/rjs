package jia;

import org.ros.node.topic.Publisher;

import arch.ROSAgArch;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;

public class person_of_interest extends DefaultInternalAction {

	  @Override
	    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
		  Publisher<std_msgs.Int32> pub = ROSAgArch.getM_rosnode().getPerson_of_interest_pub();
		  int frame = Integer.parseInt(args[0].toString().replaceAll("^\"|\"$", ""));
		  std_msgs.Int32 msg = pub.newMessage();
		  msg.setData(frame);
		  pub.publish(msg);
		  return true;
	  }
}