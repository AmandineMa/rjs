package arch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.google.common.collect.Multimap;

import jason.asSyntax.Literal;
import utils.SimpleFact;

public class InteractAgArch extends ROSAgArch {

	@Override
	public Collection<Literal> perceive() {
		Collection<Literal> l = new ArrayList<Literal>();
		if(m_rosnode != null) {
			if(percept_id != m_rosnode.getPercept_id()) {
				Multimap<String,SimpleFact> mm = m_rosnode.getPerceptions();
				synchronized (mm) {
					HashMap<String, Collection<SimpleFact>> perceptions = new HashMap<String, Collection<SimpleFact>>(mm.asMap());
					if(perceptions != null) {
						for(String agent : perceptions.keySet()) {
							for(SimpleFact percept : perceptions.get(agent)) {
								if(agent.contains("human")) {
									if(percept.getPredicate().equals("isLookingAt")
											|| percept.getPredicate().equals("isSpeakingTo")
											|| percept.getPredicate().equals("isClose")
											|| percept.getPredicate().equals("isInFrontOf")) {

										if(percept.getObject().isEmpty()) {
											l.add(Literal.parseLiteral(percept.getPredicate()+"("+agent+")"));
										}else {
											l.add(Literal.parseLiteral(percept.getPredicate()+"("+agent+","+percept.getObject()+")"));
										}
									}
								}
							}
						}
					}
				}


				percept_id = m_rosnode.getPercept_id();
			}
		}
		return l;

	}

}
