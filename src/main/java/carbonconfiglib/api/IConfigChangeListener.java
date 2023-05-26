package carbonconfiglib.api;

import carbonconfiglib.config.ConfigHandler;

public interface IConfigChangeListener {
	void onConfigCreated(ConfigHandler configHandler);
	void onConfigAdded(ConfigHandler configHandler);
	void onConfigChanged(ConfigHandler configHandler);
	void onConfigErrored(ConfigHandler configHandler);
}
