package carbonconfiglib;

import carbonconfiglib.buffer.IReadBuffer;
import carbonconfiglib.buffer.IWriteBuffer;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

public abstract class ConfigEntry<T> {
	private String key;
	private T value;
	private T defaultValue;
	private T lastValue;
	private String comment;
	private boolean used = false;
	private boolean sync = false;
	private boolean worldReload = false;
	private boolean gameReload = false;

	public ConfigEntry(String key, T defaultValue, String comment) {
		if (Helpers.validateString(key))
			throw new IllegalArgumentException("ConfigEntry key must not be null, empty or start/end with white spaces");
		if (key.contains(":") || key.contains("="))
			throw new IllegalArgumentException("ConfigEntry key must not contain any ':' or '=' signs. Key: " + key);
		if (defaultValue == null)
			throw new IllegalArgumentException("ConfigEntry default value must not be null. Key: " + key);
		this.key = key;
		this.value = defaultValue;
		this.defaultValue = defaultValue;
		this.comment = comment;
	}

	public ConfigEntry(String key, T defaultValue) {
		this(key, defaultValue, null);
	}

	public String getComment() {
		return comment;
	}

	public ConfigEntry<T> setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public T getValue() {
		return value;
	}
	
	public T getDefault() {
		return defaultValue;
	}
	
	public boolean canSet(T value) {
		return true;
	}
	
	public ConfigEntry<T> set(T value) {
		if (value != null) {
			if(sync) lastValue = this.value;
			this.value = value;
		}
		return this;
	}

	public String getKey() {
		return key;
	}
	
	final boolean isUsed() {
		return used;
	}
	
	final ConfigEntry<T> setUsed() {
		used = true;
		return this;
	}
	
	public final boolean hasChanged() {
		return used && (value.getClass().isArray() ? !Objects.deepEquals(lastValue, value) : !Objects.equals(lastValue, value));
	}
	
	public final boolean needsWorldReload() {
		return worldReload;
	}
	
	public final boolean needsGameReload() {
		return gameReload;
	}
	
	public final void onSynced() {
		lastValue = value;
	}
	
	final boolean isSynced() {
		return sync;
	}
	
	@SuppressWarnings("unchecked")
	public final <S extends ConfigEntry<T>> S setSynced() {
		sync = true;
		return (S)this;
	}
	
	@SuppressWarnings("unchecked")
	public final <S extends ConfigEntry<T>> S setWorldReload() {
		worldReload = true;
		return (S)this;
	}
	
	@SuppressWarnings("unchecked")
	public final <S extends ConfigEntry<T>> S setGameRestart() {
		gameReload = true;
		return (S)this;
	}
	
	public ConfigEntry<T> setKey(String key) {
		if (Helpers.validateString(key))
			throw new IllegalArgumentException("ConfigEntry key must not be null, empty or start/end with white spaces");
		if (key.contains(":") || key.contains("="))
			throw new IllegalArgumentException("ConfigEntry key must not contain any ':' or '=' signs. Key: " + key);
		this.key = key;
		return this;
	}
	
	public abstract char getPrefix();

	public abstract void parseValue(String value);
	
	public void resetDefault() {
		value = defaultValue;
	}

	protected String serializedValue() {
		return String.valueOf(value);
	}

	public String serialize(int indentationLevel) {
		String indentation = '\n' + Helpers.generateIndent(indentationLevel);
		StringBuilder builder = new StringBuilder();
		if (comment != null && !comment.isEmpty()) {
			builder.append('\n');
			builder.append(indentation);
			builder.append("# ");
			builder.append(comment.replaceAll("\\R", indentation + "# "));
		}
		builder.append(indentation);
		builder.append(getPrefix());
		builder.append(':');
		builder.append(key);
		builder.append('=');
		builder.append(serializedValue());
		return builder.toString();
	}

	public abstract void serialize(IWriteBuffer buffer);

	public abstract void deserialize(IReadBuffer buffer);

	public static class IntValue extends ConfigEntry<Integer> {
		private int min = Integer.MIN_VALUE;
		private int max = Integer.MAX_VALUE;

		public IntValue(String key, Integer defaultValue, String comment) {
			super(key, defaultValue, comment);
		}

		public IntValue(String key, Integer defaultValue) {
			super(key, defaultValue);
		}

		public IntValue setMin(int min) {
			this.min = min;
			return this;
		}

		public IntValue setMax(int max) {
			this.max = max;
			return this;
		}

		public IntValue setRange(int min, int max) {
			this.min = min;
			this.max = max;
			return this;
		}

		@Override
		public IntValue set(Integer value) {
			super.set(Helpers.clamp(value, min, max));
			return this;
		}
		
		@Override
		public boolean canSet(Integer value) {
			return value >= min && value <= max;
		}
		
		@Override
		public char getPrefix() {
			return 'I';
		}
		
		public int get()
		{
			return getValue().intValue();
		}
		
		@Override
		public void parseValue(String value) {
			set(Integer.parseInt(value));
		}
		
		public static IntValue parse(String key, String value, String comment)
		{
			return new IntValue(key, Integer.parseInt(value), comment);
		}

		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeInt(get());
		}

