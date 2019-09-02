package arch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.ros.node.ConnectedNode;
import org.ros.rosjava.tf.TransformTree;

import com.google.common.collect.Multimap;
import jason.architecture.MindInspectorAgArch;
import jason.asSyntax.Literal;
import ros.RosNode;
import utils.SimpleFact;
import utils.Tools;

public class ROSAgArch extends MindInspectorAgArch {
	
	static protected RosNode m_rosnode;
	
	protected ExecutorService executor;
	
	protected int percept_id = -1;

	protected Logger logger = Logger.getLogger(ROSAgArch.class.getName());
	
	
	public ROSAgArch() {
		super();
		
		executor = new CustomExecutorService(4, 4,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
		//Executors.newFixedThreadPool(4, threadFactory);
	}
	

	@Override
	public void stop() {
		executor.shutdownNow();
		super.stop();
	}


	@Override
	public void init() {
//		setupMindInspector("gui(cycle,html,history)");    
		setupMindInspector("file(cycle,xml,log)");    
	} 
	
	
	@Override
	public Collection<Literal> perceive() {
		Collection<Literal> l = new ArrayList<Literal>();
		if(m_rosnode != null) {
//			if(percept_id != m_rosnode.getPercept_id()) {
				Multimap<String,SimpleFact> mm = m_rosnode.getPerceptions();
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
				percept_id = m_rosnode.getPercept_id();
//			}
		}
		return l;
		
	}
	
	public ConnectedNode getConnectedNode() {
		return m_rosnode.getConnectedNode();
	}
	
	
	public static RosNode getM_rosnode() {
		return m_rosnode;
	}
	
	public double getRosTime() {
		return m_rosnode.getConnectedNode().getCurrentTime().toSeconds();
	}

	public TransformTree getTfTree() {
		return m_rosnode.getTfTree();
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
}
