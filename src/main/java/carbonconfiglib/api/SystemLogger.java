package carbonconfiglib.api;

public class SystemLogger implements ILogger
{
	
	@Override
	public void debug(String s) {
		System.out.println("[Debug] "+s);
	}
	
	@Override
	public void debug(String s, Object o) {
		System.out.println("[Debug] "+String.format(s.replace("{}", "%s"), o));
		if(o instanceof Throwable) ((Throwable)o).printStackTrace(System.out);
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
		System.out.println("[Info] "+String.format(s.replace("{}", "%s"), o));
		if(o instanceof Throwable) ((Throwable)o).printStackTrace(System.out);
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
		System.out.println("[Warn] "+String.format(s.replace("{}", "%s"), o));		
		if(o instanceof Throwable) ((Throwable)o).printStackTrace(System.out);
	}
	
	@Override
	public void warn(Object o) {
		System.out.println("[Warn] "+o);
	}
	
	@Override
	public void error(String s) {
		System.err.println("[Error] "+s);
	}
	
	@Override
	public void error(String s, Object o) {
		System.err.println("[Error] "+String.format(s.replace("{}", "%s"), o));
		if(o instanceof Throwable) ((Throwable)o).printStackTrace(System.err);
	}
	
	@Override
	public void error(Object o) {
		System.err.println("[Error] "+o);
	}
	
}
