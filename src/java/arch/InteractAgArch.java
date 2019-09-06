package arch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ros.exception.RemoteException;
import org.ros.message.MessageListener;
import org.ros.node.NodeConfiguration;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Publisher;

import com.google.common.collect.Multimap;

import actionlib_msgs.GoalStatus;
import geometry_msgs.PoseStamped;
import jason.RevisionFailedException;
import jason.asSemantics.ActionExec;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;
import move_base_msgs.MoveBaseActionFeedback;
import move_base_msgs.MoveBaseActionResult;
import msg_srv_impl.PoseCustom;
import pepper_engage_human.TerminateInteractionActionGoal;
import pepper_engage_human.TerminateInteractionGoal;
import std_msgs.Header;
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
								}else if(percept.getPredicate().equals("isInsideArea")) {
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
		
		MessageListener<std_msgs.Bool> is_talking = new MessageListener<std_msgs.Bool>() {
			public void onNewMessage(std_msgs.Bool msg) {
				try {
					getTS().getAg().addBel(Literal.parseLiteral("finished_talking("+String.valueOf(msg.getData())+")"));
				} catch (RevisionFailedException e) {
					e.printStackTrace();
				}
			}
		};
		m_rosnode.addListener("guiding/topics/finished_talking", std_msgs.Bool._TYPE, is_talking);
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
					Literal bel = (Literal) action.getActionTerm().getTerm(0);
					text2speech(bel, null, action);
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
				} else if (action_name.equals("localise")) {
					ServiceResponseListener<pepper_localisation.responseResponse> respListener = new ServiceResponseListener<pepper_localisation.responseResponse>() {
						public void onFailure(RemoteException e) {
							handleFailure(action, action_name, e);
						}

						public void onSuccess(pepper_localisation.responseResponse loca_resp) {
							action.setResult(loca_resp.getSuccess());
							if (!loca_resp.getSuccess()) {
								action.setFailureReason(new Atom("loca_failed"), "localisation has failed");
							}
							actionExecuted(action);
						}
					};
					Map<String, Object> parameters = new HashMap<String, Object>();
					m_rosnode.callAsyncService("localise", respListener, parameters);

				} else if (action_name.equals("reinit_loca")) {
					ServiceResponseListener<pepper_localisation.responseResponse> respListener = new ServiceResponseListener<pepper_localisation.responseResponse>() {
						public void onFailure(RemoteException e) {
							handleFailure(action, action_name, e);
						}

						public void onSuccess(pepper_localisation.responseResponse loca_resp) {
							action.setResult(loca_resp.getSuccess());
							if (!loca_resp.getSuccess()) {
								action.setFailureReason(new Atom("reinit_loca_failed"), "reinit localisation has failed");
							}
							actionExecuted(action);
						}
					};
					Map<String, Object> parameters = new HashMap<String, Object>();
					m_rosnode.callAsyncService("reinit_loca", respListener, parameters);

				}  else if (action_name.equals("move_to")) {
					String frame = action.getActionTerm().getTerm(0).toString();
					Iterator<Term> action_term_it = ((ListTermImpl) action.getActionTerm().getTerm(1)).iterator();
					List<Double> pose_values = new ArrayList<>();
					while (action_term_it.hasNext()) {
						pose_values.add(((NumberTermImpl) action_term_it.next()).solve());
					}
					action_term_it = ((ListTermImpl) action.getActionTerm().getTerm(2)).iterator();
					while (action_term_it.hasNext()) {
						pose_values.add(((NumberTermImpl) action_term_it.next()).solve());
					}
					PoseCustom pose = new PoseCustom(pose_values);
					nodeConfiguration = NodeConfiguration.newPrivate();
					messageFactory = nodeConfiguration.getTopicMessageFactory();
					PoseStamped pose_stamped = messageFactory.newFromType(PoseStamped._TYPE);
					Header header = messageFactory.newFromType(std_msgs.Header._TYPE);
					header.setFrameId(frame);
					pose_stamped.setHeader(header);
					pose_stamped.setPose(pose.getPose());
					m_rosnode.call_move_to_as(pose_stamped);
					MoveBaseActionResult move_to_result;
					MoveBaseActionFeedback move_to_fb;
					do {
						move_to_result = m_rosnode.getMove_to_result();
//							move_to_fb = m_rosnode.getMove_to_fb();
//							if(move_to_fb != null) {
//								try {
//									getTS().getAg().addBel(Literal.parseLiteral("fb(move_to, "+move_to_fb.getFeedback().getBasePosition().getPose().getPosition().getX()+","+
//											move_to_fb.getFeedback().getBasePosition().getPose().getPosition().getY()+","+
//											move_to_fb.getFeedback().getBasePosition().getPose().getPosition().getZ()+")["+task_id+"]"));
//								} catch (RevisionFailedException e) {
//									e.printStackTrace();
//								}
//							}
						sleep(200);
					} while (move_to_result == null);
					logger.info("move result: " +move_to_result.getStatus().getText());
					logger.info("move byte: " +move_to_result.getStatus().getStatus());
					if (move_to_result.getStatus().getStatus() == GoalStatus.SUCCEEDED) {
						try {
							getTS().getAg().addBel(Literal.parseLiteral("move_goal_reached"));
						} catch (RevisionFailedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						action.setResult(true);
					} else {
						action.setResult(false);
						action.setFailureReason(new Atom("move_to_failed"), "");
					}
					actionExecuted(action);
				}else if (action_name.equals("human_to_monitor")) {
					String param = action.getActionTerm().getTerm(0).toString();
					param = param.replaceAll("^\"|\"$", "");
					std_msgs.String str = human_to_monitor.newMessage();
					str.setData(param);
					human_to_monitor.publish(str);
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
