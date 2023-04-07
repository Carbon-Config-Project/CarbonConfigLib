package carbonconfiglib.utils;

import java.util.Objects;

public class ParseResult<T, U> {
	private final T value;
	private final U error;

	public ParseResult(T value) {
		this.value = value;
		this.error = null;
	}

	public ParseResult(T value, U error) {
		this.value = value;
		this.error = error;
	}

	public static <T, U> ParseResult<T, U> of(T value) {
		return new ParseResult<>(value);
	}

	public static <T, U> ParseResult<T, U> of(T value, U error) {
		return new ParseResult<>(value, error);
	}

	public T getValue() {
		return value;
	}

	public U getError() {
		return error;
	}

	public boolean hasError() {
		return error != null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ParseResult<?, ?> pair = (ParseResult<?, ?>) o;
		return Objects.equals(value, pair.value) && Objects.equals(error, pair.error);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, error);
	}

	@Override
	public String toString() {
		return "ParseResult{first=" + value + ", second=" + error + '}';
	}
}
