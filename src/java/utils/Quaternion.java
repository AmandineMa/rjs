package utils;

import org.ros.rosjava_geometry.Vector3;

import jason.asSyntax.ListTerm;
import jason.asSyntax.NumberTermImpl;

public class Quaternion extends org.ros.rosjava_geometry.Quaternion {

	public Quaternion(double x, double y, double z, double w) {
		super(x, y, z, w);
	}
	
	private Quaternion(ListTerm listTerm){
		super(((NumberTermImpl) listTerm.get(0)).solve(),
			  ((NumberTermImpl) listTerm.get(1)).solve(), 
			  ((NumberTermImpl) listTerm.get(2)).solve(),
			  ((NumberTermImpl) listTerm.get(3)).solve());
	}
	
	public static Quaternion create(ListTerm listTerm) throws  IllegalArgumentException{
		if(listTerm.size() != 4) {
			throw new IllegalArgumentException("The listTerm should have four elements to create a quaternion");
		}else {
			return new Quaternion(listTerm);
		}
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
	
	public double getYaw() {
		double yaw;

		double sqw;
		double sqx;
		double sqy;
		double sqz;

		sqx = this.getX() * this.getX();
		sqy = this.getY() * this.getY();
		sqz = this.getZ() * this.getZ();
		sqw = this.getW() * this.getW();

		// Cases derived from https://orbitalstation.wordpress.com/tag/quaternion/
		double sarg = -2 * (this.getX()*this.getZ() - this.getW()*this.getY()) / (sqx + sqy + sqz + sqw); /* normalization added from urdfom_headers */

		if (sarg <= -0.99999) {
			yaw   = -2 * Math.atan2(this.getY(), this.getX());
		} else if (sarg >= 0.99999) {
			yaw   = 2 * Math.atan2(this.getY(), this.getX());
		} else {
			yaw   = Math.atan2(2 * (this.getX()*this.getY() + this.getW()*this.getZ()), sqw + sqx - sqy - sqz);
		}
		return yaw;
	}


}
