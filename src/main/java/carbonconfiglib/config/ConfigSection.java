package carbonconfiglib.config;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;

import carbonconfiglib.api.IConfigSerializer;
import carbonconfiglib.api.IEntrySettings;
import carbonconfiglib.api.IEntrySettings.TranslatedComment;
import carbonconfiglib.api.IEntrySettings.TranslatedKey;
import carbonconfiglib.config.ConfigEntry.ArrayValue;
import carbonconfiglib.config.ConfigEntry.BoolValue;
import carbonconfiglib.config.ConfigEntry.DoubleValue;
import carbonconfiglib.config.ConfigEntry.EnumList;
import carbonconfiglib.config.ConfigEntry.EnumValue;
import carbonconfiglib.config.ConfigEntry.FloatValue;
import carbonconfiglib.config.ConfigEntry.IntValue;
import carbonconfiglib.config.ConfigEntry.LongValue;
import carbonconfiglib.config.ConfigEntry.ParsedArray;
import carbonconfiglib.config.ConfigEntry.ParsedValue;
import carbonconfiglib.config.ConfigEntry.StringValue;
import carbonconfiglib.config.ConfigEntry.TempValue;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.MultilinePolicy;
import carbonconfiglib.utils.SyncType;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.maps.impl.hash.Object2ObjectLinkedOpenHashMap;
import speiger.src.collections.objects.maps.interfaces.Object2ObjectMap;
import speiger.src.collections.objects.utils.maps.Object2ObjectMaps;

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
public class ConfigSection {
	private String name;
	private String[] comment;
	private ConfigSection parent = null;
	private IEntrySettings settings = null;
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
	
	public ConfigSection setComment(String...comment) {
		this.comment = Helpers.validateComments(comment);
		return this;
	}
	
	public ConfigSection setTranslationKey(String translationKey) {
		return addSettings(new TranslatedKey(translationKey));
	}
	
	public ConfigSection setTranslationComment(String translationKey) {
		return addSettings(new TranslatedComment(translationKey));
	}
	
	public ConfigSection addSettings(IEntrySettings settings) {
		this.settings = IEntrySettings.merge(this.settings, settings);
		return this;
	}
	
	public ConfigSection setSettings(IEntrySettings settings) {
		this.settings = settings;
		return this;
	}
	
	public void applyEntrySettings(IEntrySettings settings, Class<?> type, boolean children) {
		if(children) {
			for(ConfigSection section : subSections.values()) {
				section.applyEntrySettings(settings, type, true);
			}
		}
		if(type.isInstance(this)) setSettings(settings);
		for(ConfigEntry<?> entry : entries.values()) {
			if(type.isInstance(entry)) entry.setSettings(settings);
		}
	}
	
	void parseComment(String...comment) {
		if(this.comment != null) return;
		this.comment = Helpers.validateComments(comment);
	}
	
	@SuppressWarnings("unchecked")
	public <V, T extends ConfigEntry<V>> T add(T entry) {
		ConfigEntry<?> presentKey = entries.get(entry.getKey());
		if(presentKey != null) {
			if(presentKey instanceof TempValue) entry.deserializeValue(presentKey.serialize());
			else return (T)(presentKey.getPrefix() != entry.getPrefix() ? entry : presentKey).setUsed();
		}
		entries.put(entry.getKey(), entry.setUsed());
		return entry;
	}
	
	<V, T extends ConfigEntry<V>> T addParsed(T entry) {
		ConfigEntry<?> existingValue = entries.putIfAbsent(entry.getKey(), entry);
		if(existingValue != null) existingValue.setLoaded();
		return entry;
	}
	
	public BoolValue addBool(String key, boolean value, String... comment) {
		return add(new BoolValue(key, value, comment));
	}
	
	public BoolValue addBool(String key, boolean value) {
		return add(new BoolValue(key, value));
	}
	
	public IntValue addInt(String key, int value, String... comment) {
		return add(new IntValue(key, value, comment));
	}
	
	public IntValue addInt(String key, int value) {
		return add(new IntValue(key, value));
	}
	
	public LongValue addLong(String key, long value, String... comment) {
		return add(new LongValue(key, value, comment));
	}
	
	public LongValue addLong(String key, long value) {
		return add(new LongValue(key, value));
	}
	
