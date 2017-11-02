import hlt.*;
import lemon.halite2.strats.AdvancedStrategy;
import lemon.halite2.strats.BasicStrategy;
import lemon.halite2.strats.BasicStrategy2;
import lemon.halite2.strats.Strategy;
import lemon.halite2.util.MoveQueue;
import lemon.halite2.util.Pathfinder;
import lemon.halite2.util.ShipPriorities;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
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
			ShipPriorities shipPriorities = new ShipPriorities(gameMap);
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
				shipPriorities.update();
				handledShips.clear();
				strategy.newTurn();
				DebugLog.log("Processing Ships");
				for(ShipPriorities.Priority priority: ShipPriorities.Priority.values()) {
					for(int shipId: shipPriorities.getPrioritiesMap().get(priority)) {
						if(handledShips.contains(shipId)){ //Already handled ship from a request
							continue;
						}
						ArrayDeque<Integer> handleStack = new ArrayDeque<Integer>();
						handleStack.push(shipId);
						handledShips.add(shipId);
						while(!handleStack.isEmpty()){
							int request = strategy.handleShip(handledShips, handleStack.peek(), moveQueue);
							if(request==-1){
								handleStack.pop();
							}else{
								if(handledShips.contains(request)){
									throw new IllegalStateException(String.format("Already Handled Ship: %d", request));
								}
								handleStack.push(request);
								handledShips.add(request);
							}
						}
					}
				}
				moveQueue.flush();
				DebugLog.log("Flushed Moves");
			}
		}catch(Exception ex) {
			DebugLog.log(ex);
			System.out.println(ex.getMessage());
		}
	}
}
