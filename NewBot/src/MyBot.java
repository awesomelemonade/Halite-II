import hlt.*;
import lemon.halite2.benchmark.Benchmark;
import lemon.halite2.strats.AdvancedStrategy;
import lemon.halite2.strats.Strategy;
import lemon.halite2.util.MoveQueue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyBot {
	public static final SimpleDateFormat READABLE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	public static final SimpleDateFormat FILENAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");
	public static void main(String[] args) {
		try {
			Benchmark benchmark = new Benchmark();
			benchmark.push();
			Date currentDate = new Date();
			GameMap gameMap = Networking.initialize();
			DebugLog.initialize(String.format("logs/%s-%d.log", FILENAME_DATE_FORMAT.format(currentDate), gameMap.getMyPlayerId()));
			DebugLog.log("Initialization - "+READABLE_DATE_FORMAT.format(currentDate));
			MoveQueue moveQueue = new MoveQueue();
			List<Integer> handledShips = new ArrayList<Integer>();
			
			Strategy strategy = new AdvancedStrategy(gameMap);
			strategy.init();
			
			DebugLog.log(String.format("Intialization finished in %s seconds", Benchmark.format(benchmark.pop())));
			Networking.finalizeInitialization("Lemon");
			
			while (true) {
				benchmark.push();
				DebugLog.log("New Turn: "+gameMap.getTurnNumber());
				gameMap.updateMap(Networking.readLineIntoMetadata());
				handledShips.clear();
				DebugLog.log("Processing Strategy");
				benchmark.push();
				strategy.newTurn(moveQueue);
				DebugLog.log(String.format("Processed strategy in %s seconds", Benchmark.format(benchmark.pop())));
				moveQueue.flush();
				DebugLog.log(String.format("Total Time = %s seconds", Benchmark.format(benchmark.pop())));
			}
		}catch(Exception ex) {
			DebugLog.log(ex);
			System.out.println(ex.getMessage());
		}
	}
}
