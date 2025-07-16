package carbonconfiglib.api;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import carbonconfiglib.api.buffer.IReadBuffer;
import carbonconfiglib.api.buffer.IWriteBuffer;
import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.ParsedCollections.ParsedList;
import carbonconfiglib.utils.ParsedCollections.ParsedMap;
import carbonconfiglib.utils.structure.StructureCompound.CompoundData;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.maps.impl.hash.Object2ObjectOpenHashMap;

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
public interface IConfigSerializer<T> {
	public T getExample();
	public CompoundData getFormat();
	public ParseResult<Boolean> isValid(T value);
	
	public ParseResult<T> deserialize(ParsedMap value);
	public ParsedMap serialize(T value);
	
	public T deserialize(IReadBuffer buffer);
	public void serialize(IWriteBuffer buffer, T value);
	
	/**
	 * Data Type serializer/parser  
	 * @param <T> simple type
	 * @param format
	 * @param example
	 * @param reader
	 * @param writer
	 * @return
	 */
	public static <T> IConfigSerializer<T> noSync(CompoundData format, T example, Function<ParsedMap, ParseResult<T>> reader, Function<T, ParsedMap> writer) {
		return new FunctionWriter<>(format, example, reader, writer, null, null, null);
	}
	
	public static <T> IConfigSerializer<T> noSync(CompoundData format, T example, Function<ParsedMap, ParseResult<T>> reader, Function<T, ParsedMap> writer, Function<T, ParseResult<Boolean>> filter) {
		return new FunctionWriter<>(format, example, reader, writer, filter, null, null);
	}
	
	public static <T> IConfigSerializer<T> withSync(CompoundData format, T example, Function<ParsedMap, ParseResult<T>> reader, Function<T, ParsedMap> writer, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
		return new FunctionWriter<>(format, example, reader, writer, null, readBuffer, writeBuffer);
	}
	
	public static <T> IConfigSerializer<T> withSync(CompoundData format, T example, Function<ParsedMap, ParseResult<T>> reader, Function<T, ParsedMap> writer, Function<T, ParseResult<Boolean>> filter, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
		return new FunctionWriter<>(format, example, reader, writer, filter, readBuffer, writeBuffer);
	}
	
	public static <T> IConfigSerializer<T> simple(CompoundData format, T example, Function<ParsedMap, T> reader, Function<T, ParsedMap> writer) {
		return new SimpleWriter<>(format, example, reader, writer, null, null, null);
	}
	
	public static <T> IConfigSerializer<T> simple(CompoundData format, T example, Function<ParsedMap, T> reader, Function<T, ParsedMap> writer, Function<T, ParseResult<Boolean>> filter) {
		return new SimpleWriter<>(format, example, reader, writer, filter, null, null);
	}
	
	public static <T> IConfigSerializer<T> simpleSync(CompoundData format, T example, Function<ParsedMap, T> reader, Function<T, ParsedMap> writer, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
		return new SimpleWriter<>(format, example, reader, writer, null, readBuffer, writeBuffer);
	}
	
