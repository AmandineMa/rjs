package rjs.agent;

import java.util.logging.Logger;

import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.bb.DefaultBeliefBase;
import rjs.arch.agarch.AbstractROSAgArch;

public class TimeBB extends DefaultBeliefBase {
	
	// time in milliseconds

	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(TimeBB.class.getSimpleName());
	private AbstractROSAgArch agArch;

	@Override
	public boolean add(Literal bel) {
		annote_time(bel);
		return super.add(bel, false);
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

	protected void annote_time(Literal bel) {
		if (! hasTimeAnnot(bel) && agArch instanceof AbstractROSAgArch) {
			Structure time = new Structure("add_time");
			Double t = ((AbstractROSAgArch) agArch).getCurrentTime();
			time.addTerm(new NumberTermImpl(t.longValue()));
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
	
	public void setAgArch(AbstractROSAgArch agArch) {
		this.agArch = agArch;
	}
	
}