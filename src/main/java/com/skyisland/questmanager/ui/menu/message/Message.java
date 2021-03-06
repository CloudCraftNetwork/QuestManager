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

package com.skyisland.questmanager.ui.menu.message;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import com.skyisland.questmanager.fanciful.FancyMessage;

/**
 * Holds a messaged used with a menu.
 *
 */
public abstract class Message implements ConfigurationSerializable {
	
	protected FancyMessage sourceLabel;
	
	public void setSourceLabel(FancyMessage label) {
		this.sourceLabel = label;
	}
	
	public FancyMessage getSourceLabel() {
		return sourceLabel;
	}
	
	public abstract FancyMessage getFormattedMessage();
}
