package carbonconfiglib.api;

import carbonconfiglib.ConfigHandler;

public interface IConfigChangeListener {
	void onConfigAdded(ConfigHandler configHandler);
	void onConfigChanged(ConfigHandler configHandler);
}
