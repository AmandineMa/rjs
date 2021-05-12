package rjs.arch.agarch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
import org.ros.helpers.ParameterLoaderNode;
import org.ros.internal.loader.CommandLineLoader;
import org.ros.message.MessageFactory;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.rosjava.tf.TransformTree;

import com.google.common.collect.Lists;

import jason.RevisionFailedException;
import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.bb.BeliefBase;
import rjs.arch.actions.AbstractActionFactory;
import rjs.arch.actions.Action;
import rjs.ros.AbstractRosNode;
import rjs.utils.Tools;

public abstract class AbstractROSAgArch extends AgArch {
	
	protected AbstractRosNode rosnode;
	
	protected ExecutorService executor;
	NodeConfiguration nodeConfiguration;
	NodeConfiguration nodeConfigurationParams;
	protected NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
	protected ParameterLoaderNode parameterLoaderNode;

	private MessageFactory messageFactory;
	protected static AbstractActionFactory actionFactory;
	
	protected Logger logger = Logger.getLogger(AbstractROSAgArch.class.getName());
	
	
	public AbstractROSAgArch() {
		super();
	}
	
	@Override
	public void init() {
		executor = new CustomExecutorService(4, 4,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
		//Executors.newFixedThreadPool(4, threadFactory);
		
		nodeConfiguration = NodeConfiguration.newPrivate();
		setMessageFactory(nodeConfiguration.getTopicMessageFactory());
		
		List<String> emptyArgv = Lists.newArrayList("EmptyList");
		CommandLineLoader loader = new CommandLineLoader(emptyArgv);
		URI masterUri = null;
		nodeConfiguration = loader.build();	
		try {
			masterUri = new URI(System.getenv("ROS_MASTER_URI"));			
		} catch (URISyntaxException e) {
			logger.info("Wrong URI syntax :" + e.getMessage());
		} 
		nodeConfiguration.setMasterUri(masterUri);
		
		List<String> params = Arrays.asList("/general.yaml", "/plan_manager.yaml");
		List<ParameterLoaderNode.Resource> resourceList = new ArrayList<ParameterLoaderNode.Resource>();
		for(String p : params) {
			String file = Tools.removeQuotes(p.toString());
			resourceList.add(new ParameterLoaderNode.Resource(AbstractROSAgArch.class.getResourceAsStream(file), ""));
		}
		nodeConfigurationParams = NodeConfiguration.copyOf(nodeConfiguration);
		nodeConfigurationParams.setDefaultNodeName(getAgName()+"_parameter_loader");
		parameterLoaderNode = new ParameterLoaderNode(resourceList);
		nodeMainExecutor.execute(parameterLoaderNode, nodeConfigurationParams);
		
		nodeConfiguration.setDefaultNodeName(getAgName());
		initRosNode();
		nodeMainExecutor.execute(rosnode, nodeConfiguration);
		while(rosnode.getConnectedNode() == null) {
			Tools.sleep(100);
		}
		rosnode.init();

		try {
			setActionFactoryRosVariables();
		} catch (Exception e) {
			Tools.getStackTrace(e);
		}
	} 
	
	public abstract void initRosNode();
	
	public void setActionFactory(AbstractActionFactory actionF) {
		actionFactory = actionF;
	}
	
	public void setActionFactoryRosVariables() throws Exception  {
		if(actionFactory != null && rosnode != null)
			actionFactory.setRosVariables(rosnode);
		else
			throw new Exception("action factory or rosnode null");
	}
	
	public class AgRunnable implements Runnable {
		
		private ActionExec action;
		
		public AgRunnable(ActionExec action) {
			this.action = action;
		}
		
		@Override
		public void run() {
			String action_name = action.getActionTerm().getFunctor();
			Action actionExecutable = actionFactory.createAction(action, AbstractROSAgArch.this);
			if(actionExecutable != null) {
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
		executor.execute(new AgRunnable(action));
	}
	
	@Override
	public void stop() {
		executor.shutdownNow();
		super.stop();
	}
	
	public AbstractRosNode getRosnode() {
		return rosnode;
	}

	public ConnectedNode getConnectedNode() {
		return rosnode.getConnectedNode();
	}
	
	public abstract void initListeners();
	
	public <T> void setSubListener(String subName, MessageListener<T> listener) {
		rosnode.setSubListener(subName, listener);
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
	
	public Literal findBel(String s) {
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
	
	public void addBelief(String bel) {
		try {
			
			getTS().getAg().addBel(Literal.parseLiteral(bel));
		} catch (RevisionFailedException e) {
			logger.info(Tools.getStackTrace(e));
		}
	}
	
	public void addBelief(String functor, List<Object> terms) {
		try {
			getTS().getAg().addBel(Tools.stringFunctorAndTermsToBelLiteral(functor, terms));
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
	
	public void removeBelief(String functor, List<Object> terms) {
		try {
			getTS().getAg().abolish(Tools.stringFunctorAndTermsToBelLiteral(functor, terms), new Unifier());
		} catch (RevisionFailedException e) {
			logger.info(Tools.getStackTrace(e));
		}
	}
	
	public <T> T createMessage(String type) {
		return getMessageFactory().newFromType(type);
	}

	public MessageFactory getMessageFactory() {
		return messageFactory;
	}

	public void setMessageFactory(MessageFactory messageFactory) {
		this.messageFactory = messageFactory;
	}

	public void setNodeMainExecutor(NodeMainExecutor nodeMainExecutor) {
		this.nodeMainExecutor = nodeMainExecutor;
	}
	
	public double getCurrentTime() {
		return getConnectedNode().getCurrentTime().toSeconds() * 1000;
	}

	
	
}
