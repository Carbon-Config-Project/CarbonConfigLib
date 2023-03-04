package carbonconfiglib;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class FileSystemWatcher {
	private WatchService watchService;
	private Map<String, ConfigHandler> configsByName = new Object2ObjectLinkedOpenHashMap<>();
	private Set<ConfigHandler> syncedConfigs = new ObjectOpenHashSet<>();
	private Map<Path, ConfigHandler> configs = new Object2ObjectLinkedOpenHashMap<>();
	private Map<WatchKey, Path> folders = new Object2ObjectLinkedOpenHashMap<>();
	
	private boolean wasInit = false;
	private ILogger logger;
	private Path basePath;
	private IConfigChangeListener changeListener;
	
	public FileSystemWatcher(ILogger logger, Path basePath, IConfigChangeListener changedListener) {
		init(logger, basePath, changedListener);
	}
	
	protected void init(ILogger logger, Path basePath, IConfigChangeListener changedListener) {
		if (wasInit)
			return;
		wasInit = true;
		this.logger = logger;
		this.basePath = basePath;
		this.changeListener = changedListener;

		WatchService tmp;
		try {
			tmp = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			tmp = null;
			logger.fatal("WatchService could not be created");
			logger.fatal(e);
		}
		watchService = tmp;
	}
	
	public Path getBasePath() {
		return basePath;
	}
	
	public ILogger getLogger() {
		return logger;
	}

	
	public ConfigHandler createConfig(Config config, AutomationType setting) {
		return createConfig("", basePath, logger, config, setting);
	}
	
	public ConfigHandler createConfig(String subFolder, Config config, AutomationType setting) {
		return createConfig(subFolder, basePath, logger, config, setting);
	}
	
	public ConfigHandler createConfig(String subFolder, ILogger logger, Config config, AutomationType setting) {
		return createConfig(subFolder, basePath, logger, config, setting);
	}
	
	public ConfigHandler createConfig(String subFolder, Path baseFolder, ILogger logger, Config config, AutomationType setting) {
		return new ConfigHandler(subFolder, baseFolder, logger, config, setting).setOwner(this);
	}
	
	public void registerSyncHandler(ConfigHandler handler) {
		syncedConfigs.add(handler);
		configsByName.putIfAbsent(handler.getConfigIdentifer(), handler);
	}
	
	public void registerConfigHandler(Path configFile, ConfigHandler handler) {
		if(!wasInit) {
			return;
		}
		if(configs.putIfAbsent(configFile, handler) != null) {
			logger.warn("tried to register a config that already registered at {} path", configFile);
			return;
		}
		
		configsByName.putIfAbsent(handler.getConfigIdentifer(), handler);
		if (!folders.containsValue(configFile.getParent())) {
			try {
				folders.put(configFile.getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY), configFile.getParent());
			} catch (IOException e) {
				logger.fatal("could not register WatchService for directory {}", configFile.getParent());
			}
		}
		if(changeListener != null) changeListener.onConfigAdded(handler);
	}
	
	public ConfigHandler getConfig(String name) {
		return configsByName.get(name);
	}
	
	public void processFileSystemEvents() {
		if (watchService != null) {
			WatchKey key;
			while ((key = watchService.poll()) != null) {
				for (WatchEvent<?> event : key.pollEvents()) {
					ConfigHandler handler = configs.get(folders.get(key).resolve(((Path) event.context()).getFileName()));
					if (handler != null) {
						handler.load();
						if (changeListener != null && syncedConfigs.contains(handler)) {
							changeListener.onConfigChanged(handler);
						}
					}

				}
				key.reset();
			}
		}
	}
	
	public List<ConfigHandler> getConfigsToSync() {
		return new ObjectArrayList<>(syncedConfigs);
	}
}
