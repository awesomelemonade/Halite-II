package hlt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Collection;

public class GameMap {
	private int turnNumber;
	private int width, height;
	private int playerId;
	private List<Player> players;
	private List<Player> playersUnmodifiable;
	private Map<Integer, Planet> planets;
	private List<Ship> ships;
	private List<Ship> shipsUnmodifiable;
	// used only during parsing to reduce memory allocations
	private List<Ship> currentShips;

	public GameMap(int width, int height, int playerId) {
		this.width = width;
		this.height = height;
		this.playerId = playerId;
		this.players = new ArrayList<Player>(Constants.MAX_PLAYERS);
		this.playersUnmodifiable = Collections.unmodifiableList(players);
		this.planets = new TreeMap<Integer, Planet>();
		this.ships = new ArrayList<Ship>();
		this.shipsUnmodifiable = Collections.unmodifiableList(ships);
		this.currentShips = new ArrayList<Ship>();
		this.turnNumber = -1;
	}
	public int getTurnNumber() {
		return turnNumber;
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
	public List<Player> getPlayers() {
		return playersUnmodifiable;
	}
	public Player getMyPlayer() {
		return getPlayers().get(getMyPlayerId());
	}
	public Ship getShip(int playerId, int entityId) throws IndexOutOfBoundsException {
		return players.get(playerId).getShip(entityId);
	}
	public Planet getPlanet(int entityId) {
		return planets.get(entityId);
	}
	public Collection<Planet> getPlanets() {
		return planets.values();
	}
	public List<Ship> getShips() {
		return shipsUnmodifiable;
	}
	public ArrayList<Entity> objectsBetween(Position start, Position target) {
		ArrayList<Entity> entitiesFound = new ArrayList<Entity>();
		addEntitiesBetween(entitiesFound, start, target, planets.values());
		addEntitiesBetween(entitiesFound, start, target, ships);
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
		Map<Double, Entity> entityByDistance = new TreeMap<Double, Entity>();
		for (Planet planet : planets.values()) {
			if (planet.equals(entity)) {
				continue;
			}
			entityByDistance.put(entity.getPosition().getDistanceTo(planet.getPosition()), planet);
		}
		for (Ship ship : ships) {
			if (ship.equals(entity)) {
				continue;
			}
			entityByDistance.put(entity.getPosition().getDistanceTo(ship.getPosition()), ship);
		}
		return entityByDistance;
	}
	public GameMap updateMap(Metadata metadata) {
		int numberOfPlayers = MetadataParser.parsePlayerNum(metadata);
		players.clear();
		planets.clear();
		ships.clear();
		// update players info
		for (int i = 0; i < numberOfPlayers; ++i) {
			currentShips.clear();
			Map<Integer, Ship> currentPlayerShips = new TreeMap<Integer, Ship>();
			int playerId = MetadataParser.parsePlayerId(metadata);

			Player currentPlayer = new Player(playerId, currentPlayerShips);
			MetadataParser.populateShipList(currentShips, playerId, metadata);
			ships.addAll(currentShips);

			for (Ship ship : currentShips) {
				currentPlayerShips.put(ship.getId(), ship);
			}
			players.add(currentPlayer);
		}
		int numberOfPlanets = MetadataParser.parsePlanetNum(metadata);
		for (int i = 0; i < numberOfPlanets; ++i) {
			List<Integer> dockedShips = new ArrayList<Integer>();
			Planet planet = MetadataParser.newPlanetFromMetadata(dockedShips, metadata);
			planets.put(planet.getId(), planet);
		}
		if (!metadata.isEmpty()) {
			throw new IllegalStateException(
					"Failed to parse data from Halite game engine. Please contact maintainers.");
		}
		turnNumber++;
		return this;
	}
}
