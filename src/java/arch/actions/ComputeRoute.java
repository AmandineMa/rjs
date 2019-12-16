package arch.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import arch.ROSAgArch;
import jason.asSemantics.ActionExec;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;
import msg_srv_impl.RouteImpl;
import msg_srv_impl.SemanticRouteResponseImpl;
import semantic_route_description_msgs.Route;
import semantic_route_description_msgs.SemanticRouteResponse;
import utils.Code;

public class ComputeRoute extends AbstractAction {

	public ComputeRoute(ActionExec actionExec, ROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void execute() {
		String from = actionExec.getActionTerm().getTerm(0).toString();
		from = removeQuotes(from);

		ListTerm to_list;
		Term to = (Term) actionExec.getActionTerm().getTerm(1);
		// if there is multiple places (toilet and atm cases)
		if (to.isList()) {
			to_list = (ListTerm) to;
		}
		// if there is only one place, we convert it to a list with one element for
		// convenience
		else {
			to_list = new ListTermImpl();
			to_list.add(to);
		}
		List<SemanticRouteResponse> routes = new ArrayList<SemanticRouteResponse>();
		boolean at_least_one_ok = false;
		// we get all the possible routes for the different places
		// (we will be able then to choose between the best toilet or atm to go)
		for (Term t : to_list) {
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("from", from);
			parameters.put("to", ((StringTermImpl) t).getString());
			parameters.put("persona", actionExec.getActionTerm().getTerm(2).toString());
			parameters.put("signpost", Boolean.parseBoolean(actionExec.getActionTerm().getTerm(3).toString()));
			// call the service to compute route
			SemanticRouteResponse resp = rosnode.callSyncService("get_route", parameters);
			if(resp != null) {
				SemanticRouteResponseImpl get_route_resp = new SemanticRouteResponseImpl(resp.getCosts(),
						resp.getGoals(), resp.getRoutes());
				if (resp.getRoutes().isEmpty()) {
					get_route_resp.setCode(Code.ERROR.getCode());
				} else {
					get_route_resp.setCode(Code.OK.getCode());
				}
				routes.add(get_route_resp);
				if (get_route_resp.getCode() == Code.ERROR.getCode()) {
					actionExec.setResult(false);
					actionExec.setFailureReason(new Atom("route_not_found"), "No route has been found");

				} else {
					at_least_one_ok = true;
				}
			}
		}
		if (at_least_one_ok) {
			RouteImpl route = select_best_route(routes);
			if(route != null) {
				String route_list = route.getRoute().stream().map(s -> "\"" + s + "\"")
						.collect(Collectors.joining(", "));
				rosAgArch.addBelief("route([" + route_list + "])");
				rosAgArch.addBelief("target_place(\"" + route.getGoal() + "\")");
				actionExec.setResult(true);
			}else {
				actionExec.setResult(false);
				actionExec.setFailureReason(new Atom("route_not_found_wo_stairs"), "No route has been found without stairs");
			}
		}

	}
	
	private RouteImpl select_best_route(List<SemanticRouteResponse> routes_resp_list) {
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
			if(best_route.getRoute() == null) {
				best_route = null;
			}
		}

		return best_route;

	}

}
