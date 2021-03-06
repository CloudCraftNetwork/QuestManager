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

package com.skyisland.questmanager.magic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.collect.Lists;
import com.skyisland.questmanager.QuestManagerPlugin;
import com.skyisland.questmanager.configuration.utils.YamlWriter;
import com.skyisland.questmanager.magic.spell.effect.FireEffect;
import com.skyisland.questmanager.magic.spell.effect.HealEffect;
import com.skyisland.questmanager.magic.spell.effect.ImbuementEffect;
import com.skyisland.questmanager.player.QuestPlayer;
import com.skyisland.questmanager.player.skill.defaults.ImbuementSkill;

/**
 * Stores imbuements and the items they come from, and handle
 *
 */
public class ImbuementHandler {

	private Map<Material, Map<String, Double>> materialMap;
	
	private Map<String, ImbuementEffect> effectMap;
	
	private double minimumPotency;
	
	private int maxSlots;
	
	private boolean useSkill;
	
	private int skillSlotRate;
	
	private double bonusPotency;
	
	/**
	 * Secret variable that gives reference to the imbuement skill
	 */
	private ImbuementSkill skillLink;
		
	public ImbuementHandler(File configFile) {
		if (configFile == null) {
			return;
		}
		if (!configFile.exists()) {
			createConfig(configFile);
		}
		
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		
		if (!config.getBoolean("enabled", true)) {
			return;
		}
		
		this.minimumPotency = config.getDouble("minimumPotency", 0);
		this.maxSlots = config.getInt("maxSlots", 5);
		this.useSkill = config.getBoolean("useSkill", true);
		this.skillSlotRate = config.getInt("skillSlotRate", 25);
		this.bonusPotency = config.getDouble("bonusPotency", 0.01);
		this.effectMap = new HashMap<>();
		this.materialMap = new HashMap<>();
		if (config.contains("effects")) {
			ConfigurationSection sex = config.getConfigurationSection("effects");
			for (String key : sex.getKeys(false)) {
				try {
					effectMap.put(key, (ImbuementEffect) sex.get(key));
					effectMap.get(key).setDisplayName(YamlWriter.toStandardFormat(key));
				} catch (ClassCastException e) {
					QuestManagerPlugin.logger
						.warning("Failed to register effect " + key + " due to a bad cast");
					QuestManagerPlugin.logger
						.warning(" > Is this class a member of the ImbuementEffect's?");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		if (config.contains("items")) {
			ConfigurationSection sex = config.getConfigurationSection("items");
			ConfigurationSection sub;
			Map<String, Double> submap;
			Material mat = null;
			for (String key : sex.getKeys(false)) {
				
				try {
					mat = Material.valueOf(key);
				} catch (Exception e) {
					e.printStackTrace();
					QuestManagerPlugin.logger
						.warning("Couldn't match Material: " + key);
					continue;
				}
				
				sub = sex.getConfigurationSection(key);
				submap = new HashMap<>();
				
				for (String type : sub.getKeys(false)) {
					submap.put(type, sub.getDouble(type));
				}
				
				materialMap.put(mat, submap);
			}
		}
		
		
	}
	
	private YamlConfiguration createConfig(File configFile) {
		if (!configFile.exists()) {
			
			YamlWriter writer = new YamlWriter();
			
			writer.addLine("enabled", true, Lists.newArrayList("Turns the entire imbuement mechanic off or on (not skill)", "true | false"))
				.addLine("minimumPotency", 0, Lists.newArrayList("What level of potency should be required for an effect to", "even be applied", "[double] 0 or greater"))
				.addLine("maxSlots", 4, Lists.newArrayList("The maximum number of imbuement slots ever atainable by players", "[int] greater than 0"))
				.addLine("useSkill", true, Lists.newArrayList("Whether or not to use the default Imbuement skill to determine", "a player's ability (number of slots, efficiency, etc) with imbuement.", "If set to false, players will the max imbuement slots automatically", "true | false"))
				.addLine("skillSlotRate", 25, Lists.newArrayList("If useSkill is true, how many levels in the Imbuement skill are", "needed before gaining another imbuement slot.","If useSkill is false , this is ignored", "[int] greater than 0"))
				.addLine("bonusPotency", 0.01, Lists.newArrayList("Bonus potency given per Imbuement level", "does nothing if useSkill is false", "true | false"));
			
				Map<String, ImbuementEffect> effectExample = new HashMap<>();
				effectExample.put("fire", new FireEffect(15));
				effectExample.put("heal", new HealEffect(50));
				writer.addLine("effects", effectExample, Lists.newArrayList("Imbuements add effects to items. To let it know what is", "a good base for these effects, you must define them here.", "The names set up here are used later when deciding", "what effects an item type has.", "At 1.0 potency, the listed effects will happen", "[name]: [effect] where name is your creation", "and effect is a valid ImbuementEffect (see documentation)"));
			
				Map<String, Map<String, Double>> example = new HashMap<>();
				Map<String, Double> submap = new HashMap<>();
				submap.put("fire", 0.2);
				example.put("APPLE", submap);
				submap = new HashMap<>();
				submap.put("fire", 0.4);
				submap.put("heal", 0.25);
				example.put("BONE", submap);
				
				writer.addLine("items", example, Lists.newArrayList("A map between a type of item and the imbuement effects", "it contains. The imbuement types must be those defined in the effects section", "[MATERIAL]: Map<String, Double>"));
			
			try {
				writer.save(configFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return writer.buildYaml();
		}
		
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		return config;
	}
	
	public void setImbuementSkill(ImbuementSkill skill) {
		this.skillLink = skill;
	}
	
	public ImbuementSkill getImbuementSkill() {
		return skillLink;
	}

	public double getMinimumPotency() {
		return minimumPotency;
	}

	public int getMaxSlots() {
		return maxSlots;
	}

	public boolean isUseSkill() {
		return useSkill;
	}

	public int getSkillSlotRate() {
		return skillSlotRate;
	}

	public double getBonusPotency() {
		return bonusPotency;
	}
	
	/**
	 * Returns a list of effects associated with the given item type.
	 * No information about potency is provided. To get a list of effects to apply to
	 * an imbued item, use the {@link #getCombinedEffects(Material...)} method instead
	 */
	public List<ImbuementEffect> getAssociatedEffects(Material type) {
		if (type == null || materialMap == null || !materialMap.containsKey(type)) {
			return null;
		}
		
		Set<String> effectNames = materialMap.get(type).keySet();
		List<ImbuementEffect> effects = new ArrayList<>(effectNames.size());
		
		if (effectNames.isEmpty()) {
			return effects;
		}
		
		for (String name : effectNames) {
			effects.add(effectMap.get(name));
		}
		
		return effects;
	}
	
	/**
	 * Provides a list of effects obtained as if a player with 0 bonus imbued them together.
	 */
	public ImbuementSet getCombinedEffects(Material ... types) {
		return getCombinedEffects(0.0, types);
	}
	
	public ImbuementSet getCombinedEffects(double bonusPotency, Material ... types) {
		Set<Material> applicable = getApplicableMaterials(types);
		if (applicable.isEmpty()) {
			return new ImbuementSet(new HashMap<>());
		}
		
		Map<String, Double> totals = new HashMap<>(applicable.size());
		
		Map<String, Double> typeEffects;
		for (Material type : applicable) {
			typeEffects = materialMap.get(type);
			for (String effectName : typeEffects.keySet()) {
				if (totals.containsKey(effectName)) {
					//just add potency
					totals.put(effectName, totals.get(effectName) + typeEffects.get(effectName));
				} else {
					totals.put(effectName, typeEffects.get(effectName));
				}
			}
		}
		
		//went thorugh and total'ed each up. Now create effects, return
		Map<ImbuementEffect, Double> effects = new HashMap<>();
		
		for (String key : totals.keySet()) {
			effects.put(effectMap.get(key).getCopyAtPotency(totals.get(key)), totals.get(key));
		}
		
		
		return new ImbuementSet(effects);
	}
	
	public ImbuementSet getCombinedEffects(QuestPlayer player, Material ... types) {
		return getCombinedEffects(getPotencyBonus(player), types);
	}
	
	/**
	 * Sifts through the given types and returns a set of types with associated ImbuementEffects.
	 * @return an empty set if types is null or empty, or the set of all types defined in the config;
	 */
	public Set<Material> getApplicableMaterials(Material[] types) {
		if (types == null || types.length == 0) {
			return new HashSet<>(1);
		}
		
		Set<Material> list = new HashSet<>();
		for (Material type : types) {
			if (materialMap.containsKey(type)) {
				list.add(type);
			}
		}
		
		return list;
	}
	
	/**
	 * Calculates the bonus to potency the given player would have. This is based on the Imbuement skill.
	 * If useSkill is disabled, returns 0.
	 */
	public double getPotencyBonus(QuestPlayer player) {
		if (!useSkill) {
			return 0.0;
		}
		
		if (skillLink == null) {
			return 0.0;
		}
				
		int lvl = player.getSkillLevel(skillLink);
		return lvl * bonusPotency;
	}
	
	/**
	 * Calculates how many slots a player should have. This is basedon the Imbuement skill.
	 * If useSkill is disabled, returns maxSlots.
	 */
	public int getImbuementSlots(QuestPlayer player) {
		if (!useSkill) {
			return maxSlots;
		}
		
		if (skillLink == null) {
			return maxSlots;
		}
		
		int lvl = player.getSkillLevel(skillLink);
		return 1 + (lvl / skillSlotRate);
	}
}
