package carbonconfiglib.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import carbonconfiglib.api.ILogger;
import carbonconfiglib.config.ConfigEntry.ArrayValue;
import carbonconfiglib.config.ConfigEntry.BoolValue;
import carbonconfiglib.config.ConfigEntry.DoubleValue;
import carbonconfiglib.config.ConfigEntry.IntValue;
import carbonconfiglib.config.ConfigEntry.StringValue;
import carbonconfiglib.config.ConfigEntry.TempValue;
import carbonconfiglib.utils.AutomationType;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.MultilinePolicy;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class ConfigHandler {
	private final Path cfgDir;
	private final Path configFile;
	private final String subFolder;
	private final Config config;
	private final AutomationType setting;
	private final MultilinePolicy policy;
	
	private final ILogger logger;
	private FileSystemWatcher owner;
	
	private List<Runnable> loadedListeners = new ObjectArrayList<>();
	private Char2ObjectMap<IConfigParser> parsers = new Char2ObjectOpenHashMap<>();
	
	ConfigHandler(Config config, ConfigSettings settings) {
		this(settings.getSubFolder(), settings.getBaseFolder(), settings.getLogger(), config, settings.getAutomationType(), settings.getMultilinePolicy());
	}
	
	ConfigHandler(String subFolder, Path baseFolder, ILogger logger, Config config, AutomationType setting, MultilinePolicy policy) {
		this.config = config;
		String tmp = subFolder.trim().replace("\\\\", "/").replace("\\", "/");
		if (tmp.endsWith("/")) {
			tmp = tmp.substring(0, tmp.length() - 1);
		}
		this.subFolder = tmp;
		this.logger = logger;
		this.policy = policy;
		this.setting = setting;
		cfgDir = subFolder != null && !subFolder.isEmpty() ? baseFolder.resolve(subFolder) : baseFolder;
		configFile = cfgDir.resolve(config.getName().concat(".cfg"));
		parsers.put('I', IntValue::parse);
		parsers.put('D', DoubleValue::parse);
		parsers.put('B', BoolValue::parse);
		parsers.put('S', StringValue::parse);
		parsers.put('A', ArrayValue::parse);
		parsers.put('E', TempValue::parse);
		parsers.put('P', TempValue::parse);
	}
	
	ConfigHandler setOwner(FileSystemWatcher owner) {
		this.owner = owner;
		if(owner != null) owner.onConfigCreated(this);
		return this;
	}
	
	public void addTempParser(char id) {
		addParser(id, TempValue::parse);
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
	
	public Path getCfgDir() {
		return cfgDir;
	}
	
	public Path getConfigFile() {
		return configFile;
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
				save();
			} else if(load()) {
				save();
			}
			if(owner != null) {
				if(setting.isAutoSync()) {
					owner.registerSyncHandler(this);
				}
				if (setting.isAutoReload()) {
					owner.registerConfigHandler(configFile, this);
				}
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
	
	private int handleEntry(ConfigSection currentSection, List<String> lines, int index, String line, String[] comment) {
		if (currentSection == null) {
			logger.error("config entry not in section: {}", line);
			return 0;
		}
		String[] entryData = Helpers.trimArray(line.split("[:=]", 3));
		if (entryData.length != 3) {
			logger.error("invalid config entry: {}", line);
			return 0;
		}
		int extra = 0;
		if(entryData[2].charAt(0) == '<') {
			if(entryData[2].endsWith(">")) {
				entryData[2] = entryData[2].substring(1, entryData[2].length()-1);
			}
			else {
				StringBuilder builder = new StringBuilder();
				extra += findString(entryData[2], lines, index, builder);
				entryData[2] = builder.toString();
			}
		}
		ConfigEntry<?> entry = currentSection.getEntry(entryData[1]);
		boolean skip = false;
		if (entry == null) {
			IConfigParser parser = parsers.get(line.charAt(0));
			if(parser == null) {
				logger.error("config entry is not registered and no parser found: {}", line);
				return extra;
			}
			try {
				entry = parser.parse(entryData[1], entryData[2], comment);
				if(entry == null) {
					logger.error("config entry was able to be parsed: {}", line);
					return extra;
				}
				skip = true;
				currentSection.addParsed(entry);
			} catch (ClassCastException e) {
				logger.fatal("config entry has wrong type: {}", line);
			} catch (NumberFormatException e) {
				logger.fatal("config value is not a valid number: {}", line);
			}
		}
		if(skip) {
			return extra;
		}
		entry.setComment(comment);
		try {
			if (line.charAt(0) == entry.getPrefix()) entry.deserializeValue(entryData[2]);
			else logger.fatal("config entry has wrong type: {}", line);
		} catch (ClassCastException e) {
			logger.fatal("config entry has wrong type: {}", line);
		} catch (NumberFormatException e) {
			logger.fatal("config value is not a valid number: {}", line);
		}
		return extra;
	}
	
	private int findString(String base, List<String> lines, int index, StringBuilder builder) {
		builder.append(base.substring(1));
		int stepsDone = 0;
		while(index+1 < lines.size()) {
			index++;
			stepsDone++;
			String data = lines.get(index).trim();
			if(data.endsWith(">")) {
				builder.append(data.substring(0, data.length()-1));
				break;
			}
			else if(data.length() > 1 && data.charAt(1) == ':' && parsers.containsKey(data.charAt(0))) {
				stepsDone--;
				break;
			}
			if(data.isEmpty()) continue;
			builder.append(data);
		}
		return stepsDone;
	}
	
	public boolean load() {
		try {
			List<String> lines = Files.readAllLines(configFile);
			ConfigSection currentSection = null;
			List<String> comments = new ObjectArrayList<>();
			for (int i = 0,m=lines.size();i<m;i++) {
				String line = lines.get(i).trim();
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
						i += handleEntry(currentSection, lines, i, line, comments.toArray(new String[comments.size()]));
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
		return true;
	}
	
	public void save() {
		try (BufferedWriter writer = Files.newBufferedWriter(configFile)) {
			writer.write(config.serialize(policy));
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
