import hlt.*;

import java.util.ArrayList;
import java.util.List;

public class MyBot {
	public static void main(String[] args) {
		Networking networking = new Networking();
		GameMap gameMap = networking.initialize("Lemon");

		List<Move> moveList = new ArrayList<Move>();
		while (true) {
			moveList.clear();
			gameMap.updateMap(Networking.readLineIntoMetadata());

			for (Ship ship : gameMap.getMyPlayer().getShips().values()) {
				if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
					continue;
				}
				for (Planet planet : gameMap.getAllPlanets().values()) {
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
	}
}
