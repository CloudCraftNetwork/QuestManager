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

package com.skyisland.questmanager.ui.menu.inventory.minigames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.skyisland.questmanager.QuestManagerPlugin;
import com.skyisland.questmanager.configuration.utils.YamlWriter;
import com.skyisland.questmanager.effects.ChargeEffect;
import com.skyisland.questmanager.fanciful.FancyMessage;
import com.skyisland.questmanager.player.QuestPlayer;
import com.skyisland.questmanager.player.skill.QualityItem;
import com.skyisland.questmanager.player.skill.defaults.MiningSkill;
import com.skyisland.questmanager.scheduling.Alarm;
import com.skyisland.questmanager.scheduling.Alarmable;
import com.skyisland.questmanager.ui.menu.inventory.CloseableGui;
import com.skyisland.questmanager.ui.menu.inventory.GuiInventory;
import com.skyisland.questmanager.ui.menu.inventory.InventoryItem;

public class MiningGui extends GuiInventory implements Alarmable<Integer>, CloseableGui {
	
	private enum State {
		STOPPED,
		STARTING,
		RUNNING,
		ENDED;
	}
	
	public static MiningSkill skillLink = null;
	
	public static final Random RANDOM = new Random();
	
	public static final ItemStack DEFAULT_HIDDEN_ICON = new ItemStack(Material.BEDROCK);
	
	{
		ItemMeta meta;
		
		meta = DEFAULT_HIDDEN_ICON.getItemMeta();
		meta.setDisplayName("???");
		DEFAULT_HIDDEN_ICON.setItemMeta(meta);
	}
	
	public static final Sound MINING_SOUND = Sound.BLOCK_GRAVEL_BREAK;
	
	public static final Sound MINING_HIT_SOUND = Sound.BLOCK_STONE_BREAK;
	
	public static final Sound LOSE_SOUND = Sound.ENTITY_CAT_DEATH;
	
	public static final Sound WIN_SOUND = Sound.ENTITY_PLAYER_LEVELUP;
	
	public static final Sound CLICK_SOUND = Sound.UI_BUTTON_CLICK;
	
	public static final String LOSE_MESSAGE = ChatColor.RED + "You failed to find anything useable";
	
	public static final String TOO_LITTLE_MESSAGE = ChatColor.RED + "You didn't find enough material to be worth anything";
	
	public static final String WIN_MESSAGE = "Success!" + ChatColor.RESET + " You successfully mined ";
	
	public static final Sound START_SOUND = Sound.ITEM_ARMOR_EQUIP_IRON;
	
	public static final Sound HIT_SOUND_STONE = Sound.BLOCK_IRON_DOOR_CLOSE;
	
	public static final Sound HIT_SOUND_DIRT = Sound.BLOCK_GRAVEL_BREAK;
	
	public static final Sound HIT_SOUND_OBSIDIAN = Sound.BLOCK_ANVIL_PLACE;
	
	public static final Sound ORE_BREAK_SOUND = Sound.BLOCK_GLASS_BREAK;
	
	private static final ChargeEffect SUCCESS_EFFECT = new ChargeEffect(Effect.HAPPY_VILLAGER);
	
	private static final ChargeEffect FAIL_EFFECT = new ChargeEffect(Effect.SMALL_SMOKE);
	
	private enum BlockMaterial {
		AIR(null),
		DIRT(new ItemStack(Material.DIRT)),
		STONE(new ItemStack(Material.STONE)),
		OBSIDIAN(new ItemStack(Material.OBSIDIAN)),
		ORE(null);
		
		private ItemStack icon;
		
		BlockMaterial(ItemStack icon) {
			this.icon = icon;
		}
		
		public ItemStack getIcon() {
			return this.icon;
		}
		
		public void setIcon(ItemStack icon) {
			this.icon = icon;
		}
	}
	
	private String invName;
	
	private Player player;
	
	private Inventory inv;
	
