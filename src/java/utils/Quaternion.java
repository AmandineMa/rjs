package utils;

import org.ros.rosjava_geometry.Vector3;

public class Quaternion extends org.ros.rosjava_geometry.Quaternion {

	public Quaternion(double x, double y, double z, double w) {
		super(x, y, z, w);
	}

	public double getAngle() {
		double angle = 2.0 * Math.acos(this.getW());
		if (angle > Math.PI) {
			angle -= 2.0*Math.PI;
		}
		return angle;
	}

	public Vector3 getAxis() {
		double s = Math.sqrt(1 - this.getW() * this.getW()); // assuming quaternion normalised then w is less or equal than 1, so term always positive.
		if (s < 1e-6) { // test to avoid divide by zero, s is always positive due to sqrt
			// if s close to zero then direction of axis not important, because angle is zero
			return new Vector3(0., 0., 1.);
		}
		return new Vector3(this.getX() / s, this.getY() / s, this.getZ() / s);
	}

	public double getTheta() {
		return getAxis().getZ() * getAngle();
	}


}
