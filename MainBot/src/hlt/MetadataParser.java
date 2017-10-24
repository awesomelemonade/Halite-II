package hlt;

import java.util.List;

public class MetadataParser {
	public static void populateShipList(List<Ship> shipsOutput, int owner, Metadata shipsMetadata) {
		long numberOfShips = Long.parseLong(shipsMetadata.pop());
		for (int i = 0; i < numberOfShips; ++i) {
			shipsOutput.add(newShipFromMetadata(owner, shipsMetadata));
		}
	}
	private static Ship newShipFromMetadata(int owner, Metadata metadata) {
		int id = Integer.parseInt(metadata.pop());
		double xPos = Double.parseDouble(metadata.pop());
		double yPos = Double.parseDouble(metadata.pop());
		int health = Integer.parseInt(metadata.pop());
		// Ignoring velocity(x,y) which is always (0,0) in current version.
		metadata.pop();
		metadata.pop();
		Ship.DockingStatus dockingStatus = Ship.DockingStatus.values()[Integer.parseInt(metadata.pop())];
		int dockedPlanet = Integer.parseInt(metadata.pop());
		int dockingProgress = Integer.parseInt(metadata.pop());
		int weaponCooldown = Integer.parseInt(metadata.pop());
		return new Ship(owner, id, new Position(xPos, yPos), health, dockingStatus, dockedPlanet, dockingProgress,
				weaponCooldown);
	}
	public static Planet newPlanetFromMetadata(List<Integer> dockedShips, Metadata metadata) {
		int id = Integer.parseInt(metadata.pop());
		double xPos = Double.parseDouble(metadata.pop());
		double yPos = Double.parseDouble(metadata.pop());
		int health = Integer.parseInt(metadata.pop());
		double radius = Double.parseDouble(metadata.pop());
		int dockingSpots = Integer.parseInt(metadata.pop());
		int currentProduction = Integer.parseInt(metadata.pop());
		int remainingProduction = Integer.parseInt(metadata.pop());
		int hasOwner = Integer.parseInt(metadata.pop());
		int ownerCandidate = Integer.parseInt(metadata.pop());
		int owner;
		if (hasOwner == 1) {
			owner = ownerCandidate;
		} else {
			owner = -1; // ignore ownerCandidate
		}
		int dockedShipCount = Integer.parseInt(metadata.pop());
		for (int i = 0; i < dockedShipCount; ++i) {
			dockedShips.add(Integer.parseInt(metadata.pop()));
		}
		return new Planet(owner, id, new Position(xPos, yPos), health, radius, dockingSpots, currentProduction,
				remainingProduction, dockedShips);
	}
	public static int parsePlayerNum(Metadata metadata) {
		return Integer.parseInt(metadata.pop());
	}
	public static int parsePlayerId(Metadata metadata) {
		return Integer.parseInt(metadata.pop());
	}
	public static int parsePlanetNum(Metadata metadata) {
		return Integer.parseInt(metadata.pop());
	}
}
