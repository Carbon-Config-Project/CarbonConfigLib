package carbonconfiglib;

import java.nio.file.Path;

public class ConfigSettings {
	AutomationType type;
	ILogger logger;
	Path baseFolder;
	String subFolder;
	
	private ConfigSettings() {}
	
	public static ConfigSettings of() {
		return new ConfigSettings();
	}
	
	public static ConfigSettings withPath(Path baseFolder) {
		return new ConfigSettings().withBaseFolder(baseFolder);
	}
	
	public static ConfigSettings withSettings(AutomationType type) {
		return new ConfigSettings().withAutomation(type);
	}
	
	public static ConfigSettings withLog(ILogger logger) {
		return new ConfigSettings().withLogger(logger);
	}
	
	public static ConfigSettings withFolder(String subFolder) {
		return new ConfigSettings().withSubFolder(subFolder);
	}
	
	public ConfigSettings withBaseFolder(Path baseFolder) {
		if(this.baseFolder == null) this.baseFolder = baseFolder;
		return this;
	}
	
	public ConfigSettings withAutomation(AutomationType type) {
		if(this.type == null) this.type = type;
		return this;
	}
	
	public ConfigSettings withLogger(ILogger logger) {
		if(this.logger == null) this.logger = logger;
		return this;
	}
	
	public ConfigSettings withSubFolder(String subFolder) {
		if(this.subFolder == null) this.subFolder = subFolder;
		return this;
	}
	
	public Path getBaseFolder() {
		return baseFolder;
	}
	
	public ILogger getLogger() {
		return logger;
	}
	
	public String getSubFolder() {
		return subFolder;
	}
	
	public AutomationType getAutomationType() {
		return type;
	}
}
