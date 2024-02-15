package carbonconfiglib.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;

import carbonconfiglib.api.ConfigType;
import carbonconfiglib.api.IConfigProxy;
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
import carbonconfiglib.utils.ParseExpection;
import carbonconfiglib.utils.ParseResult;
import speiger.src.collections.chars.maps.impl.hash.Char2ObjectOpenHashMap;
import speiger.src.collections.chars.maps.interfaces.Char2ObjectMap;
import speiger.src.collections.objects.lists.ObjectArrayList;

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
public final class ConfigHandler {
	private Path cfgDir;
	private Path configFile;
	private boolean isLoaded;
	private boolean registered;
	private int wasSaving = 0;
	private final String subFolder;
	private final Config config;
	private final EnumSet<AutomationType> setting;
	private final MultilinePolicy policy;
	public final ConfigType type;
	private final List<ConfigError> errors = new ObjectArrayList<>();
	private final IConfigProxy proxy;
	private final ILogger logger;
	private FileSystemWatcher owner;
	
	private List<Runnable> loadedListeners = new ObjectArrayList<>();
	private Char2ObjectMap<IConfigParser> parsers = new Char2ObjectOpenHashMap<>();
	
	ConfigHandler(Config config, ConfigSettings settings) {
		this(settings.getSubFolder(), settings.getProxy(), settings.getLogger(), config, settings.getAutomationType(), settings.getMultilinePolicy(), settings.getType());
	}
	
	ConfigHandler(String subFolder, IConfigProxy proxy, ILogger logger, Config config, EnumSet<AutomationType> setting, MultilinePolicy policy, ConfigType type) {
		this.config = config;
		String tmp = subFolder.trim().replace("\\\\", "/").replace("\\", "/");
		if (tmp.endsWith("/")) {
			tmp = tmp.substring(0, tmp.length() - 1);
		}
		this.subFolder = tmp;
		this.logger = logger;
		this.policy = policy;
		this.setting = setting;
		this.proxy = proxy;
		this.type = type;
		parsers.put('I', IntValue::parse);
		parsers.put('D', DoubleValue::parse);
		parsers.put('B', BoolValue::parse);
		parsers.put('S', StringValue::parse);
		parsers.put('A', ArrayValue::parse);
		parsers.put('E', TempValue::parseTemp);
		parsers.put('e', TempValue::parseTemp);
		parsers.put('p', TempValue::parseTemp);
		parsers.put('P', TempValue::parseTemp);
	}
	
	ConfigHandler setOwner(FileSystemWatcher owner) {
		this.owner = owner;
		if(owner != null) owner.onConfigCreated(this);
		return this;
	}
	
	public void addTempParser(char id) {
		addParser(id, TempValue::parseTemp);
	}
	
	public void addParser(char id, IConfigParser parser) {
		if((id < 'A' || id > 'Z') && (id < 'a' || id > 'z')) throw new IllegalArgumentException("Character must be [a-zA-Z]");
		parsers.putIfAbsent(id, parser);
	}
	
	public IConfigProxy getProxy() {
		return proxy;
	}
	
	public ConfigType getConfigType() {
		return type;
	}
	
	public MultilinePolicy getMultilinePolicy() {
		return policy;
	}
	
	public Config getConfig() {
		return config;
	}
	
	public String getSubFolder() {
		return subFolder;
	}
	
	public Path createConfigFile(Path baseFolder) {
		return (!subFolder.isEmpty() ? baseFolder.resolve(subFolder) : baseFolder).resolve(config.getName().concat(".cfg"));
	}
	
	public boolean isLoaded() {
		return isLoaded;
	}
	
