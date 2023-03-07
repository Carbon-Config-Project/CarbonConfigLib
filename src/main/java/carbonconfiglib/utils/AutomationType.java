package carbonconfiglib.utils;

public enum AutomationType {
	NONE,
	AUTO_SYNC,
	AUTO_RELOAD,
	BOTH;
	
	public boolean isAutoReload() {
		return this == AUTO_RELOAD || this == BOTH;
	}
	
	public boolean isAutoSync() {
		return this == AUTO_SYNC || this == BOTH;
	}
}
