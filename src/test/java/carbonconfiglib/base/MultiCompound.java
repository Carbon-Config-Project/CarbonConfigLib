package carbonconfiglib.base;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import carbonconfiglib.api.IConfigSerializer;
import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.ParsedCollections.ParsedList;
import carbonconfiglib.utils.ParsedCollections.ParsedMap;
import carbonconfiglib.utils.structure.IStructuredData.EntryDataType;
import carbonconfiglib.utils.structure.StructureCompound.CompoundBuilder;
import carbonconfiglib.utils.structure.StructureList.ListBuilder;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.maps.impl.hash.Object2ObjectLinkedOpenHashMap;

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
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MultiCompound) {
			MultiCompound other = (MultiCompound)obj;
			return other.main == main && other.count == count && Objects.equals(other.maps, maps);
		}
		return false;
	}
	
	public static IConfigSerializer<MultiCompound> createSerializer() {
		CompoundBuilder innerBuilder = new CompoundBuilder().setNewLined(false);
		for(int i = 0;i<5;i++) {
			innerBuilder.simple(VALUES[i], EntryDataType.INTEGER).finish();
		}
		
		CompoundBuilder builder = new CompoundBuilder().setNewLined(true);
		builder.simple("main", EntryDataType.BOOLEAN).finish();
		builder.simple("count", EntryDataType.INTEGER).finish();
		builder.list("maps", ListBuilder.object(innerBuilder.build(), MultiCompound::parseMap, ParsedMap::new).build(true)).finish();
		return IConfigSerializer.noSync(builder.build(), new MultiCompound(), MultiCompound::parse, MultiCompound::serialize);
	}
	
	private static ParseResult<Map<String, Integer>> parseMap(ParsedMap map) {
		Map<String, Integer> output = new Object2ObjectLinkedOpenHashMap<>();
		for(String key : map.keySet()) {
			ParseResult<Integer> result = map.getOrError(key, Integer.class);
			if(result.hasError()) return result.onlyError();
			output.put(key, result.getValue());
		}
		return ParseResult.success(output);
	}
	
	
	public static ParseResult<MultiCompound> parse(ParsedMap map) {
		ParseResult<Boolean> main = map.getOrError("main", Boolean.class);
		if(main.hasError()) return main.onlyError();
		ParseResult<Integer> count = map.getOrError("count", Integer.class);
		if(count.hasError()) return count.onlyError();
		ParseResult<ParsedList> maps = map.getOrError("maps", ParsedList.class);
		if(maps.hasError()) return maps.onlyError();
		return ParseResult.success(new MultiCompound(main.getValue(), count.getValue(), maps.getValue().collectCollections(new ObjectArrayList<>())));
	}
	
	public ParsedMap serialize() {
		ParsedMap map = new ParsedMap();
		map.put("main", main);
		map.put("count", count);
		map.put("maps", new ParsedList(maps));
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
