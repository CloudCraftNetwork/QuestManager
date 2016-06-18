package com.skyisland.questmanager.quest;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import com.skyisland.questmanager.QuestManagerPlugin;
import com.skyisland.questmanager.configuration.state.GoalState;
import com.skyisland.questmanager.configuration.state.RequirementState;
import com.skyisland.questmanager.configuration.state.StatekeepingRequirement;
import com.skyisland.questmanager.quest.requirements.Requirement;

/**
 * Tracks objectives in a quest.
 * Goals have specific requirements that must be met before they are considered clear.
 * @author Skyler
 *
 */
public class Goal {
	
	private List<Requirement> requirements;
	
	private Integer nextGoalIndex; 
	
	private String name;
	
	private String description;
	
	private Quest quest;
	
	private int index;
	
	//private List<Chest> chests;
	

//	public static Goal fromConfig(Quest quest, YamlConfiguration config) throws InvalidConfigurationException {
//		return fromMap(quest, config.)
//	}
	
	/**
	 * Creates a goal from the provided goal configuration
	 * @throws InvalidConfigurationException 
	 */
	public static Goal fromConfig(Quest quest, int index, ConfigurationSection config) throws InvalidConfigurationException {
		/* goal construction configuration involves:
		 * Goal name, description
		 * The requirements that are in it
		 * 
		 * The req's are in a list, with each element being a con section with the
		 * key being the type of req and the value being the config section for setting
		 * up the req
		 */
		
		if (!config.contains("type") || !config.getString("type").equals("goalcnf")) {
			if (config.getString("type").equals("juncnf"))
				return Junction.fromConfig(quest, index, config);
			throw new InvalidConfigurationException();
		}
		
		String name, description;
		Integer nextIndex;
		
		name = config.getString("name");
		description = config.getString("description");
		nextIndex = (config.contains("next") ? config.getInt("next") : null);
		

		Goal goal = new Goal(quest, index, nextIndex, name, description);
		
		List<ConfigurationSection> reqs = new LinkedList<>();
		for (String requirementKey : config.getConfigurationSection("requirements").getKeys(false)) {
			reqs.add( config.getConfigurationSection("requirements")
					.getConfigurationSection(requirementKey));
		}
		
		for (ConfigurationSection req : reqs) {
			String type = req.getKeys(false).iterator().next();
			
			ConfigurationSection conf = req.getConfigurationSection(type);
			
			
			Requirement r = QuestManagerPlugin.questManagerPlugin.getRequirementManager()
					.instanceRequirement(type, goal, conf);
			
			if (r == null) {
				QuestManagerPlugin.questManagerPlugin.getLogger()
					.warning("    Invalid requirement type for goal: " + goal.name);
			}
			
			goal.addRequirement(r);
		}
		
		return goal;
		
	}
	
	public Goal(Quest quest, int index, Integer nextGoal, String name, String description) {
		this.quest = quest;
		this.name = name;
		this.description = description;
		this.index = index;
		this.nextGoalIndex = nextGoal;
		
		this.requirements = new LinkedList<>();
		//this.chests = new LinkedList<Chest>();
	}
	
	public Goal(Quest quest, int index, Integer nextGoal, String name) {
		this(quest, index, nextGoal, name, "");
	}
	
	public void loadState(GoalState state) throws InvalidConfigurationException {
		
		if (!state.getName().equals(name)) {
			QuestManagerPlugin.questManagerPlugin.getLogger().warning("Loading state information"
					+ "from a file that has a mismatched goal name!");
		}
		
		//WARNING:
		//this is assuming that the lists are maintianed in the right order.
		//it should work this way, but this is a point of error!
		ListIterator<RequirementState> states = state.getRequirementStates().listIterator();
		for (Requirement req : requirements) {
			req.sync();
			try {
				if (req instanceof StatekeepingRequirement) {
					((StatekeepingRequirement) req).loadState(states.next());
				}
			} catch (NoSuchElementException e) {
				QuestManagerPlugin.questManagerPlugin.getLogger().warning("Error when loading state for quest" 
						+ this.getQuest().getName() + "; Not enough requirement states!");
			}
		}
	}
	
	public GoalState getState() {
		
		GoalState state = new GoalState();
		state.setName(name);
		
		for (Requirement req : requirements) {
			if (req instanceof StatekeepingRequirement) {
				state.addRequirementState(((StatekeepingRequirement) req).getState());
			}
		}
		
		return state;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the quest
	 */
	public Quest getQuest() {
		return quest;
	}
	
	/**
	 * Adds a new requirement to this goal
	 */
	public void addRequirement(Requirement requirement) {
		requirements.add(requirement);
	}
	
	public List<Requirement> getRequirements() {
		return requirements;
	}
	
	/**
	 * Assesses and reports whether the goal has been completed.
	 * Please note that goals that have no requirements defaultly return true.
	 */
	public boolean isComplete() {
		if (requirements.isEmpty()) {
			return true;
		}
		
		for (Requirement req : requirements) {
			if (req.isCompleted() == false) {
				return false;
			}
		}
		
		return true;
	}
	
	public void sync() {
		if (!requirements.isEmpty()) {
			for (Requirement r : requirements) {
				r.sync();
			}
		}
	}
	
	/**
	 * Perform cleanup before exiting/reloading
	 */
	public void stop() {
		for (Requirement req : requirements) {
			if (req instanceof StatekeepingRequirement) {
				((StatekeepingRequirement) req).stop();
			}
			if (req instanceof Listener) {
				HandlerList.unregisterAll((Listener) req);
			}
		}
	}
	
	public Goal fetchNextGoal() {
		try {
			if (nextGoalIndex == null)
				return quest.getTemplate().fetchGoal(quest, index+1);
			else
				return quest.getTemplate().fetchGoal(quest, nextGoalIndex);
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
			QuestManagerPlugin.questManagerPlugin.getLogger().warning("Failed to get goal data from configuration!");
			return null;
		}
	}
	
	public int getIndex() {
		return index;
	}
}
