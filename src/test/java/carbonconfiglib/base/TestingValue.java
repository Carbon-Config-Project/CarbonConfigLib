package carbonconfiglib.base;

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
	
	public static TestingValue parse(String value) {
		String[] split = value.split(";");
		if(split.length != 3) return null;
		try {
			return new TestingValue(split[0], Integer.parseInt(split[1]), Double.parseDouble(split[2]));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String serialize() {
		return name+";"+year+";"+fluffyness;
	}
}
