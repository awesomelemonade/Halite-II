import lemon.halite2.benchmark.Benchmark;
import lemon.halite2.strats.AdvancedStrategy;
import lemon.halite2.strats.Strategy;
import lemon.halite2.util.MoveQueue;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import hlt.DebugLog;
import hlt.GameMap;
import hlt.Networking;

public class MyBot {
	public static final SimpleDateFormat READABLE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	public static final SimpleDateFormat FILENAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");
	private static long millis;
	public static void main(String[] args) {
		double timeout = 1900.0;
		if(args.length>0) {
			timeout = Double.parseDouble(args[0]);
		}
		try {
			Benchmark benchmark = new Benchmark();
			benchmark.push();
			Date currentDate = new Date();
			Networking.initialize();
			DebugLog.initialize(String.format("logs/%s-%d.log", FILENAME_DATE_FORMAT.format(currentDate), GameMap.INSTANCE.getMyPlayerId()));
			DebugLog.log("Initialization - "+READABLE_DATE_FORMAT.format(currentDate));
			MoveQueue moveQueue = new MoveQueue();
			
			Strategy strategy = new AdvancedStrategy();
			strategy.init();
			
			Thread mainThread = Thread.currentThread();
			
			Runnable runnable = new Runnable(){
				@Override
				public void run() {
					while(true){
						try {
							if(millis>0) {
								try {
									Thread.sleep(millis);
									DebugLog.log("Interrupting Main Thread: "+benchmark.peek()/1000000.0);
									mainThread.interrupt();
								}catch(InterruptedException ex) {
									DebugLog.log("Interrupted, Finished Early");
									//Ignore
								}
								millis = 0;
							}else {
								try {
									Thread.sleep(1);
								}catch(InterruptedException ex) {
									DebugLog.log("Why you interruptin");
								}
							}
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
				DebugLog.log("New Turn: "+GameMap.INSTANCE.getTurnNumber());
				GameMap.INSTANCE.updateMap(Networking.readLineIntoMetadata(benchmark)); //Pushes Benchmark
				millis = Math.max(0, (int)(timeout-Math.ceil(benchmark.peek()/1000000.0)));
				strategy.newTurn(moveQueue);
				DebugLog.log(String.format("Finished Processing in %s seconds", Benchmark.format(benchmark.peek())));
				if(!Thread.interrupted()) {
					DebugLog.log("Interrupting Timer Thread");
					thread.interrupt();
				}
				moveQueue.flush();
				DebugLog.log(String.format("Total Time = %s seconds", Benchmark.format(benchmark.pop()))); //Pops Benchmark
			}
		}catch(Exception ex) {
			DebugLog.log(ex);
			System.out.println(Arrays.toString(ex.getStackTrace()));
		}
	}
}
