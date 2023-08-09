[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![](https://jitpack.io/v/Carbon-Config-Project/CarbonConfigLib.svg)](https://jitpack.io/#Carbon-Config-Project/CarbonConfigLib)
# Carbon Config

<img src="img/logo.png" width="15%">

Carbon Config is a Library that allows simple ways to create/manage configuration files.    
It is designed to be simple yet flexible.   
On top of that it was developed with feature expansion in mind.    

## How to install

Using Gradle:

```
repositories {
	maven { url = "https://jitpack.io" }
}
dependencies {
	implementation 'com.github.Carbon-Config-Project:CarbonConfigLib:1.0.0'
}

```

## Creating a config instance

To Create a Simple Config Instance this is all the code required:    

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
	
	public void update() {
		//Detects file changes, not required fully optional
		WATCHER.processFileSystemEvents();
	}
	
	public static void changeStuff(boolean value) {
		DO_STUFF.set(value);
		CONFIG.save();
	}
```

The register function, unless differently specified, automatically loads the config.

A simple example can be found [Here](src/test/java/carbonconfiglib/SimpleTest.java)   
A more extensive example/explanation list can be found [Here](EXAMPLES.md)    


## Features

A list of Features that Carbon Config supports.   

- **Auto Reload**    
  ``Reloads the Config File if it was changed from a External Source``
- **Auto Sync**    
  ``If your application requires network synchronization the Library provides ways to support that too``
- **Delayed Load**     
  ``If a Config Value wasn't registered at the point of loading, The Config will still load said value and keeps it temporarily stored until it gets registered, Temporary Values are not saved though until they were registered.``
- **Exceptionless Design**    
  ``Carbon Config is designed to not throw Exceptions, instead a Optional like structure was used to simply collect them and reset the values instead, with manual overrides``
- **Requirement Definition**    
  ``If a Config needs to restart the software, or have other specific requirements, Carbon Config provides ways to implement that without restricting it to one specific implementation``
- **Proxy Support**
  ``Some Configs need support for either Dynamic Folders that are selected on the load or just needs to be in a very specific folder. This is where proxies come into play. They allow to control where config files are placed and provide a hierarchy where default configs should be placed if that is needed.``
- **Per Config Overrides**
  ``While the Library allows to set global settings, per config settings are also a thing that can override settings if so desired. Such as: Custom Loggers/Folders/Rules which can be applied as needed but are not required.``
- **Custom Data Types/Compounds**    
  ``If a really specific data type is required, Compounds are the solution as they allow parsing of custom data and instead of storing Strings it will instead store the data type``
- **Expandable**     
  ``Is a DataType not supported? It can be simply implemented and extended on to the library``
- **Hidden Support**    
  ``Some Configs are only there for the developers to debug things. CarbonConfig also supports Hidden Configs. These simply load their defaults and can only be changed by Manually Writing them into the Config File. Allowing for debug tools to be present yet not add extra clutter to the Config file that isn't wanted.``
- **GUI Compatible**     
  ``While no Gui is provided by default it is written with having a GUI hooked into it in mind. The entire Config is written in a way were Guis can be hooked into the parsing/validation process and allow for customization without having to guess the data types or having to write magic to set values. If a value has to be set, it requests a string and the config itself is managing the parsing process and gives feedback of what went wrong.``
 