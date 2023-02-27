package carbonconfiglib;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

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

public class FileSystemWatcher {
	private WatchService watchService;
	private Map<String, ConfigHandler> configsByName;
	private Set<ConfigHandler> syncedConfigs;
	private Map<Path, ConfigHandler> configs;
	private Map<WatchKey, Path> folders;

	private ILogger logger;
	private Path basePath;
	private IConfigChangedListener changedListener;

	public static final FileSystemWatcher INSTANCE = new FileSystemWatcher();

	public void Init(ILogger logger, Path basePath, IConfigChangedListener changedListener) {
		if (folders != null)
			return;

		this.logger = logger;
		this.basePath = basePath;
		this.changedListener = changedListener;

		WatchService tmp;
		try {
			tmp = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			tmp = null;
			logger.fatal("WatchService could not be created");
			logger.fatal(e);
		}

		watchService = tmp;
		configsByName = new Object2ObjectLinkedOpenHashMap<>();
		syncedConfigs = new ObjectOpenHashSet<>();
		configs = new Object2ObjectLinkedOpenHashMap<>();
		folders = new Object2ObjectLinkedOpenHashMap<>();
	}

	public Path getBasePath() {
		return basePath;
	}

	public ILogger getLogger() {
		return logger;
	}

	private FileSystemWatcher() {

	}
	
	public void registerSyncHandler(ConfigHandler handler) {
		syncedConfigs.add(handler);
	}

	public void registerConfigHandler(Path configFile, ConfigHandler handler) {
		configs.putIfAbsent(configFile, handler);

		configsByName.put(handler.getSubFolder() + "/" + handler.getConfig().getName(), handler);
		if (!folders.containsValue(configFile.getParent())) {
			try {
				folders.put(configFile.getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY), configFile.getParent());
			} catch (IOException e) {
				logger.fatal("could not register WatchService for directory {}", configFile.getParent());
			}
		}
	}
	
	public ConfigHandler getConfig(String name)
	{
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
						if (changedListener != null && syncedConfigs.contains(handler)) {
							changedListener.onConfigChanged(handler);
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
