package carbonconfiglib.api;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import carbonconfiglib.api.buffer.IReadBuffer;
import carbonconfiglib.api.buffer.IWriteBuffer;

public interface IConfigSerializer<T> {
	public T getExample();
	public String getFormat();
	public boolean isValid(T value);
	
	public T deserialize(String value);
	public String serialize(T value);
	
	public T deserialize(IReadBuffer buffer);
	public void serialize(IWriteBuffer buffer, T value);
	
	public static <T> IConfigSerializer<T> noSync(String format, T example, Function<String, T> reader, Function<T, String> writer) {
		return new FunctionWriter<>(format, example, reader, writer, null, null, null);
	}
	
	public static <T> IConfigSerializer<T> noSync(String format, T example, Function<String, T> reader, Function<T, String> writer, Predicate<T> filter) {
		return new FunctionWriter<>(format, example, reader, writer, filter, null, null);
	}
	
	public static <T> IConfigSerializer<T> withSync(String format, T example, Function<String, T> reader, Function<T, String> writer, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
		return new FunctionWriter<>(format, example, reader, writer, null, readBuffer, writeBuffer);
	}
	
	public static <T> IConfigSerializer<T> withSync(String format, T example, Function<String, T> reader, Function<T, String> writer, Predicate<T> filter, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
		return new FunctionWriter<>(format, example, reader, writer, filter, readBuffer, writeBuffer);
	}
	
	static class FunctionWriter<T> implements IConfigSerializer<T> {
		String format;
		T example;
		Function<String, T> reader;
		Function<T, String> writer;
		Predicate<T> filter;
		Function<IReadBuffer, T> readBuffer;
		BiConsumer<IWriteBuffer, T> writeBuffer;
		
		public FunctionWriter(String format, T example, Function<String, T> reader, Function<T, String> writer, Predicate<T> filter, Function<IReadBuffer, T> readBuffer, BiConsumer<IWriteBuffer, T> writeBuffer) {
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
		public String getFormat() {
			return format;
		}
		
		@Override
		public boolean isValid(T value) {
			return filter == null || filter.test(value);
		}
		
		@Override
		public T deserialize(String value) {
			return reader.apply(value);
		}

		@Override
		public String serialize(T value) {
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
