package arch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.ros.exception.RosRuntimeException;
import org.ros.message.MessageFactory;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.rosjava.tf.TransformTree;

import com.google.common.collect.Multimap;

import agent.TimeBB;
import arch.actions.Action;
import arch.actions.ActionFactory;
import jason.RevisionFailedException;
import jason.architecture.MindInspectorAgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Message;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.bb.BeliefBase;
import ros.RosNode;
import utils.SimpleFact;
import utils.Tools;

public class ROSAgArch extends MindInspectorAgArch {
	
	protected class UpdateTimeBB extends TimeBB{
		@Override
		   public boolean add(Literal l) {
				if(contains(l) != null) {
					remove(l);
				}
				return super.add(l);
		    }
	}
	
	static protected RosNode rosnode;
	
	protected ExecutorService executor;
	NodeConfiguration nodeConfiguration;
	MessageFactory messageFactory;
	protected int percept_id = -1;
	
//	static protected XYLineChart_AWT display = new XYLineChart_AWT("QoI", "QoI data");

	protected Logger logger = Logger.getLogger(ROSAgArch.class.getName());
	
	
	public ROSAgArch() {
		super();
		
		executor = new CustomExecutorService(4, 4,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
		//Executors.newFixedThreadPool(4, threadFactory);
		
		nodeConfiguration = NodeConfiguration.newPrivate();
		messageFactory = nodeConfiguration.getTopicMessageFactory();
	}
	

	@Override
	public void stop() {
		executor.shutdownNow();
		super.stop();
	}


	@Override
	public void init() {
//		setupMindInspector("gui(cycle,html,history)");    
//		setupMindInspector("file(cycle,xml,log)");
	} 
	
	
	@Override
	public Collection<Literal> perceive() {
		Collection<Literal> l = new ArrayList<Literal>();
		if(rosnode != null) {
//			if(percept_id != m_rosnode.getPercept_id()) {
				Multimap<String,SimpleFact> mm = rosnode.getPerceptions();
				synchronized (mm) {
					Collection<SimpleFact> perceptions = new ArrayList<SimpleFact>(mm.get("\""+getAgName()+"\""));
					if(perceptions != null) {
						for(SimpleFact percept : perceptions) {
							if(percept.getObject().isEmpty()) {
								l.add(Literal.parseLiteral(percept.getPredicate()));
							}else {
								l.add(Literal.parseLiteral(percept.getPredicate()+"("+percept.getObject()+")"));
							}
						}
					}
				}
				percept_id = rosnode.getPercept_id();
//			}
		}
		return l;
		
	}
	
	public class AgRunnable implements Runnable {
		
		private ActionExec action;
		private ROSAgArch rosAgArch;
		
		public AgRunnable(ROSAgArch rosAgArch, ActionExec action) {
			this.action = action;
			this.rosAgArch = rosAgArch;
		}
		
		@Override
		public void run() {
			String action_name = action.getActionTerm().getFunctor();
			Message msg = new Message("tell", getAgName(), "supervisor", "action_started(" + action_name + ")");
			String tmp_task_id = "";
			if (action.getIntention().getBottom().getTrigger().getLiteral().getTerms() != null)
				tmp_task_id = action.getIntention().getBottom().getTrigger().getLiteral().getTerm(0).toString();
			taskID = tmp_task_id;
			try {
				sendMsg(msg);
			} catch (Exception e) {
				Tools.getStackTrace(e);
			}
			Action actionExecutable = ActionFactory.createAction(action, rosAgArch);
			if(action != null) {
				actionExecutable.execute();
				if(actionExecutable.isSync())
					actionExecuted(action);
			} else {
				action.setResult(false);
				action.setFailureReason(new Atom("act_not_found"), "no action " + action_name + " is implemented");
				actionExecuted(action);
			}
		}
	}
	
	@Override
	public void act(final ActionExec action) {
		executor.execute(new AgRunnable(this, action));
	}
	
	public ConnectedNode getConnectedNode() {
		return rosnode.getConnectedNode();
	}
	
	
	public static RosNode getM_rosnode() {
		return rosnode;
	}
	
	public double getRosTimeSeconds() {
		return rosnode.getConnectedNode().getCurrentTime().toSeconds();
	}
	
	public double getRosTimeMilliSeconds() {
		return rosnode.getConnectedNode().getCurrentTime().toSeconds() * 1000.0;
	}

	public TransformTree getTfTree() {
		return rosnode.getTfTree();
	}
	
	void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException ex) {
		}
	}

	protected class CustomExecutorService extends ThreadPoolExecutor {

	    public CustomExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
				BlockingQueue<Runnable> workQueue) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		}

		protected void afterExecute(Runnable r, Throwable t) {
	        super.afterExecute(r, t);
	        if (t == null && r instanceof Future<?>) {
	            try {
	                Future<?> future = (Future<?>) r;
	                if (future.isDone()) {
	                    future.get();
	                }
	            } catch (CancellationException ce) {
	                t = ce;
	            } catch (ExecutionException ee) {
	                t = ee.getCause();
	            } catch (InterruptedException ie) {
	                Thread.currentThread().interrupt();
	            }
	        }
	        if (t != null) {
	            logger.info(Tools.getStackTrace(t));
	        }
	    }
		
	}
	
	public void handleFailure(ActionExec action, String srv_name, RuntimeException e) {
		RosRuntimeException RRE = new RosRuntimeException(e);
		logger.info(Tools.getStackTrace(RRE));

		action.setResult(false);
		action.setFailureReason(new Atom(srv_name+ "_ros_failure"), srv_name+" service failed");
		actionExecuted(action);
	}
	
	protected Literal findBel(String s) {
		return getTS().getAg().findBel(Literal.parseLiteral(s), new Unifier());
	}
	
	public Iterator<Literal> get_beliefs_iterator(String belief_name){
		return getTS().getAg().getBB().getCandidateBeliefs(Literal.parseLiteral(belief_name),new Unifier());
	}
	
	public boolean contains(String belief_name) {
		Iterator<Unifier> iun = Literal.parseLiteral(belief_name).logicalConsequence(getTS().getAg(), new Unifier());
		return iun != null && iun.hasNext() ? true : false;
	}
	
	protected Literal literal(String functor, String term, double value) {
		Literal l = Literal.parseLiteral(functor+"("+term+")");
		l.addTerm(new NumberTermImpl(value));
		return l;
	}
	
	public Literal findBel(Literal bel, BeliefBase bb) {
		Unifier un = new Unifier();
        synchronized (bb.getLock()) {
            Iterator<Literal> relB = bb.getCandidateBeliefs(bel, un);
            if (relB != null) {
                while (relB.hasNext()) {
                    Literal b = relB.next();

                    // recall that order is important because of annotations!
                    if (!b.isRule() && un.unifies(bel, b)) {
                        return b;
                    }
                }
            }
            return null;
        }
    }
	
	protected String taskID = "";
	
	public void addBelief(String bel) {
		try {
			
			getTS().getAg().addBel(Literal.parseLiteral(bel+("".equals(taskID)?"":"[" + taskID + "]")));
		} catch (RevisionFailedException e) {
			logger.info(Tools.getStackTrace(e));
		}
	}
	
	public void removeBelief(String bel) {
		try {
			
			getTS().getAg().abolish(Literal.parseLiteral(bel), new Unifier());
		} catch (RevisionFailedException e) {
			logger.info(Tools.getStackTrace(e));
		}
	}
	
	public <T> T createMessage(String type) {
		return messageFactory.newFromType(type);
	}
	
	public String getTaskID() {
		return taskID;
	}
	
	
}
