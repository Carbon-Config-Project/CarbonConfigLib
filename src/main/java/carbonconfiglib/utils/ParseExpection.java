package carbonconfiglib.utils;

public class ParseExpection {
	final String value;
	final Exception expection;
	final String message;
	final int index;
	
	public ParseExpection(String value, Exception expection, String message) {
		this(value, expection, message, -1);
	}
	
	public ParseExpection(String value, Exception expection, String message, int index) {
		this.value = value;
		this.expection = expection;
		this.message = message;
		this.index = index;
	}
	
	public String getValue() {
		return value;
	}
	
	public Exception getExpection() {
		return expection;
	}
	
	public String getMessage() {
		return message;
	}
	
	public int getIndex() {
		return index;
	}
	
	public ParseExpection withIndex(int index) {
		return new ParseExpection(value, expection, message, index);
	}
}
