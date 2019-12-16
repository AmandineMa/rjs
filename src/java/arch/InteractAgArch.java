package arch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.ros.exception.RemoteException;
import org.ros.message.MessageListener;
import org.ros.node.NodeConfiguration;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Publisher;

import com.google.common.collect.Multimap;

import actionlib_msgs.GoalStatus;
import arch.ROSAgArch.UpdateTimeBB;
import diagnostic_msgs.KeyValue;
import geometry_msgs.PoseStamped;
import jason.RevisionFailedException;
import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSyntax.ArithExpr;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;
import jason.bb.BeliefBase;
import move_base_msgs.MoveBaseActionFeedback;
import move_base_msgs.MoveBaseActionResult;
import msg_srv_impl.PoseCustom;
import pepper_engage_human.TerminateInteractionActionGoal;
import std_msgs.Header;
import std_srvs.EmptyResponse;
import utils.QoI;
import utils.SimpleFact;

public class InteractAgArch extends ROSAgArch {

	AgArch robot_arch = null;
	static ArrayList<Double> attentive_times = new ArrayList<>();
	protected Publisher<std_msgs.String> human_to_monitor;
	protected Logger logger = Logger.getLogger(InteractAgArch.class.getName());
	private HashMap<String, UpdateTimeBB> sessionsQoI = new HashMap<String, UpdateTimeBB>();
	double nbChatBotQoI;
	double chatBotQoIAverage;
	boolean first = true;
	boolean startChat = true;

	@Override
	public Collection<Literal> perceive() {
		Collection<Literal> l = new ArrayList<Literal>();
		if(rosnode != null) {
//			if(percept_id != m_rosnode.getPercept_id()) {
				Multimap<String,SimpleFact> mm = rosnode.getPerceptions();
				synchronized (mm) {
					HashMap<String, Collection<SimpleFact>> perceptions = new HashMap<String, Collection<SimpleFact>>(mm.asMap());
					if(perceptions != null) {
						for(String agent : perceptions.keySet()) {
							for(SimpleFact percept : perceptions.get(agent)) {
								if(percept.getPredicate().equals("isEngagedWith")) {
									l.add(Literal.parseLiteral("isAttentive("+agent+")"));
								}else if(percept.getPredicate().equals("isPerceiving")) {
									l.add(Literal.parseLiteral(percept.getPredicate()+"("+agent+","+percept.getObject()+")"));
								}else if(percept.getPredicate().equals("isInsideArea")) {
									l.add(Literal.parseLiteral(percept.getPredicate()+"("+agent+","+percept.getObject()+")"));
								}
							}
						}
//					}
				}


				percept_id = rosnode.getPercept_id();
			}
		}
		return l;

	}
	
