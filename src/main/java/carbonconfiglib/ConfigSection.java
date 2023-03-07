package carbonconfiglib;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;

import carbonconfiglib.ConfigEntry.ArrayValue;
import carbonconfiglib.ConfigEntry.BoolValue;
import carbonconfiglib.ConfigEntry.DoubleValue;
import carbonconfiglib.ConfigEntry.EnumValue;
import carbonconfiglib.ConfigEntry.IntValue;
import carbonconfiglib.ConfigEntry.ParsedValue;
import carbonconfiglib.ConfigEntry.StringValue;
import carbonconfiglib.ConfigEntry.TempValue;
import carbonconfiglib.api.IConfigSerializer;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.MultilinePolicy;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ConfigSection {
	private String name;
	private ConfigSection parent = null;
	private boolean used = false;
	private Object2ObjectMap<String, ConfigEntry<?>> entries = new Object2ObjectLinkedOpenHashMap<>();
	private Object2ObjectMap<String, ConfigSection> subSections = new Object2ObjectLinkedOpenHashMap<>();

	public ConfigSection(String name) {
		if (Helpers.validateString(name))
			throw new IllegalArgumentException("ConfigSection name must not be null, empty or start/end with white spaces");
		if (name.contains("."))
			throw new IllegalArgumentException("ConfigSection name must not contain period signs. Name: " + name);
		this.name = name;
	}

	@SuppressWarnings("unchecked")
	public <V, T extends ConfigEntry<V>> T add(T entry) {
		ConfigEntry<?> presentKey = entries.get(entry.getKey());
		if(presentKey != null)
		{
			if(presentKey instanceof TempValue) entry.parseValue(presentKey.serializedValue(MultilinePolicy.DISABLED));
			else return (T)(presentKey.getPrefix() != entry.getPrefix() ? entry : presentKey).setUsed();
		}
		entries.put(entry.getKey(), entry.setUsed());
		return entry;
	}
	
	<V, T extends ConfigEntry<V>> T addParsed(T entry) {
		entries.putIfAbsent(entry.getKey(), entry);
		return entry;
	}

	public BoolValue addBool(String key, boolean value, String comment) {
		return add(new BoolValue(key, value, comment));
	}

	public BoolValue addBool(String key, boolean value) {
		return add(new BoolValue(key, value));
	}

	public IntValue addInt(String key, int value, String comment) {
		return add(new IntValue(key, value, comment));
	}

	public IntValue addInt(String key, int value) {
		return add(new IntValue(key, value));
	}

	public DoubleValue addDouble(String key, double value, String... comment) {
		return add(new DoubleValue(key, value, comment));
	}

	public DoubleValue addDouble(String key, double value) {
		return add(new DoubleValue(key, value));
	}

	public StringValue addString(String key, String value, String... comment) {
		return add(new StringValue(key, value, comment));
	}

	public StringValue addString(String key, String value) {
		return add(new StringValue(key, value));
	}

	public ArrayValue addArray(String key, String[] value, String... comment) {
		return add(new ArrayValue(key, value, comment));
	}

	public ArrayValue addArray(String key, String[] value) {
		return add(new ArrayValue(key, value));
	}

	public ArrayValue addArray(String key, String comment) {
		return add(new ArrayValue(key, comment));
	}

	public ArrayValue addArray(String key) {
		return add(new ArrayValue(key));
	}

	public <E extends Enum<E>> EnumValue<E> addEnum(String key, E value, Class<E> enumClass, String... comment) {
		return add(new EnumValue<>(key, value, enumClass, comment));
	}

	public <E extends Enum<E>> EnumValue<E> addEnum(String key, E value, Class<E> enumClass) {
		return add(new EnumValue<>(key, value, enumClass));
	}
	
	public <T> ParsedValue<T> addParsed(String key, T value, IConfigSerializer<T> parsers, String... comment) {
		return add(new ParsedValue<T>(key, value, parsers, comment));
	}
	
	public <T> ParsedValue<T> addParsed(String key, T value, IConfigSerializer<T> parsers) {
		return add(new ParsedValue<T>(key, value, parsers));
	}
	
	public ConfigSection addSubSection(String name) {
		ConfigSection subSection = subSections.get(name);
		if(subSection == null) {
			subSection = new ConfigSection(name);
			subSection.parent = this;
			subSections.put(name, subSection);
		}
		return subSection.setUsed();
	}
	
	public ConfigSection add(ConfigSection section) {
		if (section.parent != null)
			throw new IllegalStateException("ConfigSection must not be added to multiple sections. Section name: " + section.getName());
		section.parent = this;
		subSections.putIfAbsent(section.name, section);
		return section.setUsed();
	}

	public ConfigEntry<?> getEntry(String name) {
		return entries.get(name);
	}

	public ConfigSection getSubSection(String name) {
		return subSections.get(name);
	}
	
	ConfigSection parseSubSection(String name) {
		ConfigSection subSection = subSections.get(name);
		if(subSection == null) {
			subSection = new ConfigSection(name);
			subSection.parent = this;
			subSections.put(name, subSection);
		}
		return subSection;
	}
	
	public List<ConfigSection> getChildren() {
		return new ObjectArrayList<>(subSections.values());
	}
	
	public List<ConfigEntry<?>> getEntries() {
		return new ObjectArrayList<>(entries.values());		
	}
	
	public void resetDefault() {
		subSections.values().forEach(ConfigSection::resetDefault);
		entries.values().forEach(ConfigEntry::resetDefault);
	}
	
	boolean hasChanged() {
		for(ConfigEntry<?> entry : entries.values()) {
			if(entry.hasChanged()) return true;
		}
		for(ConfigSection section : subSections.values()) {
			if(section.hasChanged()) return true;
		}
		return false;
	}
	
	public ConfigSection getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public String getSectionPath() {
		return (parent != null ? parent.getSectionPath() + "." : "") + name;
	}
	
	boolean isUsed() {
		return used;
	}
	
	ConfigSection setUsed() {
		used = true;
		return this;
	}

	public void getSyncedEntries(Map<String, ConfigEntry<?>> syncedEntries) {
		for (ConfigEntry<?> entry : entries.values()) {
			if (entry.isSynced()) {
				syncedEntries.put(getSectionPath() + "." + entry.getKey(), entry);
			}
		}
		for (ConfigSection section : subSections.values()) {
			section.getSyncedEntries(syncedEntries);
		}
	}

	public String serialize(MultilinePolicy policy) {
		return serialize(policy, 0);
	}
	
	public String serialize(MultilinePolicy policy, int indentationLevel) {
		if (entries.size() == 0 && subSections.size() == 0)
			return null;
		AtomicInteger written = new AtomicInteger(0);
		StringBuilder builder = new StringBuilder(parent == null ? "" : ('\n' + Helpers.generateIndent(indentationLevel)));
		builder.append('[');
		builder.append(getSectionPath());
		builder.append(']');
		final int finalIndentationLevel = indentationLevel + 1;
		Object2ObjectMaps.fastForEach(entries, entry -> {
			if(!entry.getValue().isUsed()) return;
			builder.append(entry.getValue().serialize(policy, finalIndentationLevel));
			written.incrementAndGet();
		});
		StringJoiner joiner = new StringJoiner("\n", entries.size() == 0 || subSections.size() == 0 ? "" : "\n", "");
		Object2ObjectMaps.fastForEach(subSections, entry -> {
			if(!entry.getValue().isUsed()) return;
			String val = entry.getValue().serialize(policy, finalIndentationLevel);
			if (val == null) return;
			joiner.add(val);
			written.getAndIncrement();
		});
		builder.append(joiner.toString());
		return written.get() > 0 ? builder.toString() : null;
	}
}
