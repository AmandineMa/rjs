package jia.qoi;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import arch.InteractAgArch;
import arch.RobotAgArch;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;
import jason.bb.BeliefBase;

public class qoi_to_file  extends DefaultInternalAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override public int getMinArgs() {
		return 3;
	}
	@Override public int getMaxArgs() {
		return 3;
	}

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
		checkArguments(args);
		
		String type = args[0].toString();	
    	String name = args[1].toString().replaceAll("^\"|\"$", "");
    	String id = args[2].toString();
    	List<String> s_list = new ArrayList<String>();
    	
    	int counter = 1;

    	File  f = new File("log/qoi");
    	if(!f.exists()){
    		f.mkdirs();
    	}
    	String file_name = name;
    	Path path = Paths.get("log/qoi/"+name);
    	while(Files.exists(path)){
    		file_name = name+"_"+counter;
    	    path = Paths.get("log/qoi/"+file_name);
    	    counter++;
    	}
    	
    	if(type.equals("task")) {
    		BeliefBase bb= ((RobotAgArch) ts.getUserAgArch()).getTaskBB(id);
	    	Iterator<Literal> taskBB = bb.getCandidateBeliefs(Literal.parseLiteral("qoi(_,_)"), new Unifier());
	    	while(taskBB.hasNext()) {
	    		s_list.add(taskBB.next().toString());
	    	}
	    	
	    	Iterator<Literal> actionBB = ((RobotAgArch) ts.getUserAgArch()).getActionBB(id).getCandidateBeliefs(Literal.parseLiteral("qoi(_,_)"), new Unifier());
	    	while(actionBB.hasNext()) {
	    		s_list.add(actionBB.next().toString());
	    	}
    	} else if(type.equals("session")) {
    		Iterator<Literal> sessionBB = ((InteractAgArch) ts.getUserAgArch()).getSessionBB(id).getCandidateBeliefs(Literal.parseLiteral("qoi(_,_)"), new Unifier());
        	while(sessionBB.hasNext()) {
        		s_list.add(sessionBB.next().toString());
        	}
        	
        	Iterator<Literal> chatBB = ((InteractAgArch) ts.getUserAgArch()).getSessionBB(id).getCandidateBeliefs(Literal.parseLiteral("qoi_task(_,_)"), new Unifier());
        	while(chatBB.hasNext()) {
        		s_list.add(chatBB.next().toString());
        	}

    	}
    	Files.write(path, s_list, StandardCharsets.UTF_8);
    	return true;
    }
	
	class LiteralTimeComparator implements Comparator<Literal> {
        @Override
        public int compare(Literal a, Literal b) {
            return (int) (getAddTime(a) - getAddTime(b)) ;
        }
    }
    
    public double getAddTime(Literal l) {
    	double result = 0;
        Literal addTime = l.getAnnot("add_time");
        if(addTime != null) {
            NumberTermImpl time = (NumberTermImpl) addTime.getTerm(0);
            if(time != null) {
                result = time.solve();
            }
        }
        return result;
    }
}
