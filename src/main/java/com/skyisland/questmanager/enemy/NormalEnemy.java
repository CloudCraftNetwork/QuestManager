package com.skyisland.questmanager.enemy;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import com.skyisland.questmanager.QuestManagerPlugin;
import com.skyisland.questmanager.loot.Loot;
import com.skyisland.questmanager.loot.Lootable;

/**
 * Enemy type with very limited, straightforward customization; namely attributes.
 * Also supports loot specification
 * @author Skyler
 *
 */
public class NormalEnemy extends Enemy implements Lootable, Listener {
	
	/**
	 * Registers this class as configuration serializable with all defined 
	 * {@link aliases aliases}
	 */
	public static void registerWithAliases() {
		for (aliases alias : aliases.values()) {
			ConfigurationSerialization.registerClass(NormalEnemy.class, alias.getAlias());
		}
	}
	
	/**
	 * Registers this class as configuration serializable with only the default alias
	 */
	public static void registerWithoutAliases() {
		ConfigurationSerialization.registerClass(NormalEnemy.class);
	}
	

	private enum aliases {
		DEFAULT(NormalEnemy.class.getName()),
		SIMPLE("NormalEnemy");
		
		private String alias;
		
		aliases(String alias) {
			this.alias = alias;
		}
		
		public String getAlias() {
			return alias;
		}
	}
	
	protected double hp;
	
	protected double attack;
	
	protected List<Loot> loot;
	
//	protected String type;
	
	public NormalEnemy(String name, EntityType type, double hp, double attack) {
		super(name, type);
		this.hp = hp;
		this.attack = attack;
		this.loot = new LinkedList<>();
		
		Bukkit.getPluginManager().registerEvents(this, QuestManagerPlugin.questManagerPlugin);
	}
	
	public NormalEnemy(String name, EntityType type, double hp, double attack, Collection<Loot> loot) {
		this(name, type, hp, attack);
		this.loot.addAll(loot);
	}
	
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		
		map.put("type", type.name());
		map.put("name", name);
		map.put("hp", hp);
		map.put("attack", attack);
		map.put("loot", loot);
		
		return map;
	}
	
	@SuppressWarnings("unchecked")
	public static NormalEnemy valueOf(Map<String, Object> map) {
		
		EntityType type;
		try {
			type = EntityType.valueOf(((String) map.get("type")).toUpperCase());
		} catch (Exception e) {
			QuestManagerPlugin.logger.warning("Unable to get EntityType " + 
					(String) map.get("type") + ", so defaulting to ZOMBIE");
			type = EntityType.ZOMBIE;
		}
		String name = (String) map.get("name");
		Double hp = (Double) map.get("hp");
		Double attack = (Double) map.get("attack");
		
		List<Loot> loot = null;
		if (map.containsKey("loot")) {
			try {
				loot = (List<Loot>) map.get("loot");
			} catch (Exception e) {
				e.printStackTrace();
				QuestManagerPlugin.logger.warning("Failed to get loot list from "
						+ "config for NormalEnemy " + type.name() + " - " + name + ". Resorting to default loot.");
			}
		}
		
		if (loot != null) {
			return new NormalEnemy(name, type, hp, attack, loot);
		}
		
		return new NormalEnemy(name, type, hp, attack);
	}
	
	@Override
	public List<Loot> getLoot() {
		return loot;
	}
	
	public void addLoot(Loot loot) {
		this.loot.add(loot);
	}
	
	@Override
	protected void handleDeath(EntityDeathEvent event) {
		//on death, drop loot (if we have any). otherwise, don't
		if (loot != null && !loot.isEmpty()) {
			event.getDrops().clear();
			event.getDrops().add(
					Lootable.pickLoot(loot).getItem()
					);
		}
	}

	@Override
	protected void spawnEntity(Entity base) {
		if (!(base instanceof LivingEntity)) {
			return;
		}
		
		LivingEntity entity = (LivingEntity) base;
		entity.setMaxHealth(hp);
		entity.setHealth(hp);
		AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
				
		if (attribute != null)
			attribute.setBaseValue(attack);
		else
			System.out.println("no GENERIC ATTACK for entity " + base.getType());
		
		entity.getEquipment().setItemInMainHandDropChance(0f);
	}
}
