package carbonconfiglib;

import java.nio.file.Paths;

import carbonconfiglib.api.SystemLogger;
import carbonconfiglib.base.MultiArrayValue;
import carbonconfiglib.base.MultiCompound;
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
import carbonconfiglib.utils.MultilinePolicy;

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
public class SimpleTest {
	public static final FileSystemWatcher WATCHER = new FileSystemWatcher(new SystemLogger(), Paths.get("run"), null);
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
		ENUMS = testSection.addEnum("EnumTest", AutomationType.AUTO_LOAD, AutomationType.class, "Testing the Enum");
		PARSED = testSection.addParsed("ParseTest", new TestingValue(), TestingValue.createSerializer());
		testSection.addParsedArray("MultilineTest", MultiArrayValue.defaultValue(), MultiArrayValue.createSerializer());
		testSection.addParsedArray("MultiCompoundTest", MultiCompound.defaultValues(), MultiCompound.createSerializer());
		ConfigHandler handler = WATCHER.createConfig(config, ConfigSettings.withLinePolicy(MultilinePolicy.MULTILINE_IF_TO_LONG));
		handler.register();
		System.out.println("Generated Config");
	}
}
