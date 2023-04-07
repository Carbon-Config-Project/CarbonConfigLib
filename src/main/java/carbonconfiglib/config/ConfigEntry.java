package carbonconfiglib.config;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;

import carbonconfiglib.api.IConfigSerializer;
import carbonconfiglib.api.IReloadMode;
import carbonconfiglib.api.ISuggestedEnum;
import carbonconfiglib.api.buffer.IReadBuffer;
import carbonconfiglib.api.buffer.IWriteBuffer;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.IEntryDataType;
import carbonconfiglib.utils.IEntryDataType.CompoundDataType;
import carbonconfiglib.utils.IEntryDataType.EntryDataType;
import carbonconfiglib.utils.MultilinePolicy;
import carbonconfiglib.utils.SyncType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public abstract class ConfigEntry<T> {
	private String key;
	private T value;
	private T defaultValue;
	private T lastValue;
	private String[] comment;
	private boolean used = false;
	private boolean serverSync = false;
	private IReloadMode reload = null;
	private SyncedConfig<ConfigEntry<T>> syncCache;
	private List<Suggestion> suggestions = new ObjectArrayList<>();

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
	
	protected abstract ConfigEntry<T> copy();
	
	public T getValue() {
		return value;
	}
	
	public T getDefault() {
		return defaultValue;
	}
	
	public abstract T parseValue(String value);
	
	public boolean canSetValue(String value) {
		return canSet(parseValue(value));
	}
	
	public boolean canSet(T value) {
		return value != null;
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
	
	public abstract IEntryDataType getDataType();
	
	protected final <S extends ConfigEntry<T>> S addSuggestionInternal(String value) {
		return addSuggestionInternal(value, value, null);
	}
	
	protected final <S extends ConfigEntry<T>> S addSuggestionInternal(String name, String value) {
		return addSuggestionInternal(name, value, null);
	}
	
	protected final <S extends ConfigEntry<T>> S addSuggestionInternal(Object extra, String value) {
		return addSuggestionInternal(value, value, extra);
	}
	
	@SuppressWarnings("unchecked")
	protected final <S extends ConfigEntry<T>> S addSuggestionInternal(String name, String value, Object extra) {
		if(!canSetValue(value)) throw new IllegalArgumentException("Value ["+value+"] is not valid. Meaning it can not be a suggestion");
		suggestions.add(new Suggestion(name, value, extra));
		return (S)this;
	}
	
	public final List<Suggestion> getSuggestions() {
		return suggestions;
	}
	
	@SuppressWarnings("unchecked")
	public final <S extends ConfigEntry<T>> S clearSuggestions() {
		suggestions.clear();
		return (S)this;
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
	
	public final boolean isDefault() {
		return used && (value.getClass().isArray() ? !Objects.deepEquals(defaultValue, value) : !Objects.equals(defaultValue, value));
	}
	
	public final IReloadMode getReloadState() {
		return reload;
	}
	
	public final void onSynced() {
		lastValue = value;
	}
	
	final SyncType getSyncType() {
		return syncCache != null ? SyncType.CLIENT_TO_SERVER : (serverSync ? SyncType.SERVER_TO_CLIENT : SyncType.NONE);
	}
	
	@SuppressWarnings("unchecked")
	public final <S extends ConfigEntry<T>> S setServerSynced() {
		if(syncCache != null) throw new IllegalStateException("Client Synced Configs can not Server Sync");
		serverSync = true;
		return (S)this;
	}
	
	@SuppressWarnings("unchecked")
	public final <S extends ConfigEntry<T>> SyncedConfig<S> setClientSynced() {
		if(serverSync) throw new IllegalStateException("Server Synced Configs can not Client Sync");
		if(syncCache == null) syncCache = new SyncedConfig<>(() -> copy(), this);
		return (SyncedConfig<S>)syncCache;
	}
	
	@SuppressWarnings("unchecked")
	public final <S extends ConfigEntry<T>> S setRequiredReload(IReloadMode mode) {
		this.reload = mode;
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
	
	public void deserializeValue(String value) {
		set(parseValue(value));
	}
	
	public void resetDefault() {
		value = defaultValue;
	}
	
	public String serializeDefault() {
		return serializedValue(MultilinePolicy.DISABLED, defaultValue);
	}
	
	public String serialize() {
		return serializedValue(MultilinePolicy.DISABLED, value);
	}
	
	protected String serializedValue(MultilinePolicy policy, T value) {
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
		String line = serializedValue(policy, value);
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
		public boolean canSetArray(List<String> entries);
		public void setArray(List<String> entries);
	}
	
	public static class Suggestion {
		String name;
		String value;
		Object extra;
		
		public Suggestion(String value) {
			this(value, value, null);
		}
		
		public Suggestion(String value, Object extra) {
			this(value, value, extra);
		}
		
		public Suggestion(String name, String value) {
			this(name, value, null);
		}
		
		public Suggestion(String name, String value, Object extra) {
			this.name = name;
			this.value = value;
			this.extra = extra;;
		}
		
		public String getName() {
			return name;
		}
		
		public String getValue() {
			return value;
		}
		
		public Object getExtra() {
			return extra;
		}
	}
	
	public static abstract class BasicConfigEntry<T> extends ConfigEntry<T> {
		
		public BasicConfigEntry(String key, T defaultValue, String... comment) {
			super(key, defaultValue, comment);
		}
		
		@SuppressWarnings("unchecked")
		public final <S extends BasicConfigEntry<T>> S addSuggestions(T... values) {
			for(T value : values) {
				addSuggestionInternal(serializedValue(MultilinePolicy.DISABLED, value));
			}
			return (S)this;
		}
		
		public final <S extends BasicConfigEntry<T>> S addSuggestion(T value) {
			return addSuggestionInternal(serializedValue(MultilinePolicy.DISABLED, value));
		}
		
		public final <S extends BasicConfigEntry<T>> S addSuggestion(T value, Object extra) {
			return addSuggestionInternal(extra, serializedValue(MultilinePolicy.DISABLED, value));
		}
		
		public final <S extends BasicConfigEntry<T>> S addSuggestion(String name, T value) {
			return addSuggestionInternal(name, serializedValue(MultilinePolicy.DISABLED, value));
		}
		
		public final <S extends BasicConfigEntry<T>> S addSuggestion(String name, T value, Object extra) {
			return addSuggestionInternal(name, serializedValue(MultilinePolicy.DISABLED, value), extra);
		}
	}
	
	public static abstract class ArrayConfigEntry<T> extends ConfigEntry<T[]> {
		
		public ArrayConfigEntry(String key, T[] defaultValue, String... comment) {
			super(key, defaultValue, comment);
		}
		
		@SuppressWarnings("unchecked")
		public final <S extends ArrayConfigEntry<T>> S addSuggestions(T... values) {
			for(T value : values) {
				addSuggestionInternal(serializedValue(MultilinePolicy.DISABLED, toArray(value)));
			}
			return (S)this;
		}
		
		public final <S extends ArrayConfigEntry<T>> S addSuggestion(T value) {
			return addSuggestionInternal(serializedValue(MultilinePolicy.DISABLED, toArray(value)));
		}
		
		public final <S extends ArrayConfigEntry<T>> S addSuggestion(T value, Object extra) {
			return addSuggestionInternal(extra, serializedValue(MultilinePolicy.DISABLED, toArray(value)));
		}
		
		public final <S extends ArrayConfigEntry<T>> S addSuggestion(String name, T value) {
			return addSuggestionInternal(name, serializedValue(MultilinePolicy.DISABLED, toArray(value)));
		}
		
		public final <S extends ArrayConfigEntry<T>> S addSuggestion(String name, T value, Object extra) {
			return addSuggestionInternal(name, serializedValue(MultilinePolicy.DISABLED, toArray(value)), extra);
		}
		
		@SuppressWarnings("unchecked")
		T[] toArray(T input) {
			T[] result = (T[])Array.newInstance(input.getClass(), 1);
			result[0] = input;
			return result;
		}
	}
	
	public static class IntValue extends BasicConfigEntry<Integer> {
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
		protected IntValue copy() {
			return new IntValue(getKey(), getDefault(), getComment()).setRange(min, max);
		}
		
		@Override
		public IntValue set(Integer value) {
			super.set(Helpers.clamp(value, min, max));
			return this;
		}
		
		@Override
		public boolean canSet(Integer value) {
			return super.canSet(value) && value >= min && value <= max;
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
		
		@Override
		public EntryDataType getDataType() {
			return EntryDataType.NUMBER;
		}
		
		public int get() {
			return getValue().intValue();
		}
		
		@Override
		public Integer parseValue(String value) {
			return Helpers.parseInt(value);
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
	
	public static class DoubleValue extends BasicConfigEntry<Double> {
		private double min = -Double.MAX_VALUE;
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
		protected DoubleValue copy() {
			return new DoubleValue(getKey(), getDefault(), getComment()).setRange(min, max);
		}
		
		@Override
		public boolean canSet(Double value) {
			return super.canSet(value) && value >= min && value <= max;
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
		
		@Override
		public EntryDataType getDataType() {
			return EntryDataType.NUMBER;
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
		public Double parseValue(String value) {
			return Helpers.parseDouble(value);
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
	
	public static class BoolValue extends BasicConfigEntry<Boolean> {
		public BoolValue(String key, Boolean defaultValue, String... comment) {
			super(key, defaultValue, comment);
		}
		
		public BoolValue(String key, Boolean defaultValue) {
			super(key, defaultValue);
		}
		
		@Override
		protected BoolValue copy() {
			return new BoolValue(getKey(), getDefault(), getComment());
		}
		
		public boolean get() {
			return getValue().booleanValue();
		}
		
		@Override
		public char getPrefix() {
			return 'B';
		}
		
		@Override
		public EntryDataType getDataType() {
			return EntryDataType.BOOLEAN;
		}
		
		@Override
		public String getLimitations() {
			return "";
		}
		
		@Override
		public Boolean parseValue(String value) {
			return Boolean.parseBoolean(value);
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
		
		@Override
		public TempValue withFilter(Predicate<String> filter) {
			throw new UnsupportedOperationException("Filters are not supported with Temp Values");
		}

		@Override
		protected TempValue copy() {
			return new TempValue(getKey(), getDefault(), getComment());
		}
	}

	public static class StringValue extends BasicConfigEntry<String> {
		protected Predicate<String> filter;
		
		public StringValue(String key, String defaultValue, String... comment) {
			super(key, defaultValue, comment);
		}
		
		public StringValue(String key, String defaultValue) {
			super(key, defaultValue);
		}
		
		public StringValue withFilter(Predicate<String> filter) {
			this.filter = filter;
			return this;
		}
		
		@Override
		protected StringValue copy() {
			return new StringValue(getKey(), getDefault(), getComment()).withFilter(filter);
		}
		
		@Override
		public char getPrefix() {
			return 'S';
		}
		
		@Override
		public EntryDataType getDataType() {
			return EntryDataType.STRING;
		}
		
		public String get() {
			return getValue();
		}
		
		@Override
		public String getLimitations() {
			return "";
		}
		
		@Override
		public boolean canSet(String value) {
			return value != null && (filter == null || filter.test(value));
		}
		
		@Override
		public String parseValue(String value) {
			return filter == null || filter.test(value) ? value : null;
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
	
	public static class ArrayValue extends ArrayConfigEntry<String> implements IArrayConfig {
		protected Predicate<String> filter;

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
		
		public ArrayValue withFilter(Predicate<String> filter) {
			this.filter = filter;
			return this;
		}
		
		@Override
		protected ArrayValue copy() {
			return new ArrayValue(getKey(), getDefault(), getComment());
		}
		
		@Override
		public char getPrefix() {
			return 'A';
		}
		
		@Override
		public EntryDataType getDataType() {
			return EntryDataType.STRING;
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
			return ObjectArrayList.wrap(getDefault());
		}
		
		@Override
		public boolean canSetArray(List<String> entries) {
			if(entries == null) return false;
			if(filter == null) return true;
			for(int i = 0;i<entries.size();i++) {
				if(!filter.test(entries.get(i))) return false;
			}
			return true;
		}
		
		@Override
		public void setArray(List<String> entries) {
			set(entries.toArray(new String[entries.size()]));
		}
		
		@Override
		public String[] parseValue(String value) {
			return Helpers.splitArray(value, ",");
		}
		
		@Override
		protected String serializedValue(MultilinePolicy policy, String[] value) {
			return serializeArray(policy, value);
		}
		
		public static ArrayValue parse(String key, String value, String... comment) {
			return new ArrayValue(key, Helpers.splitArray(value, ","), comment);
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
	
	public static class EnumValue<E extends Enum<E>> extends BasicConfigEntry<E> {
		private Class<E> enumClass;
		
		public EnumValue(String key, E defaultValue, Class<E> enumClass, String... comment) {
			super(key, defaultValue, comment);
			this.enumClass = enumClass;
			addSuggestions();
		}
		
		public EnumValue(String key, E defaultValue, Class<E> enumClass) {
			super(key, defaultValue);
			this.enumClass = enumClass;
			addSuggestions();
		}
		
		private void addSuggestions() {
			for(E value : enumClass.getEnumConstants()) {
				ISuggestedEnum<E> wrapper = ISuggestedEnum.getWrapper(value);
				if(wrapper != null) addSuggestion(wrapper.getName(value), value);
				else addSuggestion(value); 
			}
		}
		
		@Override
		protected EnumValue<E> copy() {
			return new EnumValue<>(getKey(), getDefault(), enumClass, getComment());
		}
		
		@Override
		public char getPrefix() {
			return 'E';
		}
		
		@Override
		protected String serializedValue(MultilinePolicy policy, E value) {
			return value.name();
		}
		
		@Override
		public EntryDataType getDataType() {
			return EntryDataType.STRING;
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
			return "Must be one of " + Arrays.toString(toArray());
		}
		
		private String[] toArray() {
			E[] array = enumClass.getEnumConstants();
			String[] values = new String[array.length];
			for(int i = 0,m=array.length;i<m;i++) {
				values[i] = array[i].name();
			}
			return values;
		}
		
		@Override
		public E parseValue(String value) {
			return Enum.valueOf(enumClass, value);
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
	
	public static class ParsedValue<T> extends BasicConfigEntry<T> {
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
		protected ParsedValue<T> copy() {
			return new ParsedValue<>(getKey(), getDefault(), serializer, getComment());
		}
		
		@Override
		public char getPrefix() {
			return 'P';
		}
		
		@Override
		public CompoundDataType getDataType() {
			return serializer.getFormat();
		}
		
		public T get() {
			return getValue();
		}
		
		@Override
		public boolean canSet(T value) {
			return serializer.isValid(value);
		}
		
		@Override
		public T parseValue(String value) {
			return serializer.deserialize(Helpers.splitArray(value, ";"));
		}
		
		@Override
		protected String serializedValue(MultilinePolicy policy, T value) {
			return Helpers.mergeCompound(serializer.serialize(value));
		}
		
		private String buildFormat() {
			StringJoiner joiner = new StringJoiner(";");
			for(Map.Entry<String, EntryDataType> entry : serializer.getFormat().getCompound()) {
				joiner.add(entry.getKey()+"("+Helpers.firstLetterUppercase(entry.getValue().name().toLowerCase())+")");
			}
			return joiner.toString();
		}
		
		@Override
		public String getLimitations() {
			return "Format: ["+buildFormat()+"], Example: ["+serializedValue(MultilinePolicy.DISABLED, serializer.getExample())+"]";
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
