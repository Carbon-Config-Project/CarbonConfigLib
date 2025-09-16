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

## Implement Translation Data (For GUI's)

How to add Translation Keys into the config for UI's so they can be auto translated

```java
	public static BoolValue DO_STUFF;

	public static void main(String...args) {
		Config config = new Config("example");
		ConfigSection section = config.add("general");
		DO_STUFF = section.addBool("Do Stuff?", false).setTranslationKey("MyTranslationKey").setTranslationValue("MyTranslationValue");
		CONFIG = WATCHER.create(config);
		CONFIG.register();
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
			PARSED = section.addParsed("ParseTest", new TestingValue(), TestingValue.createSerializer());
		}
		
		public class TestingValue {
			String name;
			int year;
			double fluffyness;
	
			public TestingValue() {
				this("Testing", 2000, 512.2423);
			}
			
			/**
			 * Constructor that uses the simple approach.
			 * Any Exception thrown is caught by the serializer and marked as parsing failure
			 * ParsedMap has option to safely parse data as well but this is the simplest approach
			 */
			public TestingValue(ParsedMap data) {
				name = data.getOrThrow("Name", String.class);
				year = data.getOrThrow("Year", Integer.class);
				fluffyness = data.getOrThrow("Fluffyness", Double.class);
			}
			
			public TestingValue(String name, int year, double fluffyness) {
				this.name = name;
				this.year = year;
				this.fluffyness = fluffyness;
			}
			
			/**
			 * Function that creates the serializer.
			 * The Compound Builder is used to build your data structure layout. 
			 * Where you can define if it should be compact or spread out, with the setNewLined function.
			 * A entry can be started with the "simple/variant/enum/nested" function.
			 * After that optionally a "addSuggestion" or "setComments" or "forceSuggestions" can be used to expand on information for each.
			 * Though these are mainly for GUI implementations specifically.
			 * 
			 * The nested starter is used for allowing to append custom data structures such as other compounds or list. 
			 * (Recursion is needed)
			 *
			 * Then a Example value and a Parse/Serialize function is required to complete things.
			 * There can be an optional Validator provided which double checks the values themselves and return information what went wrong. 
			 */
			public static IConfigSerializer<TestingValue> createSerializer() {
				CompoundBuilder builder = new CompoundBuilder().setNewLined(true);
				builder
					.simple("Name", EntryDataType.STRING);
					.simple("Year", EntryDataType.INTEGER);
					.simple("Fluffyness", EntryDataType.DOUBLE);
				return IConfigSerializer.simple(builder.build(), new TestingValue(), TestingValue::new, TestingValue::serialize);
			}
			
			/**
			 * The Advanced Parsing function that allows to also control the ParseResult with the failure.
			 * By default the simple function will handle that and cover most cases you would ever want with this.
			 * Reducing the Verboseness drastically.
			 * This advanced version also asks you to handle the exceptions and errors and deal with them in a controlled manor.
			 */
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
			
			
			/**
			 * Serialize function must be a ParsedMap and you throw in your variables like JsonObjects.
			 * As long as the Values inserted match the Serializer format it can automatically serialize it without any issues.
			 */
			public ParsedMap serialize() {
				ParsedMap map = new ParsedMap();
				map.put("Name", name);
				map.put("Year", year);
				map.put("Fluffyness", fluffyness);
				return map;
			}
			
			/**
			 * The equals function has to be overriden to validate that the config entries have changed or not.
			 * Otherwise gui/network implementations never know when a Object is changed.
			 */
			@Override
			public boolean equals(Object obj) {
				if(obj instanceof TestingValue) {
					TestingValue other = (TestingValue)obj;
					return Objects.equals(other.name, name) && other.year == year && Double.compare(other.fluffyness, fluffyness) == 0;
				}
				return false;
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
	public IStructuredData getDataType() {
		return SimpleData.variant(EntryDataType.INTEGER, ColorWrapper.class);
	}
	
	public int get() {
		return getValue().getColor();
	}
	
	public int getRGB() {
		return getValue().getColor() & 0xFFFFFF;
	}
	
	public int getRGBA() {
		return getValue().getColor() & 0xFFFFFFFF;
	}
	
	public String toHex() {
		return ColorWrapper.serialize(getValue().getColor());
	}
	
	public String toRGBHex() {
		return ColorWrapper.serializeRGB(getValue().getColor() & 0xFFFFFF);
	}
	
	public String toRGBAHex() {
		return ColorWrapper.serialize(getValue().getColor() & 0xFFFFFFFF);
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
	
	public static class ColorWrapper extends Number {
		private static final long serialVersionUID = -6737187197596158253L;
		int color;
		
		public ColorWrapper(int color) {
			this.color = color;
		}
		
		public int getColor() {
			return color;
		}
		
		public int intValue() { return color; }
		public long longValue() { return (long)color; }
		public float floatValue() { return (float)color; }
		public double doubleValue() { return (double)color; }
		
		public String serialize() {
			return serialize(color);
		}
		
		public static ParseResult<ColorWrapper> parse(String value) {
			try { return ParseResult.success(new ColorWrapper(Long.decode(value).intValue())); }
			catch (Exception e) { return ParseResult.error(value, e, "Couldn't parse Colour"); }
		}
		
		public static ParseResult<Integer> parseInt(String value) {
			try { return ParseResult.success(Long.decode(value).intValue()); }
			catch (Exception e) { return ParseResult.error(value, e, "Couldn't parse Colour"); }
		}
		
		public static String serializeRGB(long color) {
			return "0x"+(Long.toHexString(0xFF000000L | (color & 0xFFFFFFL)).substring(2));
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