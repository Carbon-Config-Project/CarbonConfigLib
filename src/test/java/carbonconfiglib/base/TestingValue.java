package carbonconfiglib.base;

import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.ParseResult;

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
	
	public TestingValue() {
		this("Testing", 2000, 512.2423);
	}
	
	public TestingValue(String name, int year, double fluffyness) {
		this.name = name;
		this.year = year;
		this.fluffyness = fluffyness;
	}
	
	public static ParseResult<TestingValue> parse(String[] value) {
		if(value.length != 3) return ParseResult.error(Helpers.mergeCompound(value), "3 Elements are required");
		if(value[0] == null || value[0].trim().isEmpty()) return ParseResult.error(value[0], "Value [Name] is not allowed to be null/empty");
		ParseResult<Integer> year = Helpers.parseInt(value[1]);
		if(year.hasError()) return year.onlyError("Couldn't parse [Year] argument");
		ParseResult<Double> fluffyness = Helpers.parseDouble(value[2]);
		if(fluffyness.hasError()) return fluffyness.onlyError("Couldn't parse [Fluffyness] argument");
		return ParseResult.success(new TestingValue(value[0], year.getValue(), fluffyness.getValue()));
	}
	
	public String[] serialize() {
		return new String[] {name, Integer.toString(year), Double.toString(fluffyness)};
	}
}
