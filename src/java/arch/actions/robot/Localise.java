package arch.actions.robot;

import java.util.HashMap;
import java.util.Map;

import org.ros.exception.RemoteException;
import org.ros.node.service.ServiceResponseListener;

import arch.actions.AbstractAction;
import arch.agarch.AbstractROSAgArch;
import jason.asSemantics.ActionExec;
import jason.asSyntax.Atom;

public class Localise extends AbstractAction {

	public Localise(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
	}

	@Override
	public void execute() {
		ServiceResponseListener<pepper_localisation.responseResponse> respListener = new ServiceResponseListener<pepper_localisation.responseResponse>() {
			public void onFailure(RemoteException e) {
				handleFailure(actionExec, actionName, e);
			}

			public void onSuccess(pepper_localisation.responseResponse loca_resp) {
				actionExec.setResult(loca_resp.getSuccess());
				if (!loca_resp.getSuccess()) {
					actionExec.setFailureReason(new Atom("loca_failed"), "localisation has failed");
				}
				rosAgArch.actionExecuted(actionExec);
			}
		};
		Map<String, Object> parameters = new HashMap<String, Object>();
		rosnode.callAsyncService("localise", respListener, parameters);
	}

}