		@Override
		public void deserialize(IReadBuffer buffer) {
			set(buffer.readInt());
		}
	}

	public static class DoubleValue extends ConfigEntry<Double> {
		private double min = Double.MIN_VALUE;
		private double max = Double.MAX_VALUE;

		public DoubleValue(String key, Double defaultValue, String comment) {
			super(key, defaultValue, comment);
		}

		public DoubleValue(String key, Double defaultValue) {
			super(key, defaultValue);
		}

		public DoubleValue setMin(double min) {
			this.min = min;
			return this;
		}

		public DoubleValue setMax(double max) {
			this.max = max;
			return this;
		}

		public DoubleValue setRange(double min, double max) {
			this.min = min;
			this.max = max;
			return this;
		}
		
		@Override
		public boolean canSet(Double value) {
			return value >= min && value <= max;
		}
		
		@Override
		public DoubleValue set(Double value) {
			super.set(Helpers.clamp(value, min, max));
			return this;
		}

		@Override
		public char getPrefix() {
			return 'D';
		}
		
		public double get()
		{
			return getValue().doubleValue();
		}
		
		@Override
		public void parseValue(String value) {
			set(Double.parseDouble(value));
		}
		
		public static DoubleValue parse(String key, String value, String comment)
		{
			return new DoubleValue(key, Double.parseDouble(value), comment);
		}
		
		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeDouble(get());
		}

		@Override
		public void deserialize(IReadBuffer buffer) {
			set(buffer.readDouble());
		}
	}

	public static class BoolValue extends ConfigEntry<Boolean> {
		public BoolValue(String key, Boolean defaultValue, String comment) {
			super(key, defaultValue, comment);
		}

		public BoolValue(String key, Boolean defaultValue) {
			super(key, defaultValue);
		}
		
		public boolean get()
		{
			return getValue().booleanValue();
		}
		
		@Override
		public char getPrefix() {
			return 'B';
		}
		
		@Override
		public void parseValue(String value) {
			set(Boolean.parseBoolean(value));
		}
		
		public static BoolValue parse(String key, String value, String comment)
		{
			return new BoolValue(key, Boolean.parseBoolean(value), comment);
		}

		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeBoolean(get());
		}

		@Override
		public void deserialize(IReadBuffer buffer) {
			set(buffer.readBoolean());
		}
	}

	public static class StringValue extends ConfigEntry<String> {
		public StringValue(String key, String defaultValue, String comment) {
			super(key, defaultValue, comment);
		}

		public StringValue(String key, String defaultValue) {
			super(key, defaultValue);
		}

		@Override
		public char getPrefix() {
			return 'S';
		}
		
		public String get() {
			return getValue();
		}
		
		@Override
		public void parseValue(String value) {
			set(value);
		}
		
		public static StringValue parse(String key, String value, String comment)
		{
			return new StringValue(key, value, comment);
		}

		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeString(get());
		}

		@Override
		public void deserialize(IReadBuffer buffer) {
			set(buffer.readString());
		}
	}

	public static class ArrayValue extends ConfigEntry<String[]> {
		public ArrayValue(String key, String[] defaultValue, String comment) {
			super(key, defaultValue, comment);
		}

		public ArrayValue(String key, String[] defaultValue) {
			super(key, defaultValue);
		}

		public ArrayValue(String key, String comment) {
			super(key, new String[]{}, comment);
		}

		public ArrayValue(String key) {
			super(key, new String[]{});
		}

		@Override
		public char getPrefix() {
			return 'A';
		}
		
		public String[] get()
		{
			return getValue();
		}
		
		@Override
		public void parseValue(String value) {
			set(value.isEmpty() ? new String[]{} : value.split(","));
		}

		@Override
		protected String serializedValue() {
			StringJoiner joiner = new StringJoiner(",");
			for (String s : get()) {
				joiner.add(s);
			}
			return joiner.toString();
		}
		
		public static ArrayValue parse(String key, String value, String comment)
		{
			return new ArrayValue(key, value.isEmpty() ? new String[]{} : value.split(","), comment);
		}

		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeVarInt(get().length);
			for (String val : get()) {
				buffer.writeString(val);
			}
		}

		@Override
		public void deserialize(IReadBuffer buffer) {
			String[] val = new String[buffer.readVarInt()];
			for (int i = 0; i < val.length; i++) {
				val[i] = buffer.readString();
			}
			set(val);
		}
	}

	public static class EnumValue<E extends Enum<E>> extends ConfigEntry<E> {
		private Class<E> enumClass;

		public EnumValue(String key, E defaultValue, Class<E> enumClass, String comment) {
			super(key, defaultValue, comment);
			this.enumClass = enumClass;
			setupComment();
		}

		public EnumValue(String key, E defaultValue, Class<E> enumClass) {
			super(key, defaultValue);
			this.enumClass = enumClass;
			setupComment();
		}

		private void setupComment() {
			String generated = "Must be one of " + Arrays.toString(enumClass.getEnumConstants());
			String comment = getComment();
			if (comment == null || comment.isEmpty()) {
				setComment(generated);
			} else {
				if (comment.endsWith(".")) {
					setComment(comment + " " + generated);
				} else {
					setComment(comment + ". " + generated);
				}
			}
		}

		@Override
		public char getPrefix() {
			return 'E';
		}
		
		@Override
		public boolean canSet(E value) {
			return enumClass.isInstance(value);
		}
		
		public E get()
		{
			return getValue();
		}
		
		@Override
		public void parseValue(String value) {
			set(Enum.valueOf(enumClass, value));
		}

		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeEnum(get());
		}

		@Override
		public void deserialize(IReadBuffer buffer) {
			set(buffer.readEnum(enumClass));
		}
	}
}
