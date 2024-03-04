package carbonconfiglib.utils.structure;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import carbonconfiglib.api.ISuggestionProvider;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.ParsedCollections.ParsedList;
import carbonconfiglib.utils.ParsedCollections.ParsedMap;
import carbonconfiglib.utils.structure.IStructuredData.EntryDataType;
import carbonconfiglib.utils.structure.IStructuredData.SimpleData;
import carbonconfiglib.utils.structure.StructureList.ListData;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.lists.ObjectList;
import speiger.src.collections.objects.maps.impl.hash.Object2ObjectLinkedOpenHashMap;
import speiger.src.collections.objects.maps.interfaces.Object2ObjectMap;
import speiger.src.collections.objects.utils.ObjectLists;

public class StructureCompound
{
	public static class CompoundData implements IStructuredData {
		Map<String, ICompoundEntry> entries = new Object2ObjectLinkedOpenHashMap<>();
		boolean isNewLined = true;
		
		@Override
		public StructureType getDataType() {return StructureType.COMPOUND; }
		@Override
		public CompoundData asCompound() { return this; }
		public boolean isNewLined() { return isNewLined; }
		
		public List<String> getKeys() {
			return new ObjectArrayList<>(entries.keySet());
		}
		
		public ParsedMap parse(String input) {
			return parseMap(Helpers.splitArguments(Helpers.splitCompound(input.trim()), getKeys()));
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
		
		public CompoundBuilder nested(String name, IStructuredData entry) {
			if(entry instanceof ListData) return start(new WrappedListEntry(name, (ListData)entry));
			if(entry instanceof CompoundData) return start(new WrappedCompoundEntry(name, (CompoundData)entry));
			throw new IllegalArgumentException("Only Lists and Compounds are supported");
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
		
		private CompoundBuilder start(IWritableCompoundEntry entry) {
			if(current != null) throw new IllegalStateException("Can't start another entry without finishing the previous one");
			this.current = entry;
			return this;
		}
		
		public CompoundBuilder setNewLined(boolean value) {
			result.isNewLined = value;
			return this;
		}
		
		public CompoundBuilder finish() {
			Objects.requireNonNull(current, "Can't finish a non existend entry");
			result.entries.put(current.getName(), current);
			current = null;
			return this;
		}
		
		public CompoundData build() {
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
		public void parse(Map<String, String> data, ParsedMap output);
		public void serialize(ParsedMap input, Map<String, String> output, boolean allowMultiline, int indent);
		public ObjectList<ISuggestionProvider> getSuggestions();
	}
	
	static interface IWritableCompoundEntry extends ICompoundEntry {
		public void setForced(boolean value);
		public void addSuggestions(ISuggestionProvider... providers);
		public void setComments(String...comments);
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
		public void parse(Map<String, String> input, ParsedMap output) {
			output.put(name, ParsedList.unwrap(data.parse(input.getOrDefault(name, ""))));
		}
		
		@Override
		public void serialize(ParsedMap input, Map<String, String> output, boolean allowMultiline, int indent) {
			output.put(name, data.serialize(input.get(name, ParsedList.class), allowMultiline, indent));
		}
		
		@Override
		public ObjectList<ISuggestionProvider> getSuggestions() { return ObjectLists.empty(); }
		@Override
		public void setForced(boolean value) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
		@Override
		public void addSuggestions(ISuggestionProvider... providers) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
		@Override
		public void setComments(String... comments) { this.comments = comments; }
	}
	
	static class WrappedCompoundEntry implements IWritableCompoundEntry {
		String name;
		String[] comments;
		CompoundData data;
		
		public WrappedCompoundEntry(String name, CompoundData data) {
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
		public void parse(Map<String, String> input, ParsedMap output) {
			output.put(name, data.parse(input.getOrDefault(name, "")));
		}
		
		@Override
		public void serialize(ParsedMap input, Map<String, String> output, boolean allowMultiline, int indent) {
			output.put(name, data.serialize(input.get(name, ParsedMap.class), allowMultiline, indent));
		}
		
		@Override
		public ObjectList<ISuggestionProvider> getSuggestions() { return ObjectLists.empty(); }
		@Override
		public void setForced(boolean value) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
		@Override
		public void addSuggestions(ISuggestionProvider... providers) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
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
		public void parse(Map<String, String> data, ParsedMap output) { output.put(name, parse.apply(data.getOrDefault(name, ""))); }
		@Override
		public void serialize(ParsedMap input, Map<String, String> output, boolean allowMultiline, int indent) { output.put(name, serialize.apply(input.getUnsafe(name))); }
		
		@Override
		public void setForced(boolean value) { this.forcedSuggestions = value; }
		@Override
		public void addSuggestions(ISuggestionProvider... providers) { this.providers.addAll(providers); }
		@Override
		public void setComments(String... comments) { this.comments = comments; }
		
		private static CompoundEntry<?> create(String name, EntryDataType type) {
			switch(type) {
				case BOOLEAN: return new CompoundEntry<>(name, type.toSimpleType(), Helpers::parseBoolean, String::valueOf);
				case INTEGER: return new CompoundEntry<>(name, type.toSimpleType(), Helpers::parseInt, String::valueOf);
				case DOUBLE: return new CompoundEntry<>(name, type.toSimpleType(), Helpers::parseDouble, String::valueOf);
				case STRING: return new CompoundEntry<>(name, type.toSimpleType(), Helpers::parseString, Function.identity());
				default: throw new IllegalStateException("Unsupported Type");
			}
		}
	}
}
