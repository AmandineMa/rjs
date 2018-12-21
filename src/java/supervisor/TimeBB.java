package supervisor;

import jason.asSemantics.*;
import jason.asSyntax.*;
import jason.bb.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimeBB extends DefaultBeliefBase {

	private static long start;
	private static boolean start_initialized = false;
	private static Logger logger = Logger.getLogger(TimeBB.class.getSimpleName());

	@Override
	public void init(Agent ag, String[] args) {
		if(!start_initialized) {
			start = System.currentTimeMillis();
			start_initialized = true;
		}
		super.init(ag,args);
	}

	@Override
	public boolean add(Literal bel) {
		annote_time(bel);
		return super.add(bel);
	}

	@Override
	public boolean add(int index, Literal bel) {
		annote_time(bel);
		return super.add(bel, index != 0);
	}

	@Override
	protected boolean add(Literal bel, boolean addInEnd) {
		annote_time(bel);
		return super.add(bel, addInEnd);
	}

	private void annote_time(Literal bel) {
		if (! hasTimeAnnot(bel)) {
			Structure time = new Structure("add_time");
			long pass = System.currentTimeMillis() - start;
			time.addTerm(new NumberTermImpl(pass));
			bel.addAnnot(time);
		}
	}

	private boolean hasTimeAnnot(Literal bel) {
		Literal belInBB = contains(bel);
		if (belInBB != null)
			for (Term a : belInBB.getAnnots())
				if (a.isStructure())
					if (((Structure)a).getFunctor().equals("add_time"))
						return true;
		return false;
	}
	
	public long getStartTime() {
		return start;
	}
}