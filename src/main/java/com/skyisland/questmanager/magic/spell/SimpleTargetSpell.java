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

package com.skyisland.questmanager.magic.spell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.LivingEntity;

import com.skyisland.questmanager.QuestManagerPlugin;
import com.skyisland.questmanager.magic.MagicUser;
import com.skyisland.questmanager.magic.spell.effect.SpellEffect;
import com.skyisland.questmanager.player.QuestPlayer;
import com.skyisland.questmanager.player.skill.event.MagicCastEvent;

public class SimpleTargetSpell extends TargetSpell {
	
	/**
	 * Registers this class as configuration serializable with all defined 
	 * {@link aliases aliases}
	 */
	public static void registerWithAliases() {
		for (aliases alias : aliases.values()) {
			ConfigurationSerialization.registerClass(SimpleTargetSpell.class, alias.getAlias());
		}
	}
	
	/**
	 * Registers this class as configuration serializable with only the default alias
	 */
	public static void registerWithoutAliases() {
		ConfigurationSerialization.registerClass(SimpleTargetSpell.class);
	}
	

	private enum aliases {
		DEFAULT(SimpleTargetSpell.class.getName()),
		LONG("SimpleTargetSpell"),
		SHORT("STargetSpell");
		
		private String alias;
		
		aliases(String alias) {
			this.alias = alias;
		}
		
		public String getAlias() {
			return alias;
		}
	}

	public static SimpleTargetSpell valueOf(Map<String, Object> map) {
		if (!map.containsKey("cost") || !map.containsKey("name") || !map.containsKey("description")
				|| !map.containsKey("speed") || !map.containsKey("maxdistance") 
				|| !map.containsKey("effects") || !map.containsKey("difficulty")) {
			QuestManagerPlugin.logger.warning(
					"Unable to load spell " 
						+ (map.containsKey("name") ? (String) map.get("name") : "")
						+ ": Missing some keys!"
					);
			return null;
		}
		
		SimpleTargetSpell spell = new SimpleTargetSpell(
				(int) map.get("cost"),
				(int) map.get("difficulty"),
				(String) map.get("name"),
				(String) map.get("description"),
				(double) map.get("speed"),
				(int) map.get("maxdistance")
				);
		
		@SuppressWarnings("unchecked")
		List<SpellEffect> effects = (List<SpellEffect>) map.get("effects");
		effects.forEach(spell::addSpellEffect);
		
		if (map.containsKey("projectileeffect")) {
			spell.setProjectileEffect(Effect.valueOf((String) map.get("projectileeffect")));
		}
		if (map.containsKey("contacteffect")) {
			spell.setContactEffect(Effect.valueOf((String) map.get("contacteffect")));
		}
		if (map.containsKey("castsound")) {
			spell.setCastSound(Sound.valueOf((String) map.get("castsound")));
		}
		if (map.containsKey("contactsound")) {
			spell.setContactSound(Sound.valueOf((String) map.get("contactsound")));
		}
		
		return spell;
	}
	
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		
		map.put("cost", getCost());
		map.put("name", getName());
		map.put("description", getDescription());
		map.put("speed", speed);
		map.put("maxdistance", maxDistance);
		
		map.put("effects", getSpellEffects());
		
		if (contactEffect != null) {
			map.put("contacteffect", contactEffect.name());
		}
		if (castSound != null) {
			map.put("castsound", castSound.name());
		}if (projectileEffect != null) {
			map.put("projectileeffect", projectileEffect.name());
		}
		if (contactSound != null) {
			map.put("contactsound", contactSound.name());
		}
		
		return map;
	}
	
	private double speed;
	
	private int maxDistance;
	
	private Effect projectileEffect;
	
	private Effect contactEffect;
	
	private Sound castSound;
	
	private Sound contactSound;
	
	public SimpleTargetSpell(int cost, int difficulty, String name, String description, double speed,
			int maxDistance) {
		super(cost, difficulty, name, description);
		this.speed = speed;
		this.maxDistance = maxDistance;
		this.projectileEffect = null;
		this.contactEffect = null;
		this.castSound = null;
		this.contactSound = null;
	}

	public void setProjectileEffect(Effect projectileEffect) {
		this.projectileEffect = projectileEffect;
	}



	public void setContactEffect(Effect contactEffect) {
		this.contactEffect = contactEffect;
	}



	public void setCastSound(Sound castSound) {
		this.castSound = castSound;
	}



	public void setContactSound(Sound contactSound) {
		this.contactSound = contactSound;
	}

	@Override
	public void cast(MagicUser caster) {
		if (caster instanceof QuestPlayer) {
			QuestPlayer player = (QuestPlayer) caster;
			MagicCastEvent event = new MagicCastEvent(player,
									MagicCastEvent.MagicType.MAGERY,
									this
							);
			Bukkit.getPluginManager().callEvent(event);
			
			if (event.isFail()) {
				fail(caster);
				return;
			}
			
		}
		
		new SpellProjectile(this, caster, caster.getEntity().getLocation().clone().add(0,1.5,0), 
			caster.getEntity().getLocation().getDirection(), speed, maxDistance, projectileEffect);

		if (castSound != null) {
			caster.getEntity().getWorld().playSound(caster.getEntity().getLocation(), castSound, 1, 1);
		}
	}

	@Override
	protected void onBlockHit(MagicUser caster, Location loc) {
		for (SpellEffect effect : getSpellEffects()) {
			effect.apply(loc, caster);
		}
		
		if (contactEffect != null) {
			loc.getWorld().playEffect(loc, contactEffect, 0);
		}
		if (contactSound != null) {
			loc.getWorld().playSound(loc, contactSound, 1, 1);
		}
	}

	@Override
	protected void onEntityHit(MagicUser caster, LivingEntity target) {
		//do effects
		
		for (SpellEffect effect : getSpellEffects()) {
			effect.apply(target, caster);
		}
		
		if (contactEffect != null) {
			target.getWorld().playEffect(target.getEyeLocation(), contactEffect, 0);
		}
		if (contactSound != null) {
			target.getWorld().playSound(target.getEyeLocation(), contactSound, 1, 1);
		}
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public int getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(int maxDistance) {
		this.maxDistance = maxDistance;
	}

	public Effect getProjectileEffect() {
		return projectileEffect;
	}

	public Effect getContactEffect() {
		return contactEffect;
	}

	public Sound getCastSound() {
		return castSound;
	}

	public Sound getContactSound() {
		return contactSound;
	}
}