	@Override
	public void init() {
//		attentive_ratio_test();
		super.init();
//		display.setVisible(true);

		MessageListener<TerminateInteractionActionGoal> terminate_interac = new MessageListener<TerminateInteractionActionGoal>() {
			public void onNewMessage(TerminateInteractionActionGoal msg) {
				try {
					getTS().getAg().addBel(Literal.parseLiteral("terminate_interaction("+msg.getGoal().getPersonFrame().toString().replace("human-", "")+")"));
				} catch (RevisionFailedException e) {
					e.printStackTrace();
				}
			}
		};
		rosnode.addListener("guiding/topics/terminate_interaction", TerminateInteractionActionGoal._TYPE, terminate_interac);

		MessageListener<KeyValue> rating = new MessageListener<KeyValue>() {
			public void onNewMessage(KeyValue msg) {
				try {
					getTS().getAg().addBel(Literal.parseLiteral("rating("+msg.getKey().replace("human-", "")+","+msg.getValue()+")"));
				} catch (RevisionFailedException e) {
					e.printStackTrace();
				}
			}
		};
		rosnode.addListener("guiding/topics/rating", KeyValue._TYPE, rating);

		MessageListener<std_msgs.Bool> is_talking = new MessageListener<std_msgs.Bool>() {
			public void onNewMessage(std_msgs.Bool msg) {
				try {
					getTS().getAg().addBel(Literal.parseLiteral("finished_talking("+String.valueOf(msg.getData())+")"));
				} catch (RevisionFailedException e) {
					e.printStackTrace();
				}
			}
		};
		rosnode.addListener("guiding/topics/finished_talking", std_msgs.Bool._TYPE, is_talking);
		
		human_to_monitor = rosnode.getConnectedNode().newPublisher(
				rosnode.getParameters().getString("guiding/topics/human_to_monitor"), std_msgs.String._TYPE);
	}
	
	
	@Override
	public void act(final ActionExec action) {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				String action_name = action.getActionTerm().getFunctor();
				
				if(action_name.equals("engage")) {
					String human_id = action.getActionTerm().getTerm(0).toString();
					rosnode.call_engage_as(human_id);
					action.setResult(true);
					actionExecuted(action);
				}
//				else if(action_name.equals("text2speech")) {
//					Literal bel = (Literal) action.getActionTerm().getTerm(0);
//					text2speech(bel, null, action);
//				}
				else if(action_name.equals("terminate_interaction")) {
					ServiceResponseListener<std_srvs.EmptyResponse> respListener = new ServiceResponseListener<std_srvs.EmptyResponse>() {

						@Override
						public void onFailure(RemoteException e) {
							handleFailure(action, action_name, e);
						}

						@Override
						public void onSuccess(EmptyResponse arg0) {
							action.setResult(true);
							actionExecuted(action);
						}
					};
					rosnode.callAsyncService("terminate_interaction", respListener, null);
					
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
					rosnode.callAsyncService("localise", respListener, parameters);

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
					rosnode.callAsyncService("reinit_loca", respListener, parameters);

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
					rosnode.call_move_to_as(pose_stamped);
					MoveBaseActionResult move_to_result;
					MoveBaseActionFeedback move_to_fb;
					do {
						move_to_result = rosnode.getMove_to_result();
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

	@Override
	public void reasoningCycleStarting() {
		double sessionDuration = 0;
		double taskNumber = 0;
		double taskEfficiency = 0;
		double taskLevQoI = 0;
		ArrayList<Double> qoi_values = new ArrayList<Double>();
		
		if(!contains("overBy(_)") && contains("inSession(_,_)")){
			if(first) {
//				display.insert_discontinuity("session", getRosTimeMilliSeconds());
				first = false;
			}
			
			double sessionStartTime = getRosTimeMilliSeconds();
			String session_id = "";
			Iterator<Literal> it = get_beliefs_iterator("inSession(_,_)");
			if (it != null && it.hasNext()) {
				Literal l = it.next();
				session_id = l.getTerm(1).toString();
				sessionStartTime = ((NumberTermImpl)((Literal) l.getAnnots("add_time").get(0)).getTerm(0)).solve();
				sessionDuration = QoI.logaFormula(getRosTimeSeconds() - sessionStartTime/1000.0, 150, 1.5);
			}

			it = get_beliefs_iterator("startTask(_,_)");
			int c = 0;
			while (it != null && it.hasNext()) {
				c++;
				it.next();
			}
			taskNumber = QoI.logaFormula(c, 1, 2);
			
			it = get_beliefs_iterator("task_achievement(_,_)");
			double sum = 0;
			double n = 0;
			while(it != null && it.hasNext()) {
				Literal l = it.next();
				sum = sum + ((NumberTermImpl) l.getTerm(1)).solve();
				n++;
			}	
			Double taskEfficiencyAverage = sum / n;
			taskEfficiency = QoI.normaFormulaMinus1To1(taskEfficiencyAverage, 0, 1);
			
			if(sessionsQoI.get(session_id) == null)
				sessionsQoI.put(session_id, new UpdateTimeBB());
			
			// chatbot task
			Literal qoi_chat_bot = null;
			if(!contains("inTaskWith(_,_)")) {
				if(startChat) {
//					display.insert_discontinuity("task", getRosTimeMilliSeconds());
					startChat = false;
				}
				double startTime = sessionStartTime;
				if(contains("endTask(_,_)")) {
					Literal et = findBel("endTask(_,_)");
					startTime = ((NumberTermImpl)((Literal) et.getAnnots("add_time").get(0)).getTerm(0)).solve();
				}
				double ar = attentive_ratio(startTime, getRosTimeMilliSeconds());
				qoi_chat_bot = literal("qoi_task","chat_bot",QoI.normaFormulaMinus1To1(ar, 0, 1));
				logger.info(qoi_chat_bot.toString());
				sessionsQoI.get(session_id).add(qoi_chat_bot);
				
				nbChatBotQoI++;
				chatBotQoIAverage = chatBotQoIAverage + ( ((NumberTermImpl) qoi_chat_bot.getTerm(1)).solve() - chatBotQoIAverage) / nbChatBotQoI;
			} else {
				startChat = true;
			}

			Literal l = findBel("qoi(_,_)[source(robot)]");
			if(l != null) {
				double d;
				if(l.getTerm(1).isArithExpr())
					d = ((NumberTermImpl) ((ArithExpr) l.getTerm(1)).getLHS()).solve();
				else
					d = ((NumberTermImpl) l.getTerm(1)).solve();
				taskLevQoI = ( d + chatBotQoIAverage ) / 2.0 ;
				qoi_values.add(taskLevQoI);
			} else {
				taskLevQoI =  chatBotQoIAverage;
				qoi_values.add(taskLevQoI);
			}
			
			double QoI;
			if(!taskEfficiencyAverage.isNaN())
				qoi_values.add(taskEfficiency);
			
			qoi_values.add(taskNumber);
			qoi_values.add(sessionDuration);
			
			sum = 0;
			for(Double d : qoi_values) {
				sum += d;
			}
			
			QoI = sum/qoi_values.size();

//			logger.info("QoI metrics :"+sessionDuration+","+taskNumber+","+taskEfficiency+","+taskLevQoI);
			
			sessionsQoI.get(session_id).add(literal("qoi",session_id, QoI));
			
			Literal qoi_l = findBel(Literal.parseLiteral("qoi(_,_)"), this.sessionsQoI.get(session_id));
//			display.update(qoi_l,qoi_chat_bot,null );
		} else {
			first = true;
		}
		

		super.reasoningCycleStarting();
	}
	
	public void attentive_times_add_value(double value) {
		attentive_times.add(value);
	}
	
	
	public void display_test(double test, double value) {
		if(test == value) {
			logger.info("Expected value found");
		} else {
			logger.info("Expected "+value+" but got "+ test+ " instead!");
		}
	}
	
	public void attentive_ratio_test() {
		logger.info("attentive_ratio_test START");
		attentive_times.clear();
		
		attentive_times.add(1000d);
		attentive_times.add(1100d);
		attentive_times.add(1200d);
		attentive_times.add(1300d);

		display_test(attentive_ratio(900, 1400),200d/500d);
		display_test(attentive_ratio(1050, 1400),150d/350d);
		display_test(attentive_ratio(1100, 1400),100d/300d);
		display_test(attentive_ratio(1150, 1400),100d/250d);
		display_test(attentive_ratio(900, 1150),100d/250d);
		display_test(attentive_ratio(900, 1250),150d/350d);
		display_test(attentive_ratio(900, 1350),200d/450d);
		
		attentive_times.add(1400d);
		
		display_test(attentive_ratio(900, 1500),300d/600d);
		
		attentive_times.clear();
		attentive_times.add(1000d);
		attentive_times.add(2000d);
		
		display_test(attentive_ratio(1200, 1500),1d);
		display_test(attentive_ratio(500, 700),0d);
		display_test(attentive_ratio(2200, 3000),0d);
		
		attentive_times.clear();
		attentive_times.add(1000d);
		display_test(attentive_ratio(1200, 1500),1d);
		display_test(attentive_ratio(500, 700),0d);
		display_test(attentive_ratio(500, 1500),500d/1000d);
		
		attentive_times.add(1200d);
		attentive_times.add(1300d);
		display_test(attentive_ratio(1400, 1500),1d);
		
		logger.info("attentive_ratio_test END");
	}

	
	
	public double attentive_ratio(double startTime, double endTime) {
		
		if(endTime<=startTime) 
			return 0;
		
		double time_attentive = 0;
		double previous = 0;
		if(startTime >= attentive_times.get(attentive_times.size()-1) && attentive_times.size() % 2 ==1) {
			time_attentive = endTime - startTime;
		} else {
			for(int i = 0; i < attentive_times.size(); i++) {
				double time = attentive_times.get(i);
				
				if(startTime <= time && (startTime>previous || previous == 0)) {
					if(i%2==1) {
						time_attentive = Math.min(time,endTime) - startTime;
					} else if (attentive_times.size() == 1 && endTime >= time) {
						time_attentive = endTime - time;
					}
				} else if(startTime<time && startTime<previous && endTime >= time) {
					if(i%2==1) {
						time_attentive += time-previous;
					}else if(i==attentive_times.size()-1 && i%2==0) {
						time_attentive += endTime - time;
					}
				} else if(startTime<time && startTime<previous && endTime>previous && endTime<=time) {
					if(i%2==1) {
						time_attentive += endTime-previous;
					}
				}
				
				previous = time;
			}
		}
		return time_attentive/(endTime-startTime);
	}
	
	public BeliefBase getSessionBB(String id) {
		return sessionsQoI.get(id);
	}





}
