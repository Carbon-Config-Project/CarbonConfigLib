package carbonconfiglib.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import carbonconfiglib.api.IEntrySettings;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.MultilinePolicy;
import carbonconfiglib.utils.SyncType;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.maps.impl.hash.Object2ObjectLinkedOpenHashMap;
import speiger.src.collections.objects.maps.interfaces.Object2ObjectMap;

/**
 * Copyright 2023 Speiger, Meduris
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class Config {
	private String name;
	private Object2ObjectMap<String, ConfigSection> sections = new Object2ObjectLinkedOpenHashMap<>();
	
	public Config(String name) {
		if (Helpers.validateString(name)) throw new IllegalArgumentException("Config name must not be null, empty or start/end with white spaces");
		this.name = name;
	}
	
	public ConfigSection add(ConfigSection section) {
		if (section.getParent() != null) throw new IllegalStateException("ConfigSection must not be added to multiple sections. Section: " + section.getName());
		sections.putIfAbsent(section.getName(), section);
		return section;
	}
	
	public ConfigSection add(String name) {
		return sections.computeIfAbsent(name, ConfigSection::new).setUsed();
	}
	
	public ConfigSection getSection(String name) {
		return sections.get(name);
	}
	
	public void applyEntrySettings(IEntrySettings settings, Class<?> type, boolean children) {
		for(ConfigSection section : sections.values()) {
			section.applyEntrySettings(settings, type, children);
		}
	}
	
	ConfigSection getSectionRecursive(String[] names) {
		if (names.length == 0) return null;
		ConfigSection section = sections.computeIfAbsent(names[0], ConfigSection::new);
		for (int i = 1; i < names.length && section != null; i++) {
			section = section.parseSubSection(names[i]);
		}
		return section;
	}
	
	public List<ConfigSection> getChildren() {
		return new ObjectArrayList<>(sections.values());
	}
	
	public Map<String, ConfigEntry<?>> getSyncedEntries(SyncType type) {
		if(type == SyncType.NONE) return Collections.emptyMap();
		Map<String, ConfigEntry<?>> result = new Object2ObjectLinkedOpenHashMap<>();
		for (ConfigSection section : sections.values()) {
			section.getSyncedEntries(result, type);
		}
		return result;
	}
	
	public String getName() {
		return name;
	}
	
	public Config copy() {
		Config config = new Config(name);
		for(ConfigSection sub : sections.values()) {
			config.add(sub.copy());
		}
		return config;
	}
	
	public void resetDefault() {
		sections.values().forEach(ConfigSection::resetDefault);
	}
	
	public boolean hasChanged() {
		for(ConfigSection section : sections.values()) {
			if(section.hasChanged()) return true;
		}
		return false;
	}
	
	public boolean isDefault() {
		for(ConfigSection section : sections.values()) {
			if(!section.isDefault()) return false;
		}
		return true;
	}
	
	public String serialize(MultilinePolicy policy) {
		if (sections.size() == 0) return "";
		StringJoiner joiner = new StringJoiner("\n\n");
		for(ConfigSection section : sections.values()) {
			String val = section.serialize(policy);
			if(val != null) joiner.add(val);			
		}
		return joiner.toString();
	}
}
