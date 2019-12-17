package arch.actions;

import java.util.HashMap;
import java.util.Map;

import org.ros.exception.RemoteException;
import org.ros.node.service.ServiceResponseListener;

import arch.ROSAgArch;
import jason.asSemantics.ActionExec;
import std_srvs.SetBoolResponse;

public class EnableAnimatedSpeech extends AbstractAction {

	public EnableAnimatedSpeech(ActionExec actionExec, ROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}

	@Override
	public void execute() {
		boolean b = Boolean.parseBoolean(actionExec.getActionTerm().getTerm(0).toString());
		ServiceResponseListener<SetBoolResponse> respListenerA = new ServiceResponseListener<SetBoolResponse>() {
			public void onFailure(RemoteException e) {}

			public void onSuccess(SetBoolResponse response) {}
			
		};
		Map<String, Object> parametersA = new HashMap<String, Object>();
		parametersA.put("data", b);
		actionExec.setResult(true);
		rosnode.callAsyncService("enable_animated_speech", respListenerA, parametersA);
	}

}
