import hlt.*;

import java.text.SimpleDateFormat;
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
			
			Networking.finalizeInitialization("Lemon");
			
			
			List<Move> moveList = new ArrayList<Move>();
			while (true) {
				DebugLog.log("New Turn: "+gameMap.getTurnNumber());
				moveList.clear();
				gameMap.updateMap(Networking.readLineIntoMetadata());
				
				for (Ship ship : gameMap.getMyPlayer().getShips()) {
					/*if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
						continue;
					}
					for (Planet planet : gameMap.getPlanets()) {
						if (planet.isOwned()) {
							continue;
						}
						if (ship.canDock(planet)) {
							moveList.add(new DockMove(ship, planet));
							break;
						}
						ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet,
								Constants.MAX_SPEED / 2);
						if (newThrustMove != null) {
							moveList.add(newThrustMove);
						}
						break;
					}*/
					Planet planet = getClosestPlanet(gameMap, ship.getPosition());
					moveList.add(Pathfinder.patrol(ship, ship.getPosition(), planet.getPosition(),
							planet.getRadius()+ship.getRadius()*2+Constants.BUFFER_CONSTANT));
				}
				Networking.sendMoves(moveList);
			}
		}catch(Exception ex) {
			DebugLog.log(ex);
		}
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
