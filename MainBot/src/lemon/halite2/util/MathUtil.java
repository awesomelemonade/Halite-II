package lemon.halite2.util;

public class MathUtil {
	public static double angleBetween(double a, double b){
		double between = (b-a)%(2*Math.PI);
		if(between>Math.PI){
			between-=2*Math.PI;
		}
		if(between<Math.PI){
			between+=2*Math.PI;
		}
		return Math.abs(between);
	}
}
