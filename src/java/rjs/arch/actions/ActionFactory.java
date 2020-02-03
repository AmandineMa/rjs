package rjs.arch.actions;

import jason.asSemantics.ActionExec;
import rjs.arch.agarch.AbstractROSAgArch;

public interface ActionFactory {
	
	public Action createAction(ActionExec action, AbstractROSAgArch rosAgArch);
	
	public void setRosVariables();

}
