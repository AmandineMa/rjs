package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.google.common.io.Files;

import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.bb.DefaultBeliefBase;

public class FilesAnalysis {
	
	private ArrayList<File> files;
	private float sessions_number;
	private float dialogue_OB_number;
	private float not_perceived_OB_number;
	private float average_task_number;
	private double average_dur_sessions;
	private double min_dur_sessions = Double.MAX_VALUE;
	private double max_dur_sessions;
	
	public FilesAnalysis(String directory_path) {
		this.files = new ArrayList<File>(Arrays.asList(new File(directory_path).listFiles()));
		
	}
	
	public String toString() {
		String s = "total number of sessions: "+sessions_number+
				"\nminimal session duration (sec): " + min_dur_sessions / 1000 +
				"\nmaximal session duration (sec): " + max_dur_sessions / 1000 +
				"\naverage session duration (sec): " + average_dur_sessions / 1000 +
				"\naverage task number per session: " + average_task_number +
				"\npercentage of sessions over by dialogue: " + ( dialogue_OB_number / sessions_number ) * 100+
				"\npercentage of sessions over by human not perceived: " + ( not_perceived_OB_number / sessions_number )*100;
		return s;
	}
	
	class SessionData{
		public double duration;
		public String overBy;
		public int taskNumber = 0;
		
		@Override
		public String toString() {
			String s = "session duration: "+duration+"\nsession over by: "+overBy+"\nnumber of tasks in session: "+taskNumber+"\n";
			return s;
		}
		
	};
	
	
	public SessionData get_file_session_data(DefaultBeliefBase bb) {
		SessionData data = new SessionData();
		Iterator<Literal> iu = bb.getCandidateBeliefs(Literal.parseLiteral("inSession(_,_)"),new Unifier());
		Literal inSession = null;
		if (iu != null && iu.hasNext()) {
			inSession = iu.next();
		}
		
		double s_time = ((NumberTermImpl)((Literal) inSession.getAnnots("add_time").get(0)).getTerm(0)).solve();
		
		iu = bb.getCandidateBeliefs(Literal.parseLiteral("overBy(_)"),new Unifier());
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
		
		iu = bb.getCandidateBeliefs(Literal.parseLiteral("startTask(_,_)"),new Unifier());
		while (iu != null && iu.hasNext()) {
			data.taskNumber++;
			iu.next();
		}
		
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
	
	public void sessions_analysis(SessionData sd) {
		sessions_number++;
		if(sd.duration < min_dur_sessions && sd.duration > 0)
			min_dur_sessions = sd.duration;
		
		if(sd.duration > max_dur_sessions)
			max_dur_sessions = sd.duration;
		
		if(sd.duration != 0)
			average_dur_sessions = average_dur_sessions + ( sd.duration - average_dur_sessions ) / sessions_number;
		
		average_task_number = average_task_number + ( sd.taskNumber - average_task_number ) / sessions_number;
		
		if(sd.overBy != null) {
			if(sd.overBy.equals("dialogue"))
				dialogue_OB_number++;
			else if(sd.overBy.equals("not_perceived"))
				not_perceived_OB_number++;
		}
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		FilesAnalysis fa = new FilesAnalysis("/home/amayima/Projets-robot/MuMMER/log/beliefs/");
		Iterator<File> it = fa.files.iterator();
		while(it.hasNext()) {
			File f = it.next();
			if(!f.isDirectory()) {
				if(f.getName().contains("session")) {
					SessionData sd = fa.get_file_session_data(fa.get_beliefs(f.getAbsolutePath()));
					fa.sessions_analysis(sd);
				}
			}
		}	
		System.out.println(fa.toString());
	}
	
	
	

}
