// Internal action code for project supervisor

package rjs.jia;

import java.util.Iterator;

import jason.*;
import jason.asSemantics.*;
import jason.asSyntax.*;
import jason.asSyntax.Trigger.TEOperator;
import jason.asSyntax.Trigger.TEType;

public class suspend_all extends DefaultInternalAction {

	boolean suspendIntention = false;
    public static final String SUSPENDED_INT      = "suspended-";
    public static final String SELF_SUSPENDED_INT = SUSPENDED_INT+"self-";

    @Override public int getMinArgs() {
        return 0;
    }
    @Override public int getMaxArgs() {
        return 1;
    }

    @Override protected void checkArguments(Term[] args) throws JasonException {
        super.checkArguments(args); // check number of arguments
        if (args.length == 1 && !args[0].isLiteral())
            throw JasonException.createWrongArgument(this,"first argument must be a literal");
    }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);

        suspendIntention = false;
        
        Unifier bak = un.clone();

        Circumstance C = ts.getC();

        if (args.length == 0) {
            // suspend the current intention
            Intention i = C.getSelectedIntention();
            suspendIntention = true;
            i.setSuspended(true);
            C.addPendingIntention(SELF_SUSPENDED_INT+i.getId(), i);
            return true;
        }

        // use the argument to select the intention to suspend.

        Trigger      g = new Trigger(TEOperator.add, TEType.achieve, (Literal)args[0]);

        // ** Must test in PA/PI first since some actions (as .suspend) put intention in PI

        // suspending from Pending Actions
        for (ActionExec a: C.getPendingActions().values()) {
            Intention i = a.getIntention();
            if (i.hasTrigger(g, un)) {
                i.setSuspended(true);
                C.addPendingIntention(SUSPENDED_INT+i.getId(), i);
                un = bak.clone();
            }
        }

        // suspending from Pending Intentions
        for (Intention i: C.getPendingIntentions().values()) {
            if (i.hasTrigger(g, un)) {
                i.setSuspended(true);
                un = bak.clone();
            }
        }

        Iterator<Intention> itint = C.getRunningIntentionsPlusAtomic();
        while (itint.hasNext()) {
            Intention i = itint.next();
            if (i.hasTrigger(g, un)) {
                i.setSuspended(true);
                C.removeRunningIntention(i);
                C.addPendingIntention(SUSPENDED_INT+i.getId(), i);
                un = bak.clone();
                System.out.println("sus "+g+" from I "+i.getId()+" #"+C.getPendingIntentions().size());
            }
        }

        // suspending the current intention?
        Intention i = C.getSelectedIntention();
        if (i != null && i.hasTrigger(g, un)) {
            suspendIntention = true;
            i.setSuspended(true);
            C.addPendingIntention(SELF_SUSPENDED_INT+i.getId(), i);
            un = bak.clone();
        }

        // suspending G in Events
        int c = 0;
        Iterator<Event> ie = C.getEventsPlusAtomic();
        while (ie.hasNext()) {
            Event e = ie.next();
            i = e.getIntention();
            if (un.unifies(g, e.getTrigger()) || (i != null && i.hasTrigger(g, un))) {
                C.removeEvent(e);
                C.addPendingEvent(SUSPENDED_INT+e.getTrigger()+(c++), e);
                if (i != null)
                    i.setSuspended(true);
                un = bak.clone();
                //System.out.println("sus "+g+" from E "+e.getTrigger());
            }


            /*
            if ( i != null &&
                    (i.hasTrigger(g, un) ||       // the goal is in the i's stack of IM
                     un.unifies(g, e.getTrigger())  // the goal is the trigger of the event
                    )
                ) {
                i.setSuspended(true);
                C.removeEvent(e);
                C.addPendingIntention(k, i);
            } else if (i == Intention.EmptyInt && un.unifies(g, e.getTrigger())) { // the case of !!
                // creates an intention to suspend the "event"
                i = new Intention();
                i.push(new IntendedMeans(
                        new Option(
                                new Plan(null, e.getTrigger(), Literal.LTrue,
                                        new PlanBodyImpl(BodyType.achieveNF, e.getTrigger().getLiteral())),
                                new Unifier()),
                        e.getTrigger()));
                e.setIntention(i);
                i.setSuspended(true);
                C.removeEvent(e);
                C.addPendingIntention(k, i);
            }
            */
        }

        return true;
    }

    @Override
    public boolean suspendIntention() {
        return suspendIntention;
    }
}
