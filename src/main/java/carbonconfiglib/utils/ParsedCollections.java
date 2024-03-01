package carbonconfiglib.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.maps.interfaces.Object2ObjectMap;

public class ParsedCollections
{
	public static interface IParsed {
		
	}
	
	public static class ParsedList implements IParsed
	{
		List<Object> object = new ObjectArrayList<>();
		
		public void add(Object obj) {
			object.add(obj);
		}
		
		public int size() { return object.size(); }
		
		public <T> T get(int index, Class<T> type) {
			Object obj = object.get(index);
			return type.isInstance(obj) ? type.cast(obj) : null;
		}
		
		@SuppressWarnings("unchecked")
		public <T> T getUnsafe(int index) {
			return (T)object.get(index);
		}
		
		public <T> Iterable<T> typedIterator(Class<T> type) {
			return () -> new Iterator<T>() {
				Iterator<Object> iter = object.iterator();
				
				@Override
				public boolean hasNext() { return iter.hasNext(); }
				@Override
				public T next() {
					Object obj = iter.next();
					return type.isInstance(obj) ? type.cast(obj) : null; 
				}
			};
		}
	}
	
	public static class ParsedMap implements IParsed
	{
		Map<String, Object> parsed = Object2ObjectMap.builder().linkedMap();
		
		public void put(String key, Object obj) {
			parsed.put(key, obj);
		}
		
		public <T> T get(String key, Class<T> type) {
			Object obj = parsed.get(key);
			return type.isInstance(obj) ? type.cast(obj) : null;
		}
		
		@SuppressWarnings("unchecked")
		public <T> T getUnsafe(String key) {
			return (T)parsed.get(key);
		}
	}
}
