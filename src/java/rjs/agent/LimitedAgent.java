package rjs.agent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import jason.JasonException;
import jason.asSemantics.Agent;
import jason.asSemantics.GoalListener;
import jason.asSemantics.Intention;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Pred;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.asSyntax.Trigger;

public class LimitedAgent extends Agent {

	private HashMap<String,Counter> counters;

	public LimitedAgent() {
		counters = new HashMap<String,Counter>();
	}

	private enum CountingType { attempt, exec } ;

	@Override
	public void initAg() {
		super.initAg();
		getTS().addGoalListener(new GoalListener() {

			@Override
			public void goalFinished(Trigger goal, GoalStates result) {
				removeTrigger(goal);
			}

			@Override
			public void goalFailed(Trigger goal, Term reason) {
				decrementCounter(System.identityHashCode(goal));
				removeTrigger(goal);	
			}
			
			private void removeTrigger(Trigger goal) {
				int hashcode = System.identityHashCode(goal);
				Counter c = findCounter(hashcode);
				if(c != null)
					c.removeTrigger(hashcode);
			}
			
		});
	}

	@Override
	public Intention selectIntention(Queue<Intention> intentions) {
		Intention selected_i = super.selectIntention(intentions);
		if(selected_i.peek() != null) {
			Pred label = selected_i.peek().getPlan().getLabel();

			ListTerm annots = label.getAnnots();
			for(Term t : annots) {
				if(t.isStructure()) {
					String f = ((Structure) t).getFunctor();
					if("max_attempts".equalsIgnoreCase(f) && ((Structure) t).hasTerm() || "max_exec".equalsIgnoreCase(f) && ((Structure) t).hasTerm()) {
						long att = Math.round(((NumberTermImpl) ((Structure) t).getTerm(0)).solve());
						String l = label.getFunctor();
						Trigger trig = selected_i.peek().getTrigger();
						String p;
						if(!trig.getLiteral().hasTerm())
							p = "";
						else	
							p = trig.getLiteral().getTerms().toString();

						Counter c = findCounter(l, p);
						if(c == null) {
							if("max_attempts".equalsIgnoreCase(f))
								c = new Counter(l, p, (int) att, CountingType.attempt);
							else
								c = new Counter(l, p, (int) att, CountingType.exec);
							counters.put(trig.getLiteral().getFunctor(),c);
						}

						if(c.isMaxed() & !c.hasTrigger(System.identityHashCode(trig))) {
							ListTerm failAnnots;
							Atom atom;
							if(c.getCounting_type() == CountingType.attempt) {
								failAnnots = JasonException.createBasicErrorAnnots("max_attempts", "the limit of attempts ("+att+") is reached");
								atom = ASSyntax.createAtom("max_attempts");
							}else {
								failAnnots = JasonException.createBasicErrorAnnots("max_exec", "the limit of executions ("+att+") is reached");
								atom = ASSyntax.createAtom("max_exec");
							}
							try {
								getTS().generateGoalDeletion(selected_i, failAnnots, atom);
							} catch (JasonException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							selected_i = null;

						}else {
							if(c.addTrigger(System.identityHashCode(trig)))
								incrementCounter(System.identityHashCode(trig));
						}
					}
				}	
			}
		}else {
			selected_i = null;
		}

		return selected_i;
	}

	public void removeCounter(String label) {
		counters.remove(label);
	}

	public void removeCounters() {
		counters.clear();
	}

	private int incrementCounter(Integer t) {
		int result = -1;
		Counter c = findCounter(t);
		if(c != null)
			result = c.increment();
		return result;
	}

	private int decrementCounter(Integer t) {
		int result = -1;
		Counter c = findCounter(t);
		if(c != null) {
			if(c.getCounting_type() == CountingType.exec)
				result = c.decrement();
		}
		return result;
	}

	private Counter findCounter(Integer t) {
		for(Counter c : counters.values())
			if(c.hasTrigger(t)) return c;
		return null;
	}


	private Counter findCounter(String label, String params) {
		for(Counter c : counters.values()) {
			if(c.getLabel().equalsIgnoreCase(label) && c.getParams().equalsIgnoreCase(params)) return c;
		}
		return null;
	}

	private class Counter {

		private int attempts = 0;
		private int max_attempts = 0;
		private String params = "";
		private String label = "";
		private Set<Integer> triggers;
		private CountingType counting_type;


		public int increment() {
			//			attempts = Math.min(attempts+1, max_attempts);
			attempts += 1;

			return attempts;
		}

		public int decrement() {
			attempts = Math.max(attempts-1, 0);

			return attempts;
		}

		public void removeTrigger(Integer t) {
			triggers.remove(t);
		}

		public boolean addTrigger(Integer t) {
			return triggers.add(t);
		}

		public String getParams() {
			return params;
		}

		public String getLabel() {
			return label;
		}


		public CountingType getCounting_type() {
			return counting_type;
		}

		public Counter(String l, String p, int ma, CountingType ct) {
			max_attempts = ma;
			params = p;
			label = l;
			triggers = new HashSet<Integer>();
			counting_type = ct;
		}

		public boolean isMaxed() {
			return attempts>=max_attempts;
		}

		public boolean hasTrigger(Integer trig) {
			return triggers.contains(trig);
		}

		public String toString() {
			return "Counter: "+label+"/"+params+" Attempts: " + attempts+"/"+max_attempts+" NbTrig: "+triggers.size();
		}

	}

}
