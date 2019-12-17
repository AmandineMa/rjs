package arch.actions;

import org.ros.exception.RemoteException;
import org.ros.node.service.ServiceResponseListener;

import arch.ROSAgArch;
import jason.asSemantics.ActionExec;
import std_srvs.EmptyResponse;

public class PauseASR extends AbstractAction implements Action {

	public PauseASR(ActionExec actionExec, ROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}
	
	@Override
	public void execute() {
		ServiceResponseListener<std_srvs.EmptyResponse> respListener = new ServiceResponseListener<std_srvs.EmptyResponse>() {

			@Override
			public void onFailure(RemoteException e) {}

			@Override
			public void onSuccess(EmptyResponse arg0) {}
		};
		actionExec.setResult(true);
		rosnode.callAsyncService("pause_asr", respListener, null);
		rosnode.callAsyncService("web_view_start_processing", respListener, null);
	}

}
