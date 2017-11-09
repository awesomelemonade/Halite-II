package lemon.halite2.util;

import hlt.Position;

public class MathUtil {
	public static double angleBetween(double a, double b){
		double between = (b-a)%(2*Math.PI);
		if(between>Math.PI){
			between-=2*Math.PI;
		}
		if(between<-Math.PI){
			between+=2*Math.PI;
		}
		return Math.abs(between);
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