	private double averageHardness;
	
	private double hardnessDeviation;
	
	private int startingSpots;
	
	private int blockHits;
	
	private int maxBlockHits;
	
	private double bonusQuality;
	
	private Map<Integer, BlockMaterial> backend;
	
	private Map<Integer, Boolean> hiddenMap;
	
	private QualityItem inputOre;
	
	private ItemStack hiddenIcon;
	
	private int skillLevel;
	
	private int depth;
	
	private BossBar displayBar;
	
	private List<Integer> oreSlots;
	
	private QualityItem result;
	
	private int oreCount;
	
	private State gameState;
	
	public MiningGui(Player player, QualityItem result, int skillLevel, int oreCount, int depth, int blockHits,
			double averageHardness,	double hardnessDeviation, int startingSpots, double bonusQuality, ItemStack oreIcon) {
		this(player, result, skillLevel, oreCount, depth, blockHits, averageHardness, hardnessDeviation, 
				startingSpots, bonusQuality, oreIcon, "Mining - " + player.getUniqueId().toString().substring(0, 8));
	}
	
	public MiningGui(Player player, QualityItem result, int skillLevel, int oreCount, int depth, int blockHits,
			double averageHardness,	double hardnessDeviation, int startingSpots, double bonusQuality,
			ItemStack oreIcon, String name) {
		this.player = player;
		this.invName = name;
		this.inputOre = result;
		this.skillLevel = skillLevel;
		this.blockHits = this.maxBlockHits = blockHits;
		this.averageHardness = averageHardness;
		this.hardnessDeviation = hardnessDeviation;
		this.depth = Math.min(Math.max(depth, 1), 6);
		this.result = null;
		this.oreCount = oreCount;
		this.bonusQuality = bonusQuality;
		this.startingSpots = Math.max(1, startingSpots);
		
		this.backend = new HashMap<>();
		this.hiddenMap = new HashMap<>();
		this.oreSlots = new LinkedList<>();
		
		this.hiddenIcon = DEFAULT_HIDDEN_ICON;
		BlockMaterial.ORE.setIcon(oreIcon);
				
		int size = 9 * this.depth;
		this.inv = Bukkit.createInventory(null, size, invName);
		
		this.gameState = State.STOPPED;
		
	}
	
	@Override
	public Inventory getFormattedInventory(QuestPlayer player) {
				
		return inv;
	}

	@Override
	public Map<String, Object> serialize() {
		return null; //Runtime only, not serializable
	}

	@Override
	public InventoryItem getItem(int pos, InventoryAction action) {
		if (this.backend == null) {
			return null;
		}
		
		if (this.gameState != State.RUNNING) {
			return null;
		}
		
		
		/*
		 * Check slot. If mystery, just cancel.
		 * If top or empty slot, ignore
		 * otherwise, break block if dirt or break it down and update
		 */
		int size = 9 * depth;
		
		if (pos > size) {
			return null;
		}
		
		if (hiddenMap.get(pos)) {
			return null;
		}
		
		//somewhere in the ground
		if (backend.containsKey(pos) && backend.get(pos) != BlockMaterial.AIR) {
			performHit(pos);
			update();
			return null;
		}
		
		
		return null; //tell it to do nothing
	}
	
	private void performHit(int pos) {
		this.blockHits--;

		//break a cross section
		//inv.setItem(pos, null);
		
		int cache;
		
		if (player.isOnline())
		switch (backend.get(pos)) {
		case AIR:
		case DIRT:
		case ORE:
			player.playSound(player.getLocation(), HIT_SOUND_DIRT, 1, 1);
			break;
		case STONE:
			player.playSound(player.getLocation(), HIT_SOUND_STONE, 1, 1);
			break;
		case OBSIDIAN:
		default:
			player.playSound(player.getLocation(), HIT_SOUND_OBSIDIAN, 1, 1);
		}
		
		hitBlock(pos);
		for (int i = 1; i < 10; i += 8)
		for (int j = -1; j < 2; j += 2) {
			cache = pos + (j * i);
			if (cache < 0 || cache >= depth * 9) {
				continue;
			}
			if (Math.abs((cache % 9) - (pos % 9)) > 1) {
				continue; //horizontal wrap
			}
			hitBlock(cache);
		}
		
	}
	
