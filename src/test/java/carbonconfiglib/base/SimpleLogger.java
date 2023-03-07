package carbonconfiglib.base;

import carbonconfiglib.api.ILogger;

public class SimpleLogger implements ILogger {

	
	@Override
	public void debug(String s) {
		System.out.println("[Debug] "+s);
	}

	@Override
	public void debug(String s, Object o) {
		System.out.println("[Debug] "+String.format(s, o));
	}

	@Override
	public void debug(Object o) {
		System.out.println("[Debug] "+o);
	}
	
	@Override
	public void info(String s) {
		System.out.println("[Info] "+s);
	}

	@Override
	public void info(String s, Object o) {
		System.out.println("[Info] "+String.format(s, o));
	}

	@Override
	public void info(Object o) {
		System.out.println("[Info] "+o);
	}
	
	@Override
	public void warn(String s) {
		System.out.println("[Warn] "+s);
	}

	@Override
	public void warn(String s, Object o) {
		System.out.println("[Warn] "+String.format(s, o));
	}

	@Override
	public void warn(Object o) {
		System.out.println("[Warn] "+o);
	}
	
	@Override
	public void error(String s) {
		System.out.println("[Error] "+s);
	}
	
	@Override
	public void error(String s, Object o) {
		System.out.println("[Error] "+String.format(s, o));
	}

	@Override
	public void error(Object o) {
		System.out.println("[Error] "+o);
	}
	
	@Override
	public void fatal(String s) {
		System.out.println("[Fatal] "+s);
	}
	
	@Override
	public void fatal(String s, Object o) {
		System.out.println("[Fatal] "+String.format(s, o));
	}

	@Override
	public void fatal(Object o) {
		System.out.println("[Fatal] "+o);
	}

}
