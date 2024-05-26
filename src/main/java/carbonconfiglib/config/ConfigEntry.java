package carbonconfiglib.config;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import carbonconfiglib.api.IConfigSerializer;
import carbonconfiglib.api.IEntrySettings;
import carbonconfiglib.api.IReloadMode;
import carbonconfiglib.api.ISuggestionProvider;
import carbonconfiglib.api.ISuggestionProvider.Suggestion;
import carbonconfiglib.api.buffer.IReadBuffer;
import carbonconfiglib.api.buffer.IWriteBuffer;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.MultilinePolicy;
import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.SyncType;
import carbonconfiglib.utils.structure.IStructuredData;
import carbonconfiglib.utils.structure.IStructuredData.EntryDataType;
import carbonconfiglib.utils.structure.IStructuredData.SimpleData;
import carbonconfiglib.utils.structure.StructureCompound.CompoundData;
import carbonconfiglib.utils.structure.StructureList.ListBuilder;
import carbonconfiglib.utils.structure.StructureList.ListData;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.utils.ObjectLists;

/**
 * Copyright 2023 Speiger, Meduris
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public abstract class ConfigEntry<T> {
	private String key;
	private T value;
	private T defaultValue;
	private T lastValue;
	private String[] comment;
	private boolean used = false;
	private boolean serverSync = false;
	private boolean hidden = false;
	private boolean wasLoaded = false;
	private boolean forcedSuggestions = false;
	private IReloadMode reload = null;
	private IEntrySettings settings = null;
	private SyncedConfig<ConfigEntry<T>> syncCache;
	private List<ISuggestionProvider> providers = new ObjectArrayList<>();

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
	
	void parseComment(String...comment) {
		if(this.comment == null) this.comment = Helpers.validateComments(comment);
	}
	
	public ConfigEntry<T> setComment(String... comment) {
		this.comment = Helpers.validateComments(comment);
		return this;
	}
	
	protected ConfigEntry<T> deepCopy() {
		ConfigEntry<T> copy = copy();
		copy.providers.addAll(providers);
		copy.hidden = hidden;
		copy.wasLoaded = wasLoaded;
		copy.reload = reload;
		copy.settings = settings;
		return copy;
	}
	
	protected abstract ConfigEntry<T> copy();
	
	public T getValue() {
		return value;
	}
	
	public T getDefault() {
		return defaultValue;
	}
	
	public abstract ParseResult<T> parseValue(String value);
	
	public ParseResult<Boolean> canSetValue(String value) {
		ParseResult<T> result = parseValue(value);
		return result.hasError() ? result.withDefault(false) : canSet(result.getValue());
	}
	
	public ParseResult<Boolean> canSet(T value) {
		return ParseResult.result(value != null, NullPointerException::new, "Value isn't allowed to be null");
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
	
	public abstract IStructuredData getDataType();
	
	public final <S extends ConfigEntry<T>> S addSingleSuggestion(Suggestion suggestion) {
		return addSuggestionProvider(ISuggestionProvider.single(suggestion));
	}
	
	@SuppressWarnings("unchecked")
	public final <S extends ConfigEntry<T>> S addSuggestionProvider(ISuggestionProvider provider) {
		providers.add(provider);
		return (S)this;
	}
		
	public final List<Suggestion> getSuggestions(Predicate<Suggestion> filter) {
		List<Suggestion> suggestions = new ObjectArrayList<>();
		for(ISuggestionProvider provider : this.providers) {
			provider.provideSuggestions(T -> addInternal(T, suggestions), filter);
		}
		return suggestions;
	}
	
	private void addInternal(Suggestion value, List<Suggestion> output) {
		if(canSetValue(value.getValue()).isValid()) {
			output.add(value);
		}
	}
	
	public final List<ISuggestionProvider> getProviders() {
		return providers;
	}
	
	@SuppressWarnings("unchecked")
	public final <S extends ConfigEntry<T>> S clearSuggestions() {
		providers.clear();
		return (S)this;
	}
	
	final boolean isUsed() {
		return used;
	}
	
	final ConfigEntry<T> setUsed() {
		used = true;
		return this;
	}
	
	final ConfigEntry<T> setLoaded() {
		wasLoaded = true;
		return this;
	}
	
	public final boolean isNotHidden() {
		return !hidden || wasLoaded;
	}
	
	public final boolean hasChanged() {
		return used && (value.getClass().isArray() ? !Objects.deepEquals(lastValue, value) : !Objects.equals(lastValue, value));
	}
	
	public final boolean isDefault() {
		return used && (value.getClass().isArray() ? Objects.deepEquals(defaultValue, value) : Objects.equals(defaultValue, value));
	}
	
	public final boolean areSuggestionsForced() {
		return forcedSuggestions;
	}
	
	public final IReloadMode getReloadState() {
		return reload;
	}
	
	public final IEntrySettings getSettings() {
		return settings;
	}
	
	public final void onSynced() {
		lastValue = value;
	}
	
	final SyncType getSyncType() {
		return syncCache != null ? SyncType.CLIENT_TO_SERVER : (serverSync ? SyncType.SERVER_TO_CLIENT : SyncType.NONE);
	}
	
	@SuppressWarnings("unchecked")
	public final <S extends ConfigEntry<T>> S setHidden() {
		hidden = true;
		return (S)this;
	}
	
	@SuppressWarnings("unchecked")
	public final <S extends ConfigEntry<T>> S forceSuggestions(boolean value) {
		forcedSuggestions = value;
		return (S)this;
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
	
	@SuppressWarnings("unchecked")
	public final <S extends ConfigEntry<T>> S setSettings(IEntrySettings settings) {
		this.settings = settings;
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
	
	public ParseResult<String> deserializeValue(String value) {
		ParseResult<T> result = parseValue(value);
		if(result.hasError()) return result.withDefault(value);
		set(result.getValue());
		setLoaded();
		return ParseResult.success(value);
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
		if (comment != null && comment.length > 0) {
			builder.append('\n');
			for(int i = 0;i<comment.length;i++) {
				builder.append(indentation);
				builder.append("# ");
				builder.append(comment[i].replaceAll("\\R", indentation + "# "));
			}
		}
		String limits = getLimitations();
		if(limits != null && !limits.isEmpty()) {
			if(builder.length() == 0) builder.append("\n");
			builder.append(indentation);
			builder.append("#").append('\u200b').append(" ");
			builder.append(limits.replaceAll("\\R", indentation + "#\u200b "));
		}
		builder.append(indentation);
		builder.append(getPrefix());
		builder.append(':');
		builder.append(key);
		builder.append('=');
		String line = serializedValue(policy, value);
		if(policy != MultilinePolicy.DISABLED && line.contains("\n")) {
			String indent = "\n"+Helpers.generateIndent(indentationLevel+1);
			builder.append("<<<").append(indent).append(line.replaceAll("\\R", indent));
			builder.append(indentation).append(">>>");
		}
		else {
			builder.append(line);
		}
		return builder.toString();
	}
	
	public abstract void serialize(IWriteBuffer buffer);
	public void deserialize(IReadBuffer buffer, UUID owner) {
		if(syncCache != null) {
			syncCache.onSync(buffer, owner);
			return;
		}
		deserializeValue(buffer);
	}
	
	protected abstract void deserializeValue(IReadBuffer buffer);
	
	public static abstract class BasicConfigEntry<T> extends ConfigEntry<T> {
		
		public BasicConfigEntry(String key, T defaultValue, String... comment) {
			super(key, defaultValue, comment);
		}
		
		@SuppressWarnings("unchecked")
		public final <S extends BasicConfigEntry<T>> S addSuggestions(T... values) {
			List<Suggestion> suggestions = new ObjectArrayList<>();
			for(T value : values) {
				suggestions.add(Suggestion.value(serializedValue(MultilinePolicy.DISABLED, value)));
			}
			return addSuggestionProvider(ISuggestionProvider.list(suggestions));
		}
		
		public final <S extends BasicConfigEntry<T>> S addSuggestion(T value) {
			return addSingleSuggestion(Suggestion.value(serializedValue(MultilinePolicy.DISABLED, value)));
		}
		
		public final <S extends BasicConfigEntry<T>> S addSuggestion(T value, Object type) {
			return addSingleSuggestion(Suggestion.typedValue(serializedValue(MultilinePolicy.DISABLED, value), type));
		}
		
		public final <S extends BasicConfigEntry<T>> S addSuggestion(String name, T value) {
			return addSingleSuggestion(Suggestion.namedValue(name, serializedValue(MultilinePolicy.DISABLED, value)));
		}
		
		public final <S extends BasicConfigEntry<T>> S addSuggestion(String name, T value, Object type) {
			return addSingleSuggestion(Suggestion.namedTypeValue(name, serializedValue(MultilinePolicy.DISABLED, value), type));
		}
	}
	
	public static abstract class ArrayConfigEntry<T> extends ConfigEntry<T[]> {
		
		public ArrayConfigEntry(String key, T[] defaultValue, String... comment) {
			super(key, defaultValue, comment);
		}
		
		public <K, V> MappedConfig<K, V> createdMappedConfig(ConfigHandler handler, Function<T, K> keyGenerator, Function<T, V> valueGenerator) {
			return MappedConfig.create(handler, this, keyGenerator, valueGenerator);
		}
		
		@SuppressWarnings("unchecked")
		public final <S extends ArrayConfigEntry<T>> S addSuggestions(T... values) {
			List<Suggestion> suggestions = new ObjectArrayList<>();
			for(T value : values) {
				suggestions.add(Suggestion.value(serializedValue(MultilinePolicy.DISABLED, toArray(value))));
			}
			return addSuggestionProvider(ISuggestionProvider.list(suggestions));
		}
		
		public final <S extends ArrayConfigEntry<T>> S addSuggestion(T value) {
			return addSingleSuggestion(Suggestion.value(serializedValue(MultilinePolicy.DISABLED, toArray(value))));
		}
		
		public final <S extends ArrayConfigEntry<T>> S addSuggestion(T value, Object type) {
			return addSingleSuggestion(Suggestion.typedValue(serializedValue(MultilinePolicy.DISABLED, toArray(value)), type));
		}
		
		public final <S extends ArrayConfigEntry<T>> S addSuggestion(String name, T value) {
			return addSingleSuggestion(Suggestion.namedValue(name, serializedValue(MultilinePolicy.DISABLED, toArray(value))));
		}
		
		public final <S extends ArrayConfigEntry<T>> S addSuggestion(String name, T value, Object type) {
			return addSingleSuggestion(Suggestion.namedTypeValue(name, serializedValue(MultilinePolicy.DISABLED, toArray(value)), type));
		}
		
		@SuppressWarnings("unchecked")
		T[] toArray(T input) {
			T[] result = (T[])Array.newInstance(input.getClass(), 1);
			result[0] = input;
			return result;
		}
	}
	
	public static abstract class CollectionConfigEntry<T, E extends Collection<T>> extends ConfigEntry<E> {
		
		public CollectionConfigEntry(String key, E defaultValue, String... comment) {
			super(key, defaultValue, comment);
		}
		
		public <K, V> MappedConfig<K, V> createdMappedConfig(ConfigHandler handler, Function<T, K> keyGenerator, Function<T, V> valueGenerator) {
			return MappedConfig.create(handler, this, keyGenerator, valueGenerator);
		}
		
		@SuppressWarnings("unchecked")
		public final <S extends CollectionConfigEntry<T, E>> S addSuggestions(T... values) {
			List<Suggestion> suggestions = new ObjectArrayList<>();
			for(T value : values) {
				suggestions.add(Suggestion.value(serializedValue(MultilinePolicy.DISABLED, create(value))));
			}
			return addSuggestionProvider(ISuggestionProvider.list(suggestions));
		}
		
		public final <S extends CollectionConfigEntry<T, E>> S addSuggestion(T value) {
			return addSingleSuggestion(Suggestion.value(serializedValue(MultilinePolicy.DISABLED, create(value))));
		}
		
		public final <S extends CollectionConfigEntry<T, E>> S addSuggestion(T value, Object type) {
			return addSingleSuggestion(Suggestion.typedValue(serializedValue(MultilinePolicy.DISABLED, create(value)), type));
		}
		
		public final <S extends CollectionConfigEntry<T, E>> S addSuggestion(String name, T value) {
			return addSingleSuggestion(Suggestion.namedValue(name, serializedValue(MultilinePolicy.DISABLED, create(value))));
		}
		
		public final <S extends CollectionConfigEntry<T, E>> S addSuggestion(String name, T value, Object extra) {
			return addSingleSuggestion(Suggestion.namedTypeValue(name, serializedValue(MultilinePolicy.DISABLED, create(value)), extra));
		}
		
		protected abstract E create(T value);
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
		
		public int getMin() {
			return min; 
		}
		
		public int getMax() {
			return max;
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
		public ParseResult<Boolean> canSet(Integer value) {
			ParseResult<Boolean> result = super.canSet(value);
			if(result.hasError()) return result;
			return ParseResult.result(value >= min && value <= max, IllegalArgumentException::new, "Value ["+value+"] has to be within ["+min+" ~ "+max+"]");
		}
		
		@Override
		public String getLimitations() {
			if(min == Integer.MIN_VALUE) {
				if(max == Integer.MAX_VALUE) return "";
				return "Range: < "+max;
			}
			if(max == Integer.MAX_VALUE) {
				return "Range: > "+min;
			}
			return "Range: "+min+" ~ "+max;
		}
		
		@Override
		public char getPrefix() {
			return 'I';
		}
		
		@Override
		public SimpleData getDataType() {
			return EntryDataType.INTEGER.toSimpleType();
		}
		
		public int get() {
			return getValue().intValue();
		}
		
		@Override
		public ParseResult<Integer> parseValue(String value) {
			return Helpers.parseInt(value);
		}
		
		public static ParseResult<IntValue> parse(String key, String value, String... comment) {
			ParseResult<Integer> result = Helpers.parseInt(value);
			if (result.hasError()) return result.withDefault(new IntValue(key, 0, comment));
			return ParseResult.success(new IntValue(key, result.getValue(), comment));
		}

		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeInt(get());
		}

		@Override
		public void deserializeValue(IReadBuffer buffer) {
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
		
		public double getMin() {
			return min;
		}
		
		public double getMax() {
			return max;
		}
		
		@Override
		protected DoubleValue copy() {
			return new DoubleValue(getKey(), getDefault(), getComment()).setRange(min, max);
		}
		
		@Override
		public ParseResult<Boolean> canSet(Double value) {
			ParseResult<Boolean> result = super.canSet(value);
			if(result.hasError()) return result;
			return ParseResult.result(value >= min && value <= max, IllegalArgumentException::new, "Value ["+value+"] has to be within ["+min+" ~ "+max+"]");
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
		public SimpleData getDataType() {
			return EntryDataType.DOUBLE.toSimpleType();
		}
		
		public double get() {
			return getValue().doubleValue();
		}
		
		@Override
		public String getLimitations() {
			if(min == -Double.MAX_VALUE) {
				if(max == Double.MAX_VALUE) return "";
				return "Range: < "+max;
			}
			if(max == Double.MAX_VALUE) {
				return "Range: > "+min;
			}
			return "Range: "+min+" ~ "+max;
		}
		
		@Override
		public ParseResult<Double> parseValue(String value) {
			return Helpers.parseDouble(value);
		}
		
		public static ParseResult<DoubleValue> parse(String key, String value, String... comment) {
			ParseResult<Double> result = Helpers.parseDouble(value);
			if (result.hasError()) return result.withDefault(new DoubleValue(key, 0D, comment));
			return ParseResult.success(new DoubleValue(key, result.getValue(), comment));
		}
		
		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeDouble(get());
		}
		
		@Override
		public void deserializeValue(IReadBuffer buffer) {
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
		public SimpleData getDataType() {
			return EntryDataType.BOOLEAN.toSimpleType();
		}
		
		@Override
		public String getLimitations() {
			return "";
		}
		
		@Override
		public ParseResult<Boolean> parseValue(String value) {
			return ParseResult.success(Boolean.parseBoolean(value));
		}
		
		public static ParseResult<BoolValue> parse(String key, String value, String... comment) {
			return ParseResult.success(new BoolValue(key, Boolean.parseBoolean(value), comment));
		}
		
		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeBoolean(get());
		}
		
		@Override
		public void deserializeValue(IReadBuffer buffer) {
			set(buffer.readBoolean());
		}
	}
	
	public static class TempValue extends StringValue {
		private TempValue(String key, String defaultValue, String[] comment) {
			super(key, defaultValue, comment);
		}
		
		public static ParseResult<TempValue> parseTemp(String key, String value, String... comment) {
			return ParseResult.success(new TempValue(key, value, comment));
		}
		
		@Override
		public TempValue withFilter(Predicate<String> filter) {
			throw new UnsupportedOperationException("Filters are not supported with Temp Values");
		}
		
		@Override
		public Predicate<String> getFilter() { throw new UnsupportedOperationException("Filters are not supported with Temp Values"); }
		
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
		
		public Predicate<String> getFilter() {
			return filter;
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
		public SimpleData getDataType() {
			return EntryDataType.STRING.toSimpleType();
		}
		
		public String get() {
			return getValue();
		}
		
		@Override
		public String getLimitations() {
			return "";
		}
		
		@Override
		public ParseResult<Boolean> canSet(String value) {
			if(value == null) return ParseResult.partial(false, NullPointerException::new, "Value isn't allowed to be null");
			if(filter == null || filter.test(value)) return ParseResult.success(true);
			return ParseResult.partial(false, IllegalStateException::new, "Value ["+value+"] isn't valid");
		}
		
		@Override
		public ParseResult<String> parseValue(String value) {
			return ParseResult.successOrError(value, filter == null || filter.test(value), IllegalArgumentException::new, "Value ["+value+"] is not valid");
		}
		
		public static ParseResult<StringValue> parse(String key, String value, String... comment) {
			return ParseResult.success(new StringValue(key, value, comment));
		}
		
		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeString(get());
		}
		
		@Override
		public void deserializeValue(IReadBuffer buffer) {
			set(buffer.readString());
		}
	}
	
	public static class ArrayValue extends ArrayConfigEntry<String> {
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
		
		public Predicate<String> getFilter() {
			return filter; 
		}
		
		@Override
		protected ArrayValue copy() {
			return new ArrayValue(getKey(), getDefault(), getComment()).withFilter(filter);
		}
		
		@Override
		public char getPrefix() {
			return 'A';
		}
		
		@Override
		public ListData getDataType() {
			return ListBuilder.of(EntryDataType.STRING).setSettings(getSettings()).addSuggestions(ISuggestionProvider.wrapper(this::getSuggestions)).build(true);
		}
		
		public String[] get() {
			return getValue();
		}
		
		@Override
		public String getLimitations() {
			return "";
		}
		
		@Override
		public ParseResult<Boolean> canSet(String[] value) {
			if(value == null) return ParseResult.partial(false, NullPointerException::new, "Value isn't allowed to be null");
			if(filter == null) return ParseResult.success(true);
			for(int i = 0,m=value.length;i<m;i++) {
				if(!filter.test(value[i])) return ParseResult.partial(false, IllegalStateException::new, "Value ["+value[i]+"] isn't valid");
			}
			return ParseResult.success(true);
		}

		@Override
		public ParseResult<String[]> parseValue(String value) {
			return ParseResult.success(Helpers.splitArray(value, ","));
		}
		
		@Override
		protected String serializedValue(MultilinePolicy policy, String[] value) {
			return serializeArray(policy, value);
		}
		
		public static ParseResult<ArrayValue> parse(String key, String value, String... comment) {
			return ParseResult.success(new ArrayValue(key, Helpers.splitArray(value, ","), comment));
		}
		
		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeVarInt(get().length);
			for (String val : get()) {
				buffer.writeString(val);
			}
		}
		
		@Override
		public void deserializeValue(IReadBuffer buffer) {
			String[] val = new String[buffer.readVarInt()];
			for (int i = 0; i < val.length; i++) {
				val[i] = buffer.readString();
			}
			set(val);
		}
	}
	
	public static class EnumList<E extends Enum<E>> extends CollectionConfigEntry<E, List<E>> {
		Class<E> enumClass;
		
		public EnumList(String key, List<E> defaultValue, Class<E> enumClass, String... comment) {
			super(key, defaultValue, comment);
			this.enumClass = enumClass;
			addSuggestionProvider(ISuggestionProvider.enums(enumClass));
			forceSuggestions(true);
		}
		
		public EnumList(String key, List<E> defaultValue, Class<E> enumClass) {
			super(key, defaultValue);
			this.enumClass = enumClass;
			addSuggestionProvider(ISuggestionProvider.enums(enumClass));
			forceSuggestions(true);
		}
		
		public List<E> get() {
			return getValue();
		}
		
		@Override
		public ParseResult<List<E>> parseValue(String value) {
			List<E> result = new ObjectArrayList<>();
			for(String s : Helpers.splitArray(value, ",")) {
				ParseResult<E> entry = Helpers.parseEnum(enumClass, s);
				if(entry.isValid()) result.add(entry.getValue());
			}
			return ParseResult.success(result);
		}
		
		@Override
		protected String serializedValue(MultilinePolicy policy, List<E> value) {
			String[] result = new String[value.size()];
			for(int i = 0,m=value.size();i<m;i++) {
				result[i] = value.get(i).name();
			}
			return serializeArray(policy, result);
		}

		@Override
		protected List<E> create(E value) {
			return ObjectLists.singleton(value);
		}
		
		@Override
		public ParseResult<Boolean> canSet(List<E> value) {
			if(value == null) return ParseResult.partial(false, NullPointerException::new, "Value isn't allowed to be null");
			for(int i = 0,m=value.size();i<m;i++) {
				E entry = value.get(i);
				if(entry == null) return ParseResult.partial(false, NullPointerException::new, "Value isn't allowed to be null");
				if(!enumClass.isInstance(entry)) return ParseResult.partial(false, IllegalArgumentException::new, "Value must be one of the following: "+Arrays.toString(Helpers.toArray(enumClass)));
			}
			return ParseResult.success(true);
		}
		
		@Override
		protected EnumList<E> copy() {
			return new EnumList<>(getKey(), getDefault(), enumClass, getComment());
		}
		
		@Override
		public IStructuredData getDataType() {
			return ListBuilder.enums(enumClass).build(false);
		}

		@Override
		public char getPrefix() {
			return 'e';
		}

		@Override
		public String getLimitations() {
			return "Must be one of " + Arrays.toString(Helpers.toArray(enumClass));
		}

		@Override
		public void serialize(IWriteBuffer buffer) {
			List<E> value = getValue();
			buffer.writeVarInt(value.size());
			value.forEach(buffer::writeEnum);
		}

		@Override
		protected void deserializeValue(IReadBuffer buffer) {
			List<E> list = new ObjectArrayList<>();
			int size = buffer.readVarInt();
			for(int i = 0;i<size;i++) {
				list.add(buffer.readEnum(enumClass));
			}
			set(list);
		}
		
	}
	
	public static class EnumValue<E extends Enum<E>> extends BasicConfigEntry<E> {
		private Class<E> enumClass;
		
		public EnumValue(String key, E defaultValue, Class<E> enumClass, String... comment) {
			super(key, defaultValue, comment);
			this.enumClass = enumClass;
			addSuggestionProvider(ISuggestionProvider.enums(enumClass));
			forceSuggestions(true);
		}
		
		public EnumValue(String key, E defaultValue, Class<E> enumClass) {
			super(key, defaultValue);
			this.enumClass = enumClass;
			addSuggestionProvider(ISuggestionProvider.enums(enumClass));
			forceSuggestions(true);
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
		public SimpleData getDataType() {
			return EntryDataType.ENUM.toSimpleType();
		}
		
		@Override
		public ParseResult<Boolean> canSet(E value) {
			return ParseResult.result(enumClass.isInstance(value), IllegalArgumentException::new, "Value must be one of the following: "+Arrays.toString(Helpers.toArray(enumClass)));
		}
		
		public E get() {
			return getValue();
		}
		
		@Override
		public String getLimitations() {
			return "Must be one of " + Arrays.toString(Helpers.toArray(enumClass));
		}

		
		@Override
		public ParseResult<E> parseValue(String value) {
			return Helpers.parseEnum(enumClass, value);
		}
		
		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeEnum(get());
		}
		
		@Override
		public void deserializeValue(IReadBuffer buffer) {
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
			return 'p';
		}
		
		@Override
		public CompoundData getDataType() {
			return serializer.getFormat();
		}
		
		public T get() {
			return getValue();
		}
		
		@Override
		public ParseResult<Boolean> canSet(T value) {
			ParseResult<Boolean> result = super.canSet(value);
			if(result.hasError()) return result;
			return serializer.isValid(value);
		}
		
		@Override
		public ParseResult<T> parseValue(String value) {
			return serializer.deserialize(serializer.getFormat().parse(value));
		}
		
		@Override
		protected String serializedValue(MultilinePolicy policy, T value) {
			return serializer.getFormat().serialize(serializer.serialize(value), policy != MultilinePolicy.DISABLED);
		}
		
		private String buildFormat() {
			StringBuilder builder = new StringBuilder();
			serializer.getFormat().appendFormat(builder, true);
			return builder.toString();
		}
		
		@Override
		public String getLimitations() {
			return "Format: ["+buildFormat()+"],\nExample: "+serializedValue(MultilinePolicy.DISABLED, serializer.getExample());
		}
		
		@Override
		public void serialize(IWriteBuffer buffer) {
			serializer.serialize(buffer, getValue());
		}
		
		@Override
		public void deserializeValue(IReadBuffer buffer) {
			set(serializer.deserialize(buffer));
		}
	}
	
	public static class ParsedArray<T> extends CollectionConfigEntry<T, List<T>> {
		IConfigSerializer<T> serializer;

		public ParsedArray(String key, List<T> defaultValue, IConfigSerializer<T> serializer, String... comment) {
			super(key, defaultValue, comment);
			this.serializer = serializer;
		}
		
		public ParsedArray(String key, List<T> defaultValue, IConfigSerializer<T> serializer) {
			super(key, defaultValue);
			this.serializer = serializer;
		}

		@Override
		protected ParsedArray<T> copy() {
			return new ParsedArray<>(getKey(), getValue(), serializer, getComment());
		}

		@Override
		public ParseResult<List<T>> parseValue(String value) {
			List<T> result = new ObjectArrayList<>();
			for(String s : Helpers.splitCompoundArray(value)) {
				ParseResult<T> entry = serializer.deserialize(serializer.getFormat().parse(s));
				if(entry.isValid()) result.add(entry.getValue());
			}
			return ParseResult.success(result);
		}
		
		@Override
		protected String serializedValue(MultilinePolicy policy, List<T> value) {
			String[] result = new String[value.size()];
			for(int i = 0,m=value.size();i<m;i++) {
				result[i] = serializer.getFormat().serialize(serializer.serialize(value.get(i)), policy != MultilinePolicy.DISABLED);
			}
			return serializeArray(policy, result);
		}
		
		@Override
		public ParseResult<Boolean> canSet(List<T> value) {
			if(value == null) return ParseResult.partial(false, NullPointerException::new, "Value isn't allowed to be null");
			for(int i = 0,m=value.size();i<m;i++) {
				T entry = value.get(i);
				if(entry == null) return ParseResult.partial(false, NullPointerException::new, "Value isn't allowed to be null");
				ParseResult<Boolean> result = serializer.isValid(entry);
				if(!result.getValue()) return result;
			}
			return ParseResult.success(true);
		}
		
		@Override
		public ListData getDataType() {
			return ListBuilder.object(serializer.getFormat(), serializer::deserialize, serializer::serialize).setSettings(getSettings()).build(false);
		}
		
		@Override
		public char getPrefix() {
			return 'P';
		}

		private String buildFormat() {
			StringBuilder builder = new StringBuilder();
			serializer.getFormat().appendFormat(builder, true);
			return builder.toString();
		}
		
		@Override
		public String getLimitations() {
			return "Format: ["+buildFormat()+"],\nExample: "+serializedValue(MultilinePolicy.DISABLED, ObjectLists.singleton(serializer.getExample()));
		}

		@Override
		public void serialize(IWriteBuffer buffer) {
			List<T> values = getValue();
			buffer.writeVarInt(values.size());
			for(int i = 0,m=values.size();i<m;i++) {
				serializer.serialize(buffer, values.get(i));
			}
		}

		@Override
		public void deserializeValue(IReadBuffer buffer) {
			List<T> values = new ObjectArrayList<>();
			int size = buffer.readVarInt();
			for(int i = 0;i<size;i++) {
				T value = serializer.deserialize(buffer);
				if(value != null) values.add(value);
			}
			set(values);
		}

		@Override
		protected List<T> create(T value) {
			return ObjectLists.singleton(value);
		}
	}
}
