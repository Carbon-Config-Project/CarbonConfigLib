package carbonconfiglib.base;

import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.ParseResult;

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
