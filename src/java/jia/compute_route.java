// Internal action code for project supervisor

package jia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import arch.ROSAgArch;
import arch.RobotAgArch;

//import java.util.logging.Logger;

import jason.asSemantics.*;
import jason.asSyntax.*;
import msg_srv_impl.RouteImpl;
import msg_srv_impl.SemanticRouteResponseImpl;
import ontologenius_msgs.OntologeniusServiceResponse;
import semantic_route_description_msgs.Route;
import semantic_route_description_msgs.SemanticRouteResponse;
import utils.Code;

public class compute_route extends DefaultInternalAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    	String from = args[0].toString();
		from = from.replaceAll("^\"|\"$", "");
		
		ListTerm to_list;
		Term to = args[1];
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
		boolean result = false;
		// we get all the possible routes for the different places 
		// (we will be able then to choose between the best toilet or atm to go)
		for (Term t: to_list) {		
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("from", from);
			parameters.put("to", ((StringTermImpl)t).getString());
			parameters.put("persona", args[2].toString());
			parameters.put("signpost", Boolean.parseBoolean(args[3].toString()));
			// call the service to compute route
			SemanticRouteResponse resp = ROSAgArch.getM_rosnode().callSyncService("get_route", parameters);
			if(resp!= null) {
				SemanticRouteResponseImpl get_route_resp = new SemanticRouteResponseImpl(resp.getCosts(), resp.getGoals(), resp.getRoutes());
				if(resp.getRoutes().isEmpty()) {
					get_route_resp.setCode(Code.ERROR.getCode());
				}else {
					get_route_resp.setCode(Code.OK.getCode());
				}
				
				routes.add(get_route_resp);
				if(get_route_resp.getCode() != Code.ERROR.getCode()) {
					at_least_one_ok = true;
				}
			}
		}        
		if(at_least_one_ok){
			int n_routes = Integer.parseInt(args[4].toString());
			
			if(n_routes == 1) {
				RouteImpl route = select_best_route(routes);
				ListTerm route_list = new ListTermImpl();
				String s_route_list = route.getRoute().stream()
						  .map(s -> "\"" + s + "\"")
						  .collect(Collectors.joining(", "));
				route_list = ListTermImpl.parseList("["+s_route_list+"]");
				LiteralImpl l = new LiteralImpl("route");
				l.addTerm(new StringTermImpl(route.getGoal()));
				l.addTerm(route_list);
				result = un.unifies(args[5], l);
			}else if(n_routes == 2) {
				RouteImpl[] best_routes;
				best_routes = select_2_best_routes(routes);
				ListTermImpl list = new ListTermImpl();
				// route 0
				ListTerm route_list = new ListTermImpl();
				String s_route_list = best_routes[0].getRoute().stream()
						  .map(s -> "\"" + s + "\"")
						  .collect(Collectors.joining(", "));
				route_list = ListTermImpl.parseList("["+s_route_list+"]");
				LiteralImpl l = new LiteralImpl("route");
				l.addTerm(new StringTermImpl(best_routes[0].getGoal()));
				l.addTerm(route_list);
				list.add(l);
				// route 1
				route_list = new ListTermImpl();
				s_route_list = best_routes[1].getRoute().stream()
						  .map(s -> "\"" + s + "\"")
						  .collect(Collectors.joining(", "));
				route_list = ListTermImpl.parseList("["+s_route_list+"]");
				l = new LiteralImpl("route");
				l.addTerm(new StringTermImpl(best_routes[1].getGoal()));
				l.addTerm(route_list);
				list.add(l);
				result = un.unifies(args[5], list);
			}
			
		}
		return result;	
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
	
	public RouteImpl[] select_2_best_routes(List<SemanticRouteResponse> routes_resp_list) {
		RouteImpl[] best_routes = new RouteImpl[2];
		best_routes[0] = new RouteImpl();
		best_routes[1] = new RouteImpl();
		float min_cost1 = Float.MAX_VALUE;
		float min_cost2 = Float.MAX_VALUE;
		int list_size = routes_resp_list.size();
		if (list_size > 2) {
			for (SemanticRouteResponse route_resp : routes_resp_list) {
				List<Route> routes = route_resp.getRoutes();
				float[] costs = route_resp.getCosts();
				List<String> goals = route_resp.getGoals();

				for (int i = 0; i < routes.size(); i++) {
					if (costs[i] < min_cost1) {
						min_cost2 = min_cost1;
						best_routes[0].setRoute(routes.get(i).getRoute());
						best_routes[0].setGoal(goals.get(i));
						min_cost1 = costs[i];
					}else if (costs[i] < min_cost2) {
						min_cost2 = costs[i];
						best_routes[1].setRoute(routes.get(i).getRoute());
						best_routes[1].setGoal(goals.get(i));
					}
				}
			}
		}else {
			return null;
		}

		return best_routes;

	}


}
