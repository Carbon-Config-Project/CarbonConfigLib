package carbonconfiglib;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import carbonconfiglib.ConfigEntry.ArrayValue;
import carbonconfiglib.ConfigEntry.BoolValue;
import carbonconfiglib.ConfigEntry.DoubleValue;
import carbonconfiglib.ConfigEntry.IntValue;
import carbonconfiglib.ConfigEntry.StringValue;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ConfigHandler {
	private final Path cfgDir;
	private final Path configFile;
	private final String subFolder;
	private final Config config;
	private final AutomationType setting;

	private final ILogger logger;

	private List<Runnable> loadedListeners = new ObjectArrayList<>();
	private Char2ObjectMap<IConfigParser> parsers = new Char2ObjectOpenHashMap<>();
	
	public ConfigHandler(String subFolder, Path baseFolder, ILogger logger, Config config, AutomationType setting) {
		this.config = config;

		String tmp = subFolder.trim().replace("\\\\", "/").replace("\\", "/");
		if (tmp.endsWith("/")) {
			tmp = tmp.substring(0, tmp.length() - 1);
		}

		this.subFolder = tmp;
		this.logger = logger;

		this.setting = setting;
		if(!subFolder.isEmpty())
		{
			cfgDir = baseFolder.resolve(subFolder);
		}
		else 
		{
			cfgDir = baseFolder;
		}
		configFile = cfgDir.resolve(config.getName().concat(".cfg"));
		parsers.put('I', IntValue::parse);
		parsers.put('D', DoubleValue::parse);
		parsers.put('B', BoolValue::parse);
		parsers.put('S', StringValue::parse);
		parsers.put('A', ArrayValue::parse);
		parsers.put('E', StringValue::parse);
	}

	public ConfigHandler(String subFolder, ILogger logger, Config config, AutomationType setting) {
		this(subFolder, FileSystemWatcher.INSTANCE.getBasePath(), logger, config, setting);
	}

	public ConfigHandler(String subFolder, Config config, AutomationType setting) {
		this(subFolder, FileSystemWatcher.INSTANCE.getBasePath(), FileSystemWatcher.INSTANCE.getLogger(), config, setting);
	}
	
	public ConfigHandler(Config config, AutomationType setting) {
		this("", config, setting);
	}
	
	public void addParser(char id, IConfigParser parser) {
		parsers.putIfAbsent(id, parser);
	}
	
	public Config getConfig() {
		return config;
	}

	public String getSubFolder() {
		return subFolder;
	}
	
	public String getConfigIdentifer() {
		return subFolder + "/" + config.getName();
	}
	
	public void init() {
		try {
			if (Files.notExists(cfgDir)) {
				Files.createDirectories(cfgDir);
			}
			if (Files.notExists(configFile)) {
				Files.createFile(configFile);
				save();
			} else {
				load();
				save();
			}
			if(setting.isAutoSync()) {
				FileSystemWatcher.INSTANCE.registerSyncHandler(this);
			}
			if (setting.isAutoReload()) {
				FileSystemWatcher.INSTANCE.registerConfigHandler(configFile, this);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addLoadedListener(Runnable listener) {
		loadedListeners.add(listener);
	}
	
	public void onSynced() {
		for (Runnable r : loadedListeners) {
			r.run();
		}
	}
	
	private void handleEntry(ConfigSection currentSection, String line, String[] comment) {
		if (currentSection == null) {
			logger.error("config entry not in section: {}", line);
			return;
		}
		String[] entryData = line.split("[:=]", 3);
		if (entryData.length != 3) {
			logger.error("invalid config entry: {}", line);
			return;
		}
		ConfigEntry<?> entry = currentSection.getEntry(entryData[1]);
		boolean skip = false;
		if (entry == null) {
			IConfigParser parser = parsers.get(line.charAt(0));
			if(parser == null)
			{
				logger.error("config entry is not registered and no parser found: {}", line);
				return;
			}
			try
			{
				entry = parser.parse(entryData[1], entryData[2], comment);
				if(entry == null)
				{
					logger.error("config entry was able to be parsed: {}", line);
					return;
				}
				skip = true;
				currentSection.addParsed(entry);
			} catch (ClassCastException e) {
				logger.fatal("config entry has wrong type: {}", line);
			} catch (NumberFormatException e) {
				logger.fatal("config value is not a valid number: {}", line);
			}
		}
		if(skip)
		{
			return;
		}
		entry.setComment(comment);
		try {
			if (line.charAt(0) == entry.getPrefix())
				entry.parseValue(entryData[2]);
			else
				logger.fatal("config entry has wrong type: {}", line);
		} catch (ClassCastException e) {
			logger.fatal("config entry has wrong type: {}", line);
		} catch (NumberFormatException e) {
			logger.fatal("config value is not a valid number: {}", line);
		}
	}
	
	public void load() {
		try {
			List<String> lines = Files.readAllLines(configFile);
			ConfigSection currentSection = null;
			List<String> comments = new ObjectArrayList<>();
			for (String line : lines) {
				line = line.trim();
				if (line.length() == 0)
					continue;
				switch (line.charAt(0)) {
					case '[':
						currentSection = config.getSectionRecursive(line.substring(1, line.length() - 1).split("\\."));
						comments.clear();
						break;
					case '#':
						if(line.charAt(1) == '\u200b') break;
						comments.add(line.substring(1).trim());
						break;
					default:
						handleEntry(currentSection, line, comments.toArray(new String[comments.size()]));
						comments.clear();
						break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (Runnable r : loadedListeners) {
			r.run();
		}
	}

	public void save() {
		try (BufferedWriter writer = Files.newBufferedWriter(configFile)) {
			writer.write(config.serialize());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FunctionalInterface
	public interface IConfigParser
	{
		ConfigEntry<?> parse(String key, String value, String[] comment);
	}
}