	/**
	 * Hits the block (just the block) at the given position, possibly breaking it. Reveals surrounding
	 * blocks
	 */
	private void hitBlock(int pos) {
		switch (backend.get(pos)) {
		case AIR:
		case DIRT:
			backend.put(pos, BlockMaterial.AIR);
			break;
		case ORE:
			backend.put(pos, BlockMaterial.AIR);
			if (player.isOnline())
				player.playSound(player.getLocation(), ORE_BREAK_SOUND, 1, 1);
			break;
		case STONE:
			backend.put(pos, BlockMaterial.DIRT);
			break;
		case OBSIDIAN:
		default:
			backend.put(pos, BlockMaterial.STONE);
		}
		
		hiddenMap.put(pos, false);
		inv.setItem(pos, backend.get(pos).getIcon());
		
		int cache;
		for (int i = 1; i < 10; i += 8)
		for (int j = -1; j < 2; j += 2) {
			cache = pos + (j * i);
			if (cache < 0 || cache >= depth * 9) {
				continue;
			}
			if (Math.abs((cache % 9) - (pos % 9)) > 1) {
				continue; //horizontal wrap
			}
			if (hiddenMap.get(cache)) {
				hiddenMap.put(cache, false);
				inv.setItem(cache, backend.get(cache).getIcon());
			}
		}
		
	}
	
	public void start() {
		if (gameState != State.STOPPED) {
			return;
		}

		//initialize blocks
		int max = depth * 9, cur;
		for (int i = 0; i < max; i++) {
			hiddenMap.put(i, true);
			cur = (int) Math.round((RANDOM.nextGaussian() * hardnessDeviation) + averageHardness);
			if (cur == 1)
				backend.put(i, BlockMaterial.STONE);
			else if (cur >= 2)
				backend.put(i, BlockMaterial.OBSIDIAN);
			else
				backend.put(i, BlockMaterial.DIRT);
			
			
			inv.setItem(i, hiddenIcon);
		}
		
		
		//pick spot to put first piece of ore from row 1+
		Queue<Integer> newSpots = new LinkedList<>();
		List<Integer> dump;
		newSpots.add(RANDOM.nextInt(max));
		
		oreSlots = new ArrayList<>(oreCount);
		int len, buf;
		while (oreSlots.size() < oreCount) {
			len = RANDOM.nextInt(newSpots.size()) + 1;
			for (int index = 0; index < len && oreSlots.size() < oreCount; index++) {
				//some RANDOM i times, expand those items
				cur = newSpots.remove();
				
				oreSlots.add(cur);
				backend.put(cur, BlockMaterial.ORE);
				//inv.setItem(cur, BlockMaterial.ORE.getIcon());
				//get spots around it and add them
				for (int i = 1; i < 10; i += 8)
				for (int j = -1; j < 2; j += 2) {
					buf = cur + (i * j);
					if (buf < 0 || buf >= depth * 9) {
						continue;
					}
					if (Math.abs((buf % 9) - (cur % 9)) > 1) {
						continue; //horizontal wrap
					}
					if (backend.get(buf) == BlockMaterial.ORE)
						continue;
					else {
						newSpots.add(buf);
					}
				}
			}
			
			dump = new ArrayList<>(newSpots);
			Collections.shuffle(dump);
			newSpots.clear();
			newSpots.addAll(dump);
		}
		
		Alarm.getScheduler().schedule(this, 0, 1);
		player.getPlayer().sendMessage(ChatColor.RED + "Get ready...");
		player.playSound(player.getLocation(), START_SOUND, 1, 1);
		
		this.gameState = State.STARTING;
		
	}
	
