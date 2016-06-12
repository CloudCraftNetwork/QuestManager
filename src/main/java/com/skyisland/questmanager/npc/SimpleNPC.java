/*
 *  QuestManager: An RPG plugin for the Bukkit API.
 *  Copyright (C) 2015-2016 Github Contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.skyisland.questmanager.npc;

import com.skyisland.questmanager.scheduling.DispersedScheduler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Describes NPCs with simple movement pattern: they occasionally attempt to
 * move back to their original spot
 * @author Skyler
 *
 */
public abstract class SimpleNPC extends NPC {
	
	private Location startingLoc;
	
	/**
	 * Defines how far an NPC can be when they are ticked before being teleported
	 * back to their original location
	 */
	private static final double range = 20.0;
	
	protected SimpleNPC(Location startingLoc) {
		super();
		this.startingLoc = startingLoc;
		
		DispersedScheduler.getScheduler().register(this);
	}
	
	/**
	 * Motivate entity to move back to the original location, if we hve one set
	 */
	@Override
	public void tick() {
		Entity e = getEntity();
		
		if (e == null || startingLoc == null) {
			return;
		}
		

		if (!e.getLocation().getChunk().isLoaded() || !startingLoc.getChunk().isLoaded()) {
			return;
		}
		
		if (!e.getLocation().getWorld().getName().equals(
				startingLoc.getWorld().getName()) 
				|| e.getLocation().distance(startingLoc) > range) {
			//if we're in a different world (whut?) or range is too big,
			//teleport them back!
			e.getLocation().getChunk().load();
			startingLoc.getChunk().load();
			e.teleport(startingLoc);
		}
	}

	/**
	 * @return the startingLoc
	 */
	public Location getStartingLoc() {
		return startingLoc;
	}

	/**
	 * @param startingLoc the startingLoc to set
	 */
	public void setStartingLoc(Location startingLoc) {
		this.startingLoc = startingLoc;
	}
}