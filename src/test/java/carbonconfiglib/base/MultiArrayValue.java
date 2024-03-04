package carbonconfiglib.base;

import java.util.List;

import carbonconfiglib.api.IConfigSerializer;
import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.ParsedCollections.ParsedList;
import carbonconfiglib.utils.ParsedCollections.ParsedMap;
import carbonconfiglib.utils.structure.IStructuredData.EntryDataType;
import carbonconfiglib.utils.structure.StructureCompound.CompoundBuilder;
import carbonconfiglib.utils.structure.StructureList.ListBuilder;
import speiger.src.collections.objects.lists.ObjectArrayList;

public class MultiArrayValue
{
	private static final String[] VALUES = new String[] {"One", "Two", "Three", "Four", "Five"};
	
	boolean main;
	int count;
	List<List<String>> values;
	
	public MultiArrayValue() {
		this(false, 5, createList());
	}
	
	public MultiArrayValue(boolean main, int count, List<List<String>> values) {
		this.main = main;
		this.count = count;
		this.values = values;
	}
	
	public static List<MultiArrayValue> defaultValue() {
		List<MultiArrayValue> result = new ObjectArrayList<>();
		result.add(new MultiArrayValue());
		result.add(new MultiArrayValue());
		result.add(new MultiArrayValue());
		result.add(new MultiArrayValue());
		return result;
	}
	
	public static ParseResult<MultiArrayValue> parse(ParsedMap map) {
		ParseResult<Boolean> main = map.getOrError("main", Boolean.class, "Variable [main] couldn't be Parsed");
		if(main.hasError()) return main.onlyError();
		ParseResult<Integer> count = map.getOrError("count", Integer.class, "Variable [count] couldn't be Parsed");
		if(count.hasError()) return count.onlyError();
		ParseResult<ParsedList> values = map.getOrError("values", ParsedList.class, "Variable [values] couldn't be Parsed");
		if(values.hasError()) return values.onlyError();
		List<List<String>> list = new ObjectArrayList<>();
		for(ParsedList entry : values.getValue().typedIterator(ParsedList.class)) {
			list.add(entry.collect(String.class, new ObjectArrayList<>()));
		}
		return ParseResult.success(new MultiArrayValue(main.getValue(), count.getValue(), list));
	}
	
	public static IConfigSerializer<MultiArrayValue> createSerializer() {
		CompoundBuilder builder = new CompoundBuilder().setNewLined(true);
		builder.simple("main", EntryDataType.BOOLEAN).finish();
		builder.simple("count", EntryDataType.INTEGER).finish();
		builder.nested("values", ListBuilder.variants(ListBuilder.of(EntryDataType.STRING).build(false)).build(true)).finish();
		return IConfigSerializer.noSync(builder.build(), new MultiArrayValue(), MultiArrayValue::parse, MultiArrayValue::serialize);
	}
	
	public ParsedMap serialize() {
		ParsedMap map = new ParsedMap();
		map.put("main", main);
		map.put("count", count);
		ParsedList list = new ParsedList();
		for(List<String> entry : values) {
			list.add(new ParsedList(entry));
		}
		map.put("values", list);
		return map;
	}
	
	private static List<List<String>> createList() {
		List<List<String>> list = new ObjectArrayList<>();
		for(int i = 0;i<5;i++) {
			List<String> entries = new ObjectArrayList<>();
			for(int j = 0;j<=i;j++) {
				entries.add(VALUES[j]);
			}
			list.add(entries);
		}
		return list;
	}
}
