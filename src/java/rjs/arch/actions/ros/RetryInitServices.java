package rjs.arch.actions.ros;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import jason.asSemantics.ActionExec;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Term;
import rjs.arch.actions.AbstractAction;
import rjs.arch.agarch.AbstractROSAgArch;

public class RetryInitServices extends AbstractAction {

	public RetryInitServices(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}

	@Override
	public void execute() {
		actionExec.setResult(true);
		LogicalFormula logExpr = Literal.parseLiteral("~connected_srv(X)");
		Iterator<Unifier> iu = logExpr.logicalConsequence(rosAgArch.getTS().getAg(), new Unifier());
		Set<String> list = new HashSet<String>();
		Term var = Literal.parseLiteral("X");
        while (iu.hasNext()) {
        	Term term = var.capply(iu.next());
        	list.add(term.toString());
        }
		HashMap<String, Boolean> services_status = getRosNode().retryInitServiceClients(list);
		for(Entry<String, Boolean> entry : services_status.entrySet()) {
			if(entry.getValue()) {
				rosAgArch.addBelief("connected_srv("+entry.getKey()+")");
				rosAgArch.removeBelief("~connected_srv("+entry.getKey()+")");
			}else {
				rosAgArch.addBelief("~connected_srv("+entry.getKey()+")");
				actionExec.setResult(false);
				actionExec.setFailureReason(new Atom("srv_not_connected"), "Some services are not connected");
			}
		}
	}

}
