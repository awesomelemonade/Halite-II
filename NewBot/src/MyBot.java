import hlt.*;
import lemon.halite2.strats.AdvancedStrategy;
import lemon.halite2.strats.Strategy;
import lemon.halite2.util.MoveQueue;
import lemon.halite2.util.Pathfinder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyBot {
	public static final SimpleDateFormat READABLE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	public static final SimpleDateFormat FILENAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");
	public static void main(String[] args) {
		try {
			Date currentDate = new Date();
			GameMap gameMap = Networking.initialize();
			DebugLog.initialize(String.format("logs/%s-%d.log", FILENAME_DATE_FORMAT.format(currentDate), gameMap.getMyPlayerId()));
			DebugLog.log("Initialization - "+READABLE_DATE_FORMAT.format(currentDate));
			Pathfinder.init(gameMap);
			MoveQueue moveQueue = new MoveQueue(gameMap);
			List<Integer> handledShips = new ArrayList<Integer>();
			
			Strategy strategy = new AdvancedStrategy(gameMap);
			strategy.init();
			//Rate Planets
			
			Networking.finalizeInitialization("Lemon");
			
			while (true) {
				DebugLog.log("New Turn: "+gameMap.getTurnNumber());
				gameMap.updateMap(Networking.readLineIntoMetadata());
				Pathfinder.update();
				handledShips.clear();
				DebugLog.log("Processing Strategy");
				strategy.newTurn(moveQueue);
				moveQueue.flush();
				DebugLog.log("Flushed Moves");
			}
		}catch(Exception ex) {
			DebugLog.log(ex);
			System.out.println(ex.getMessage());
		}
	}
}
