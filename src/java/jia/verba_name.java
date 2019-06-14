package jia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import arch.ROSAgArch;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import msg_srv_impl.RouteImpl;
import msg_srv_impl.SemanticRouteResponseImpl;
import semantic_route_description_msgs.Route;
import semantic_route_description_msgs.SemanticRouteResponse;
import utils.Code;

public class verba_name extends word_individual {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	Term[] args_i = new Term[3];
		args_i[0] = new StringTermImpl("getName");
		args_i[1] = null;
		args_i[2] = new VarTerm("Name");
		Unifier u_i = new Unifier();
    	if(args[0].isList()) {
    		Iterator<Term> i = ((ListTermImpl) args[0]).iterator();
    		ListTermImpl name_list = new ListTermImpl();
    		while(i.hasNext()) {
    			args_i[1] = i.next();
    			u_i = new Unifier();
    			super.execute(ts, u_i, args_i);
    			name_list.add(u_i.get("Name"));
    		}
    		return un.unifies(args[1], name_list);	
    	}else if(args[0].isString() || args[0].isAtom()) {
    		args_i[1] = args[0];
    		super.execute(ts, u_i, args_i);
    		return un.unifies(args[1], u_i.get("Name"));	
    	}else {
    		return false;
    	}
    	
    }
    
    void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException ex) {
		}
	}



}

