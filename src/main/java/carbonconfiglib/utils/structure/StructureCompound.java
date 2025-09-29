package carbonconfiglib.utils.structure;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

import carbonconfiglib.api.IEntrySettings;
import carbonconfiglib.api.IEntrySettings.TranslatedComment;
import carbonconfiglib.api.IEntrySettings.TranslatedKey;
import carbonconfiglib.api.IRange;
import carbonconfiglib.api.ISuggestionProvider;
import carbonconfiglib.api.ISuggestionProvider.Suggestion;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.ParsedCollections.ParsedList;
import carbonconfiglib.utils.ParsedCollections.ParsedMap;
import carbonconfiglib.utils.structure.IStructuredData.EntryDataType;
import carbonconfiglib.utils.structure.IStructuredData.SimpleData;
import carbonconfiglib.utils.structure.StructureList.CompoundWrapper;
import carbonconfiglib.utils.structure.StructureList.IWritableListEntry;
import carbonconfiglib.utils.structure.StructureList.ListData;
import carbonconfiglib.utils.structure.StructureList.ListEntry;
import carbonconfiglib.utils.structure.StructureList.ListWrapper;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.lists.ObjectList;
import speiger.src.collections.objects.maps.impl.hash.Object2ObjectLinkedOpenHashMap;
import speiger.src.collections.objects.maps.interfaces.Object2ObjectMap;
import speiger.src.collections.objects.utils.ObjectLists;

