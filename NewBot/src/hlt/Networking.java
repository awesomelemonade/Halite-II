package hlt;

import java.io.PrintWriter;

import lemon.halite2.benchmark.Benchmark;
import lemon.halite2.util.MathUtil;

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
			ThrustMove thrustMove = (ThrustMove)move;
			if(thrustMove.getThrustPlan().getThrust()!=0){
				Networking.writeThrustMoveEncoding(builder, thrustMove, -1);
			}
			break;
		}
	}
	public static void writeThrustMoveEncoding(StringBuilder builder, ThrustMove move, int encoding) {
		builder.append(THRUST_KEY).append(' ').append(move.getShipId()).append(' ')
				.append(move.getThrustPlan().getThrust()).append(' ')
				.append(move.getThrustPlan().getAngle()+MathUtil.TAU_DEGREES*(encoding+1)).append(' ');
	}
	public static void send(String string) {
		writer.print(string);
		writer.flush();
	}
	public static void flush() {
		writer.println();
		writer.flush();
	}
	private static String readLine(Benchmark benchmark) {
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
				if(builder.length()==0&&benchmark!=null) {
					benchmark.push();
				}
				builder = builder.append((char) buffer);
			}
			return builder.toString();
		} catch (Exception e) {
			return null;
		}
	}
	public static Metadata readLineIntoMetadata(Benchmark benchmark) {
		return new Metadata(readLine(benchmark).trim());
	}
	public static void initialize() {
		int myId = Integer.parseInt(readLine(null));
		Metadata inputStringMapSize = readLineIntoMetadata(null);
		int width = Integer.parseInt(inputStringMapSize.pop());
		int height = Integer.parseInt(inputStringMapSize.pop());
		GameMap.INSTANCE.init(width, height, myId);
		Metadata inputStringMetadata = readLineIntoMetadata(null);
		GameMap.INSTANCE.updateMap(inputStringMetadata);
	}
	public static void finalizeInitialization(String botName) {
		writer.println(botName);
		writer.flush();
	}
}
