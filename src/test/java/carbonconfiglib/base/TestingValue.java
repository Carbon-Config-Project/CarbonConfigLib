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
		boolean second = false;
		try {
			int year = Integer.parseInt(value[1]);
			second = true;
			double fluffyness = Double.parseDouble(value[2]);
			return ParseResult.success(new TestingValue(value[0], year, fluffyness));
		} catch (Exception e) {
			return ParseResult.error(Helpers.mergeCompound(value), e, "Couldn't parse ["+(second ? "Fluffyness" : "Year")+"] argument");
		}
	}
	
	public String[] serialize() {
		return new String[] {name, Integer.toString(year), Double.toString(fluffyness)};
	}
}
