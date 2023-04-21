package carbonconfiglib.api;

import java.util.function.BiConsumer;
import java.util.function.Function;

import carbonconfiglib.api.buffer.IReadBuffer;
import carbonconfiglib.api.buffer.IWriteBuffer;
import carbonconfiglib.utils.IEntryDataType.CompoundDataType;
import carbonconfiglib.utils.ParseResult;

public interface IConfigSerializer<T> {
	public T getExample();
	public CompoundDataType getFormat();
	public ParseResult<Boolean> isValid(T value);
	
	public ParseResult<T> deserialize(String[] value);
	public String[] serialize(T value);
	
	public T deserialize(IReadBuffer buffer);
	public void serialize(IWriteBuffer buffer, T value);
	
	public static <T> IConfigSerializer<T> noSync(CompoundDataType format, T example, Function<String[], ParseResult<T>> reader, Function<T, String[]> writer) {
		return new FunctionWriter<>(format, example, reader, writer, null, null, null);
	}
	
	public static <T> IConfigSerializer<T> noSync(CompoundDataType format, T example, Function<String[], ParseResult<T>> reader, Function<T, String[]> writer, Function<T, ParseResult<Boolean>> filter) {
		return new FunctionWriter<>(format, example, reader, writer, filter, null, null);
	}
	
	public static <T> IConfigSerializer<T> withSync(CompoundDataType format, T example, Function<String[], ParseResult<T>> reader, Function<T, String[]> writer, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
		return new FunctionWriter<>(format, example, reader, writer, null, readBuffer, writeBuffer);
	}
	
	public static <T> IConfigSerializer<T> withSync(CompoundDataType format, T example, Function<String[], ParseResult<T>> reader, Function<T, String[]> writer, Function<T, ParseResult<Boolean>> filter, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
		return new FunctionWriter<>(format, example, reader, writer, filter, readBuffer, writeBuffer);
	}
	
	static class FunctionWriter<T> implements IConfigSerializer<T> {
		CompoundDataType format;
		T example;
		Function<String[], ParseResult<T>> reader;
		Function<T, String[]> writer;
		Function<T, ParseResult<Boolean>> filter;
		Function<IReadBuffer, T> readBuffer;
		BiConsumer<IWriteBuffer, T> writeBuffer;
		
		public FunctionWriter(CompoundDataType format, T example, Function<String[], ParseResult<T>> reader, Function<T, String[]> writer, Function<T, ParseResult<Boolean>> filter, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
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
		public CompoundDataType getFormat() {
			return format;
		}
		
		@Override
		public ParseResult<Boolean> isValid(T value) {
			return filter == null ? ParseResult.success(true) : filter.apply(value);
		}
		
		@Override
		public ParseResult<T> deserialize(String[] value) {
			return reader.apply(value);
		}
		
		@Override
		public String[] serialize(T value) {
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
