package carbonconfiglib;

public interface IConfigChangeListener {
	void onConfigAdded(ConfigHandler configHandler);
	void onConfigChanged(ConfigHandler configHandler);
}
