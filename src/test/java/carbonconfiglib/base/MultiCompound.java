package carbonconfiglib.base;

import java.util.List;
import java.util.Map;

import carbonconfiglib.api.IConfigSerializer;
import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.ParsedCollections.ParsedList;
import carbonconfiglib.utils.ParsedCollections.ParsedMap;
import carbonconfiglib.utils.structure.IStructuredData.EntryDataType;
import carbonconfiglib.utils.structure.StructureCompound.CompoundBuilder;
import carbonconfiglib.utils.structure.StructureList.ListBuilder;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.maps.impl.hash.Object2ObjectLinkedOpenHashMap;

public class MultiCompound
{
	private static final String[] VALUES = new String[] {"One", "Two", "Three", "Four", "Five"};
	boolean main;
	int count;
	List<Map<String, Integer>> maps;
	
	public MultiCompound() {
		this(true, 5, create());
	}
	
	public MultiCompound(boolean main, int count, List<Map<String, Integer>> maps) {
		this.main = main;
		this.count = count;
		this.maps = maps;
	}
	
	public static IConfigSerializer<MultiCompound> createSerializer() {
		CompoundBuilder innerBuilder = new CompoundBuilder().setNewLined(false);
		for(int i = 0;i<5;i++) {
			innerBuilder.simple(VALUES[i], EntryDataType.INTEGER).finish();
		}
		
		CompoundBuilder builder = new CompoundBuilder().setNewLined(true);
		builder.simple("main", EntryDataType.BOOLEAN).finish();
		builder.simple("count", EntryDataType.INTEGER).finish();
		builder.nested("maps", ListBuilder.variants(innerBuilder.build()).build(true)).finish();
		return IConfigSerializer.noSync(builder.build(), new MultiCompound(), MultiCompound::parse, MultiCompound::serialize);
	}
	
	public static ParseResult<MultiCompound> parse(ParsedMap map) {
		ParseResult<Boolean> main = map.getOrError("main", Boolean.class, "Variable [main] couldn't be Parsed");
		if(main.hasError()) return main.onlyError();
		ParseResult<Integer> count = map.getOrError("count", Integer.class, "Variable [count] couldn't be Parsed");
		if(count.hasError()) return count.onlyError();
		ParseResult<ParsedList> maps = map.getOrError("maps", ParsedList.class, "Variable [maps] couldn't be Parsed");
		if(maps.hasError()) return maps.onlyError();
		List<Map<String, Integer>> list = new ObjectArrayList<>();
		for(ParsedMap entry : maps.getValue().typedIterator(ParsedMap.class)) {
			Map<String, Integer> output = new Object2ObjectLinkedOpenHashMap<>();
			for(String key : entry.keySet()) {
				ParseResult<Integer> result = entry.getOrError(key, Integer.class, "Variable["+key+"] couldn't be Parsed");
				if(result.hasError()) return result.onlyError();
				output.put(key, result.getValue());
			}
			list.add(output);
		}
		return ParseResult.success(new MultiCompound(main.getValue(), count.getValue(), list));
	}
	
	public ParsedMap serialize() {
		ParsedMap map = new ParsedMap();
		map.put("main", main);
		map.put("count", count);
		ParsedList list = new ParsedList();
		for(Map<String, Integer> entryMap : maps) {
			list.add(new ParsedMap(entryMap));
		}
		map.put("maps", list);
		return map;
	}
	
	public static List<MultiCompound> defaultValues() {
		List<MultiCompound> list = new ObjectArrayList<>();
		list.add(new MultiCompound());
		list.add(new MultiCompound());
		list.add(new MultiCompound());
		list.add(new MultiCompound());
		list.add(new MultiCompound());
		return list;
	}
	
	private static List<Map<String, Integer>> create() {
		Map<String, Integer> result = new Object2ObjectLinkedOpenHashMap<>();
		for(int i = 0;i<5;i++) {
			result.put(VALUES[i], i);
		}
		List<Map<String, Integer>> list = new ObjectArrayList<>();
		for(int i = 0;i<5;i++) {
			list.add(result);
		}
		return list;
	}
}
