package carbonconfiglib;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import carbonconfiglib.api.IConfigSerializer;
import carbonconfiglib.api.buffer.IReadBuffer;
import carbonconfiglib.api.buffer.IWriteBuffer;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.MultilinePolicy;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public abstract class ConfigEntry<T> {
	private String key;
	private T value;
	private T defaultValue;
	private T lastValue;
	private String[] comment;
	private boolean used = false;
	private boolean sync = false;
	private boolean worldReload = false;
	private boolean gameReload = false;

	public ConfigEntry(String key, T defaultValue, String... comment) {
		if (Helpers.validateString(key))
			throw new IllegalArgumentException("ConfigEntry key must not be null, empty or start/end with white spaces");
		if (key.contains(":") || key.contains("="))
			throw new IllegalArgumentException("ConfigEntry key must not contain any ':' or '=' signs. Key: " + key);
		if (defaultValue == null)
			throw new IllegalArgumentException("ConfigEntry default value must not be null. Key: " + key);
		this.key = key;
		this.value = defaultValue;
		this.defaultValue = defaultValue;
		this.comment = Helpers.validateComments(comment);
	}
	
	public String[] getComment() {
		return comment;
	}

	public ConfigEntry<T> setComment(String... comment) {
		this.comment = Helpers.validateComments(comment);
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

	protected String serializedValue(MultilinePolicy policy) {
		return String.valueOf(value);
	}
	
	protected String serializeArray(MultilinePolicy policy, String... lines) {
		if(policy == MultilinePolicy.MULTILINE_IF_TO_LONG) {
			StringBuilder builder = new StringBuilder();
			int lineAmount = 0;
			for(String s : lines) {
				if(lineAmount > 0 && lineAmount + s.length() > 75) {
					builder.append('\n');
					lineAmount = 0;
				}
				builder.append(s).append(", ");
				lineAmount += s.length()+2;
			}
			builder.setLength(builder.length()-2);
			return builder.toString();
		}
		StringJoiner joiner = new StringJoiner(policy == MultilinePolicy.ALWAYS_MULTILINE ? ", \n" : ", ");
		for (String s : lines) {
			joiner.add(s);
		}
		return joiner.toString();
	}
	
	public abstract String getLimitations();

	public final String serialize(MultilinePolicy policy, int indentationLevel) {
		String indentation = '\n' + Helpers.generateIndent(indentationLevel);
		StringBuilder builder = new StringBuilder();
		if (comment != null) {
			builder.append('\n');
			for(int i = 0;i<comment.length;i++)
			{
				builder.append(indentation);
				builder.append("# ");
				builder.append(comment[i].replaceAll("\\R", indentation + "# "));
			}
		}
		String limits = getLimitations();
		if(limits != null && !limits.isEmpty()) {
			builder.append(indentation);
			builder.append("#").append('\u200b').append(" ");
			builder.append(limits);
		}
		builder.append(indentation);
		builder.append(getPrefix());
		builder.append(':');
		builder.append(key);
		builder.append('=');
		String line = serializedValue(policy);
		if(policy != MultilinePolicy.DISABLED && this instanceof IArrayConfig && line.contains("\n")) {
			String indent = "\n"+Helpers.generateIndent(indentationLevel+1);
			builder.append(" < ").append(indent).append(line.replaceAll("\\R", indent));
			builder.append(indentation).append(">");
		}
		else {
			builder.append(line);
		}
		return builder.toString();
	}

	public abstract void serialize(IWriteBuffer buffer);

	public abstract void deserialize(IReadBuffer buffer);
	
	public static interface IArrayConfig {
		public List<String> getEntries();
		public List<String> getDefaults();
		public boolean canSet(List<String> entries);
		public void set(List<String> entries);
	}
	
	public static class IntValue extends ConfigEntry<Integer> {
		private int min = Integer.MIN_VALUE;
		private int max = Integer.MAX_VALUE;

		public IntValue(String key, Integer defaultValue, String... comment) {
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
		public String getLimitations() {
			if(min == Integer.MIN_VALUE) {
				if(max == Integer.MAX_VALUE) return "";
				return "Range: < "+max;
			}
			if(max == Integer.MAX_VALUE) {
				if(min == Integer.MIN_VALUE) return "";
				return "Range: > "+min;
			}
			return "Range: "+min+" ~ "+max;
		}
		
		@Override
		public char getPrefix() {
			return 'I';
		}
		
		public int get() {
			return getValue().intValue();
		}
		
		@Override
		public void parseValue(String value) {
			set(Integer.parseInt(value));
		}
		
		public static IntValue parse(String key, String value, String... comment) {
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

		public DoubleValue(String key, Double defaultValue, String... comment) {
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
		
		public double get() {
			return getValue().doubleValue();
		}
		
		@Override
		public String getLimitations() {
			if(min == Double.MIN_VALUE) {
				if(max == Double.MAX_VALUE) return "";
				return "Range: < "+max;
			}
			if(max == Double.MAX_VALUE) {
				if(min == Double.MIN_VALUE) return "";
				return "Range: > "+min;
			}
			return "Range: "+min+" ~ "+max;
		}
		
		@Override
		public void parseValue(String value) {
			set(Double.parseDouble(value));
		}
		
		public static DoubleValue parse(String key, String value, String... comment) {
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
		public BoolValue(String key, Boolean defaultValue, String... comment) {
			super(key, defaultValue, comment);
		}

		public BoolValue(String key, Boolean defaultValue) {
			super(key, defaultValue);
		}
		
		public boolean get() {
			return getValue().booleanValue();
		}
		
		@Override
		public char getPrefix() {
			return 'B';
		}
		
		@Override
		public String getLimitations() {
			return "";
		}
		
		@Override
		public void parseValue(String value) {
			set(Boolean.parseBoolean(value));
		}
		
		public static BoolValue parse(String key, String value, String... comment) {
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
	
	public static class TempValue extends StringValue {
		private TempValue(String key, String defaultValue, String[] comment) {
			super(key, defaultValue, comment);
		}
		
		public static TempValue parse(String key, String value, String... comment) {
			return new TempValue(key, value, comment);
		}
	}

	public static class StringValue extends ConfigEntry<String> {
		public StringValue(String key, String defaultValue, String... comment) {
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
		public String getLimitations() {
			return "";
		}
		
		@Override
		public void parseValue(String value) {
			set(value);
		}
		
		public static StringValue parse(String key, String value, String... comment) {
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

	public static class ArrayValue extends ConfigEntry<String[]> implements IArrayConfig {
		public ArrayValue(String key, String[] defaultValue, String... comment) {
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
		
		public String[] get() {
			return getValue();
		}
		
		@Override
		public String getLimitations() {
			return "";
		}
		
		@Override
		public List<String> getEntries() {
			return new ObjectArrayList<>(getValue());
		}

		@Override
		public List<String> getDefaults() {
			return ObjectArrayList.wrap(getValue());
		}

		@Override
		public boolean canSet(List<String> entries) {
			return true;
		}

		@Override
		public void set(List<String> entries) {
			set(entries.toArray(new String[entries.size()]));
		}

		@Override
		public void parseValue(String value) {
			set(value.isEmpty() ? new String[]{} : Helpers.trimArray(value.split(",")));
		}
		
		@Override
		protected String serializedValue(MultilinePolicy policy) {
			return serializeArray(policy, get());
		}
		
		public static ArrayValue parse(String key, String value, String... comment) {
			return new ArrayValue(key, value.isEmpty() ? new String[]{} : Helpers.trimArray(value.split(",")), comment);
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

		public EnumValue(String key, E defaultValue, Class<E> enumClass, String... comment) {
			super(key, defaultValue, comment);
			this.enumClass = enumClass;
		}

		public EnumValue(String key, E defaultValue, Class<E> enumClass) {
			super(key, defaultValue);
			this.enumClass = enumClass;
		}
		
		@Override
		public char getPrefix() {
			return 'E';
		}
		
		@Override
		public boolean canSet(E value) {
			return enumClass.isInstance(value);
		}
		
		public E get() {
			return getValue();
		}
		
		@Override
		public String getLimitations() {
			return "Must be one of " + Arrays.toString(enumClass.getEnumConstants());
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
	
	public static class ParsedValue<T> extends ConfigEntry<T> {
		IConfigSerializer<T> serializer;
		
		public ParsedValue(String key, T defaultValue, IConfigSerializer<T> serializer, String[] comment) {
			super(key, defaultValue, comment);
			this.serializer = serializer;
		}
		
		public ParsedValue(String key, T defaultValue, IConfigSerializer<T> serializer) {
			super(key, defaultValue);
			this.serializer = serializer;
		}

		@Override
		public char getPrefix() {
			return 'P';
		}
		
		public T get() {
			return getValue();
		}
		
		@Override
		public boolean canSet(T value) {
			return serializer.isValid(value);
		}
		
		@Override
		public void parseValue(String value) {
			T parsed = serializer.deserialize(value);
			if(parsed == null) throw new IllegalStateException("Value ["+value+"] is not correct please check the format");
			set(parsed);
		}
		
		@Override
		protected String serializedValue(MultilinePolicy policy) {
			return serializer.serialize(get());
		}

		@Override
		public String getLimitations() {
			return "Format: ["+serializer.getFormat()+"], Example: ["+serializer.serialize(serializer.getExample())+"]";
		}
		
		@Override
		public void serialize(IWriteBuffer buffer) {
			serializer.serialize(buffer, getValue());
		}

		@Override
		public void deserialize(IReadBuffer buffer) {
			set(serializer.deserialize(buffer));
		}
		
	}
}
