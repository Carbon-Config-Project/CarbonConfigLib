package carbonconfiglib.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.lists.ObjectList;
import speiger.src.collections.objects.maps.interfaces.Object2ObjectMap;

/**
 * Copyright 2024 Speiger, Meduris
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

public class ParsedCollections
{
	public static interface IParsed {}
	
	public static class ParsedList implements IParsed
	{
		ObjectList<Object> objects = new ObjectArrayList<>();
		
		public ParsedList() {}
		
		public ParsedList(Collection<? extends Object> elements) {
			objects.addAll(elements);
		}
		
		public ParsedList(Object... elements) {
			objects.addAll(elements);
		}
		
		public void addAll(Collection<? extends Object> elements) {
			objects.addAll(elements);
		}
		
		public void addAll(Object...elements) {
			objects.addAll(elements);
		}
		
		public void add(Object element) {
			objects.add(element);
		}
		
		@SuppressWarnings("unchecked")
		public static ParseResult<ParsedList> unwrap(ParsedList list) {
			for(int i = 0,m=list.size();i<m;i++) {
				Object obj = list.objects.get(i);
				if(obj instanceof ParseResult) {
					ParseResult<Object> result = (ParseResult<Object>)obj;
					if(result.hasError()) return result.onlyError();
					list.objects.set(i, result.getValue());
				}
			}
			return ParseResult.success(list);
		}
		
		public int size() { return objects.size(); }
		
		public <T> T get(int index, Class<T> type) {
			Object obj = objects.get(index);
			return type.isInstance(obj) ? type.cast(obj) : null;
		}
		
		public <T> ParseResult<T> getOrError(int index, Class<T> type, String errorMessage) {
			Object obj = objects.get(index);
			return type.isInstance(obj) ? ParseResult.success(type.cast(obj)) : ParseResult.error(NullPointerException::new, errorMessage);
		}
		
		@SuppressWarnings("unchecked")
		public <T> T getUnsafe(int index) {
			return (T)objects.get(index);
		}
		
		public <T, E extends Collection<T>> E collect(Class<T> type, E output) {
			for(Object obj : objects) {
				if(type.isInstance(obj)) {
					output.add(type.cast(obj));
				}
			}
			return output;
		}
		
		/*
		 * Note: doesn't check type safety and is inherently unsafe as is, but as long as you have your types correct you will be fine.
		 */
		@SuppressWarnings("unchecked")
		public <T, E extends Collection<T>> E collectCollections(E output) {
			output.addAll((Collection<? extends T>)objects);
			return output;
		}

		
		public <T> Iterable<T> typedIterator(Class<T> type) {
			return () -> new Iterator<T>() {
				Iterator<Object> iter = objects.iterator();
				
				@Override
				public boolean hasNext() { return iter.hasNext(); }
				@Override
				public T next() {
					Object obj = iter.next();
					return type.isInstance(obj) ? type.cast(obj) : null; 
				}
			};
		}
		
		@Override
		public String toString() {
			return objects.toString();
		}
	}
	
	public static class ParsedMap implements IParsed
	{
		Map<String, Object> parsed = Object2ObjectMap.builder().linkedMap();
		
		public ParsedMap() {}
		
		public ParsedMap(String key, Object obj) {
			put(key, obj);
		}
		
		public ParsedMap(Map<String, ? extends Object> c) {
			putAll(c);
		}
		
		public void put(String key, Object obj) {
			parsed.put(key, obj);
		}
		
		public void putAll(Map<String, ? extends Object> c) {
			parsed.putAll(c);
		}
		
		public Set<String> keySet() {
			return Collections.unmodifiableSet(parsed.keySet());
		}
		
		@SuppressWarnings("unchecked")
		public <T> T get(String key, Class<T> type) {
			Object obj = parsed.get(key);
			if(obj instanceof ParseResult) {
				obj = ((ParseResult<Object>)obj).getValue();
			}
			return type.isInstance(obj) ? type.cast(obj) : null;
		}
		
		public <T> ParseResult<T> getOrError(String key, Class<T> type) {
			return getOrError(key, type, "Variable ["+key+"] couldn't be parsed");
		}
		
		public <T> ParseResult<T> getOrError(String key, Class<T> type, String errorMessage) {
			Object obj = parsed.get(key);
			if(obj instanceof ParseResult) {
				ParseResult<?> value = (ParseResult<?>)obj;
				return value.validateType(type, errorMessage);
			}
			return ParseResult.error(NullPointerException::new, errorMessage);
		}
		
		@SuppressWarnings("unchecked")
		public <T> T getUnsafe(String key) {
			return (T)parsed.get(key);
		}
		
		@Override
		public String toString() {
			return parsed.toString();
		}
	}
}
