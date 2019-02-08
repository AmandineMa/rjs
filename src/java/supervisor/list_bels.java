package supervisor;

import jason.asSemantics.*;
import jason.asSyntax.*;

@SuppressWarnings("serial")
public class list_bels extends DefaultInternalAction {

   @Override
   public Object execute(TransitionSystem ts, 
                         Unifier un, 
                         Term[] args) throws Exception {

      for (Literal b: ts.getAg().getBB()) {
         ts.getLogger().info(b.toString());
      }
      return true;
   }
}