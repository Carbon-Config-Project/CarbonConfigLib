package carbonconfiglib.utils.structure;

import java.util.List;
import java.util.function.Function;

import carbonconfiglib.api.ISuggestionProvider;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.ParsedCollections.ParsedList;
import carbonconfiglib.utils.ParsedCollections.ParsedMap;
import carbonconfiglib.utils.structure.IStructuredData.EntryDataType;
import carbonconfiglib.utils.structure.IStructuredData.SimpleData;
import carbonconfiglib.utils.structure.StructureCompound.CompoundData;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.lists.ObjectList;
import speiger.src.collections.objects.utils.ObjectLists;

public class StructureList
{
	public static class ListData implements IStructuredData {
		IListEntry type;
		boolean isNewLined;
		
		@Override
		public StructureType getDataType() { return StructureType.LIST; }
		@Override
		public ListData asList() { return this; }
		public boolean isNewLined() { return isNewLined; }
		public IStructuredData getType() { return type.getType(); }
		
		public ParsedList parse(String data) {
			ParsedList list = new ParsedList();
			type.parse(ObjectArrayList.wrap(Helpers.splitCompoundArray(data)), list);
			return list;
		}
		
		public String serialize(ParsedList data) {
			List<String> output = new ObjectArrayList<>();
			type.serialize(data, output);
			return Helpers.mergeCompoundArray(output, isNewLined);
		}
	}
	
	public static class ListBuilder {
		ListData result = new ListData();
		IWritableListEntry entry;
		
		private ListBuilder(IWritableListEntry writable) {
			entry = writable;
		}
		
		public ListBuilder addSuggestions(ISuggestionProvider... providers) {
			entry.addSuggestions(providers);
			return this;
		}
		
		public ListBuilder setForceSuggestions(boolean value) {
			entry.setForced(value);
			return this;
		}
		
		public ListData build(boolean newLine) {
			result.isNewLined = newLine;
			result.type = entry;
			ListData output = result;
			result = null;
			return output;
		}
		
		public static ListBuilder of(EntryDataType type) { return new ListBuilder(ListEntry.create(type)); }
		public static <T extends Enum<T>> ListBuilder enums(Class<T> clz) { return new ListBuilder(new ListEntry<>(EntryDataType.ENUM.toSimpleType(), E -> Helpers.parseEnum(clz, E), Enum::name)).addSuggestions(ISuggestionProvider.enums(clz)); }
		public static <T> ListBuilder variants(Class<T> type, Function<String, ParseResult<T>> parse, Function<T, String> serialize) { return new ListBuilder(new ListEntry<>(SimpleData.variant(type), parse, serialize)); }
		public static ListBuilder variants(IStructuredData type) {
			if(type instanceof ListData) return new ListBuilder(new ListWrapper((ListData)type));
			else if(type instanceof CompoundData) return new ListBuilder(new CompoundWrapper((CompoundData)type));
			throw new IllegalArgumentException("Only Lists and Compounds are supported");
		}
	}
	
	public static interface IListEntry {
		public IStructuredData getType();
		public boolean isForced();
		public void parse(List<String> input, ParsedList output);
		public void serialize(ParsedList input, List<String> output);
		public ObjectList<ISuggestionProvider> getSuggestions();
	}
	
	static interface IWritableListEntry extends IListEntry {
		public void setForced(boolean value);
		public void addSuggestions(ISuggestionProvider... providers);
	}
	
	static class CompoundWrapper implements IWritableListEntry {
		CompoundData data;

		public CompoundWrapper(CompoundData data) {
			this.data = data;
		}
		
		@Override
		public IStructuredData getType() { return data; }
		@Override
		public boolean isForced() { return false; }
		@Override
		public void parse(List<String> input, ParsedList output) {
			for(int i = 0,m=input.size();i<m;i++) {
				output.add(data.parse(input.get(i)));
			}
		}
		
		@Override
		public void serialize(ParsedList input, List<String> output) {
			for(int i = 0,m=input.size();i<m;i++) {
				output.add(data.serialize(input.get(i, ParsedMap.class)));
			}
		}

		@Override
		public ObjectList<ISuggestionProvider> getSuggestions() { return ObjectLists.empty(); }
		@Override
		public void setForced(boolean value) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
		@Override
		public void addSuggestions(ISuggestionProvider... providers) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
	}
	
	static class ListWrapper implements IWritableListEntry {
		ListData data;

		public ListWrapper(ListData data) {
			this.data = data;
		}

		@Override
		public IStructuredData getType() { return data; }
		@Override
		public boolean isForced() { return false; }
		@Override
		public void parse(List<String> input, ParsedList output) {
			for(int i = 0,m=input.size();i<m;i++) {
				output.add(data.parse(input.get(i)));
			}
		}
		
		@Override
		public void serialize(ParsedList input, List<String> output) {
			for(int i = 0,m=input.size();i<m;i++) {
				output.add(data.serialize(input.get(i, ParsedList.class)));
			}
		}

		@Override
		public ObjectList<ISuggestionProvider> getSuggestions() { return ObjectLists.empty(); }
		@Override
		public void setForced(boolean value) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
		@Override
		public void addSuggestions(ISuggestionProvider... providers) { throw new UnsupportedOperationException("Not supported for nested wrappers"); }
	}
	
	static class ListEntry<T> implements IWritableListEntry {
		ObjectList<ISuggestionProvider> providers = new ObjectArrayList<>();
		final Function<String, ParseResult<T>> parse;
		final Function<T, String> serialize;
		final IStructuredData type;
		boolean forcedSuggestions;
		
		public ListEntry(IStructuredData type, Function<String, ParseResult<T>> parse, Function<T, String> serialize) {
			this.type = type;
			this.parse = parse;
			this.serialize = serialize;
		}
		
		@Override
		public IStructuredData getType() { return type; }
		@Override
		public boolean isForced() { return forcedSuggestions; }
		@Override
		public ObjectList<ISuggestionProvider> getSuggestions() { return providers.unmodifiable(); }
		@Override
		public void parse(List<String> input, ParsedList output) {
			for(int i = 0,m=input.size();i<m;i++) {
				output.add(parse.apply(input.get(i)));
			}
		}
		
		@Override
		public void serialize(ParsedList input, List<String> output) {
			for(int i = 0,m=input.size();i<m;i++) {
				output.add(serialize.apply(input.getUnsafe(i)));
			}
		}
		
		@Override
		public void setForced(boolean value) { this.forcedSuggestions = value; }
		@Override
		public void addSuggestions(ISuggestionProvider... providers) { this.providers.addAll(providers); }
		
		private static ListEntry<?> create(EntryDataType type) {
			switch(type) {
				case BOOLEAN: return new ListEntry<>(type.toSimpleType(), Helpers::parseBoolean, String::valueOf);
				case INTEGER: return new ListEntry<>(type.toSimpleType(), Helpers::parseInt, String::valueOf);
				case DOUBLE: return new ListEntry<>(type.toSimpleType(), Helpers::parseDouble, String::valueOf);
				case STRING: return new ListEntry<>(type.toSimpleType(), Helpers::parseString, Function.identity());
				default: throw new IllegalStateException("Unsupported Type");
			}
		}
	}
}