	public FloatValue addFloat(String key, float value, String... comment) {
		return add(new FloatValue(key, value, comment));
	}
	
	public FloatValue addFloat(String key, float value) {
		return add(new FloatValue(key, value));
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
	
	public <E extends Enum<E>> EnumList<E> addEnumList(String key, List<E> value, Class<E> enumClass, String... comment) {
		return add(new EnumList<>(key, value, enumClass, comment));
	}
	
	public <E extends Enum<E>> EnumList<E> addEnumList(String key, List<E> value, Class<E> enumClass) {
		return add(new EnumList<>(key, value, enumClass));
	}
	
	public <T> ParsedValue<T> addParsed(String key, T value, IConfigSerializer<T> parsers, String... comment) {
		return add(new ParsedValue<T>(key, value, parsers, comment));
	}
	
	public <T> ParsedValue<T> addParsed(String key, T value, IConfigSerializer<T> parsers) {
		return add(new ParsedValue<T>(key, value, parsers));
	}
	
	public <T> ParsedArray<T> addParsedArray(String key, List<T> value, IConfigSerializer<T> parsers, String... comment) {
		return add(new ParsedArray<T>(key, value, parsers, comment));
	}
	
	public <T> ParsedArray<T> addParsedArray(String key, List<T> value, IConfigSerializer<T> parsers) {
		return add(new ParsedArray<T>(key, value, parsers));
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
	
	protected ConfigSection copy() {
		ConfigSection copy = new ConfigSection(name);
		for(ConfigSection sub : subSections.values()) {
			copy.add(sub.copy());
		}
		for(ConfigEntry<?> entry : entries.values()) {
			copy.add(entry.deepCopy());
		}
		return copy;
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
	
	boolean isDefault() {
		for(ConfigEntry<?> entry : entries.values()) {
			if(!entry.isDefault()) {
				return false;
			}
		}
		for(ConfigSection section : subSections.values()) {
			if(!section.isDefault()) {
				return false;
			}
		}
		return true;		
	}
	
	public ConfigSection getParent() {
		return parent;
	}
	
	public String getName() {
		return name;
	}
	
	public String[] getComment() {
		return comment;
	}
	
	public String getTranslationKey() {
		TranslatedKey comment = settings == null ? null : settings.get(TranslatedKey.class);
		return comment == null ? null : comment.getTranslationKey();
	}
	
	public String getTranslationComment() {
		TranslatedComment comment = settings == null ? null : settings.get(TranslatedComment.class);
		return comment == null ? null : comment.getTranslationKey();
	}
	
	public <S extends IEntrySettings> S getSetting(Class<S> clz) {
		return settings == null ? null : settings.get(clz);
	}
	
	public IEntrySettings getSettings() {
		return settings;
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
	
	void getSyncedEntries(Map<String, ConfigEntry<?>> syncedEntries, SyncType type) {
		for (ConfigEntry<?> entry : entries.values()) {
			if (entry.getSyncType() == type) {
				syncedEntries.put(getSectionPath() + "." + entry.getKey(), entry);
			}
		}
		for (ConfigSection section : subSections.values()) {
			section.getSyncedEntries(syncedEntries, type);
		}
	}
	
	public String serialize(MultilinePolicy policy) {
		return serialize(policy, 0);
	}
	
	public String serialize(MultilinePolicy policy, int indentationLevel) {
		if (entries.size() == 0 && subSections.size() == 0) return null;
		AtomicInteger written = new AtomicInteger(0);
		StringBuilder builder = new StringBuilder(parent == null ? "" : '\n' + Helpers.generateIndent(indentationLevel));
		if (comment != null && comment.length > 0) {
			String indentation = '\n' + Helpers.generateIndent(indentationLevel);
			for(int i = 0;i<comment.length;i++) {
				builder.append(indentation);
				builder.append("# ");
				builder.append(comment[i].replaceAll("\\R", indentation + "# "));
			}
			builder.append(indentation);
			builder.delete(0, 1);
		}
		builder.append('[');
		builder.append(getSectionPath());
		builder.append(']');
		final int finalIndentationLevel = indentationLevel + 1;
		Object2ObjectMaps.fastForEach(entries, entry -> {
			if(!entry.getValue().isUsed() || !entry.getValue().isNotHidden()) return;
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
