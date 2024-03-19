package carbonconfiglib.base;

import java.util.List;
import java.util.Objects;

import carbonconfiglib.api.IConfigSerializer;
import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.ParsedCollections.ParsedList;
import carbonconfiglib.utils.ParsedCollections.ParsedMap;
import carbonconfiglib.utils.structure.IStructuredData.EntryDataType;
import carbonconfiglib.utils.structure.StructureCompound.CompoundBuilder;
import carbonconfiglib.utils.structure.StructureList.ListBuilder;
import speiger.src.collections.objects.lists.ObjectArrayList;

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
public class TestingValue {
	String name;
	int year;
	double fluffyness;
	List<String> list;
	
	public TestingValue() {
		this("Testing", 2000, 512.2423, ObjectArrayList.wrap("One", "Two", "Three", "Four"));
	}
	
	public TestingValue(String name, int year, double fluffyness, List<String> list) {
		this.name = name;
		this.year = year;
		this.fluffyness = fluffyness;
		this.list = list;
	}
	
	public static IConfigSerializer<TestingValue> createSerializer() {
		CompoundBuilder builder = new CompoundBuilder().setNewLined(true);
		builder.simple("Name", EntryDataType.STRING).finish();
		builder.simple("Year", EntryDataType.INTEGER).finish();
		builder.simple("Fluffyness", EntryDataType.DOUBLE).finish();
		builder.list("Counter", ListBuilder.of(EntryDataType.STRING).build(false)).finish();
		return IConfigSerializer.noSync(builder.build(), new TestingValue(), TestingValue::parse, TestingValue::serialize);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof TestingValue) {
			TestingValue other = (TestingValue)obj;
			return Objects.equals(other.name, name) && other.year == year && Double.compare(other.fluffyness, fluffyness) == 0 && Objects.equals(other.list, list);
		}
		return false;
	}
	
	public static ParseResult<TestingValue> parse(ParsedMap map) {
		ParseResult<String> name = map.getOrError("Name", String.class);
		if(name.hasError()) return name.onlyError();
		ParseResult<Integer> year = map.getOrError("Year", Integer.class);
		if(year.hasError()) return year.onlyError();
		ParseResult<Double> fluff = map.getOrError("Fluffyness", Double.class);
		if(fluff.hasError()) return fluff.onlyError();
		ParseResult<ParsedList> list = map.getOrError("Counter", ParsedList.class);
		if(list.hasError()) return list.onlyError();
		return ParseResult.success(new TestingValue(name.getValue(), year.getValue(), fluff.getValue(), list.getValue().collect(String.class, new ObjectArrayList<>())));
	}
	
	public ParsedMap serialize() {
		ParsedMap map = new ParsedMap();
		map.put("Name", name);
		map.put("Year", year);
		map.put("Fluffyness", fluffyness);
		ParsedList list = new ParsedList();
		this.list.forEach(list::add);
		map.put("Counter", list);
		return map;
	}
}