	public boolean isRegistered() {
		return registered;
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
	
	public boolean hasErrors() {
		return errors.size() > 0;
	}
	
	public List<ConfigError> getErrors() {
		return errors;
	}
	
	public void register() {
		if(owner != null) {
			owner.registerConfigHandler(this);
			registered = true;
			if(!proxy.isDynamicProxy() && setting.contains(AutomationType.AUTO_LOAD)) {
				load();
			}
		}
	}
	
	public void createDefaultConfig() {
		if(!registered) return;
		if(proxy.isDynamicProxy() && setting.contains(AutomationType.AUTO_LOAD)) {
			Path baseFolder = proxy.getBasePaths(createConfigFile(Paths.get("")));
			Path file = createConfigFile(baseFolder);
			if(Files.notExists(file)) {
				save(file);
				wasSaving--;
			}
		}
	}
	
	public void load() {
		findConfigFile();
		if(owner != null) {
			if(setting.contains(AutomationType.AUTO_SYNC)) {
				owner.registerSyncHandler(this);
			}
			if(setting.contains(AutomationType.AUTO_RELOAD)) {
				owner.registerReloadHandler(configFile, this);
			}
		}
		if(loadInternally()) {
			save();
		}
		isLoaded = true;
	}
	
	public boolean reload() {
		if(!isLoaded) return false;
		if(wasSaving > 0) {
			wasSaving--;
			return false;
		}
		loadInternally();
		return true;
	}
	
	public void unload() {
		isLoaded = false;
		if(owner != null && setting.contains(AutomationType.AUTO_RELOAD)) {
			owner.unregisterReloadHandler(configFile);
		}
	}
	
	private void findConfigFile() {
		Path baseFolder = proxy.getBasePaths(createConfigFile(Paths.get("")));
		if(baseFolder == null) throw new IllegalStateException("Proxy has no Folder");
		configFile = createConfigFile(baseFolder);
		cfgDir = configFile.getParent();
		Helpers.ensureFolder(cfgDir);
	}
	
	public void addLoadedListener(Runnable listener) {
		loadedListeners.add(listener);
	}
	
	public void onSynced() {
		for (Runnable r : loadedListeners) {
			r.run();
		}
	}
	
	private int handleEntry(ConfigSection currentSection, List<String> lines, int index, String line, String[] comment, boolean logErrors) {
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
		if(entryData[2].length() > 0 && entryData[2].charAt(0) == '<') {
			if(entryData[2].endsWith(">")) {
				entryData[2] = entryData[2].substring(1, entryData[2].length()-1);
			} else {
				StringBuilder builder = new StringBuilder();
				extra += findString(entryData[2], lines, index, builder);
				entryData[2] = builder.toString();
			}
		}
		try {
			ConfigEntry<?> entry = currentSection.getEntry(entryData[1]);
			if(entry == null) {
				IConfigParser parser = parsers.get(line.charAt(0));
				if(parser == null) {
					logger.warn("config entry is not registered and no parser found: {}", line);
					return extra;
				}
				ParseResult<? extends ConfigEntry<?>> result = parser.parse(entryData[1], entryData[2], comment);
				entry = result.getValue();
				currentSection.addParsed(entry);
				if(result.hasError() && logErrors) {
					logger.warn("couldn't parse value: {}", result.getValue());
					errors.add(new ConfigError(entry, result.getError()));
				}
				return extra;
			}
			entry.parseComment(comment);
			if (line.charAt(0) == entry.getPrefix()) {
				ParseResult<String> result = entry.deserializeValue(entryData[2]);
				if(result.hasError() && logErrors) {
					logger.warn("couldn't parse value: {}", result.getValue());
					errors.add(new ConfigError(entry, result.getError()));
				}
			}
			else logger.warn("config entry has wrong type: {}", line);
		}
		catch(Throwable e) {
			logger.error("Crash during parsing. THIS SHOULD NEVER HAPPEN!", e);
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
	
	private boolean loadInternally() {
		if(Files.notExists(configFile)) return true;
		try {
			errors.clear();
			load(this, config, Files.readAllLines(configFile), true);
			for (Runnable r : loadedListeners) {
				r.run();
			}
			if(errors.size() > 0 && owner != null) {
				owner.onConfigErrored(this);
			}
			return true;
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	public static boolean load(ConfigHandler handler, Config output, List<String> linesToParse, boolean logErrors) {
		ConfigSection currentSection = null;
		List<String> comments = new ObjectArrayList<>();
		for (int i = 0,m=linesToParse.size();i<m;i++) {
			String line = linesToParse.get(i).trim();
			if (line.length() == 0)
				continue;
			switch (line.charAt(0)) {
				case '[':
					currentSection = output.getSectionRecursive(line.substring(1, line.length() - 1).split("\\."));
					currentSection.parseComment(comments.toArray(new String[comments.size()]));
					comments.clear();
					break;
				case '#':
					if(line.charAt(1) == '\u200b') break;
					comments.add(line.substring(1).trim());
					break;
				default:
					i += handler.handleEntry(currentSection, linesToParse, i, line, comments.toArray(new String[comments.size()]), logErrors);
					comments.clear();
					break;
			}
		}
		return true;
	}
	
	public void save() {
		save(configFile);
		if(owner != null) owner.onConfigChanged(this);
		onSynced();
	}
	
	public void saveQuietly() {
		save(configFile);
	}
	
	private void save(Path file) {
		wasSaving++;
		try (BufferedWriter writer = Files.newBufferedWriter(file)) {
			writer.write(config.serialize(policy));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FunctionalInterface
	public interface IConfigParser {
		ParseResult<? extends ConfigEntry<?>> parse(String key, String value, String[] comment);
	}
	
	public class ConfigError {
		ConfigEntry<?> entry;
		ParseExpection error;
		
		public ConfigError(ConfigEntry<?> entry, ParseExpection error) {
			this.entry = entry;
			this.error = error;
		}
		
		public ConfigEntry<?> getEntry() {
			return entry;
		}
		
		public ParseExpection getError() {
			return error;
		}
	}
}
