import hlt.*;
import lemon.halite2.util.MoveQueue;
import lemon.halite2.util.Pathfinder;
import lemon.halite2.util.ShipPriorities;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyBot {
	public static final SimpleDateFormat READABLE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	public static final SimpleDateFormat FILENAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");
	public static void main(String[] args) {
		try {
			Date currentDate = new Date();
			GameMap gameMap = Networking.initialize();
			DebugLog.initialize(String.format("logs/%s-%d.log", FILENAME_DATE_FORMAT.format(currentDate), gameMap.getMyPlayerId()));
			DebugLog.log("Initialization - "+READABLE_DATE_FORMAT.format(currentDate));
			Pathfinder.setGameMap(gameMap);
			ShipPriorities shipPriorities = new ShipPriorities(gameMap);
			MoveQueue moveQueue = new MoveQueue(gameMap);
			List<Integer> handledShips = new ArrayList<Integer>();
			
			//Rate Planets
			
			Networking.finalizeInitialization("Lemon");
			
			while (true) {
				DebugLog.log("New Turn: "+gameMap.getTurnNumber());
				gameMap.updateMap(Networking.readLineIntoMetadata());
				shipPriorities.update();
				handledShips.clear();
				for(ShipPriorities.Priority priority: ShipPriorities.Priority.values()) {
					for(int shipId: shipPriorities.getPrioritiesMap().get(priority)) {
						ArrayDeque<Integer> handleStack = new ArrayDeque<Integer>();
						handleStack.push(shipId);
						handledShips.add(shipId);
						while(!handleStack.isEmpty()){
							int request = handleShip(handledShips, handleStack.peek(), moveQueue);
							if(request==-1){
								handleStack.pop();
							}else{
								if(handledShips.contains(request)){
									throw new IllegalStateException(String.format("Already Handled Ship: %d", request));
								}
								handleStack.push(request);
								handledShips.add(shipId);
							}
						}
					}
				}
				moveQueue.flush();
			}
		}catch(Exception ex) {
			DebugLog.log(ex);
		}
	}
	public static int handleShip(List<Integer> handledShips, int shipId, MoveQueue moveQueue){
		
		return -1;
	}
	public static Planet getClosestPlanet(GameMap gameMap, Position position) {
		Planet closestPlanet = null;
		double closestDistance = Double.MAX_VALUE;
		for(Planet planet: gameMap.getPlanets()) {
			double distance = position.getDistanceSquared(planet.getPosition());
			if(closestDistance>distance) {
				closestDistance = distance;
				closestPlanet = planet;
			}
		}
		return closestPlanet;
	}
}
