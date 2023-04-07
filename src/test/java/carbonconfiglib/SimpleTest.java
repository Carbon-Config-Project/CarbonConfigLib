package carbonconfiglib;

import carbonconfiglib.api.IConfigSerializer;
import carbonconfiglib.base.SimpleLogger;
import carbonconfiglib.base.TestingValue;
import carbonconfiglib.config.Config;
import carbonconfiglib.config.ConfigEntry.ArrayValue;
import carbonconfiglib.config.ConfigEntry.BoolValue;
import carbonconfiglib.config.ConfigEntry.DoubleValue;
import carbonconfiglib.config.ConfigEntry.EnumValue;
import carbonconfiglib.config.ConfigEntry.IntValue;
import carbonconfiglib.config.ConfigEntry.ParsedValue;
import carbonconfiglib.config.ConfigEntry.StringValue;
import carbonconfiglib.config.ConfigHandler;
import carbonconfiglib.config.ConfigSection;
import carbonconfiglib.config.ConfigSettings;
import carbonconfiglib.config.FileSystemWatcher;
import carbonconfiglib.utils.AutomationType;
import carbonconfiglib.utils.IEntryDataType;
import carbonconfiglib.utils.IEntryDataType.CompoundDataType;
import carbonconfiglib.utils.IEntryDataType.EntryDataType;
import carbonconfiglib.utils.MultilinePolicy;

import java.nio.file.Paths;

public class SimpleTest {
	public static final FileSystemWatcher WATCHER = new FileSystemWatcher(new SimpleLogger(), Paths.get("run"), null);
	public static BoolValue FLAG;
	public static IntValue INTS;
	public static DoubleValue DOUBLES;
	public static StringValue STRINGS;
	public static ArrayValue ARRAY;
	public static EnumValue<AutomationType> ENUMS;
	public static ParsedValue<TestingValue> PARSED;
	
	public static void main(String...args) {
		Config config = new Config("testing");
		ConfigSection testSection = config.add("general");
		FLAG = testSection.addBool("booleanTest", false, "Testing the Boolean");
		INTS = testSection.addInt("IntTest", 0, "Testing the Int").setMax(512);
		DOUBLES = testSection.addDouble("DoubleTests", 0, "Testing the Double").setMax(512);
		STRINGS = testSection.addString("StringTest", "Testing my StringValue", "Test the String");
		ARRAY = testSection.addArray("ArrayTest", new String[] {"Testing1", "Testing2", "Testing3", "Testing4", "Testing5", "Testing6", "Testing7", "Testing8", "Testing9", "Testing10"}, "Testing the Array");
		ENUMS = testSection.addEnum("EnumTest", AutomationType.NONE, AutomationType.class, "Testing the Enum");
		CompoundDataType type = new CompoundDataType().with("Name", EntryDataType.STRING).with("Year", EntryDataType.NUMBER).with("Fluffyness", EntryDataType.NUMBER);
		PARSED = testSection.addParsed("ParseTest", new TestingValue(), IConfigSerializer.noSync(type, new TestingValue(), TestingValue::parse, TestingValue::serialize));
		
		ConfigHandler handler = WATCHER.createConfig(config, ConfigSettings.withLinePolicy(MultilinePolicy.MULTILINE_IF_TO_LONG));
		handler.init();
		System.out.println("Generated Config");
		System.out.println(ARRAY.getEntries());
	}
}
