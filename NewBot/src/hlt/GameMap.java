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
	private Position centerPosition;
	// used only during parsing to reduce memory allocations
	private List<Ship> currentShips;

	public GameMap(int width, int height, int playerId) {
		this.width = width;
		this.height = height;
		this.playerId = playerId;
		this.players = new ArrayList<Player>(GameConstants.MAX_PLAYERS);
		this.playersUnmodifiable = Collections.unmodifiableList(players);
		this.planets = new TreeMap<Integer, Planet>();
		this.ships = new ArrayList<Ship>();
		this.shipsUnmodifiable = Collections.unmodifiableList(ships);
		this.currentShips = new ArrayList<Ship>();
		this.turnNumber = -1;
		centerPosition = new Position(((double)width)/2, ((double)height)/2);
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
	public Position getCenterPosition(){
		return centerPosition;
	}
	public GameMap updateMap(Metadata metadata) {
		int numberOfPlayers = MetadataParser.parsePlayerNum(metadata);
		players.clear();
		planets.clear();
		ships.clear();
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
