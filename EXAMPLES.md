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

A Proper Tutorial can be found [Here](EXAMPLES.md).   
