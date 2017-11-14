import hlt.*;
import lemon.halite2.benchmark.Benchmark;
import lemon.halite2.strats.AdvancedStrategy;
import lemon.halite2.strats.Strategy;
import lemon.halite2.util.MoveQueue;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MyBot {
	public static final SimpleDateFormat READABLE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	public static final SimpleDateFormat FILENAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");
	private static boolean flag = false;
	public static void main(String[] args) {
		try {
			Benchmark benchmark = new Benchmark();
			benchmark.push();
			Date currentDate = new Date();
			GameMap gameMap = Networking.initialize();
			DebugLog.initialize(String.format("logs/%s-%d.log", FILENAME_DATE_FORMAT.format(currentDate), gameMap.getMyPlayerId()));
			DebugLog.log("Initialization - "+READABLE_DATE_FORMAT.format(currentDate));
			MoveQueue moveQueue = new MoveQueue();
			
			Strategy strategy = new AdvancedStrategy(gameMap);
			strategy.init();
			
			Thread currentThread = Thread.currentThread();
			
			Runnable runnable = new Runnable(){
				@Override
				public void run() {
					while(true){
						while(!flag){
							try {
								Thread.sleep(1);
							} catch (InterruptedException e) {
								DebugLog.log("Why you interruptin?");
							}
						}
						flag = false;
						strategy.newTurn(moveQueue);
						currentThread.interrupt();
					}
				}
			};
			Thread thread = new Thread(runnable);
			
			DebugLog.log(String.format("Intialization finished in %s seconds", Benchmark.format(benchmark.pop())));
			Networking.finalizeInitialization("Lemon");
			
			while (true) {
				benchmark.push();
				DebugLog.log("New Turn: "+gameMap.getTurnNumber());
				benchmark.push();
				try{
					benchmark.push();
					gameMap.updateMap(Networking.readLineIntoMetadata());
					flag = true;
					Thread.sleep((int)(1900.0-Math.ceil(benchmark.peek()/1000000.0))); //nano to milli
					DebugLog.log("Interrupting Thread");
					thread.interrupt();
				}catch(InterruptedException ex){
					//Ignore
				}
				DebugLog.log(String.format("Finished Processing in %s seconds", Benchmark.format(benchmark.pop())));
				moveQueue.flush();
				DebugLog.log(String.format("Total Time = %s seconds", Benchmark.format(benchmark.pop())));
			}
		}catch(Exception ex) {
			DebugLog.log(ex);
			System.out.println(ex.getMessage());
		}
	}
}
