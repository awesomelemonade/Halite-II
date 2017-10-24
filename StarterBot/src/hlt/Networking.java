package hlt;

public class Networking {
	private static final char UNDOCK_KEY = 'u';
	private static final char DOCK_KEY = 'd';
	private static final char THRUST_KEY = 't';
	public static void sendMoves(Iterable<Move> moves) {
		StringBuilder moveString = new StringBuilder();
		for (Move move : moves) {
			switch (move.getType()) {
			case Noop:
				continue;
			case Undock:
				moveString.append(UNDOCK_KEY).append(" ").append(move.getShip().getId()).append(" ");
				break;
			case Dock:
				moveString.append(DOCK_KEY).append(" ").append(move.getShip().getId()).append(" ")
						.append(((DockMove) move).getDestinationId()).append(" ");
				break;
			case Thrust:
				moveString.append(THRUST_KEY).append(" ").append(move.getShip().getId()).append(" ")
						.append(((ThrustMove) move).getThrust()).append(" ").append(((ThrustMove) move).getAngle())
						.append(" ");
				break;
			}
		}
		System.out.println(moveString);
	}
	private static String readLine() {
		try {
			StringBuilder builder = new StringBuilder();
			int buffer;
			while ((buffer = System.in.read()) >= 0) {
				if (buffer == '\n') {
					break;
				}
				if (buffer == '\r') {
					// Ignore carriage return if on windows for manual testing.
					continue;
				}
				builder = builder.append((char) buffer);
			}
			return builder.toString();
		} catch (Exception e) {
			System.exit(1);
			return null;
		}
	}
	public static Metadata readLineIntoMetadata() {
		return new Metadata(readLine().trim().split(" "));
	}
	public static GameMap initialize() {
		int myId = Integer.parseInt(readLine());
		DebugLog.initialize(String.format("logs/log-%d-%d.log", myId, System.currentTimeMillis()));
		Metadata inputStringMapSize = readLineIntoMetadata();
		int width = Integer.parseInt(inputStringMapSize.pop());
		int height = Integer.parseInt(inputStringMapSize.pop());
		GameMap gameMap = new GameMap(width, height, myId);
		Metadata inputStringMetadata = readLineIntoMetadata();
		gameMap.updateMap(inputStringMetadata);
		return gameMap;
	}
	public static void finalizeInitialization(String botName) {
		System.out.println(botName);
	}
}
