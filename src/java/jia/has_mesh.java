// Internal action code for project supervisor

package jia;

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
		String param = args[0].toString();
		param = param.replaceAll("^\"|\"$", "");

		ROSAgArch.getM_rosnode().call_has_mesh_srv("robot/merged", param);
		HasMeshResponse has_mesh;
		do {
			has_mesh = ROSAgArch.getM_rosnode().getHas_mesh_resp();
			sleep(100);
		}while(has_mesh == null);

		return has_mesh.getHasMesh();
    }
    
    void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException ex) {
		}
	}

}
