package rjs.arch.actions;

import jason.asSemantics.ActionExec;
import rjs.arch.agarch.AbstractROSAgArch;
import rjs.ros.AbstractRosNode;

public interface ActionFactory {
	
	public Action createAction(ActionExec action, AbstractROSAgArch rosAgArch);
	
	public void setRosVariables(AbstractRosNode rosnode);

}
