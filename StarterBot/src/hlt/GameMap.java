package hlt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Collection;

public class GameMap {
	private int width, height;
	private int playerId;
	private List<Player> players;
	private List<Player> playersUnmodifiable;
	private Map<Integer, Planet> planets;
	private List<Ship> allShips;
	private List<Ship> allShipsUnmodifiable;
	// used only during parsing to reduce memory allocations
	private List<Ship> currentShips = new ArrayList<>();

	public GameMap(int width, int height, int playerId) {
		this.width = width;
		this.height = height;
		this.playerId = playerId;
		players = new ArrayList<>(Constants.MAX_PLAYERS);
		playersUnmodifiable = Collections.unmodifiableList(players);
		planets = new TreeMap<>();
		allShips = new ArrayList<>();
		allShipsUnmodifiable = Collections.unmodifiableList(allShips);
	}
	public int getHeight() {
		return height;
	}
	public int getWidth() {
		return width;
	}
	public int getMyPlayerId() {
		return playerId;
	}
	public List<Player> getAllPlayers() {
		return playersUnmodifiable;
	}
	public Player getMyPlayer() {
		return getAllPlayers().get(getMyPlayerId());
	}
	public Ship getShip(int playerId, int entityId) throws IndexOutOfBoundsException {
		return players.get(playerId).getShip(entityId);
	}
	public Planet getPlanet(int entityId) {
		return planets.get(entityId);
	}
	public Map<Integer, Planet> getAllPlanets() {
		return planets;
	}
	public List<Ship> getAllShips() {
		return allShipsUnmodifiable;
	}
	public ArrayList<Entity> objectsBetween(Position start, Position target) {
		ArrayList<Entity> entitiesFound = new ArrayList<>();
		addEntitiesBetween(entitiesFound, start, target, planets.values());
		addEntitiesBetween(entitiesFound, start, target, allShips);
		return entitiesFound;
	}
	private static void addEntitiesBetween(List<Entity> entitiesFound, Position start, Position target,
			Collection<? extends Entity> entitiesToCheck) {
		for (Entity entity : entitiesToCheck) {
			if (entity.getPosition().equals(start) || entity.getPosition().equals(target)) {
				continue;
			}
			if (Collision.segmentCircleIntersect(start, target, entity, Constants.FORECAST_FUDGE_FACTOR)) {
				entitiesFound.add(entity);
			}
		}
	}
	public Map<Double, Entity> nearbyEntitiesByDistance(Entity entity) {
		Map<Double, Entity> entityByDistance = new TreeMap<>();
		for (Planet planet : planets.values()) {
			if (planet.equals(entity)) {
				continue;
			}
			entityByDistance.put(entity.getPosition().getDistanceTo(planet.getPosition()), planet);
		}
		for (Ship ship : allShips) {
			if (ship.equals(entity)) {
				continue;
			}
			entityByDistance.put(entity.getPosition().getDistanceTo(ship.getPosition()), ship);
		}
		return entityByDistance;
	}
	public GameMap updateMap(Metadata mapMetadata) {
		DebugLog.addLog("--- NEW TURN ---");
		int numberOfPlayers = MetadataParser.parsePlayerNum(mapMetadata);
		players.clear();
		planets.clear();
		allShips.clear();
		// update players info
		for (int i = 0; i < numberOfPlayers; ++i) {
			currentShips.clear();
			Map<Integer, Ship> currentPlayerShips = new TreeMap<>();
			int playerId = MetadataParser.parsePlayerId(mapMetadata);

			Player currentPlayer = new Player(playerId, currentPlayerShips);
			MetadataParser.populateShipList(currentShips, playerId, mapMetadata);
			allShips.addAll(currentShips);

			for (Ship ship : currentShips) {
				currentPlayerShips.put(ship.getId(), ship);
			}
			players.add(currentPlayer);
		}
		int numberOfPlanets = Integer.parseInt(mapMetadata.pop());
		for (int i = 0; i < numberOfPlanets; ++i) {
			List<Integer> dockedShips = new ArrayList<>();
			Planet planet = MetadataParser.newPlanetFromMetadata(dockedShips, mapMetadata);
			planets.put(planet.getId(), planet);
		}
		if (!mapMetadata.isEmpty()) {
			throw new IllegalStateException(
					"Failed to parse data from Halite game engine. Please contact maintainers.");
		}
		return this;
	}
}
