package carbonconfiglib.config;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import carbonconfiglib.api.ConfigType;
import carbonconfiglib.api.IConfigChangeListener;
import carbonconfiglib.api.ILogger;
import carbonconfiglib.utils.AutomationType;
import carbonconfiglib.utils.MultilinePolicy;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.maps.impl.hash.Object2ObjectLinkedOpenHashMap;
import speiger.src.collections.objects.sets.ObjectOpenHashSet;

public class FileSystemWatcher {
	private WatchService watchService;
	private Map<String, ConfigHandler> configsByName = new Object2ObjectLinkedOpenHashMap<>();
	private Set<ConfigHandler> syncedConfigs = new ObjectOpenHashSet<>();
	private Map<Path, ConfigHandler> configs = new Object2ObjectLinkedOpenHashMap<>();
	private Map<WatchKey, Path> folders = new Object2ObjectLinkedOpenHashMap<>();
	
	private ILogger logger;
	private Path basePath;
	private IConfigChangeListener changeListener;
	private Object sync = new Object();
	
	public FileSystemWatcher(ILogger logger, Path basePath, IConfigChangeListener changedListener) {
		init(logger, basePath, changedListener);
	}
	
	protected void init(ILogger logger, Path basePath, IConfigChangeListener changedListener) {
		this.logger = logger;
		this.basePath = basePath;
		this.changeListener = changedListener;
		WatchService tmp;
		try {
			tmp = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			tmp = null;
			logger.error("WatchService could not be created");
			logger.error(e);
		}
		watchService = tmp;
	}
	
	public Path getBasePath() {
		return basePath;
	}
	
	public ILogger getLogger() {
		return logger;
	}
	
	void onConfigCreated(ConfigHandler handler) {
		if(changeListener != null) changeListener.onConfigCreated(handler);
	}
	
	void onConfigErrored(ConfigHandler handler) {
		if(changeListener != null) changeListener.onConfigErrored(handler);
	}
	
	public ConfigHandler createConfig(Config config) {
		return createConfig(config, ConfigSettings.of());
	}
	
	public ConfigHandler createConfig(Config config, ConfigSettings settings) {
		settings.withAutomations(AutomationType.AUTO_LOAD, AutomationType.AUTO_RELOAD, AutomationType.AUTO_SYNC).withBaseFolder(basePath).withType(ConfigType.SHARED).withSubFolder("").withLogger(logger).withMultiline(MultilinePolicy.ALWAYS_MULTILINE);
		return new ConfigHandler(config, settings).setOwner(this);
	}
	
	public void registerSyncHandler(ConfigHandler handler) {
		synchronized(sync) {
			syncedConfigs.add(handler);
			configsByName.putIfAbsent(handler.getConfigIdentifer(), handler);
		}
	}
	
	public void registerConfigHandler(ConfigHandler handler) {
		synchronized(sync) {
			configsByName.putIfAbsent(handler.getConfigIdentifer(), handler);
			if(changeListener != null) changeListener.onConfigAdded(handler);
		}
	}
	
	public void registerReloadHandler(Path configFile, ConfigHandler handler) {
		synchronized(sync) {
			if(configs.putIfAbsent(configFile, handler) != null) {
				logger.warn("tried to register a config that already registered at {} path", configFile);
				return;
			}
			configsByName.putIfAbsent(handler.getConfigIdentifer(), handler);
			if (!folders.containsValue(configFile.getParent())) {
				try {
					folders.put(configFile.getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY), configFile.getParent());
				} catch (IOException e) {
					logger.error("could not register WatchService for directory {}", configFile.getParent());
				}
			}
		}
	}
	
	public void unregisterReloadHandler(Path configFile) {
		synchronized(sync) {
			if(configs.remove(configFile) == null) return;
			Set<Path> parents = new ObjectOpenHashSet<>();
			for(Path path : configs.keySet()) {
				parents.add(path.getParent());
			}
			for(Iterator<Map.Entry<WatchKey, Path>> iter = folders.entrySet().iterator();iter.hasNext();) {
				Map.Entry<WatchKey, Path> entry = iter.next();
				if(parents.contains(entry.getValue())) continue;
				entry.getKey().cancel();
				iter.remove();
			}
		}
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
						if(handler.reload() && changeListener != null && syncedConfigs.contains(handler)) {
							changeListener.onConfigChanged(handler);
						}
					}
				}
				key.reset();
			}
		}
	}
	
	public List<ConfigHandler> getAllConfigs() {
		return new ObjectArrayList<>(configsByName.values());
	}
	
	public List<ConfigHandler> getConfigsToSync() {
		return new ObjectArrayList<>(syncedConfigs);
	}
}
