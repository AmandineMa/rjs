package supervisor;

import jason.RevisionFailedException;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Message;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;
import ontologenius_msgs.OntologeniusServiceResponse;
import perspectives_msgs.HasMeshResponse;
import pointing_planner_msgs.PointingActionResult;
import pointing_planner_msgs.VisibilityScoreResponse;
import route_verbalization.VerbalizeRegionRouteResponse;
import semantic_route_description_msgs.Route;
import semantic_route_description_msgs.SemanticRouteResponse;
import speech_wrapper_msgs.SpeakResponse;
import speech_wrapper_msgs.SpeakToResponse;
import supervisor.Code;
import msg_srv_impl.RouteImpl;
import msg_srv_impl.SemanticRouteResponseImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import actionlib_msgs.GoalStatus;
import deictic_gestures_msgs.PointAtResponse;
import geometry_msgs.Pose;



public class RobotAgArch extends ROSAgArch {
    
	@SuppressWarnings("unused")
	private Logger logger = Logger.getLogger(RobotAgArch.class.getName());
    
    @Override
    public void act(ActionExec action) {
    	String action_name = action.getActionTerm().getFunctor();
    	Message msg = new Message("tell", getAgName(), "supervisor", "action_started("+action_name+")");
  
		try {
			sendMsg(msg);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		if(action_name.equals("act")) {
			action.setResult(true);
			actionExecuted(action);
		}else if(action_name.equals("compute_route")) {
    		// to remove the extra ""
    		String from = action.getActionTerm().getTerm(0).toString();
    		from = from.replaceAll("^\"|\"$", "");
    		
    		
    		ListTerm to_list;
    		Term to = (Term) action.getActionTerm().getTerm(1);
    		// if there is multiple places (toilet and atm cases)
    		if(to.isList()) {
    			to_list = (ListTerm) to;
    		}
    		// if there is only one place, we convert it to a list with one element for convenience
    		else {
    			to_list = new ListTermImpl();
    			to_list.add(to);
    		}
    		List<SemanticRouteResponse> routes = new ArrayList<SemanticRouteResponse>();
    		boolean at_least_one_ok = false;
    		// we get all the possible routes for the different places 
    		// (we will be able then to choose between the best toilet or atm to go)
    		for (Term t: to_list) {
    			// call the service to compute route
    			m_rosnode.call_get_route_srv(from, 
    									t.toString(),
    									action.getActionTerm().getTerm(2).toString(), 
    									Boolean.parseBoolean(action.getActionTerm().getTerm(3).toString()));

    			SemanticRouteResponseImpl resp = new SemanticRouteResponseImpl();
    			// we wait the result return from the service
                do {
                	 resp = m_rosnode.get_get_route_resp();
                	if (resp != null) {
                		routes.add(resp);
                	}
                	try {
    					Thread.sleep(100);
    				} catch (InterruptedException e) {
    					e.printStackTrace();
    				}
                }while(resp == null);
                if(resp.getCode() == Code.ERROR.getCode()) {
                	action.setResult(false);
                	action.setFailureReason(new Atom("route_not_found"), "No route has been found");
                	
                }else {
                	at_least_one_ok = true;
                }
    		}        
    		if(!at_least_one_ok) {
    			actionExecuted(action);
    		}else {
	            RouteImpl route = select_best_route(routes);
	            
	            try {
	            	getTS().getAg().addBel(Literal.parseLiteral("route("+route.getRoute()+")"));
	            	getTS().getAg().addBel(Literal.parseLiteral("target_place("+route.getGoal()+")"));
	            	action.setResult(true);
	            	actionExecuted(action);
	            	
				} catch (RevisionFailedException e) {
					e.printStackTrace();
				}
    		}

    	} else if(action_name.equals("get_individual_type")) {
    		m_rosnode.call_onto_indivual_srv("getType", action.getActionTerm().getTerm(0).toString());
    		OntologeniusServiceResponse places;
    		do {
    			places = m_rosnode.get_onto_individual_resp();
            	try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            }while(places == null);
    		try {
				getTS().getAg().addBel(Literal.parseLiteral("possible_places("+places.getValues()+")"));
				action.setResult(true);
	        	actionExecuted(action);
			} catch (RevisionFailedException e) {
				e.printStackTrace();
			}
    	} else if(action_name.equals("get_onto_name")) {
    		// to remove the extra ""
    		String param = action.getActionTerm().getTerm(0).toString();
    		param = param.replaceAll("^\"|\"$", "");
    		
			m_rosnode.call_onto_indivual_srv("find", param);
			OntologeniusServiceResponse place;
			do {
				place = m_rosnode.get_onto_individual_resp();
	        	try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	        }while(place == null);
			if(place.getCode() == Code.OK.getCode()) {
				try {
					getTS().getAg().addBel(Literal.parseLiteral("onto_place("+place.getValues().get(0)+")"));
					action.setResult(true);
		        	actionExecuted(action);
				} catch (RevisionFailedException e) {
					e.printStackTrace();
				}
			}else {
				action.setResult(false);
				action.setFailureReason(new Atom("name_not_found"), "No place matching "+param+" has been found in the ontology");
	        	actionExecuted(action);
			} 
	} else if(action_name.equals("get_human_abilities")) { //TODO to proper implement
		try {
			getTS().getAg().addBel(Literal.parseLiteral("persona_asked(old)"));
		} catch (RevisionFailedException e) {
			e.printStackTrace();
		}
		action.setResult(true);
    	actionExecuted(action);
	} else if(action_name.equals("get_placements")) {
		// to remove the extra ""
		ArrayList<String> params = new ArrayList<String>();
		for(Term term : action.getActionTerm().getTerms()) {
			params.add(term.toString().replaceAll("^\"|\"$", ""));
		}
		
		String target = params.get(0);
		String direction = params.get(1);
		String human = params.get(2);
		m_rosnode.call_svp_planner(target, direction, human);
		PointingActionResult placements_result;
		do {
			placements_result = m_rosnode.get_get_placements_result();
		}while(placements_result == null);
		if(placements_result.getStatus().getStatus() == GoalStatus.SUCCEEDED) {
			try {
				Pose robot_pose = placements_result.getResult().getRobotPose().getPose();
				Pose human_pose = placements_result.getResult().getHumanPose().getPose();
				getTS().getAg().addBel(Literal.parseLiteral("robot_pos("+robot_pose.getPosition().getX()+","
																		+robot_pose.getPosition().getY()+","
																		+robot_pose.getPosition().getZ()+")"));
				getTS().getAg().addBel(Literal.parseLiteral("human_pos("+human_pose.getPosition().getX()+","
																		+human_pose.getPosition().getY()+","
																		+human_pose.getPosition().getZ()+")"));
				int nb_ld_to_point = placements_result.getResult().getPointedLandmarks().size();
				if(nb_ld_to_point == 0) {
					getTS().getAg().addBel(Literal.parseLiteral("ld_to_point(None)"));
				} else if(nb_ld_to_point==1) {
					getTS().getAg().addBel(Literal.parseLiteral("ld_to_point("+placements_result.getResult().getPointedLandmarks().get(0)+")"));
				} else if(nb_ld_to_point==2){
					getTS().getAg().addBel(Literal.parseLiteral("ld_to_point("+placements_result.getResult().getPointedLandmarks().get(0)+","
																		  	  +placements_result.getResult().getPointedLandmarks().get(1)+")"));
				}
			} catch (RevisionFailedException e) {
				e.printStackTrace();
			}
			action.setResult(true);
        	actionExecuted(action);
		}else {
			action.setResult(false);
			action.setFailureReason(new Atom("svp_failure"), "SVP planner goal status :"+placements_result.getStatus().getStatus());
			actionExecuted(action);
		}
	}else if(action_name.equals("has_mesh")) {
		// to remove the extra ""
		String param = action.getActionTerm().getTerm(0).toString();
		param = param.replaceAll("^\"|\"$", "");
		
		m_rosnode.call_has_mesh_srv("base", param);
		HasMeshResponse has_mesh;
		do {
			has_mesh = m_rosnode.getHas_mesh_resp();
        	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }while(has_mesh == null);
		if(has_mesh.getHasMesh()) {
			action.setResult(true);
		}else {
			action.setResult(false);
			if(!has_mesh.getHasMesh()) {
				action.setFailureReason(new Atom("has_no_mesh"), param+" does not have a mesh");
			}else {
				action.setFailureReason(new Atom("srv_has_mesh_failed"), "has_mesh service failed");
			}
		}
		actionExecuted(action);
	} else if(action_name.equals("can_be_visible")) {
		// to remove the extra ""
		String human = action.getActionTerm().getTerm(0).toString();
		human = human.replaceAll("^\"|\"$", "");
		String place = action.getActionTerm().getTerm(1).toString();
		place = place.replaceAll("^\"|\"$", "");
		m_rosnode.call_visibility_score_srv(human, place);
		VisibilityScoreResponse vis_resp;
		do {
			vis_resp = m_rosnode.getVisibility_score_resp();
        	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }while(vis_resp == null);
		if(vis_resp.getIsVisible()) {
			action.setResult(true);
			try {
				getTS().getAg().addBel(Literal.parseLiteral("canBeVisibleFor("+place+","+human+")"));
			} catch (RevisionFailedException e) {
				e.printStackTrace();
			}
		}else {
			action.setResult(false);
			action.setFailureReason(new Atom("not_visible"), place+" is not visible");
		}
		actionExecuted(action);
	} else if(action_name.equals("point_at")) {
		// to remove the extra ""
		String place = action.getActionTerm().getTerm(0).toString();
		place = place.replaceAll("^\"|\"$", "");
		m_rosnode.call_point_at_srv(place);
		PointAtResponse point_at_resp;
		do {
			point_at_resp = m_rosnode.getPoint_at_resp();
        	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }while(point_at_resp == null);
		if(point_at_resp.getSuccess()) {
			action.setResult(true);
		}else {
			action.setResult(false);
			action.setFailureReason(new Atom("point_at_failed"), "the pointing failed for "+place);
		}
		actionExecuted(action);
	} else if(action_name.equals("text2speech")) {
		// to remove the extra ""
		String human = action.getActionTerm().getTerm(0).toString();
		human = human.replaceAll("^\"|\"$", "");
		Literal bel = (Literal) action.getActionTerm().getTerm(1);
		String text;
		String bel_functor = bel.getFunctor();
		switch(bel_functor) {
		case "should_look_place": text = new String("Look "+bel.getTerm(0)+" over there"); break;
		case "should_look_orientation": text = new String("You should look a bit more at the "+bel.getTerm(0)); break;
		case "able_to_see": text = new String("I note that you must be looking at the place right now"); break;
		case "route_verbalization" : text = bel.getTerm(0).toString().replaceAll("^\"|\"$", ""); break;
		default : action.setResult(false); action.setFailureReason(new Atom("empty_string"), "no speech to say"); actionExecuted(action); return;
		}
		m_rosnode.call_speak_to_srv(human, text);
		SpeakToResponse speak_to_resp;
		do {
			speak_to_resp = m_rosnode.getSpeak_to_resp();
        	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }while(speak_to_resp == null);
		if(speak_to_resp.getSuccess()) {
			action.setResult(true);
		}else {
			action.setResult(false);
			action.setFailureReason(new Atom("speak_to_failed"), "the speech service failed");
		}
		actionExecuted(action);
	} else if(action_name.equals("get_route_verbalization")) {
		// to remove the extra ""
		@SuppressWarnings("unchecked")
		List<Term> route_temp = (List<Term>) action.getActionTerm().getTerm(0);
		List<String> route = new ArrayList<>();
		for(Term t : route_temp) {
			route.add(t.toString());
		}
		String robot_place = action.getActionTerm().getTerm(1).toString();
		robot_place = robot_place.replaceAll("^\"|\"$", "");
		String place = action.getActionTerm().getTerm(2).toString();
		place = place.replaceAll("^\"|\"$", "");
		m_rosnode.call_route_verbalization_srv(route, robot_place, place);
		VerbalizeRegionRouteResponse verba_resp;
		do {
			verba_resp = m_rosnode.getVerbalization_resp();
        	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }while(verba_resp == null);
		String verba = new String(verba_resp.getRegionRoute());
		if(verba_resp.getSuccess() & verba != "") {
			action.setResult(true);
			try {
				getTS().getAg().addBel(Literal.parseLiteral("verbalization(\""+verba+"\")"));
			} catch (RevisionFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			action.setResult(false);
			action.setFailureReason(new Atom("route_verba_failed"), "the route verbalization service failed");
		}
		actionExecuted(action);
	} 
	else {
			super.act(action);
		}
    }
    
    
    public RouteImpl select_best_route(List<SemanticRouteResponse> routes_resp_list) {
    	RouteImpl best_route = new RouteImpl();
    	float min_cost = Float.MAX_VALUE;
    	if (routes_resp_list.size() > 0) {
    		for (SemanticRouteResponse route_resp : routes_resp_list) {
    			List<Route> routes = route_resp.getRoutes();
    			float[] costs = route_resp.getCosts();
    			List<String> goals = route_resp.getGoals();
    			
    			for (int i = 0; i < routes.size(); i++) {
    				if (costs[i] < min_cost) {
    					best_route.setRoute(routes.get(i).getRoute());
    					best_route.setGoal(goals.get(i));
    					min_cost = costs[i];
    				}
    			}
    		}
    	}
    	
    	return best_route;
    	
    }





	@Override
	public void actionExecuted(ActionExec act) {
		String action_name = act.getActionTerm().getFunctor();
		Message msg;
		if(act.getResult()) {
    		msg = new Message("tell", getAgName(), "supervisor", "action_over("+action_name+")");
    	}else {
//    		msg = new Message("tell", getAgName(), "supervisor", "action_failed("+action_name+","+new StringTermImpl(act.getFailureReason().toString())+")");
    		msg = new Message("tell", getAgName(), "supervisor", "action_failed("+action_name+","+act.getFailureReason().toString()+")");
    	}
    	try {
			sendMsg(msg);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		super.actionExecuted(act);
	}
    
    
    
};


