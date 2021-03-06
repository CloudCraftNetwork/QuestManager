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

package com.skyisland.questmanager.ui.menu.action;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.skyisland.questmanager.QuestManagerPlugin;
import com.skyisland.questmanager.effects.AuraEffect;
import com.skyisland.questmanager.player.QuestPlayer;
import com.skyisland.questmanager.scheduling.Alarm;
import com.skyisland.questmanager.scheduling.Alarmable;

/**
 * An action that must charge for a while, and then happens
 *
 */
public class ChargeAction implements Listener, Alarmable<Integer> {
	
	public static final String DISTURBED_MESSAGE = ChatColor.RED + "Your charging action was disturbed" + ChatColor.RESET;
	
	public static final String CANCEL_MESSAGE = ChatColor.YELLOW + "You cancelled your action" + ChatColor.RESET;
	
	private static final Effect DEFAULT_EFFECT = Effect.MAGIC_CRIT;
	
	private static final Sound DEFAULT_SOUND = Sound.BLOCK_BREWING_STAND_BREW;
	
	private boolean canMove;
	
	private boolean canGetHit;
	
	private boolean canChangeItems;
	
	private MenuAction action;
	
	private QuestPlayer player;
	
	private AuraEffect effect;
	
	public ChargeAction(MenuAction action, QuestPlayer player, boolean canMove, boolean canGetHit, 
			boolean canChangeItems, double chargingTime) {
		this(action, player, DEFAULT_EFFECT, DEFAULT_SOUND, canMove, canGetHit, canChangeItems, chargingTime);
	}
	
	public ChargeAction(MenuAction action, QuestPlayer player, Effect effect, Sound sound, boolean canMove, 
			boolean canGetHit, boolean canChangeItems, double chargingTime) {
		if (!player.getPlayer().isOnline()) {
			return;
		}
		
		this.action = action;
		this.canMove = canMove;
		this.canGetHit = canGetHit;
		this.canChangeItems = canChangeItems;
		this.player = player;
		this.effect = new AuraEffect(effect);
		this.effect.play(player.getEntity());
		
		player.getEntity().getWorld().playSound(player.getEntity().getLocation(), sound, 1, 1);

		Bukkit.getPluginManager().registerEvents(this, QuestManagerPlugin.questManagerPlugin);
		Alarm.getScheduler().schedule(this, 0, chargingTime);
	}
	
	private void stopListening() {
		HandlerList.unregisterAll(this);
	}
	
	private void doneCasting() {
		effect.stop();
		stopListening();
		Alarm.getScheduler().unregister(this);
	}

	@EventHandler
	public void onEntityMove(PlayerMoveEvent e) {
		if (canMove) {
			return;
		}
		
		if (!QuestManagerPlugin.questManagerPlugin.getPluginConfiguration().getWorlds()
				.contains(e.getPlayer().getWorld().getName())) {
			return;
		}
		QuestPlayer qp = QuestManagerPlugin.questManagerPlugin.getPlayerManager().getPlayer(e.getPlayer());
		
		if (!qp.equals(this.player)) {
			return;
		}
		
		if (e.getTo().toVector().equals(e.getPlayer().getLocation().toVector())) {
			return; //just turned head?
		}
		
		//a current charge has moved, and is not allowed to
		e.getPlayer().sendMessage(DISTURBED_MESSAGE);
		doneCasting();
	}
	
	@EventHandler
	public void onEntityHurt(EntityDamageEvent e) {
		if (canGetHit) {
			return;
		}
		
		if (!(e.getEntity() instanceof Player)) {
			return;
		}
		
		if (!QuestManagerPlugin.questManagerPlugin.getPluginConfiguration().getWorlds()
				.contains(e.getEntity().getWorld().getName())) {
			return;
		}
		QuestPlayer qp = QuestManagerPlugin.questManagerPlugin.getPlayerManager().getPlayer((Player) e.getEntity());
		
		if (!qp.equals(this.player)) {
			return;
		}
		
		//a current charge has moved, and is not allowed to
		((Player) e.getEntity()).sendMessage(DISTURBED_MESSAGE);
		doneCasting();
	}
	
	@EventHandler
	public void onItemSwitch(PlayerItemHeldEvent e) {
		if (canChangeItems) {
			return;
		}
		
		if (!player.getPlayer().isOnline()) {
			return;
		}
		
		if (!e.getPlayer().getUniqueId().equals(player.getPlayer().getUniqueId())) {
			return;
		}
		
		if (!(QuestManagerPlugin.questManagerPlugin.getPluginConfiguration().getWorlds()
				.contains(e.getPlayer().getWorld().getName()))) {
			return;
		}
		
		//not allowed to switch weapons, cheater!
		doneCasting();
		e.getPlayer().sendMessage(DISTURBED_MESSAGE);
	}
	
	public void alarm(Integer a) {
		doneCasting();
		action.onAction();
	}
}
