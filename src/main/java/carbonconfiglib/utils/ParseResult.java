package carbonconfiglib.utils;

import java.util.Objects;
import java.util.function.Function;

public class ParseResult<T> {
	private final T value;
	private final ParseExpection error;

	private ParseResult(T value, ParseExpection error) {
		this.value = value;
		this.error = error;
	}
	
	public static ParseResult<Boolean> result(boolean isSuccess, Function<String, Exception> error, String message) {
		return new ParseResult<Boolean>(isSuccess, isSuccess ? null : new ParseExpection(null, error.apply(message), message));
	}
	
	public static <T> ParseResult<T> successOrError(T value, boolean isSuccess, Function<String, Exception> error, String message) {
		return new ParseResult<T>(value, isSuccess ? null : new ParseExpection(null, error.apply(message), message));
	}
	
	public static <T> ParseResult<T> success(T value) {
		return new ParseResult<>(value, null);
	}
	
	public static <T> ParseResult<T> partial(T value, Exception e) {
		return new ParseResult<T>(value, new ParseExpection(null, e, null));
	}
	
	public static <T> ParseResult<T> partial(T value, Function<String, Exception> e, String message) {
		return new ParseResult<T>(value, new ParseExpection(null, e.apply(message), message));
	}
	
	public static <T> ParseResult<T> partial(T value, ParseExpection error) {
		return new ParseResult<>(value, error);
	}
	
	public static <T> ParseResult<T> error(String value, Exception excpetion) {
		return new ParseResult<>(null, new ParseExpection(value, excpetion, null));
	}
	
	public static <T> ParseResult<T> error(String value, String message) {
		return new ParseResult<>(null, new ParseExpection(value, null, message));
	}
	
	public static <T> ParseResult<T> error(String value, Exception excpetion, String message) {
		return new ParseResult<>(null, new ParseExpection(value, excpetion, message));
	}
	
	public static <T> ParseResult<T> error(ParseExpection error) {
		return new ParseResult<>(null, error);
	}
	
	public T getValue() {
		return value;
	}

	public ParseExpection getError() {
		return error;
	}
	
	public boolean isValid() {
		return error == null;
	}
	
	public boolean hasError() {
		return error != null;
	}
	
	public <S> ParseResult<S> onlyError() {
		return new ParseResult<>(null, error);
	}
	
	public <S> ParseResult<S> withDefault(S value) {
		return new ParseResult<S>(value, error);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ParseResult<?> pair = (ParseResult<?>) o;
		return Objects.equals(value, pair.value) && Objects.equals(error, pair.error);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, error);
	}

	@Override
	public String toString() {
		return "ParseResult{value=" + value + ", error=" + error + '}';
	}

}
