package carbonconfiglib.api;

import carbonconfiglib.ConfigHandler;

public interface IConfigChangeListener {
	void onConfigCreated(ConfigHandler configHandler);
	void onConfigAdded(ConfigHandler configHandler);
	void onConfigChanged(ConfigHandler configHandler);
}
