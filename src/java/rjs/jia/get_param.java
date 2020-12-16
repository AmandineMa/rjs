// Internal action code for project supervisor

package rjs.jia;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.MapTermImpl;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;
import rjs.arch.agarch.AbstractROSAgArch;
import rjs.utils.Tools;

/** Handle only lists of Double/Integer **/

public class get_param extends DefaultInternalAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String param_name;
	
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	try {
	    	String class_name = args[1].toString().replaceAll("^\"|\"$", "");
	    	String prefix;
	    	param_name = args[0].toString().replaceAll("^\"|\"$", "");
	    	if(class_name.equals("List") || class_name.equals("Map")) {
	    		prefix = "java.util.";
	    	}else {
	    		prefix = "java.lang.";
	    	}
			Class<?> c = Class.forName(prefix+class_name);
			Method m = AbstractROSAgArch.getRosnode().getParameters().getClass().getMethod("get"+class_name, String.class);
			if(c.equals(Boolean.class)){
				Boolean p = (Boolean) m.invoke(AbstractROSAgArch.getRosnode().getParameters(),param_name);
				return un.unifies(args[2], new Atom(Literal.parseLiteral(p.toString())));
			}else if(c.equals(Double.class)) {
				Double p = (Double) m.invoke(AbstractROSAgArch.getRosnode().getParameters(), param_name);
				return un.unifies(args[2], new NumberTermImpl(p));
			}else if(c.equals(Integer.class)) {
				Integer p = (Integer) m.invoke(AbstractROSAgArch.getRosnode().getParameters(), param_name);
				return un.unifies(args[2], new NumberTermImpl(p));
			}else if(c.equals(String.class)) {
				String p = (String) m.invoke(AbstractROSAgArch.getRosnode().getParameters(), param_name);
				return un.unifies(args[2], new StringTermImpl(p));
			}else if(c.equals(List.class)) {
				List<?> p = (List<?>) m.invoke(AbstractROSAgArch.getRosnode().getParameters(), param_name);
				return un.unifies(args[2], getListTermImpl(p));
			}else if(c.equals(Map.class)){
				Map<?,?> p = (Map<?,?>) m.invoke(AbstractROSAgArch.getRosnode().getParameters(), param_name);
				return un.unifies(args[2], getMapTermImpl(p));
			}else {
				return false;
			}
    	}catch(InvocationTargetException e){
    		e.printStackTrace();
    		return false;
    	}
    }
    
    private MapTermImpl getMapTermImpl(Map<?,?> p) {
    	MapTermImpl map = new MapTermImpl();
    	for(Map.Entry<?,?> entry : p.entrySet()) {
    		map.put(new StringTermImpl((String) entry.getKey()), objectToTerm(entry.getValue()));
		}
    	return map;
    }
    
    private ListTermImpl getListTermImpl(List listValues) {
    	ListTermImpl listTerm = new ListTermImpl();
		for(int i=0; i < listValues.size(); i++) {
			listTerm.add((Term) objectToTerm(listValues.get(i)));
		}
		return listTerm;
    }
    
    private ListTermImpl getListTermImpl(Object[] array) {
    	ListTermImpl listTerm = new ListTermImpl();
		for(int i=0; i < array.length; i++) {
			listTerm.add((Term) objectToTerm(array[i]));
		}
		return listTerm;
    }
    
    
    
    private Term objectToTerm(Object value) {
    	Double d = null;
    	if(value instanceof Integer)
			d = ((Integer) value).doubleValue();
		else if(value instanceof Double)
			d = (Double) value;
		if(d != null)
			return new NumberTermImpl(d);
		else if(value instanceof String)
			return new StringTermImpl((String) value);
		else if(value instanceof List) {
			return getListTermImpl((List) value);
		}else if(value instanceof Map) {
			return getMapTermImpl((Map<?,?>) value);
		}else if(value.getClass().getName().toString().startsWith("[Ljava.lang.Object;")) {
			return getListTermImpl((Object[]) value);
		}
		return null;
    }
    

}
