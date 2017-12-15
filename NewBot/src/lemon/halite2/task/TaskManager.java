package lemon.halite2.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hlt.GameMap;
import hlt.Planet;
import hlt.Ship;
import hlt.Ship.DockingStatus;
import lemon.halite2.task.experimental.FindEnemyTask;
import lemon.halite2.task.projection.FindProjectedDockedEnemyTask;

public enum TaskManager {
	INSTANCE;
	private List<Task> tasks;
	private Map<Integer, DockTask> dockTasks;
	public void init() {
		tasks = new ArrayList<Task>();
		dockTasks = new HashMap<Integer, DockTask>();
	}
	public void update() {
		tasks.clear();
		dockTasks.clear();
		tasks.add(new WanderTask());
		for(Planet planet: GameMap.INSTANCE.getPlanets()){
			DockTask task = new DockTask(planet);
			dockTasks.put(planet.getId(), task);
			tasks.add(task);
		}
		for(Ship ship: GameMap.INSTANCE.getShips()){
			if(ship.getOwner()==GameMap.INSTANCE.getMyPlayerId()){
				if(ship.getDockingStatus()!=DockingStatus.UNDOCKED){
					tasks.add(new DefendDockedShipTask(ship));
				}
			}else{
				if(ship.getDockingStatus()==DockingStatus.UNDOCKED){
					tasks.add(new FindProjectedDockedEnemyTask(ship));
					tasks.add(new FindEnemyTask(ship));
					tasks.add(new AttackEnemyTask(ship));
				}else{
					tasks.add(new AttackDockedEnemyTask(ship));
				}
			}
		}
	}
	public List<Task> getTasks(){
		return tasks;
	}
}
