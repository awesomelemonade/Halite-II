import lemon.halite2.benchmark.Benchmark;
import lemon.halite2.strats.AdvancedStrategy;
import lemon.halite2.strats.Strategy;
import lemon.halite2.util.MoveQueue;

import java.text.SimpleDateFormat;
import java.util.Date;

import hlt.DebugLog;
import hlt.GameMap;
import hlt.Networking;

public class MyBot {
	public static final SimpleDateFormat READABLE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	public static final SimpleDateFormat FILENAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");
	private static boolean flag = false;
	private static boolean flag2 = false;
	public static void main(String[] args) {
		double timeout = 1900.0;
		if(args.length>0) {
			timeout = Double.parseDouble(args[0]);
		}
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
						try {
							while(!flag){
								try {
									Thread.sleep(1);
								} catch (InterruptedException e) {
									DebugLog.log("Why you interruptin?");
								}
							}
							flag = false;
							DebugLog.log("Processing Turn");
							strategy.newTurn(moveQueue);
							if(!Thread.interrupted()) {
								currentThread.interrupt();
							}
							flag2 = true;
						}catch(Exception ex) {
							DebugLog.log(ex);
						}
					}
				}
			};
			Thread thread = new Thread(runnable);
			thread.start();
			
			DebugLog.log(String.format("Intialization finished in %s seconds", Benchmark.format(benchmark.pop())));
			Networking.finalizeInitialization("Lemon");
			
			while (true) {
				benchmark.push();
				DebugLog.log("New Turn: "+gameMap.getTurnNumber());
				try{
					benchmark.push();
					gameMap.updateMap(Networking.readLineIntoMetadata());
					flag = true;
					Thread.sleep(Math.max(0, (int)(timeout-Math.ceil(benchmark.peek()/1000000.0)))); //nano to milli
					DebugLog.log("Interrupting Thread");
					thread.interrupt();
				}catch(InterruptedException ex){
					//Ignore
				}
				while(!flag2) {
					try {
						Thread.sleep(1);
					}catch(InterruptedException ex) {}
				}
				flag2 = false;
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
