package carbonconfiglib.config;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import carbonconfiglib.config.ConfigEntry.ArrayConfigEntry;
import carbonconfiglib.config.ConfigEntry.CollectionConfigEntry;
import speiger.src.collections.objects.maps.impl.hash.Object2ObjectLinkedOpenHashMap;
import speiger.src.collections.objects.maps.interfaces.Object2ObjectMap;

public abstract class MappedConfig<K, V> implements Runnable
{
	Object2ObjectMap<K, V> mapped = new Object2ObjectLinkedOpenHashMap<>();
	Function<Object, K> keyGenerator;
	Function<Object, V> valueGenerator;
	
	protected MappedConfig(Function<Object, K> keyGenerator, Function<Object, V> valueGenerator) {
		this.keyGenerator = keyGenerator;
		this.valueGenerator = valueGenerator;
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V, T> MappedConfig<K, V> create(ConfigHandler handler, ArrayConfigEntry<T> config, Function<T, K> keyGenerator, Function<T, V> valueGenerator) {
		MappedConfig<K, V> values = new ArrayMappedConfig<>(config, (Function<Object, K>)keyGenerator, (Function<Object, V>)valueGenerator);
		handler.addLoadedListener(values);
		return values;
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V, T, E extends Collection<T>> MappedConfig<K, V> create(ConfigHandler handler, CollectionConfigEntry<T, E> config, Function<T, K> keyGenerator, Function<T, V> valueGenerator) {
		MappedConfig<K, V> values = new CollectionMappedConfig<>(config, (Function<Object, K>)keyGenerator, (Function<Object, V>)valueGenerator);
		handler.addLoadedListener(values);
		return values;
	}
	
	protected abstract void getElements(Consumer<Object> entry);
	
	public Set<Map.Entry<K, V>> entrySet() {
		return mapped.entrySet();
	}
	
	public Set<K> keySet() {
		return mapped.keySet();
	}
	
	public Collection<V> values() {
		return mapped.values();
	}
	
	public boolean contains(K key) {
		return mapped.containsKey(key);
	}
	
	public V get(K key) {
		return mapped.get(key);
	}
	
	public V getOrDefault(K key, V defaultValue) {
		return mapped.getOrDefault(key, defaultValue);
	}
	
	@Override
	public void run() {
		Object2ObjectMap<K, V> mapped = new Object2ObjectLinkedOpenHashMap<>();
		getElements(T -> mapped.put(keyGenerator.apply(T), valueGenerator.apply(T)));
		this.mapped = mapped;
	}
	
	public static class ArrayMappedConfig<K, V, T> extends MappedConfig<K, V> {
		ArrayConfigEntry<T> config;
		
		public ArrayMappedConfig(ArrayConfigEntry<T> config, Function<Object, K> keyGenerator, Function<Object, V> valueGenerator) {
			super(keyGenerator, valueGenerator);
			this.config = config;
		}
		
		@Override
		protected void getElements(Consumer<Object> entry) {
			T[] values = config.getValue();
			for(int i = 0;i<values.length;i++) {
				entry.accept(values[i]);
			}
		}
	}
	
	public static class CollectionMappedConfig<K, V, T, E extends Collection<T>> extends MappedConfig<K, V> {
		CollectionConfigEntry<T, E> config;
		
		public CollectionMappedConfig(CollectionConfigEntry<T, E> config, Function<Object, K> keyGenerator, Function<Object, V> valueGenerator) {
			super(keyGenerator, valueGenerator);
			this.config = config;
		}
		
		@Override
		protected void getElements(Consumer<Object> entry) {
			config.getValue().forEach(entry);
		}
	}
}
