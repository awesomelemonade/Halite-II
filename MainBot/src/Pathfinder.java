import hlt.DebugLog;
import hlt.GameMap;
import hlt.Position;
import hlt.Ship;
import hlt.ThrustMove;

public class Pathfinder {
	private static GameMap gameMap;

	public static void setGameMap(GameMap gameMap) {
		Pathfinder.gameMap = gameMap;
	}
	
	public static ThrustMove patrol(Ship ship, Position position, Position center, double buffer) {
		double direction = calculateTangent(position, center, buffer);
		return new ThrustMove(ship, 3, direction);
	}
	public static double calculateTangent(Position position, Position center, double radius) {
		/*                                         
                       -.        /--------- Radius
                      `y        /                 
                      y. -///:///-`               
                     +oo+-    /  -+o:             
                    -N+      /      :y`           
                   `m+`    <-        `h`          
                   y+so///-           -o          
                  /m+:    ./s/ <-------s---- Center          
                 .y:+      +/         :+          
                 y` y-   :o`         .h`          
                o-   oo.o:          +s`           
               :o     oyo/.     `:oo-             
              `y    .o-  `://://:.                
              y.   +/                             
             +:  -o`  <--------- Distance     
            -y++o: <----------- Angle you're solving for
           `y /o`                                 
           s:o-                                   
          /d+                                     
          s.         
		 */
		
		double distance = position.getDistanceTo(center);
		DebugLog.log(distance+" - "+radius);
		if(distance<radius) { //No Tangent, you're in the circle
			return center.getDirectionTowards(position)-(Math.PI/2)*(distance/radius); //Return direction away from center
																					//scales based off distance
		}
		return position.getDirectionTowards(center)+Math.asin(radius/distance);
	}
	public static ThrustMove pathfind(Position start, Position end, double startBuffer, double endBuffer) {
		return pathfind(start, end.addPolar(endBuffer, end.getDirectionTowards(start)), startBuffer);
	}
	public static ThrustMove pathfind(Position start, Position end, double buffer) {
		return null;
	}
	//Segment - Circle
	public static double segmentCircleIntersect(Position start, Position end, Position center, double radius) {
		// Parameterize the segment as start + t * (end-start),
		// and substitute into the equation of a circle
		// Solve for t
		double dx = end.getX() - start.getX();
		double dy = end.getY() - start.getY();

		double a = dx * dx + dy * dy; // Distance squared between start and end

		double b = -2 * (start.getX() * start.getX() - start.getX() * end.getX() - start.getX() * center.getX()
				+ end.getX() * center.getX() + start.getY() * start.getY() - start.getY() * end.getY()
				- start.getY() * center.getY() + end.getY() * center.getY());
		
		// y=ax^2+bx+c
		
		return -b / (2*a); // Time along segment when closest to the circle (vertex of the quadratic
	}
	//Arc - Circle
	//Segment - Point Distance
	public static double test(Position start, Position end, Position point){
		//Standard form of the line ax+by+c = 0
		double a = start.getY()-end.getY();
		double b = end.getX()-start.getX();
		double c = (start.getX()-end.getX())*start.getY()+(end.getY()-start.getY())*start.getX();
		//Calculate closest to point on the line above
		double x = (b*(b*point.getX()-a*point.getY())-a*c)/(a*a+b*b);
		double y = (a*(-b*point.getX()+a*point.getY())-b*c)/(a*a+b*b);
		//Check if (x, y) is between start and end
		if(((start.getX()<=x&&x<=end.getX())||(start.getX()>=x&&x>=end.getX()))&&
				((start.getY()<=y&&y<=end.getY())||(start.getY()>=y&&y>=end.getY()))){
			return Math.sqrt((point.getX()-x)*(point.getX()-x)+(point.getY()-y)*(point.getY()-y));
		}else{
			double i = point.getDistanceSquared(start);
			double j = point.getDistanceSquared(end);
			return i<j?Math.sqrt(i):Math.sqrt(j);
		}
	}
}
