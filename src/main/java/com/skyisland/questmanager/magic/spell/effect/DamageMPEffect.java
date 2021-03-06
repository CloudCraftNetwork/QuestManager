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
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.skyisland.questmanager.QuestManagerPlugin;
import com.skyisland.questmanager.configuration.utils.YamlWriter;
import com.skyisland.questmanager.magic.MagicUser;
import com.skyisland.questmanager.player.PlayerOptions;
import com.skyisland.questmanager.player.QuestPlayer;
import com.skyisland.questmanager.player.skill.event.MagicApplyEvent;

public class DamageMPEffect extends ImbuementEffect {
	
	/**
	 * Registers this class as configuration serializable with all defined 
	 * {@link aliases aliases}
	 */
	public static void registerWithAliases() {
		for (aliases alias : aliases.values()) {
			ConfigurationSerialization.registerClass(DamageMPEffect.class, alias.getAlias());
		}
	}
	
	/**
	 * Registers this class as configuration serializable with only the default alias
	 */
	public static void registerWithoutAliases() {
		ConfigurationSerialization.registerClass(DamageMPEffect.class);
	}
	

	private enum aliases {
		DEFAULT(DamageMPEffect.class.getName()),
		OLD("com.SkyIsland.QuestManager.Magic.Spell.Effect." + DamageMPEffect.class.getSimpleName()),
		LONGI("SpellDamageMP"),
		LONG("DamageMPSpell"),
		SHORT("SDamageMP");
		
		private String alias;
		
		aliases(String alias) {
			this.alias = alias;
		}
		
		public String getAlias() {
			return alias;
		}
	}
	
	public static DamageMPEffect valueOf(Map<String, Object> map) {
		return new DamageMPEffect((double) map.get("damage"));
	}
	
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		
		map.put("damage", damage);
		
		return map;
	}
	
	private double damage;
	
	public DamageMPEffect(double damage) {
		this.damage = damage;
	}
	
	@Override
	public void apply(Entity e, MagicUser cause) {
		double curDamage = damage;
		if (cause instanceof QuestPlayer) {
			QuestPlayer qp = (QuestPlayer) cause;
			MagicApplyEvent aEvent = new MagicApplyEvent(qp, curDamage);
			Bukkit.getPluginManager().callEvent(aEvent);
			
			curDamage = aEvent.getFinalDamage();
		}
		
		if (e instanceof Player) {
			QuestPlayer qp = QuestManagerPlugin.questManagerPlugin.getPlayerManager()
					.getPlayer((Player) e);
			
			if (qp.getOptions().getOption(PlayerOptions.Key.CHAT_COMBAT_DAMAGE)) {
				
				String msg;
				if (cause instanceof QuestPlayer && ((QuestPlayer) cause).getPlayer().getUniqueId()
						.equals(qp.getPlayer().getUniqueId())) {
					//healed self
					msg = ChatColor.DARK_GRAY + "You lost " + ChatColor.DARK_BLUE + "%.2f"
							+ ChatColor.DARK_GRAY + " mana" + ChatColor.RESET;
				} else {
					String name = cause.getEntity().getCustomName();
					if (name == null) {
						name = cause.getEntity().getType().toString();
					}
					msg = ChatColor.GRAY + cause.getEntity().getCustomName() + ChatColor.DARK_GRAY 
							+ " damaged your mana by " + ChatColor.DARK_BLUE + "%.2f" + ChatColor.DARK_GRAY
							+ " points";
				}
				
				qp.getPlayer().getPlayer().sendMessage(String.format(msg, curDamage));
				
			}
			
			qp.addMP((int) -curDamage);
			//return;
		} else if (e instanceof MagicUser) {
			((MagicUser) e).addMP((int) -curDamage);
		}
		
		if (cause instanceof QuestPlayer) {
			QuestPlayer qp = (QuestPlayer) cause;
			if (qp.getOptions().getOption(PlayerOptions.Key.CHAT_COMBAT_DAMAGE)
				&& qp.getPlayer().isOnline()) {
				Player p = qp.getPlayer().getPlayer();
				
				String msg;
				String name = e.getCustomName();
				if (name == null) {
					name = YamlWriter.toStandardFormat(cause.getEntity().getType().toString());
				}
				msg = ChatColor.DARK_GRAY + "You drained " + ChatColor.DARK_BLUE + "%.2f" 
						+ ChatColor.DARK_GRAY + " mana from" + ChatColor.GRAY + name + ChatColor.RESET;
				
				p.sendMessage(String.format(msg, curDamage));
			}
		}
	}
	
	@Override
	public void apply(Location loc, MagicUser cause) {
		//can't damage a location
		//do nothing 
		;
	}

	@Override
	public ImbuementEffect getCopyAtPotency(double potency) {
		DamageMPEffect effect = new DamageMPEffect(damage * potency);
		effect.setDisplayName(getDisplayName());
		return effect;		
	}
	
	public double getDamage() {
		return damage;
	}
}