/**
 * Copyright 2024 Speiger, Meduris
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
public class StructureCompound
{
	public static class CompoundData implements IStructuredData {
		Map<String, ICompoundEntry> entries = new Object2ObjectLinkedOpenHashMap<>();
		Map<String, IEntrySettings> entrySettings = new Object2ObjectLinkedOpenHashMap<>();
		IEntrySettings settings;
		boolean isNewLined = true;
		
		@Override
		public StructureType getDataType() {return StructureType.COMPOUND; }
		public IEntrySettings getSettings() { return settings; }
		@Override
		public CompoundData asCompound() { return this; }
		public boolean isNewLined() { return isNewLined; }
		
		@Override
		public String generateDefaultValue(Function<SimpleData, String> defaultFactory) {
			Map<String, String> result = Object2ObjectMap.builder().linkedMap();
			for(Map.Entry<String, ICompoundEntry> entry : entries.entrySet()) {
				result.put(entry.getKey(), entry.getValue().getType().generateDefaultValue(defaultFactory));
			}
			return Helpers.mergeCompound(result, false, 0);
		}
		
		public List<String> getKeys() {
			return new ObjectArrayList<>(entries.keySet());
		}
		
		public ParsedMap parse(String input) {
			return parseMap(Helpers.splitArguments(Helpers.splitCompound(input.trim()), getKeys(), false));
		}
		
		public String serialize(ParsedMap map, boolean allowMultiline) {
			return serialize(map, allowMultiline, 0);
		}
		
		public String serialize(ParsedMap map, boolean allowMultiline, int indent) {
			return Helpers.mergeCompound(serializeMap(map, allowMultiline, indent), isNewLined && allowMultiline, indent);
		}
		
		public ParsedMap parseMap(Map<String, String> info) {
			ParsedMap output = new ParsedMap();
			for(ICompoundEntry entry : entries.values()) {
				entry.parse(info, output);
			}
			return output;
		}
		
		public Map<String, String> serializeMap(ParsedMap map, boolean allowMultine, int indent) {
			Map<String, String> parsed = Object2ObjectMap.builder().linkedMap();
			for(ICompoundEntry entry : entries.values()) {
				entry.serialize(map, parsed, allowMultine, indent+1);
			}
			return parsed;
		}
		
		public Map<String, IStructuredData> getFormat() {
			Map<String, IStructuredData> data = new Object2ObjectLinkedOpenHashMap<>();
			entries.forEach((K, V) -> data.put(K, V.getType()));
			return data;
		}
		
		public boolean isForcedSuggestion(String name) {
			ICompoundEntry entry = entries.get(name);
			return entry != null && entry.isForced();
		}
		
		public String[] getComments(String name) {
			ICompoundEntry entry = entries.get(name);
			return entry == null ? null : entry.getComment();
		}
		
		public IEntrySettings getEntrySetting(String name) {
			return entrySettings.get(name);
		}
		
		public IRange getRange(String name) {
			ICompoundEntry entry = entries.get(name);
			return entry == null ? null : entry.getRange();
		}
		
		public String getTranslationKey(String name) {
			IEntrySettings settings = entrySettings.get(name);
			TranslatedKey comment = settings == null ? null : settings.get(TranslatedKey.class);
			return comment == null ? null : comment.getTranslationKey();
		}
		
		public String getTranslationComment(String name) {
			IEntrySettings settings = entrySettings.get(name);
			TranslatedComment comment = settings == null ? null : settings.get(TranslatedComment.class);
			return comment == null ? null : comment.getTranslationKey();
		}
		
		public List<Suggestion> getSuggestions(String name, BiPredicate<String, String> filter) {
			ICompoundEntry entry = entries.get(name);
			if(entry == null) return ObjectLists.empty();
			List<ISuggestionProvider> providers = entry.getSuggestions();
			List<Suggestion> output = new ObjectArrayList<>();
			for(ISuggestionProvider provider : providers) {
				provider.provideSuggestions(output::add, T -> filter.test(name, T.getValue()));
			}
			return output;
		}
		
		@Override
		public void appendFormat(StringBuilder builder, boolean start) {
			if(!start) builder.append("Compound[");
			for(ICompoundEntry entry : entries.values()) {
				builder.append(entry.getName()).append(": (");
				entry.getType().appendFormat(builder, false);
				builder.append("); ");
			}
			if(builder.charAt(builder.length()-1) == ' ') builder.deleteCharAt(builder.length()-1);
			if(!start) builder.append("]");
		}
	}
	
	public static class CompoundBuilder {
		CompoundData result = new CompoundData();
		IWritableCompoundEntry current;
		
		public CompoundBuilder simple(String name, EntryDataType type) {
			if(type == EntryDataType.ENUM) throw new IllegalStateException("Use enums instead");
			if(type == EntryDataType.CUSTOM) throw new IllegalStateException("Use variants instead");
			return start(CompoundEntry.create(name, type));
		}
		
		public <T extends Enum<T>> CompoundBuilder enums(String name, Class<T> clz) {
			start(new CompoundEntry<>(name, EntryDataType.ENUM.toSimpleType(), E -> Helpers.parseEnum(clz, E), Enum::name));
			addSuggestions(ISuggestionProvider.enums(clz));
			return this;
		}
		
		public <T> CompoundBuilder variants(String name, EntryDataType displayType, Class<T> type, Function<String, ParseResult<T>> parse, Function<T, String> serialize) {
			return start(new CompoundEntry<>(name, SimpleData.variant(displayType, type), parse, serialize));
		}
		
		public CompoundBuilder list(String name, ListData entry) {
			return start(new WrappedListEntry(name, entry));
		}
		
		public CompoundBuilder listList(String name, ListData entry, boolean newLine) {
			return start(new WrappedEditListEntry(name, new ListWrapper(entry), newLine));
		}
		
		public CompoundBuilder listSimple(String name, EntryDataType type, boolean newLine) {
			return start(new WrappedEditListEntry(name, ListEntry.create(type), newLine));
		}
		
		public <T extends Enum<T>> CompoundBuilder listEnum(String name, Class<T> clz, boolean newLine) {
			ListEntry<T> list = new ListEntry<>(EntryDataType.ENUM.toSimpleType(), E -> Helpers.parseEnum(clz, E), Enum::name);
			list.addSuggestions(ISuggestionProvider.enums(clz));
			return start(new WrappedEditListEntry(name, list, newLine));
		}
		
		public <T> CompoundBuilder listVariant(String name, EntryDataType displayType, Class<T> type, boolean newLine, Function<String, ParseResult<T>> parse, Function<T, String> serialize) {
			return start(new WrappedEditListEntry(name, new ListEntry<>(SimpleData.variant(displayType, type), parse, serialize), newLine));
		}
		
		public <T> CompoundBuilder object(String name, CompoundData data, Function<ParsedMap, ParseResult<T>> parse, Function<T, ParsedMap> serialize) {
			return start(new WrappedCompoundEntry<>(name, data, parse, serialize));
		}
		
		public <T> CompoundBuilder objectList(String name, CompoundData data, boolean newLine, Function<ParsedMap, ParseResult<T>> parse, Function<T, ParsedMap> serialize) {
			return start(new WrappedEditListEntry(name, new CompoundWrapper<>(data, parse, serialize), newLine));
		}
		
		public CompoundBuilder addSuggestions(ISuggestionProvider... providers) {
			Objects.requireNonNull(current, "No Entry to configure").addSuggestions(providers);
			return this;
		}
		
		public CompoundBuilder setComments(String... comments) {
			Objects.requireNonNull(current, "No Entry to configure").setComments(comments);
			return this;
		}
		
		public CompoundBuilder forceSuggestions(boolean value) {
			Objects.requireNonNull(current, "No Entry to configure").setForced(value);
			return this;
		}
		
		public CompoundBuilder setTranslationKey(String translationKey) {
			return addEntrySetting(new TranslatedKey(translationKey));
		}
		
		public CompoundBuilder setTranslationComment(String translationKey) {
			return addEntrySetting(new TranslatedComment(translationKey));
		}
		
		public CompoundBuilder addEntrySetting(IEntrySettings settings) {
			result.entrySettings.compute(Objects.requireNonNull(current, "No Entry to configure").getName(), (K, V) -> IEntrySettings.merge(V, settings));
			return this;
		}
		
		public CompoundBuilder setEntrySettings(IEntrySettings settings) {
			result.entrySettings.put(Objects.requireNonNull(current, "No Entry to configure").getName(), settings);
			return this;
		}
		
		public CompoundBuilder addSetting(IEntrySettings settings) {
			result.settings = IEntrySettings.merge(result.settings, settings);
			return this;
		}
		
		public CompoundBuilder setSettings(IEntrySettings settings) {
			result.settings = settings;
			return this;
		}
		
		private CompoundBuilder start(IWritableCompoundEntry entry) {
			if(current != null) result.entries.put(current.getName(), current);
			this.current = entry;
			return this;
		}
		
		public CompoundBuilder setNewLined(boolean value) {
			result.isNewLined = value;
			return this;
		}
		
		@Deprecated
		public CompoundBuilder finish() {
			Objects.requireNonNull(current, "Can't finish a non existend entry");
			result.entries.put(current.getName(), current);
			current = null;
			return this;
		}
		
		public CompoundData build() {
			if(current != null) {
				result.entries.put(current.getName(), current);
				current = null;
			}
			CompoundData out = result;
			result = null;
			return out;
		}
	}
	
	public static interface ICompoundEntry {
		public IStructuredData getType();
		public boolean isForced();
		public String[] getComment();
		public String getName();
		public IRange getRange();
		public void parse(Map<String, String> data, ParsedMap output);
		public void serialize(ParsedMap input, Map<String, String> output, boolean allowMultiline, int indent);
		public ObjectList<ISuggestionProvider> getSuggestions();
	}
	
	static interface IWritableCompoundEntry extends ICompoundEntry {
		public void setForced(boolean value);
		public void addSuggestions(ISuggestionProvider... providers);
		public void setComments(String...comments);
		public void withRange(IRange range);
	}
	
	static class WrappedEditListEntry implements IWritableCompoundEntry {
		String name;
		String[] comments;
		IWritableListEntry entry;
		boolean newLine;
		
		public WrappedEditListEntry(String name, IWritableListEntry entry, boolean newLine) {
			this.name = name;
			this.entry = entry;
			this.newLine = newLine;
		}
		
		@Override
		public IStructuredData getType() { return entry.getType(); }
		@Override
		public boolean isForced() { return entry.isForced(); }
		@Override
		public String[] getComment() { return comments; }
		@Override
		public String getName() { return name; }
		@Override
		public ObjectList<ISuggestionProvider> getSuggestions() { return entry.getSuggestions(); }
		@Override
		public IRange getRange() { return entry.getRange(); }
		
		@Override
		public void parse(Map<String, String> input, ParsedMap output) {
			ParsedList list = new ParsedList();
			entry.parse(ObjectArrayList.wrap(Helpers.splitCompoundArray(Helpers.removeLayer(input.getOrDefault(name, "").trim(), 0))), list);
			output.put(name, ParsedList.unwrap(list));
		}
		
		@Override
		public void serialize(ParsedMap input, Map<String, String> result, boolean allowMultiline, int indent) {
			List<String> output = new ObjectArrayList<>();
			entry.serialize(input.get(name, ParsedList.class), output, newLine && allowMultiline, indent+1);
			result.put(name, Helpers.mergeCompoundArray(output, newLine && allowMultiline, indent));
		}
		
		@Override
		public void setForced(boolean value) { entry.setForced(value); }
		@Override
		public void withRange(IRange range) { entry.withRange(range); }
		@Override
		public void addSuggestions(ISuggestionProvider... providers) { entry.addSuggestions(providers); }
		@Override
		public void setComments(String... comments) { this.comments = comments; }
	}
	
	static class WrappedListEntry implements IWritableCompoundEntry {
		String name;
		String[] comments;
		ListData data;
		
		public WrappedListEntry(String name, ListData data) {
			this.name = name;
			this.data = data;
		}
		
		@Override
		public IStructuredData getType() { return data; }
		@Override
		public boolean isForced() { return false; }
		@Override
		public String getName() { return name; }
		@Override
		public String[] getComment() { return comments; }
		@Override
		public ObjectList<ISuggestionProvider> getSuggestions() { return ObjectLists.empty(); }
		@Override
		public IRange getRange() { return data.getRange(); }
		
		@Override
		public void parse(Map<String, String> input, ParsedMap output) {
			output.put(name, ParsedList.unwrap(data.parse(input.getOrDefault(name, ""))));
		}
		
		@Override
		public void serialize(ParsedMap input, Map<String, String> output, boolean allowMultiline, int indent) {
			output.put(name, data.serialize(input.get(name, ParsedList.class), allowMultiline, indent));
		}
		
		@Override
		public void setForced(boolean value) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
		@Override
		public void addSuggestions(ISuggestionProvider... providers) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
		@Override
		public void withRange(IRange range) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
		@Override
		public void setComments(String... comments) { this.comments = comments; }
	}
	
	static class WrappedCompoundEntry<T> implements IWritableCompoundEntry {
		String name;
		String[] comments;
		CompoundData data;
		Function<ParsedMap, ParseResult<T>> parse;
		Function<T, ParsedMap> serialize;
		
		public WrappedCompoundEntry(String name, CompoundData data, Function<ParsedMap, ParseResult<T>> parse, Function<T, ParsedMap> serialize) {
			this.name = name;
			this.data = data;
			this.parse = parse;
			this.serialize = serialize;
		}
		
		@Override
		public IStructuredData getType() { return data; }
		@Override
		public boolean isForced() { return false; }
		@Override
		public String getName() { return name; }
		@Override
		public String[] getComment() { return comments; }
		@Override
		public ObjectList<ISuggestionProvider> getSuggestions() { return ObjectLists.empty(); }
		@Override
		public IRange getRange() { return null; }
		@Override
		public void parse(Map<String, String> input, ParsedMap output) {
			output.put(name, parse.apply(data.parse(input.getOrDefault(name, ""))));
		}
		
		@Override
		public void serialize(ParsedMap input, Map<String, String> output, boolean allowMultiline, int indent) {
			output.put(name, data.serialize(serialize.apply(input.getUnsafe(name)), allowMultiline, indent));
		}
		
		@Override
		public void setForced(boolean value) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
		@Override
		public void addSuggestions(ISuggestionProvider... providers) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
		@Override
		public void withRange(IRange range) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
		@Override
		public void setComments(String... comments) { this.comments = comments; }
	}
	
	static class CompoundEntry<T> implements IWritableCompoundEntry {
		ObjectList<ISuggestionProvider> providers = new ObjectArrayList<>();
		final Function<String, ParseResult<T>> parse;
		final Function<T, String> serialize;
		final IStructuredData type;
		final String name;
		String[] comments;
		boolean forcedSuggestions;
		IRange range;
		
		public CompoundEntry(String name, IStructuredData type, Function<String, ParseResult<T>> parse, Function<T, String> serialize) {
			this.name = name;
			this.type = type;
			this.parse = parse;
			this.serialize = serialize;
		}
		
		@Override
		public String getName() { return name; }
		@Override
		public IStructuredData getType() { return type; }
		@Override
		public boolean isForced() { return forcedSuggestions; }
		@Override
		public String[] getComment() { return comments; }
		@Override
		public ObjectList<ISuggestionProvider> getSuggestions() { return providers.unmodifiable(); }
		@Override
		public IRange getRange() { return range; }
		@Override
		public void parse(Map<String, String> data, ParsedMap output) { output.put(name, parse.apply(data.getOrDefault(name, ""))); }
		@Override
		public void serialize(ParsedMap input, Map<String, String> output, boolean allowMultiline, int indent) { output.put(name, serialize.apply(input.getUnsafe(name))); }
		
		@Override
		public void withRange(IRange range) { this.range = range; }
		@Override
		public void setForced(boolean value) { this.forcedSuggestions = value; }
		@Override
		public void addSuggestions(ISuggestionProvider... providers) { this.providers.addAll(providers); }
		@Override
		public void setComments(String... comments) { this.comments = comments; }
		
		private static CompoundEntry<?> create(String name, EntryDataType type) {
			switch(type) {
				case BOOLEAN: return new CompoundEntry<>(name, type.toSimpleType(), Helpers::parseBoolean, String::valueOf);
				case INTEGER: return new CompoundEntry<>(name, type.toSimpleType(), Helpers::parseInt, Helpers::fuzzyIntegerToString);
				case DOUBLE: return new CompoundEntry<>(name, type.toSimpleType(), Helpers::parseDouble, Helpers::fuzzyDoubleToString);
				case STRING: return new CompoundEntry<>(name, type.toSimpleType(), Helpers::parseString, Function.identity());
				default: throw new IllegalStateException("Unsupported Type");
			}
		}
	}
}