	private void update() {
		//win if we need to, and update bar
		if (this.blockHits <= 0) {
			this.loseGame();
			return;
		}
		
		this.displayBar.setProgress((float) blockHits / (float) maxBlockHits);
		int ores = isWon();
		if (ores != -1) {
			winGame(ores);
		}
	}
	
	/**
	 * Returns the number of intact ore blocks, or -1 if they are not separated.
	 * If non-negative, the game is finished
	 */
	private int isWon() {
		//find first non-null ore slot
		int fullSlot = -1;
		for (int slot : oreSlots) {
			if (backend.get(slot) == BlockMaterial.ORE) {
				fullSlot = slot;
				break;
			}
		}
		
		//if fullSlot is still -1, all ore was lost
		if (fullSlot == -1) {
			//loseGame();
			return 0;
		}
		
		//go through and check all neighbors to see if all are null
		Queue<Integer> searchList = new LinkedList<>();
		Set<Integer> doneList = new HashSet<>(oreCount);
		
		searchList.add(fullSlot);
		int cur;
		while (!searchList.isEmpty()) {
//			if (searchList.isEmpty()) {
//				break;
//			}
			cur = searchList.remove();
			
			if (backend.get(cur) == BlockMaterial.AIR) {
				continue;
			}
			
			if (backend.get(cur) != BlockMaterial.ORE) {
				return -1;
			}
			
			doneList.add(cur);
			
			if (cur % 9 != 0) 
			if (!doneList.contains(cur - 1) && !searchList.contains(cur - 1)) {
					searchList.add(cur - 1);
			}
			
			if (cur % 9 != 8) 
			if (!doneList.contains(cur + 1) && !searchList.contains(cur + 1)) {
					searchList.add(cur + 1);
			}
			
			if (cur / 9 > 1) 
			if (!doneList.contains(cur - 9) && !searchList.contains(cur - 9)) {
					searchList.add(cur - 9);
			}
			
			if (cur / 9 < depth - 1) 
			if (!doneList.contains(cur + 9) && !searchList.contains(cur + 9)) {
					searchList.add(cur + 9);
			}
			
		}
		
		return doneList.size();
	}

	
	private void loseGame() {
		if (player == null) {
			return;
		}
		
		if (!player.isOnline()) {
			return;
		}
		
		this.gameState = State.STOPPED;
		
		
		QuestPlayer qp = QuestManagerPlugin.questManagerPlugin.getPlayerManager().getPlayer(player);
		
		if (skillLink != null) {
			int range = QuestManagerPlugin.questManagerPlugin.getPluginConfiguration().getSkillCutoff();
			skillLink.perform(qp, Math.max(qp.getSkillLevel(skillLink) - range, Math.min(qp.getSkillLevel(skillLink) + range, skillLevel)), true);
		}
		
		//player.sendMessage(LOSE_MESSAGE);
		player.getWorld().playSound(player.getLocation(), LOSE_SOUND, 1, 1);
		player.sendMessage(LOSE_MESSAGE);


		clean();
		player.closeInventory();
		
		
	}
	
