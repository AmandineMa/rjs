package jia;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;

public class beliefs_to_file  extends DefaultInternalAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override public int getMinArgs() {
		return 1;
	}
	@Override public int getMaxArgs() {
		return 1;
	}

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
		checkArguments(args);
		
		ListTermImpl b_list = (ListTermImpl) args[0];
		List<LiteralImpl> l_list = new ArrayList<LiteralImpl>();
		Iterator<Term> ite = b_list.iterator();
    	List<String> s_list = new ArrayList<String>();

    	String name = "task";

    	int counter = 1;
    	
    	while(ite.hasNext()) {
    		LiteralImpl b = (LiteralImpl) ite.next();
    		l_list.add(b);
    		if(b.getFunctor().equals("task")) {
    			name = b.getTerm(3).toString();
    			name = name.replaceAll("^\"|\"$", "");
    		}else if(b.getFunctor().equals("inSession")) {
    			name = "session_"+b.getTerm(1).toString();
    		}
    	}
    	File  f = new File("log/beliefs");
    	if(!f.exists()){
    		f.mkdirs();
    	}
    	String file_name = name;
    	Path path = Paths.get("log/beliefs/"+file_name);
    	while(Files.exists(path)){
    		file_name = name+"_"+counter;
    	    path = Paths.get("log/beliefs/"+file_name);
    	    counter++;
    	}
    	Comparator<Literal> cmp = new LiteralTimeComparator();
    	Collections.sort(l_list, cmp);
    	Iterator<LiteralImpl> ite2 = l_list.iterator();
    	while(ite2.hasNext()) {
    		s_list.add(ite2.next().toString());
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
