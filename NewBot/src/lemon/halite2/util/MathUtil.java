package lemon.halite2.util;

import hlt.Position;

public class MathUtil {
	public static final double TAU = 2*Math.PI;
	public static final int PI_DEGREES = 180;
	public static final int TAU_DEGREES = 360;
	public static final double ONE_DEGREE = Math.toRadians(1);
	public static int normalizeDegrees(int degrees) {
		return (degrees%TAU_DEGREES+TAU_DEGREES)%TAU_DEGREES;
	}
	public static double normalizeDegrees(double degrees) {
		return (degrees%TAU_DEGREES+TAU_DEGREES)%TAU_DEGREES;
	}
	public static double normalizeRadians(double radians) {
		return (radians%TAU+TAU)%TAU;
	}
	public static double angleBetweenRadians(double a, double b){
		double between = normalizeRadians(b-a);
		if(between>Math.PI){
			between = TAU-between;
		}
		return between;
	}
	public static double angleBetweenDegrees(double a, double b) {
		double between = normalizeDegrees(b-a);
		if(between>PI_DEGREES) {
			between = TAU_DEGREES-between;
		}
		return between;
	}
	//https://gamedev.stackexchange.com/questions/97337/detect-if-two-objects-are-going-to-collide
	public static double getMinTime(Position a, Position b, Position velocityA, Position velocityB) {
		double deltaVelocityX = velocityA.getX()-velocityB.getX();
		double deltaVelocityY = velocityA.getY()-velocityB.getY();
		return -(deltaVelocityX*(a.getX()-b.getX())+deltaVelocityY*(a.getY()-b.getY()))/
				(deltaVelocityX*deltaVelocityX+deltaVelocityY*deltaVelocityY);
	}
	public static double getDistanceSquared(Position a, Position b, Position velocityA, Position velocityB, double time) {
		double deltaX = (a.getX()+velocityA.getX()*time)-(b.getX()+velocityB.getX()*time);
		double deltaY = (a.getY()+velocityA.getY()*time)-(b.getY()+velocityB.getY()*time);
		return deltaX*deltaX+deltaY*deltaY;
	}
}
