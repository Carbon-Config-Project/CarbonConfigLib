package carbonconfiglib;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class Config {
	private String name;
	private Object2ObjectMap<String, ConfigSection> sections = new Object2ObjectLinkedOpenHashMap<>();
	
	public Config(String name) {
		if (Helpers.validateString(name))
			throw new IllegalArgumentException("Config name must not be null, empty or start/end with white spaces");
		this.name = name;
	}
	
	public ConfigSection add(ConfigSection section) {
		if (section.getParent() != null)
			throw new IllegalStateException("ConfigSection must not be added to multiple sections. Section: " + section.getName());
		sections.putIfAbsent(section.getName(), section);
		return section;
	}
	
	public ConfigSection add(String name) {
		ConfigSection section = sections.get(name);
		if(section == null)
		{
			section = new ConfigSection(name);
			sections.put(name, section);
		}
		return section.setUsed();
	}
	
	public ConfigSection getSection(String name) {
		return sections.get(name);
	}
	
	ConfigSection getSectionRecursive(String[] names) {
		if (names.length == 0)
			return null;
		ConfigSection section = sections.get(names[0]);
		if(section == null)
		{
			section = new ConfigSection(names[0]);
			sections.put(names[0], section);
		}
		for (int i = 1; i < names.length && section != null; i++) {
			section = section.parseSubSection(names[i]);
		}
		return section;
	}
	
	public List<ConfigSection> getChildren() {
		return new ObjectArrayList<>(sections.values());
	}
	
	public Map<String, ConfigEntry<?>> getSyncedEntries() {
		Map<String, ConfigEntry<?>> result = new Object2ObjectLinkedOpenHashMap<>();
		for (ConfigSection section : sections.values()) {
			section.getSyncedEntries(result);
		}
		return result;
	}
	
	public String getName() {
		return name;
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
	
	public String serialize() {
		if (sections.size() == 0)
			return "";

		StringJoiner joiner = new StringJoiner("\n\n");
		Object2ObjectMaps.fastForEach(sections, entry -> {
			String val = entry.getValue().serialize();
			if (val != null)
				joiner.add(val);
		});
		return joiner.toString();
	}
}
