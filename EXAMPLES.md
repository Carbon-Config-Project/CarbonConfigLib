# Examples

This Page provides Examples how to do certain things.
Everything a expansion of the [Basic Config](#basic-config) example.

## Basic Config

How to create a Basic Config Instance

```java
public static final FileSystemWatcher WATCHER = new FileSystemWatcher(new SystemLogger(), Paths.get("config"), null);
public static ConfigHandler CONFIG;
public static BoolValue DO_STUFF;

	public static void main(String...args) {
		Config config = new Config("example");
		ConfigSection section = config.add("general");
		DO_STUFF = section.addBool("Do Stuff?", false);
		CONFIG = WATCHER.create(config);
		CONFIG.register();
	}
	
	public static void changeStuff(boolean value) {
		DO_STUFF.set(value);
		CONFIG.save();
	}
```

## Implement Auto Reloading

To Implement Auto Reload functionality it is simply required to process file system changes.    
Which can be done by simply calling: FileSystemWatcher.processFileSystemEvents    
This can be done on any thread, but the thread that is executing this code is also the code that also triggers the Reload-Listeners.    

```java
	public void update() {
		//Detects file changes, not required fully optional
		WATCHER.processFileSystemEvents();
	}
```

## Implement Auto Synchronization

There is a few things that have to be implemented by the user.    
Such as networking.
For networking specifically there has to be 3 things that have to be implemented.    
- 1: The packet itself that sync's the data over. For obvious reason that can not be provided.    
- 2: Implement the initial connection logic that send the initial synchronized configs.    
- 3: Implement the network synchronization when a config changes, which can be done with the IConfigChangeListener#onConfigChanged function.    

The first one is the hardest to do.    
But here is a example.   

<details>
<summary>Packet Code</summary>
<p>

```java
	public class SyncPacket
	
		String identifier;
		SyncType type;
		Map<String, byte[]> entries = new Object2ObjectLinkedOpenHashMap<>();
		
		public SyncPacket() {
		}
		
		public SyncPacket(String identifier, SyncType type, Map<String, byte[]> entries) {
			this.identifier = identifier;
			this.type = type;
			this.entries = entries;
		}
		
		public static SyncPacket create(ConfigHandler handler, SyncType type, boolean forceSync) {
			if(!handler.isLoaded()) return null;
			Map<String, byte[]> data = new Object2ObjectLinkedOpenHashMap<>();
			ByteArrayOutputStream buf = new ByteArrayOutputStream(); //using javaIO as a example here but isn't required.
			IWriteBuffer buffer = //My Write Buffer implementation.
			for(Map.Entry<String, ConfigEntry<?>> entry : handler.getConfig().getSyncedEntries(type).entrySet()) {
				ConfigEntry<?> value = entry.getValue();
				if(forceSync || value.hasChanged()) {
					buf.clear();
					value.serialize(buffer);
					data.put(entry.getKey(), buf.array());
					value.onSynced();
				}
			}
			return data.isEmpty() ? null : new SyncPacket(handler.getConfigIdentifer(), type, data);
		}
		
		public ReloadMode processEntry(UUID connection) {
			if(entries.isEmpty()) return null;
			ConfigHandler cfg = //My FileSystemWatcher getting the config based on the Identifier
			if(cfg == null) {
				CarbonConfig.LOGGER.warn("Received packet for ["+identifier+"] which didn't exist!");
				return null;
			}
			Map<String, ConfigEntry<?>> mapped = cfg.getConfig().getSyncedEntries(type);
			boolean hasChanged = false;
			UUID owner = player.getUUID();
			ReloadMode mode = null;
			for(Entry<String, byte[]> dataEntry : entries.entrySet()) {
				ConfigEntry<?> entry = mapped.get(dataEntry.getKey());
				if(entry != null) {
					IReadBuffer buffer = //MyReadBuffer implementation.
					entry.deserialize(buffer, connection);
					if(entry.hasChanged()) {
						hasChanged = true;
						mode = ReloadMode.or(mode, entry.getReloadState());
					}
					entry.onSynced();
				}
			}
			if(hasChanged) {
				cfg.onSynced();
				return mode;
			}
			return null;
		}
```

</p>
</details>

The second problem is basically a callback that just gathers all configs on the side it is currently on and sends it to the other one.   
While the last one is simply using the IConfigChangeListener#onConfigChanged that can be used to gather all changes and send them to the opposite side.

## Creating Server to Client Synced Entries.

This is under the assumption that Networking is provided by the user of said library.    

```java
public static BoolValue SERVER_TO_CLIENT_EXAMPLE;
public static SyncedConfig<BoolValue> CLIENT_TO_SERVER_EXAMPLE;


	public static void main(String...args) {
		SERVER_TO_CLIENT_EXAMPLE = section.addBool("Server Variable", false).setServerSynced();
		CLIENT_TO_SERVER_EXAMPLE = section.addBool("Client Variable", false).setClientSynced();
	}
	
	public boolean get(UUID client) {
		return CLIENT_TO_SERVER_EXAMPLE.get(client).get();
	}
	
```
Server to client sync will automatically sync the variables from server/to client when a change happens and updates it accordingly.    
Client to server sync will also automatically sync from the client to the server upon change.     
And this uses the UUID system since that is a common standard for identifying users in the first place.    
If no user variable was defined for whatever reason it will use the server default setting instead of the clients override.    

Note this relies on that the network synchronization was implemented.    

## Creating Mapped Configs

Sometimes you have a config that has a given key but all the provided implementations only provide Singletons/Array/Collections which are not key/value based.    
This is where Mapped Configs come into play.   
These use a Key/Value Generator to generate the key and the value.   

```java
public static MappedConfig<String, String> MAPPED_CONFIG;

	public static void main(String...args) {
		MAPPED_CONFIG = MappedConfig.create(CONFIG, section.addArray("Mapped Example", new String[]{"key1:value1", "key2:value2", "key3:value1"}), T -> T.split(":")[0], T -> T.split(":")[1]);
	}
```
As can be seen its fully control over how the key and the value is generated and it works with any Array/Collection based config.


## Config Settings

The Config Settings are custom overrides for each config file to allow customization.   

### SubFolder

Creates a SubFolder the Config is placed in.    
Multiple Configs can have the same subfolder.

```java
	public static void main(String...args) {
		CONFIG = WATCHER.create(config, ConfigSettings.withFolder("subFolder"));
	}
```

### Custom Logger

If the config should have its own Logger implementation this can be done with the "withLog/withLogger" function.

```java
	public static void main(String...args) {
		CONFIG = WATCHER.create(config, ConfigSettings.withLog(new MyILoggerImplementation()));
	}
```

### Custom Paths/Proxies

Sometimes a custom path or a Instance specific path is required.    
This can be done with Proxies.     
Proxies give you the ability to define where the base path is actually set.    
And on top of that allow multiple layers of Config Files to fetch from.    

Here is a basic Path example:    

```java
	public static void main(String...args) {
		CONFIG = WATCHER.create(config, ConfigSettings.withPath(Paths.get("myCustomFolder")));
	}
```

This simply creates a Singleton Proxy that links to the exact path you have provided.

Another way to do this is like this:    

```java
	public static void main(String...args) {
		CONFIG = WATCHER.create(config, ConfigSettings.withFolderProxy(new MyCustomProxy()));
	}
	
	public static class MyCustomProxy implements IConfigProxy {
		public Path getBasePath(Path configFile) {
			Path path = Paths.get("MyBaseFolder/newInstance");
			return Files.exist(path.resolve(configFile) ? path : Paths.get("MyBaseFolder");
		}
		
		public boolean isDynamicProxy() {
			return true;
		}
		
		public List<? extends IPotentialTarget> getPotentialConfigs() {
			List<Path> potentialPaths = Arrays.asList(Paths.get("MyBaseFolder/newInstance"), Paths.get("MyBaseFolder"));
			return potentialPaths.stream().map(path -> new SimpleTarget(path, Helpers.firstLetterUppercase(path.getFileName().toString()))).toList();
		}
	}
```

As can be seen a Proxy allows to provide a bunch of paths where the Config Folder should be present.   
This folder structure can be seen as a Stack where the top most element is the First folder a config should be checked for.    
And from then each layer you go down on the config file, if not present, will be copied from the previous upper layer.    
This allows to provide default Configs Files too that will be automatically copied when a new instance is created.    
"isDynamic" proxy just defines if the config can be automatically loaded upon registration.    
getPotentialConfigs is a helper function for GUI implementations so all folders that could contain the config can be found.    


## Custom DataTypes

Sometimes your Data type is so complex that you have to do it in a String form and parse it later.    
But then the format has to be explained and validated in some form.    
And if a GUI is provided it automatically doesn't support it easily for the user.   
This is where ParsedValues come into play.    
Parsed Values are basically a Compound that you provide a spec and a parse/serialization function for.   
And the Config itself manages everything in between.    

```java
public static ParsedValue<TestingValue> PARSED;

		public static void main(String...args) {
			/**
			 * Creating the Spec.
			 * Which has to be the order it is parsed in.
			 * Valid dataTypes are: Boolean, Integer, Double, String, Custom
			 * Custom can be for example a "Color" which is a Integer in the background but still should be maybe treated differently in the gui then in the text config.
			 */
			CompoundDataType type = new CompoundDataType().with("Name", EntryDataType.STRING).with("Year", EntryDataType.INTEGER).with("Fluffyness", EntryDataType.DOUBLE);
			
			/**
			 * Creating the Serializer.
			 * Which requires a Spect, a Example value, The parse function, which is a function that is expected to never throw exceptions, and the serialize function.
			 * Optionally if Sync should be supported a Network Buffer write/read function should be provided.
			 * Serializer can be shared and do not have to be created between config instances.
			 * A default value has to be provided because the Spec with a Example is automatically generated with the config description so this doesn't have to be done by default.
			 */
			IConfigSerializer serializer = IConfigSerializer.noSync(type, new TestingValue(), TestingValue::parse, TestingValue::serialize);
			
			/**
			 * Creating the Config.
			 */
			PARSED = section.addParsed("ParseTest", new TestingValue(), serializer);
		}
		
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
			
			/**
			 * The Parsing function, that creates out of a String array the Value that is desired.
			 * Instead of Throwing Exceptions the idea here is that the Parse Result is returned with a Error.
			 * Said errors should have a description what wen't wrong or if a exception during parsing was thrown, for example number parsing, then the exception is put into the parse result itself.
			 * Helper functions like error only are there to simply redirect the errors further up the chain.
			 * Extra information can be optionally included if so desired.
			 * Also another helper function: "withDefault" can be used that just replaces the return value instead that can be used later on.
			 * Not specifically with "ParsedValues" but with custom Config Entries.
			 */
			public static ParseResult<TestingValue> parse(String[] value) {
				if(value.length != 3) return ParseResult.error(Helpers.mergeCompound(value), "3 Elements are required");
				if(value[0] == null || value[0].trim().isEmpty()) return ParseResult.error(value[0], "Value [Name] is not allowed to be null/empty");
				ParseResult<Integer> year = Helpers.parseInt(value[1]);
				if(year.hasError()) return year.onlyError("Couldn't parse [Year] argument");
				ParseResult<Double> fluffyness = Helpers.parseDouble(value[2]);
				if(fluffyness.hasError()) return fluffyness.onlyError("Couldn't parse [Fluffyness] argument");
				return ParseResult.success(new TestingValue(value[0], year.getValue(), fluffyness.getValue()));
			}
			
			/**
			 * Serialization function. Simply return a String array where each entry is a value within the spec.
			 */
			public String[] serialize() {
				return new String[] {name, Integer.toString(year), Double.toString(fluffyness)};
			}
		}
```

## Custom Config Entries

Carbon Config provides a somewhat simple way to create custom Config Entries   
This can be easily done by implementing one of the 3 abstract classes:   
- BasicConfigEntry for simple single config values
- ArrayConfigEntry for anything that should use an array
- CollectionConfigEntry for anything that should use a collection.

Here is a example of a Color Config that parses from/serializes into hex values.

<details>
<summary>ColorValue Code</summary>
<p>

```java
public class ColorValue extends BasicConfigEntry<ColorWrapper>
{
	public ColorValue(String key, int defaultValue, String... comment) {
		super(key, new ColorWrapper(defaultValue), comment);
	}
	
	/**
	 * Creates a copy of the Entry.
	 * Make sure everything is included, including filters.
	 */
	@Override
	protected ColorValue copy() {
		return new ColorValue(getKey(), get(), getComment());
	}
	
	/**
	 * Parse function
	 */
	@Override
	public ParseResult<ColorWrapper> parseValue(String value) {
		ParseResult<Integer> result = ColorWrapper.parseInt(value);
		return result.hasError() ? result.onlyError() : ParseResult.success(new ColorWrapper(result.getValue()));
	}
	
	/**
	 * Data Type for GUI Implementations.
	 * In this example the color type is expected to be supported using the custom type.
	 * (The special thing is that the color would be displayed in the GUI as you type it in)
	 */
	@Override
	public IEntryDataType getDataType() {
		return SimpleDataType.ofVariant(ColorWrapper.class);
	}
	
	public int get() {
		return getValue().getColor();
	}
	
	/**
	 *	Serializer override that can be used to properly display all values.
	 * Required for arrays but a array specific helper function called "serializeArray" is provided.
	 */
	protected String serializedValue(MultilinePolicy policy, ColorWrapper value) {
		return ColorWrapper.serialize(value.getColor());
	}
	
	/**
	 * Identifier for "Late Loading" Support.
	 * Make sure its a unused one.
	 */
	@Override
	public char getPrefix() {
		return 'C';
	}
	
	/**
	 * Function for displaying "Range limitations" or valid values if these are easily detectable.
	 * For Enum for example it returns all valid enum values, while for numbers the valid range.
	 */
	@Override
	public String getLimitations() {
		return "";
	}
	
	/**
	 * Network support. Write function.
	 */
	@Override
	public void serialize(IWriteBuffer buffer) {
		buffer.writeInt(get());
	}
	
	/**
	 * Network support. Read function.
	 */
	@Override
	protected void deserializeValue(IReadBuffer buffer) {
		set(new ColorWrapper(buffer.readInt()));
	}
	
	/**
	 * Helper function for late loading. This one is used registered with.
	 */
	public static ParseResult<ColorValue> parse(String key, String value, String... comment) {
		ParseResult<Integer> result = ColorWrapper.parseInt(value);
		if (result.hasError()) return result.withDefault(new ColorValue(key, 0, comment));
		return ParseResult.success(new ColorValue(key, result.getValue(), comment));
	}
	
	public static class ColorWrapper {
		int color;
		
		public ColorWrapper(int color) {
			this.color = color;
		}
		
		public int getColor() {
			return color;
		}
		
		public static ParseResult<Integer> parseInt(String value) {
			try { return ParseResult.success(Long.decode(value).intValue()); }
			catch (Exception e) { return ParseResult.error(value, e, "Couldn't parse Colour"); }
		}
		
		public static String serialize(long color) {
			return "0x"+(Long.toHexString(0xFF00000000L | (color & 0xFFFFFFFFL)).substring(2));
		}
	}
}
```

</p>
</details>

As can be seen it is rather straight forward.   
The simple implementation is enough to use said custom entry.   
But that alone doesn't support late loading. For that just 1 tiny function is required.   

```java
	public static void main(String...args) {
		CONFIG = WATCHER.create(config);
		CONFIG.addParser('C', ColorValue::parse);
		CONFIG.register();
	}
```
If your parse for some reason can't be parsed until a certain step you can also use "addTempParser" which just stores it as a string and parses it when the config entry is registered.