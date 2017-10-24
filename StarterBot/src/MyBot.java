import hlt.*;

import java.util.ArrayList;
import java.util.List;

public class MyBot {
	public static void main(String[] args) {
		try {
			DebugLog.initialize(String.format("logs/log-%d-%f.log", System.currentTimeMillis(), Math.random()));
			GameMap gameMap = Networking.initialize();
			//DebugLog.initialize(String.format("logs/log-%d-%d.log", gameMap.getMyPlayerId(), System.currentTimeMillis()));
			DebugLog.log("Initialization");
			
			Networking.finalizeInitialization("Lemon");
			
			
			List<Move> moveList = new ArrayList<Move>();
			while (true) {
				DebugLog.log("New Turn: "+gameMap.getTurnNumber());
				moveList.clear();
				gameMap.updateMap(Networking.readLineIntoMetadata());
				
				for (Ship ship : gameMap.getMyPlayer().getShips().values()) {
					if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
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
					}
				}
				Networking.sendMoves(moveList);
			}
		}catch(Exception ex) {
			DebugLog.log(ex);
		}
	}
}
