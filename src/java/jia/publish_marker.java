package jia;

import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;
import org.ros.node.topic.Publisher;
import arch.ROSAgArch;
import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.Vector3;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;
import std_msgs.ColorRGBA;
import std_msgs.Header;
import visualization_msgs.Marker;

public class publish_marker extends DefaultInternalAction {

	  @Override
	    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
		  if(args.length != 1) {
		  String frame = args[0].toString();
		  frame = frame.replaceAll("^\"|\"$", "");
		  NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
		  MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();
		  Pose pose = messageFactory.newFromType(Pose._TYPE);
		  ListTermImpl point_term =  ((ListTermImpl) args[1]);
		  Point point = messageFactory.newFromType(Point._TYPE);
		  point.setX(((NumberTermImpl)point_term.get(0)).solve());
		  point.setY(((NumberTermImpl)point_term.get(1)).solve());
		  point.setZ(((NumberTermImpl)point_term.get(2)).solve());
		  pose.setPosition(point);
		  Publisher<visualization_msgs.Marker> pub = ROSAgArch.getM_rosnode().getMarker_pub();
		  Marker marker = pub.newMessage();
		  marker.setPose(pose);
		  String color = args[2].toString();
		  ColorRGBA rgba = null;
		  if(color.equals("blue")) {
			  rgba = messageFactory.newFromType(ColorRGBA._TYPE);
			  rgba.setR(0);
			  rgba.setG((float) 0.6);
			  rgba.setB((float) 0.9);
			  rgba.setA((float) 1.0);
		  }
		  if(color.equals("yellow")) {
			  rgba = messageFactory.newFromType(ColorRGBA._TYPE);
			  rgba.setR(1);
			  rgba.setG((float) 1);
			  rgba.setB((float) 0.1);
			  rgba.setA((float) 1.0);
		  }
		  marker.setColor(rgba);
		  marker.setNs("supervisor");
		  marker.setType(Marker.SPHERE);
		  marker.setAction(Marker.ADD);
		  marker.setId(0);
		  Vector3 vec = messageFactory.newFromType(Vector3._TYPE);
		  vec.setX(0.3);
		  vec.setY(0.3);
		  vec.setZ(0.3);
		  marker.setScale(vec);
		  Header header = messageFactory.newFromType(Header._TYPE);
		  header.setFrameId(frame);
		  marker.setHeader(header);
		  pub.publish(marker);
		  }else {
			  Publisher<visualization_msgs.Marker> pub = ROSAgArch.getM_rosnode().getMarker_pub();
			  Marker marker = pub.newMessage();
			  marker.setNs("supervisor");
			  marker.setAction(Marker.DELETEALL);
			  pub.publish(marker);
		  }
		  return true;
	    }

}