	private void winGame(int oreBlocks) {
		if (player == null) {
			return;
		}
		
		if (!player.isOnline()) {
			return;
		}
		
		if (oreBlocks == 0) {
			loseGame();
			return;
		}

		this.gameState = State.STOPPED;
		QuestPlayer qp = QuestManagerPlugin.questManagerPlugin.getPlayerManager().getPlayer(player);
		
		
		if (skillLink != null) {
			int range = QuestManagerPlugin.questManagerPlugin.getPluginConfiguration().getSkillCutoff();
			skillLink.performMajor(qp, Math.max(qp.getSkillLevel(skillLink) - range, Math.min(qp.getSkillLevel(skillLink) + range, skillLevel)), false);
		}
		
		FancyMessage msg;
		if (inputOre == null) {
			msg = new FancyMessage("Despite your effects, you found no useable ore!")
					.color(ChatColor.YELLOW);
		} else {
			double oreRatio = (double) oreBlocks / (double) oreCount;
			this.result = inputOre.clone();
			result.getUnderlyingItem().setAmount((int) (result.getUnderlyingItem().getAmount() * Math.round(oreRatio)));
			result.setQuality(inputOre.getQuality() * (oreRatio + bonusQuality));
			
			if (result.getUnderlyingItem().getAmount() <= 0)
				msg = new FancyMessage(TOO_LITTLE_MESSAGE);
			else {
				String name;
				if (inputOre.getItem().getItemMeta() == null || result.getItem().getItemMeta().getDisplayName() == null) {
					name = YamlWriter.toStandardFormat(result.getItem().getType().toString());
				} else {
					name = result.getItem().getItemMeta().getDisplayName();
				}
				
				msg = new FancyMessage(WIN_MESSAGE)
						.color(ChatColor.GREEN)
					.then(result.getItem().getAmount() > 1 ? result.getItem().getAmount() + "x " : "a ")
					.then("[" + name + "]")
						.color(ChatColor.DARK_PURPLE)
						.itemTooltip(result.getItem());
			}
		}
		
		msg.send(player);
		player.getWorld().playSound(player.getEyeLocation(), WIN_SOUND, 1, 1);
		
		this.inputOre = null;

		clean();
		player.closeInventory();
		
	}
	
	/**
	 * Returns the <i>inputOre the player didn't get!</i> If null, the player completed and fishing was
	 * successful, or null was passed in to begin with
	 */
	@Override
	public void onClose() {
		//return inputOre. If null, already was given
		ItemStack ret = (result == null ? null :result.getItem());
		
		if (backend != null) {
			loseGame();
			clean();
		}
		
		if (ret == null || ret.getAmount() <= 0) {
			//p.sendMessage(FishingGui.LOSE_MESSAGE);
			FAIL_EFFECT.play(player, null);
		} else {
			SUCCESS_EFFECT.play(player, null);
			if (!(player.getInventory().addItem(ret)).isEmpty()) {
				player.sendMessage(ChatColor.RED + "There is no space left in your inventory");
				player.getWorld().dropItem(player.getEyeLocation(), ret);
			}
		}
	}

	public static void setMiningSkill(MiningSkill skill) {
		skillLink = skill;
	}
	
	private void clean() {
		this.inv = null;
		this.hiddenIcon = null;
		if (backend != null)
			this.backend.clear();
		this.backend = null;
		if (displayBar != null)
			displayBar.removeAll();
		displayBar = null;
		this.inputOre = null;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MiningGui) {
			return ((MiningGui) o).player.getUniqueId().equals(player.getUniqueId());
		}
		
		return false;
	}
	
	@Override
	public void alarm(Integer key) {
		if (gameState == State.STARTING) {
			if (key >= startingSpots) {
				if (player.isOnline())
					player.sendMessage(ChatColor.GREEN + "Go!");
				this.gameState = State.RUNNING;
				displayBar = Bukkit.createBossBar("Ore Stability", BarColor.BLUE, BarStyle.SEGMENTED_20, new BarFlag[0]);
				displayBar.setProgress(1f);
				displayBar.addPlayer(player);
				return;
			}
				
				
			//pick a spots to reveal
			
			int cur = RANDOM.nextInt(depth * 9);
			if (backend.get(cur) == BlockMaterial.ORE || backend.get(cur) == BlockMaterial.AIR) {
				alarm(key);
			} else {
				//else reveal it
				if (player.isOnline())
					player.playSound(player.getLocation(), HIT_SOUND_STONE, 1, 1.5f);
				for (int j = 0; j < 3; j++)
					hitBlock(cur);
			}
			
			Alarm.getScheduler().schedule(this, key + 1, .5);
			
		}
	}
}
