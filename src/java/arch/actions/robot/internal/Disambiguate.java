package arch.actions.robot.internal;

import java.util.HashMap;
import java.util.Map;

import org.ros.exception.RemoteException;
import org.ros.node.service.ServiceResponseListener;

import arch.actions.AbstractAction;
import arch.agarch.AbstractROSAgArch;
import jason.asSemantics.ActionExec;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.StringTermImpl;
import knowledge_sharing_planner_msgs.DisambiguationResponse;

public class Disambiguate extends AbstractAction {

	public Disambiguate(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
	}

	@Override
	public void execute() {
		String individual = removeQuotes(actionExec.getActionTerm().getTerm(0).toString());
		String ontology = removeQuotes(actionExec.getActionTerm().getTerm(1).toString());
		
		ServiceResponseListener<DisambiguationResponse> respListener = new ServiceResponseListener<DisambiguationResponse>() {

			@Override
			public void onFailure(RemoteException e) {
				handleFailure(actionExec, actionName, e);
			}

			@Override
			public void onSuccess(DisambiguationResponse resp) {
				ListTermImpl list = new ListTermImpl();
				for(String e : resp.getSparqlResult()) {
					list.add(new StringTermImpl(e));
				}
				rosAgArch.addBelief("sparql_result("+new StringTermImpl(individual)+","+list.toString()+")");
				actionExec.setResult(true);
				rosAgArch.actionExecuted(actionExec);
			}
		};
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("individual", individual);
		parameters.put("ontology", rosnode.getParameters().getString("disambi/ontologies/"+ontology));
		rosnode.callAsyncService("disambiguate", respListener, parameters);

	}

}
