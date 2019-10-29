package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import jason.asSemantics.Agent;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.bb.DefaultBeliefBase;

public class FilesAnalysis {
	
	private ArrayList<File> files;
	private float sessionsNumber;
	private float dialogueOBNumber;
	private float notPerceivedOBNumber;
	private float averageTaskNumberPerSessions;
	private float taskNumber;
	private double averageDurTasks;
	private float averageCompletion;
	private float completLess25;
	private float completOver25;
	private float completOver50;
	private float completOver75;
	private LinkedHashMap<String, Double> plannedSteps = new LinkedHashMap<String, Double>();
	private LinkedHashMap<String, Double> notExecutedSteps = new LinkedHashMap<String, Double>();
	private int succeededTasks;
	private int preemptedTasks;
	private int failedTasks;
	private double averageDurSessions;
	private double minDurSessions = Double.MAX_VALUE;
	private double maxDurSessions;
	private Agent ag;
	
	public FilesAnalysis(String directory_path) {
		ag = new Agent();
		ag.initAg();
		this.files = new ArrayList<File>(Arrays.asList(new File(directory_path).listFiles()));
		
	}
	
	public String toString() {
		final PrettyPrinter printer = new PrettyPrinter();
		LinkedHashMap<String, Double> successRateSteps = new LinkedHashMap<String, Double>();
		Iterator<Entry<String, Double>> it = plannedSteps.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, Double> pair = (Map.Entry<String, Double>)it.next();
			successRateSteps.put(pair.getKey(), ( 1 - notExecutedSteps.get(pair.getKey()) / plannedSteps.get(pair.getKey()) ) * 100 );
		}
		String s = "\n\n\n----------------------------------------------------------------------------\n\n" +
				"Sessions:" +
				"\ntotal number of sessions: "+sessionsNumber+
				"\nminimal session duration (sec): " + minDurSessions / 1000.0 +
				"\nmaximal session duration (sec): " + maxDurSessions / 1000.0 +
				"\naverage session duration (sec): " + averageDurSessions / 1000.0 +
				"\naverage task number per session: " + averageTaskNumberPerSessions +
				"\npercentage of sessions over by dialogue: " + ( dialogueOBNumber / sessionsNumber ) * 100 + " %" +
				"\npercentage of sessions over by human not perceived: " + ( notPerceivedOBNumber / sessionsNumber ) * 100 + " %" +
				"\n\n\n----------------------------------------------------------------------------\n\n" +
				"\nTasks:" +
				"\ntasks numer: " + taskNumber + 
				"\naverage task duration (sec): " + averageDurTasks / 1000.0 +
//				"\naverage task completion : " + averageCompletion +  " %" +
				"\npercentage of succeeded tasks: " + succeededTasks / taskNumber * 100 + " %" +
				"\npercentage of preempted tasks: " + preemptedTasks / taskNumber * 100 + " %" +
				"\npercentage of failed tasks: " + failedTasks / taskNumber * 100 +
				"\npercentage of tasks with completion rate less than 25%: " + completLess25 / taskNumber * 100 + " %" +
				"\npercentage of tasks with completion rate between 25% and 50%: " + completOver25 / taskNumber * 100 + " %" +
				"\npercentage of tasks with completion rate between 50% and 75%: " + completOver50 / taskNumber * 100 + " %" +
				"\npercentage of tasks with completion rate between 75% and 100%: " + completOver75 / taskNumber * 100 + " %" +
				"\n\nsuccess rate of planned steps: \n"+
				printer.hashMapToString(successRateSteps);
				
