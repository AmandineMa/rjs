package rjs.arch.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hatp_msgs.Plan;
import hatp_msgs.PlanningRequest;
import hatp_msgs.PlanningRequestRequest;
import hatp_msgs.PlanningRequestResponse;
import hatp_msgs.Request;
import jason.asSemantics.ActionExec;
import jason.asSyntax.Atom;
import jason.asSyntax.Term;
import rjs.arch.agarch.AbstractROSAgArch;
import rjs.utils.Tools;

public class GetHATPPlan extends AbstractAction {

	public GetHATPPlan(ActionExec actionExec, AbstractROSAgArch rosAgArch) {
		super(actionExec, rosAgArch);
		setSync(true);
	}

	@Override
	public void execute() {
		String task_name = actionExec.getActionTerm().getTerm(0).toString();
		task_name = Tools.removeQuotes(task_name);
		Request request = rosAgArch.createMessage(Request._TYPE);
		
		if(actionExec.getActionTerm().getArity() > 1) {
			@SuppressWarnings("unchecked")
			List<Term> parameters_temp = (List<Term>) actionExec.getActionTerm().getTerm(1);
			List<String> planParameters = new ArrayList<>();
			for(Term t : parameters_temp) {
				planParameters.add(t.toString());
			}
			request.setParameters(planParameters);
		}
		request.setTask(task_name);
		request.setType("1");
		
		PlanningRequestRequest planreq = (PlanningRequestRequest) getRosNode().newServiceRequestFromType(PlanningRequest._TYPE);
		planreq.setRequest(request);
		PlanningRequestResponse hatpPlannerResp = getRosNode().callSyncService("hatp_planner", planreq);
		
		if(hatpPlannerResp != null) {
			Plan plan = hatpPlannerResp.getSolution();
			if(plan.getReport().equals("OK")) {
				actionExec.setResult(true);
				ArrayList<Integer> taskIDs = new ArrayList<Integer>();
				for(hatp_msgs.Task task : plan.getTasks()) {
					taskIDs.add(task.getId());
					String agents = Tools.arrayToListTerm(task.getAgents()).toString();
					if(task.getType()) {
						rosAgArch.addBelief("action", new ArrayList<Object>(Arrays.asList(task.getId(), "planned", task.getName(), agents, task.getCost())));
						if(!task.getParameters().isEmpty()) {
							// first parameter is agent name
							task.getParameters().remove(0);
							String taskParameters = Tools.arrayToListTerm(task.getParameters()).toString();
							rosAgArch.addBelief("actionParams", new ArrayList<Object>(Arrays.asList(task.getId(),taskParameters)));
						}
					}
				}
				for(hatp_msgs.StreamNode stream : plan.getStreams()) {
					String belief = "link("+stream.getTaskId();
					String pred = Tools.arrayToStringArray(stream.getPredecessors());
					belief = belief+","+pred;
					belief = belief+")";
					rosAgArch.addBelief(belief);
				}
				rosAgArch.addBelief("plan", new ArrayList<Object>(Arrays.asList(plan.getId(),taskIDs)));
			} 
		} else {
			actionExec.setResult(false);
			actionExec.setFailureReason(new Atom("no_plan_found"), "hatp planner could not find any feasible plan");
		}
	}


}
