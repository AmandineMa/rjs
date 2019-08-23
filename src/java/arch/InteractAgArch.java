package arch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.ros.message.MessageListener;
import org.ros.node.NodeConfiguration;
import org.ros.node.topic.Publisher;

import com.google.common.collect.Multimap;

import jason.RevisionFailedException;
import jason.asSemantics.ActionExec;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import pepper_engage_human.TerminateInteractionActionGoal;
import pepper_engage_human.TerminateInteractionGoal;
import utils.SimpleFact;

public class InteractAgArch extends RobotAgArch {

	private Publisher<std_msgs.String> disengaging_human;
	private Publisher<TerminateInteractionActionGoal> terminate_interaction;

	@Override
	public Collection<Literal> perceive() {
		Collection<Literal> l = new ArrayList<Literal>();
		if(m_rosnode != null) {
//			if(percept_id != m_rosnode.getPercept_id()) {
				Multimap<String,SimpleFact> mm = m_rosnode.getPerceptions();
				synchronized (mm) {
					HashMap<String, Collection<SimpleFact>> perceptions = new HashMap<String, Collection<SimpleFact>>(mm.asMap());
					if(perceptions != null) {
						for(String agent : perceptions.keySet()) {
							for(SimpleFact percept : perceptions.get(agent)) {
								if(percept.getPredicate().equals("isEngagedWith")) {
									l.add(Literal.parseLiteral(percept.getPredicate()+"("+agent+","+percept.getObject()+")"));
								}else if(percept.getPredicate().equals("isPerceiving")) {
									l.add(Literal.parseLiteral(percept.getPredicate()+"("+agent+","+percept.getObject()+")"));
								}
							}
						}
//					}
				}


				percept_id = m_rosnode.getPercept_id();
			}
		}
		return l;

	}
	
	@Override
	public void init() {
		super.init();
		
		disengaging_human = m_rosnode.getConnectedNode().newPublisher(
				m_rosnode.getParameters().getString("guiding/topics/disengaging"), std_msgs.String._TYPE);
		
		terminate_interaction = m_rosnode.getConnectedNode().newPublisher(
				m_rosnode.getParameters().getString("guiding/topics/terminate_interaction"), TerminateInteractionActionGoal._TYPE);
		
		MessageListener<TerminateInteractionActionGoal> terminate_interac = new MessageListener<TerminateInteractionActionGoal>() {
			public void onNewMessage(TerminateInteractionActionGoal msg) {
				try {
					getTS().getAg().addBel(Literal.parseLiteral("terminate_interaction(\""+msg.getGoal().getPersonFrame().toString().replace("human-", "")+"\")"));
				} catch (RevisionFailedException e) {
					e.printStackTrace();
				}
			}
		};
		m_rosnode.addListener("guiding/topics/terminate_interaction", TerminateInteractionActionGoal._TYPE, terminate_interac);
	}
	
	
	@Override
	public void act(final ActionExec action) {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				String action_name = action.getActionTerm().getFunctor();
				
				if(action_name.equals("engage")) {
					String human_id = action.getActionTerm().getTerm(0).toString();
					m_rosnode.call_engage_as(human_id);
					action.setResult(true);
					actionExecuted(action);
				}else if(action_name.equals("text2speech")) {
					String human = action.getActionTerm().getTerm(0).toString();
					human = human.replaceAll("^\"|\"$", "");
					Literal bel = (Literal) action.getActionTerm().getTerm(1);
					text2speech(human, bel, null, action);
				}else if(action_name.equals("disengaging_human")){
					String human = action.getActionTerm().getTerm(0).toString();
					human = human.replaceAll("^\"|\"$", "");
					std_msgs.String str = disengaging_human.newMessage();
					str.setData(human);
					disengaging_human.publish(str);
					action.setResult(true);
					actionExecuted(action);
				}else if(action_name.equals("terminate_interaction")) {
					String human = action.getActionTerm().getTerm(0).toString();
					human = human.replaceAll("^\"|\"$", "");
					TerminateInteractionActionGoal goal = terminate_interaction.newMessage();
					nodeConfiguration = NodeConfiguration.newPrivate();
					messageFactory = nodeConfiguration.getTopicMessageFactory();
					TerminateInteractionGoal term = messageFactory.newFromType(TerminateInteractionGoal._TYPE);
					term.setPersonFrame("human-"+human);
					goal.setGoal(term);
					terminate_interaction.publish(goal);
					action.setResult(true);
					actionExecuted(action);
				}else {
					action.setResult(false);
					action.setFailureReason(new Atom("act_not_found"), "no action " + action_name + " is implemented");
					actionExecuted(action);
				}
			}
			
		});
	}
	

}
