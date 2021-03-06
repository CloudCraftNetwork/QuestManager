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

package com.skyisland.questmanager.magic.spell.effect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import com.skyisland.questmanager.QuestManagerPlugin;
import com.skyisland.questmanager.magic.MagicUser;
import com.skyisland.questmanager.magic.SpellPylon;
import com.skyisland.questmanager.player.QuestPlayer;

/**
 * Creates in the world a pylon ties to the given player of a certain type
 *
 */
public class CastPylonEffect extends SpellEffect {
	
	public static final String DISTANCE_ERROR_MESSAGE = ChatColor.RED + "The pylon would be too far from the others, and was not set!";
	
	/**
	 * Registers this class as configuration serializable with all defined 
	 * {@link aliases aliases}
	 */
	public static void registerWithAliases() {
		for (aliases alias : aliases.values()) {
			ConfigurationSerialization.registerClass(CastPylonEffect.class, alias.getAlias());
		}
	}
	
	/**
	 * Registers this class as configuration serializable with only the default alias
	 */
	public static void registerWithoutAliases() {
		ConfigurationSerialization.registerClass(CastPylonEffect.class);
	}
	

	private enum aliases {
		DEFAULT(CastPylonEffect.class.getName()),
		OLD("com.SkyIsland.QuestManager.Magic.Spell.Effect." + CastPylonEffect.class.getSimpleName()),
		LONGI("SpellPylon"),
		LONG("PylonSpell"),
		SHORT("SPylon");
		
		private String alias;
		
		aliases(String alias) {
			this.alias = alias;
		}
		
		public String getAlias() {
			return alias;
		}
	}
	
	private String type;
	
	private ItemStack icon;
	
	public static CastPylonEffect valueOf(Map<String, Object> map) {
		return new CastPylonEffect((String) map.get("type"), (ItemStack) map.get("icon"));
	}
	
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		
		map.put("type", this.type);
		map.put("icon", icon);
		
		return map;
	}
	
	public CastPylonEffect(String name, ItemStack icon) {
		this.type = name;
		this.icon = icon;
	}

	@Override
	public void apply(Entity e, MagicUser cause) {
		//cast it at location of entity?
		apply(e.getLocation(), cause);
//		SpellPylon pylon = new SpellPylon(type, icon, e.getLocation());
//		
//		cause.addSpellPylon(pylon);
		
	}

	@Override
	public void apply(Location loc, MagicUser cause) {
		List<SpellPylon> active = cause.getSpellPylons();
		double maxDistance = QuestManagerPlugin.questManagerPlugin.getPluginConfiguration().getMaxPylonDistance();
		if (active != null && !active.isEmpty()) {
			for (SpellPylon pylon : active) {
				
				if (pylon.getLocation().distance(loc) > maxDistance) {
					if (cause instanceof QuestPlayer) {
						QuestPlayer qp = (QuestPlayer) cause;
						if (qp.getPlayer().isOnline())
							qp.getPlayer().getPlayer().sendMessage(DISTANCE_ERROR_MESSAGE);
					}
					return; //cancel everything. just stop, cause it's gonna be too big.
				}
			}
		}
		
		
		SpellPylon pylon = new SpellPylon(type, icon, loc);
		
		cause.addSpellPylon(pylon);
	}

	public String getType() {
		return type;
	}

	public ItemStack getIcon() {
		return icon;
	}
}
