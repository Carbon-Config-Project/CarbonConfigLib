package carbonconfiglib;

import java.nio.file.Paths;

import carbonconfiglib.ConfigEntry.ArrayValue;
import carbonconfiglib.ConfigEntry.BoolValue;
import carbonconfiglib.ConfigEntry.DoubleValue;
import carbonconfiglib.ConfigEntry.EnumValue;
import carbonconfiglib.ConfigEntry.IntValue;
import carbonconfiglib.ConfigEntry.StringValue;
import carbonconfiglib.base.SimpleLogger;

public class SimpleTest {
	public static final FileSystemWatcher WATCHER = new FileSystemWatcher(new SimpleLogger(), Paths.get("run"), null);
	public static BoolValue FLAG;
	public static IntValue INTS;
	public static DoubleValue DOUBLES;
	public static StringValue STRINGS;
	public static ArrayValue ARRAY;
	public static EnumValue<AutomationType> ENUMS;
	
	public static void main(String...args) {
		Config config = new Config("testing");
		ConfigSection testSection = config.add("general");
		FLAG = testSection.addBool("booleanTest", false, "Testing the Boolean");
		INTS = testSection.addInt("IntTest", 0, "Testing the Int").setMax(512);
		DOUBLES = testSection.addDouble("DoubleTests", 0, "Testing the Double").setMax(512);
		STRINGS = testSection.addString("StringTest", "Testing my StringValue", "Test the String");
		ARRAY = testSection.addArray("ArrayTest", new String[] {"Test1", "Test2", "Test3"}, "Testing the Array");
		ENUMS = testSection.addEnum("EnumTest", AutomationType.NONE, AutomationType.class, "Testing the Enum");
		
		ConfigHandler handler = WATCHER.createConfig(config, ConfigSettings.of());
		handler.init();
		System.out.println("Generated Config");
		System.out.println(ARRAY.getEntries());
	}
}
