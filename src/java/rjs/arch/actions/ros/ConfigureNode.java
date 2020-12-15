package rjs.arch.actions.ros;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.ros.internal.loader.CommandLineLoader;

import com.google.common.collect.Lists;

import jason.asSemantics.ActionExec;
import rjs.arch.actions.AbstractAction;
import rjs.arch.agarch.AbstractROSAgArch;

public class ConfigureNode extends AbstractAction {

	public ConfigureNode(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}

	@Override
	public void execute() {
		if(System.getenv("ROS_MASTER_URI") != null && System.getenv("ROS_IP") != null) {
			List<String> emptyArgv = Lists.newArrayList("EmptyList");
			CommandLineLoader loader = new CommandLineLoader(emptyArgv);
			URI masterUri = null;
			rosAgArch.setNodeConfiguration(loader.build());	
			try {
				masterUri = new URI(System.getenv("ROS_MASTER_URI"));			
			} catch (URISyntaxException e) {
				logger.info("Wrong URI syntax :" + e.getMessage());
			} 
			rosAgArch.getNodeConfiguration().setMasterUri(masterUri);
			actionExec.setResult(true);
		}else {
			actionExec.setResult(false);
			if(System.getenv("ROS_MASTER_URI") == null)
				logger.info("ROS_MASTER_URI has not been set");
			if(System.getenv("ROS_IP") == null)
				logger.info("ROS_IP has not been set");
			else if (System.getenv("ROS_IP").equals("127.0.0.1"))
				logger.info("ROS_IP should not be localhost");
		}
		//TODO: investigate on how to active actionExecuted
//		rosAgArch.actionExecuted(actionExec);
	}

}