	public static <T> IConfigSerializer<T> simpleSync(CompoundData format, T example, Function<ParsedMap, T> reader, Function<T, ParsedMap> writer, Function<T, ParseResult<Boolean>> filter, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
		return new SimpleWriter<>(format, example, reader, writer, filter, readBuffer, writeBuffer);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> IConfigSerializer<T> reflectNoSync(CompoundData format, T example) {
		return new FunctionWriter<>(format, example, new Parser<>((Class<T>)example.getClass(), format), new Serializer<>((Class<T>)example.getClass(), format), null, null, null);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> IConfigSerializer<T> reflectNoSync(CompoundData format, T example, Function<T, ParseResult<Boolean>> filter) {
		return new FunctionWriter<>(format, example, new Parser<>((Class<T>)example.getClass(), format), new Serializer<>((Class<T>)example.getClass(), format), filter, null, null);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> IConfigSerializer<T> reflectSync(CompoundData format, T example, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
		return new FunctionWriter<>(format, example, new Parser<>((Class<T>)example.getClass(), format), new Serializer<>((Class<T>)example.getClass(), format), null, readBuffer, writeBuffer);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> IConfigSerializer<T> reflectSync(CompoundData format, T example, Function<T, ParseResult<Boolean>> filter, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
		return new FunctionWriter<>(format, example, new Parser<>((Class<T>)example.getClass(), format), new Serializer<>((Class<T>)example.getClass(), format), filter, readBuffer, writeBuffer);
	}
	
	static class SimpleWriter<T> implements IConfigSerializer<T> {
		CompoundData format;
		T example;
		Function<ParsedMap, T> reader;
		Function<T, ParsedMap> writer;
		Function<T, ParseResult<Boolean>> filter;
		Function<IReadBuffer, T> readBuffer;
		BiConsumer<IWriteBuffer, T> writeBuffer;
		
		public SimpleWriter(CompoundData format, T example, Function<ParsedMap, T> reader, Function<T, ParsedMap> writer, Function<T, ParseResult<Boolean>> filter, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
			this.format = format;
			this.example = example;
			this.reader = reader;
			this.writer = writer;
			this.filter = filter;
			this.readBuffer = readBuffer;
			this.writeBuffer = writeBuffer;
		}
		
		@Override
		public T getExample() {
			return example;
		}
		
		@Override
		public CompoundData getFormat() {
			return format;
		}
		
		@Override
		public ParseResult<Boolean> isValid(T value) {
			return filter == null ? ParseResult.success(true) : filter.apply(value);
		}
		
		@Override
		public ParseResult<T> deserialize(ParsedMap value) {
			ParseResult<Object> preCheck = value.validate(format);
			if(preCheck.hasError()) return preCheck.onlyError();
			try {
				T result = reader.apply(value);
				if(result == null) return ParseResult.error(NullPointerException::new, "Parsing of ["+example.getClass()+"] resulted in a null");
				return ParseResult.success(result);
			}
			catch(Exception e) {
				e.printStackTrace();
				return ParseResult.error(value.toString(), e, "Parsing of ["+example.getClass()+"] Failed");
			}
		}
		
		@Override
		public ParsedMap serialize(T value) {
			return writer.apply(value);
		}
		
		@Override
		public T deserialize(IReadBuffer buffer) {
			if(readBuffer == null || writeBuffer == null) throw new UnsupportedOperationException("No Read/Write Buffer Provided");
			return readBuffer.apply(buffer); 
		}
		
		@Override
		public void serialize(IWriteBuffer buffer, T value) {
			if(readBuffer == null || writeBuffer == null) throw new UnsupportedOperationException("No Read/Write Buffer Provided");
			writeBuffer.accept(buffer, value);
		}
	}
	
	
	static class FunctionWriter<T> implements IConfigSerializer<T> {
		CompoundData format;
		T example;
		Function<ParsedMap, ParseResult<T>> reader;
		Function<T, ParsedMap> writer;
		Function<T, ParseResult<Boolean>> filter;
		Function<IReadBuffer, T> readBuffer;
		BiConsumer<IWriteBuffer, T> writeBuffer;
		
		public FunctionWriter(CompoundData format, T example, Function<ParsedMap, ParseResult<T>> reader, Function<T, ParsedMap> writer, Function<T, ParseResult<Boolean>> filter, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
			this.format = format;
			this.example = example;
			this.reader = reader;
			this.writer = writer;
			this.filter = filter;
			this.readBuffer = readBuffer;
			this.writeBuffer = writeBuffer;
		}
		
		@Override
		public T getExample() {
			return example;
		}
		
		@Override
		public CompoundData getFormat() {
			return format;
		}
		
		@Override
		public ParseResult<Boolean> isValid(T value) {
			return filter == null ? ParseResult.success(true) : filter.apply(value);
		}
		
		@Override
		public ParseResult<T> deserialize(ParsedMap value) {
			ParseResult<Object> preCheck = value.validate(format);
			if(preCheck.hasError()) return preCheck.onlyError();
			return reader.apply(value);
		}
		
		@Override
		public ParsedMap serialize(T value) {
			return writer.apply(value);
		}
		
		@Override
		public T deserialize(IReadBuffer buffer) {
			if(readBuffer == null || writeBuffer == null) throw new UnsupportedOperationException("No Read/Write Buffer Provided");
			return readBuffer.apply(buffer); 
		}
		
		@Override
		public void serialize(IWriteBuffer buffer, T value) {
			if(readBuffer == null || writeBuffer == null) throw new UnsupportedOperationException("No Read/Write Buffer Provided");
			writeBuffer.accept(buffer, value);
		}
	}
	
    public static class Parser<T> implements Function<ParsedMap, ParseResult<T>> {
        private static final Map<Class<?>, Class<?>> WRAPPERS = createWrappers();
        
        private static Map<Class<?>, Class<?>> createWrappers() {
        	Map<Class<?>, Class<?>> result = new Object2ObjectOpenHashMap<>();
        	result.put(boolean.class, Boolean.class);
            result.put(byte.class, Byte.class);
            result.put(char.class, Character.class);
            result.put(short.class, Short.class);
            result.put(int.class, Integer.class);
            result.put(long.class, Long.class);
            result.put(float.class, Float.class);
            result.put(double.class, Double.class);
            return result;
        }
        
    	Class<T> type;
    	CompoundData data;
    	
		public Parser(Class<T> type, CompoundData data) {
			this.type = type;
			this.data = data;
		}
		
		@Override
		public ParseResult<T> apply(ParsedMap t) {
			try {
				T value = type.getConstructor().newInstance();
				for(String key : data.getKeys()) {
					try {
						Field field = type.getDeclaredField(key);
						field.setAccessible(true);
						Class<?> type = field.getType();
						boolean collection = Collection.class.isAssignableFrom(type);
						ParseResult<?> data = t.getOrError(key, (Class<?>)(collection ? ParsedList.class : WRAPPERS.getOrDefault(type, type)));
						if(data.hasError()) return data.onlyError();
						if(collection) {
							field.set(value, unwrap((ParsedList)data.getValue()));
							continue;
						}
						field.set(value, data.getValue());
					}
					catch(Exception e) {
						e.printStackTrace();
						return ParseResult.error(t.toString(), e, "Couldn't Set Field ["+key+"] in class ["+type.getSimpleName()+"]");
					}
				}
				return ParseResult.success(value);
			}
			catch(Exception e) {
				e.printStackTrace();
				return ParseResult.error(t.toString(), e, "Couldn't create instance of ["+type.getSimpleName()+"]");
			}
		}
		
		@SuppressWarnings("unchecked")
		private <S> List<S> unwrap(ParsedList list) {
			List<S> result = new ObjectArrayList<>();
			for(Object entry : list.objectIterator()) {
				if(entry instanceof ParsedList) result.add((S)unwrap((ParsedList)entry));
				else result.add((S)entry);
			}
			return result;
		}
    }
    
    public static class Serializer<T> implements Function<T, ParsedMap> {
    	Class<T> type;
    	CompoundData data;
    	
		public Serializer(Class<T> type, CompoundData data) {
			this.type = type;
			this.data = data;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ParsedMap apply(T t) {
			ParsedMap map = new ParsedMap();
			try {
				for(String key : data.getKeys()) {
					Field field = type.getDeclaredField(key);
					field.setAccessible(true);
					Object data = field.get(t);
					if(data instanceof Collection) map.put(key, wrapList((Collection<? extends Object>)data));
					else map.put(key, data);
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return map;
		}
		
		@SuppressWarnings("unchecked")
		private ParsedList wrapList(Collection<? extends Object> data) {
			ParsedList list = new ParsedList();
			for(Object entry : data) {
				if(entry instanceof Collection) list.add(wrapList((Collection<? extends Object>)entry));
				else list.add(entry);
			}
			return list;
		}
    }
}
