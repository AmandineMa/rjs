package arch.agarch.disambi;

import org.ros.message.MessageListener;

import jason.asSyntax.StringTermImpl;

public class RobotAgArch extends DisambiAgArch {

	public RobotAgArch() {
		super();
	}

	@Override
	public void init() {
		MessageListener<std_msgs.String> clicked_object = new MessageListener<std_msgs.String>() {
			public void onNewMessage(std_msgs.String msg) {
				StringTermImpl str = new StringTermImpl(msg.getData());
				addBelief("clicked_object("+str+")");
			}
		};
		rosnode.addListener("disambi/topics/clicked_object", std_msgs.String._TYPE, clicked_object);
	}


}
