package arch.actions.robot.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ros.exception.RemoteException;
import org.ros.node.service.ServiceResponseListener;

import arch.actions.AbstractAction;
import arch.agarch.AbstractROSAgArch;
import jason.asSemantics.ActionExec;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;
import knowledge_sharing_planner_msgs.VerbalizationResponse;

public class GetSparqlVerba extends AbstractAction {

	public GetSparqlVerba(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
	}

	@Override
	public void execute() {
		ListTermImpl listTerm = (ListTermImpl) actionExec.getActionTerm().getTerm(0);
		@SuppressWarnings("unchecked")
		List<String> sparql = removeQuotes((List<Term>) actionExec.getActionTerm().getTerm(0));
		
		ServiceResponseListener<VerbalizationResponse> respListener = new ServiceResponseListener<VerbalizationResponse>() {

			@Override
			public void onFailure(RemoteException e) {
				handleFailure(actionExec, actionName, e);
			}

			@Override
			public void onSuccess(VerbalizationResponse resp) {
				String object = rosAgArch.findBel("sparql_result(_,"+listTerm+")").getTerm(0).toString();
				StringTermImpl sentence = new StringTermImpl(resp.getVerbalization());
				rosAgArch.addBelief("verba("+object+","+sentence+")");
				actionExec.setResult(true);
				rosAgArch.actionExecuted(actionExec);
			}
		}; 
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("sparqlquery", sparql);
		rosnode.callAsyncService("verbalize", respListener, parameters);

	}

}
