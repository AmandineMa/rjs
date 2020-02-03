package rjs.tf;
// Internal action code for project supervisor

import org.ros.rosjava.tf.Transform;
import org.ros.rosjava.tf.TransformTree;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;
import rjs.arch.agarch.AbstractROSAgArch;

public class is_dist_human2robot_sup extends DefaultInternalAction {

	  @Override
	    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
	    	String human = args[0].toString();
	    	human = human.replaceAll("^\"|\"$", "");
			TransformTree tfTree = ((AbstractROSAgArch) ts.getUserAgArch()).getTfTree();
			Atom is_dist_sup = new Atom(Literal.parseLiteral("no_transform"));
			Transform h_tf = tfTree.lookupMostRecent("map", human);
			Transform r_tf = tfTree.lookupMostRecent("map", "base_footprint");
			if(h_tf != null && r_tf != null) {
				double h_dist_to_r = Math.hypot(h_tf.translation.x - r_tf.translation.x, h_tf.translation.y - r_tf.translation.y);
				
				is_dist_sup = h_dist_to_r > ((NumberTermImpl) args[1]).solve() ? new Atom(Literal.parseLiteral("true")) :  new Atom(Literal.parseLiteral("false"));
			}
			return un.unifies(args[2], is_dist_sup);

	    }

}
