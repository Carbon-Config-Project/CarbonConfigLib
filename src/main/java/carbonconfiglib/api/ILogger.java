package carbonconfiglib.api;

public interface ILogger {
	void debug(String s);
	void debug(String s, Object o);
	void debug(Object o);
	void info(String s);
	void info(String s, Object o);
	void info(Object o);
	void warn(String s);
	void warn(String s, Object o);
	void warn(Object o);
	void error(String s);
	void error(String s, Object o);
	void error(Object o);
}
