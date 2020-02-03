package rjs.tf;
// Internal action code for project supervisor

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.ros.rosjava.tf.Transform;
import org.ros.rosjava.tf.TransformTree;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;
import rjs.arch.agarch.AbstractROSAgArch;

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
			TransformTree tfTree = ((AbstractROSAgArch) ts.getUserAgArch()).getTfTree();
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
