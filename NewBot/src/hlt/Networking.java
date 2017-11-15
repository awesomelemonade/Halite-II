package hlt;

import java.io.PrintWriter;

public class Networking {
	private static final PrintWriter writer;
	private static final char UNDOCK_KEY = 'u';
	private static final char DOCK_KEY = 'd';
	private static final char THRUST_KEY = 't';
	static {
		writer = new PrintWriter(System.out);
	}
	public static void writeMove(StringBuilder builder, Move move) {
		switch (move.getType()) {
		case UNDOCK:
			builder.append(UNDOCK_KEY).append(' ').append(move.getShipId()).append(' ');
			break;
		case DOCK:
			builder.append(DOCK_KEY).append(' ').append(move.getShipId()).append(' ')
					.append(((DockMove) move).getPlanetId()).append(' ');
			break;
		case THRUST:
			ThrustPlan plan = ((ThrustMove)move).getThrustPlan();
			if(plan.getThrust()!=0){
				builder.append(THRUST_KEY).append(' ').append(move.getShipId()).append(' ')
						.append(plan.getThrust()).append(' ')
						.append(plan.getAngle()).append(' ');
			}
			break;
		}
	}
	public static void send(String string) {
		writer.println(string);
		writer.flush();
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
			return null;
		}
	}
	public static Metadata readLineIntoMetadata() {
		return new Metadata(readLine().trim());
	}
	public static GameMap initialize() {
		int myId = Integer.parseInt(readLine());
		Metadata inputStringMapSize = readLineIntoMetadata();
		int width = Integer.parseInt(inputStringMapSize.pop());
		int height = Integer.parseInt(inputStringMapSize.pop());
		GameMap gameMap = new GameMap(width, height, myId);
		Metadata inputStringMetadata = readLineIntoMetadata();
		gameMap.updateMap(inputStringMetadata);
		return gameMap;
	}
	public static void finalizeInitialization(String botName) {
		writer.println(botName);
		writer.flush();
	}
}
