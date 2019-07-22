// Internal action code for project supervisor

package jia;

import java.util.HashMap;
import java.util.Map;

import arch.ROSAgArch;

//import java.util.logging.Logger;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import perspectives_msgs.HasMeshResponse;

public class has_mesh extends DefaultInternalAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	// to remove the extra ""
		String param = args[0].toString().replaceAll("^\"|\"$", "");

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("world", "robot/merged");
		parameters.put("name", param);
		HasMeshResponse result = ROSAgArch.getM_rosnode().callSyncService("has_mesh", parameters);
		return result.getHasMesh();
    }
    
    void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException ex) {
		}
	}

}
