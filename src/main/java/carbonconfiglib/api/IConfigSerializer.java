package carbonconfiglib.api;

import java.util.function.BiConsumer;
import java.util.function.Function;

import carbonconfiglib.api.buffer.IReadBuffer;
import carbonconfiglib.api.buffer.IWriteBuffer;
import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.ParsedCollections.ParsedMap;
import carbonconfiglib.utils.structure.StructureCompound.CompoundData;

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
}
