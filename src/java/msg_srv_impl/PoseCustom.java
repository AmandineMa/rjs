package msg_srv_impl;

import java.util.List;


import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;

import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.Quaternion;

public class PoseCustom {
	
	Point position;
	Quaternion orientation;
	Pose pose;
	
	public PoseCustom() {}
	
	public PoseCustom(Pose pose) {
		setPose(pose);
	}
	
	public PoseCustom(List<Double> pose_values) {
		if(pose_values.size()!=7) {
			throw new IllegalArgumentException("Input should have 7 elements");
		}
		NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
		MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();
		Point position = messageFactory.newFromType(Point._TYPE);
		position.setX(pose_values.get(0));
		position.setY(pose_values.get(1));
		position.setZ(pose_values.get(2));
		this.position = position;
		Quaternion orientation = messageFactory.newFromType(Quaternion._TYPE);
		orientation.setX(pose_values.get(3));
		orientation.setY(pose_values.get(4));
		orientation.setZ(pose_values.get(5));
		orientation.setW(pose_values.get(6));
		this.orientation = orientation;
		pose = messageFactory.newFromType(Pose._TYPE);
		pose.setOrientation(orientation);
		pose.setPosition(position);
	}
	
	
	
	public Point getPosition() {
		return position;
	}
	public void setPosition(Point position) {
		this.pose.setPosition(position);
		this.position = position;
	}
	public Quaternion getOrientation() {
		return orientation;
	}
	public void setOrientation(Quaternion orientation) {
		this.pose.setOrientation(orientation);
		this.orientation = orientation;
	}
	
	public Pose getPose() {
		return pose;
	}

	public void setPose(Pose pose) {
		this.orientation = pose.getOrientation();
		this.position = pose.getPosition();
		this.pose = pose;
	}

	@Override
	public String toString() {
		return "["+position.getX()+","
				+position.getY()+","
				+position.getZ()+"],["
				+orientation.getX()+","
				+orientation.getY()+","
				+orientation.getZ()+","
				+orientation.getW()+"]";
	}
		

}
