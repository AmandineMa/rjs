package supervisor;

import java.util.ArrayList;
import java.util.List;

import org.ros.internal.message.RawMessage;

import geometry_msgs.PoseStamped;
import pointing_planner_msgs.PointingPlannerResponse;

public class PointingPlannerResponseImpl implements PointingPlannerResponse {
	
	private List<String> pointed_landmarks;
	private PoseStamped robot_pose;
	private PoseStamped human_pose;

	public PointingPlannerResponseImpl() {
	}
	

	public PointingPlannerResponseImpl(List<String> pointed_landmarks, PoseStamped robot_pose, PoseStamped human_pose) {
		this.pointed_landmarks = pointed_landmarks;
		this.robot_pose = robot_pose;
		this.human_pose = human_pose;
	}


	public RawMessage toRawMessage() {
		return null;
	}

	public PoseStamped getHumanPose() {
		return human_pose;
	}

	public List<String> getPointedLandmarks() {
		return pointed_landmarks;
	}

	public PoseStamped getRobotPose() {
		return robot_pose;
	}

	public void setHumanPose(PoseStamped arg0) {
		human_pose = arg0;
	}

	public void setPointedLandmarks(List<String> arg0) {
		pointed_landmarks = new ArrayList<String>(arg0);
	}

	public void setRobotPose(PoseStamped arg0) {
		robot_pose = arg0;
	}

}