		return s;
	}
	
	class SessionData{
		public double duration;
		public String overBy;
		public float taskNumber;
		public int succeededTasks;
		public int preemptedTasks;
		public int failedTasks;
		
		@Override
		public String toString() {
			String s = "session duration (sec): " + duration / 1000.0 +
					"\nsession over by: " +overBy +
					"\nnumber of tasks in session: " + taskNumber +
					"\npercentage of succeeded tasks: " + succeededTasks / taskNumber * 100 + " %" +
					"\npercentage of preempted tasks: " + preemptedTasks / taskNumber * 100 + " %" +
					"\npercentage of failed tasks: " + failedTasks / taskNumber * 100 + " %\n";
			return s;
		}
		
	};
	
	class TaskData{
		public double duration;
		public float completion;
		public String status;
		public String failureReason;
		public String lastExecutedStep;
		public ArrayList<String> stepsList = new ArrayList<String>();
		public ArrayList<String> missingSteps = new ArrayList<String>();
		
		@Override
		public String toString() {
			String s = "task duration (sec): " + duration / 1000.0 +
					"\ntask status: " + status +
					"\ntask completion: " + completion + " %" +
					"\ntask failure reason: " + failureReason +
					"\nlast executed step: " + lastExecutedStep +
					"\nmissing steps: " + missingSteps +"\n";
			return s;
		}
	}
	
	
	public SessionData get_file_session_data(DefaultBeliefBase bb) {
		ag.setBB(bb);
		SessionData data = new SessionData();
		Iterator<Literal> iu = get_beliefs_iterator("inSession(_,_)");
		Literal inSession = null;
		if (iu != null && iu.hasNext()) {
			inSession = iu.next();
		}
		
		double s_time = ((NumberTermImpl)((Literal) inSession.getAnnots("add_time").get(0)).getTerm(0)).solve();
		
		iu = get_beliefs_iterator("overBy(_)");
		Literal overBy = null;
		if (iu != null && iu.hasNext()) {
			overBy = iu.next();
		}
		if(overBy != null) {
			double e_time = ((NumberTermImpl)((Literal) overBy.getAnnots("add_time").get(0)).getTerm(0)).solve();
			
			data.duration = e_time - s_time;
			data.overBy = ((Literal) overBy.getTerm(0)).toString();
		}else {
			data.duration = -1;
			data.overBy = null;
		}
		
		iu = get_beliefs_iterator("startTask(_,_)");
		while (iu != null && iu.hasNext()) {
			data.taskNumber++;
			iu.next();
		}
		
//		iu = get_beliefs_iterator(bb, "endTask(_,_,_)");
//		while (iu != null && iu.hasNext()) {
//			Literal s = iu.next();
//			if(((Literal) s.getTerm(0)).getFunctor().toString().contains("succeeded"))
//				data.succeededTasks++;
//			else if(((Literal) s.getTerm(0)).getFunctor().toString().contains("preempted"))
//				data.preemptedTasks++;
//			else if(((Literal) s.getTerm(0)).getFunctor().toString().contains("failed"))
//				data.failedTasks++;
//		}
		
		System.out.println(data);
		return data;
		
	}
	
	public TaskData get_file_task_data(DefaultBeliefBase bb) {
		ag.setBB(bb);
		float steps = 3;
		float step = 0;
		ArrayList<String> steps_list = new ArrayList<String>(Arrays.asList("agree_goal","show_target","check_understand"));
		ListIterator<String> ite = steps_list.listIterator(1);
		TaskData data = new TaskData();
		
		// calculate the total number of steps of the task
		
		if (contains("persona_asked(_)")) {
			steps += 1.0;
			ite.add("ask_stairs");
		}
		ite.next();
		
		if (contains("target_to_point(_)")) {
			steps += 1.0;
			ite.add("check_target_seen");
		}
		
		if (contains("direction(_)") || steps_list.contains("ask_stairs")) {
			steps += 1.0;
			ite.add("show_direction");
			
			if (contains("direction_to_point(_)")) { //change that to dir_to_point for files created after fixing name
				steps += 1.0;
				ite.add("check_direction_seen");
			}
		}
		
		// determine in which step the task has been stopped
		
		if(contains("got_answer(ask_understand,\"yes\",_)")) {
			step = steps_list.size() - 1;
		}else {
			if(contains("said(ask_understand,_)")) {
				step = steps_list.indexOf(ite.previous()); 
			}else {
				if( ( contains("said(route_verbalization(_),0)") || contains("said(route_verbalization_n_vis(_),0)") ) && steps_list.contains("show_direction")) {
						step = steps_list.indexOf("show_direction");
				}else {
					if(contains("got_answer(cannot_tell_seen,\"yes\",_)") && steps_list.contains("check_target_seen")) {
						step = steps_list.indexOf("check_target_seen");
					}else {
						if(contains("explained")) {
							step = steps_list.indexOf("show_target");
						}else {
							if(contains("persona_asked(_)")) {
								step = steps_list.indexOf("ask_stairs");
							}else {				
								if(contains("guiding_goal_nego(_,_)")) {
									step =  steps_list.indexOf("agree_goal");
								}else {
									step = -1;
								}
							}
						}
					}
					
				}
			}
		}
		
		data.completion = (step + 1) / steps * 100;
		if(step != -1) {
			data.lastExecutedStep = steps_list.get((int) step);
			ite = steps_list.listIterator((int) step + 1);
			while(ite.hasNext()) {
				String a = ite.next();
				data.missingSteps.add(a);
			}
		} else {
			data.lastExecutedStep = null;
			data.missingSteps = steps_list;
		}
		
		Iterator<Literal> iu = get_beliefs_iterator("started");
		Literal start = null;
		if (iu != null && iu.hasNext()) {
			start = iu.next();
		}
		
		double s_time = ((NumberTermImpl)((Literal) start.getAnnots("add_time").get(0)).getTerm(0)).solve();
		
		iu = get_beliefs_iterator("end_task(_,_)");
		Literal end = null;
		if (iu != null && iu.hasNext()) {
			end = iu.next();
		}
		if(end != null) {
			double e_time = ((NumberTermImpl)((Literal) end.getAnnots("add_time").get(0)).getTerm(0)).solve();
			
			data.duration = e_time - s_time;
			
			data.status = ((Literal) end.getTerm(0)).getFunctor().toString();
			if(data.status.equals("failed")){
				iu = get_beliefs_iterator("failure(_,_,_)");
				if(iu != null && iu.hasNext()) {
					data.failureReason = ((Literal) iu.next().getTerm(2)).getFunctor().toString();
				}
			}
		}else {
			data.duration = -1;
		}
		
		data.stepsList = steps_list;
		
		System.out.println(data);
		return data;
		
	}
	
	public DefaultBeliefBase get_beliefs(String path) {
		System.out.println(path);
		DefaultBeliefBase bb = new DefaultBeliefBase();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();
			while(line != null) {
				bb.add(Literal.parseLiteral(line));
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bb; 
	}
	
	// take annotations into account
	public Iterator<Literal> get_beliefs_iterator(String belief_name){
		return ag.getBB().getCandidateBeliefs(Literal.parseLiteral(belief_name),new Unifier());
	}
	
	public boolean contains(String belief_name) {
		Iterator<Unifier> iun = Literal.parseLiteral(belief_name).logicalConsequence(ag, new Unifier());
		return iun != null && iun.hasNext() ? true : false;
	}
	
	public void sessions_analysis(SessionData sd) {
		sessionsNumber++;
		if(sd.duration < minDurSessions && sd.duration > 0)
			minDurSessions = sd.duration;
		
		if(sd.duration > maxDurSessions)
			maxDurSessions = sd.duration;
		
		if(sd.duration != -1)
			averageDurSessions = averageDurSessions + ( sd.duration - averageDurSessions ) / sessionsNumber;
		
		averageTaskNumberPerSessions = averageTaskNumberPerSessions + ( sd.taskNumber - averageTaskNumberPerSessions ) / sessionsNumber;
//		
//		succeededTasks += sd.succeededTasks;
//		preemptedTasks += sd.preemptedTasks;
//		failedTasks += sd.failedTasks;
		
		if(sd.overBy != null) {
			if(sd.overBy.equals("dialogue"))
				dialogueOBNumber++;
			else if(sd.overBy.equals("not_perceived"))
				notPerceivedOBNumber++;
		}
	}
	
	public void tasks_analysis(TaskData td) {
		taskNumber++;
		
		if(td.duration != -1)
			averageDurTasks = averageDurTasks + ( td.duration - averageDurTasks ) / taskNumber;
		
		averageCompletion = averageCompletion + (td.completion - averageCompletion ) / taskNumber;
		
		if(td.completion < 25.0)
			completLess25++;
		else if(td.completion >= 75.0)
			completOver75++;
		else if(td.completion >= 50.0)
			completOver50++;
		else if(td.completion >= 25.0)
			completOver25++;
		
		if(td.status.equals("succeeded"))
			succeededTasks++;
		else if(td.status.equals("preempted"))
			preemptedTasks++;
		else
			failedTasks++;
		
		Iterator<String> it = td.stepsList.iterator();
		while(it.hasNext()) {
			String s = it.next();
			plannedSteps.put(s, plannedSteps.get(s)+1);
			if(td.missingSteps.contains(s))
				notExecutedSteps.put(s, notExecutedSteps.get(s)+1);
		}
	}
	
	@SuppressWarnings("serial")
	public static void main(String[] args) {
		FilesAnalysis fa = new FilesAnalysis("/home/amayima/Projets-robot/MuMMER/log/beliefs");
		fa.plannedSteps = new LinkedHashMap<String, Double>() {{
	        put("agree_goal", 0.0);
	        put("ask_stairs", 0.0);
	        put("show_target", 0.0);
	        put("check_target_seen", 0.0);
	        put("show_direction", 0.0);
	        put("check_direction_seen", 0.0);
	        put("check_understand", 0.0);
	    }};
	    fa.notExecutedSteps = new LinkedHashMap<String, Double>(fa.plannedSteps);
		Iterator<File> it = fa.files.iterator();
		while(it.hasNext()) {
			File f = it.next();
			if(!f.isDirectory()) {
				if(f.getName().contains("session")) {
					SessionData sd = fa.get_file_session_data(fa.get_beliefs(f.getAbsolutePath()));
					fa.sessions_analysis(sd);
				}else {
					TaskData td = fa.get_file_task_data(fa.get_beliefs(f.getAbsolutePath()));
					fa.tasks_analysis(td);
				}
			}
		}	
		System.out.println(fa.toString());
	}
	
	
	

}
