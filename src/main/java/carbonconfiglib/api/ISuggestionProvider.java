package carbonconfiglib.api;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import speiger.src.collections.objects.lists.ObjectArrayList;

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
public interface ISuggestionProvider
{
	public void provideSuggestions(Consumer<Suggestion> output, Predicate<Suggestion> filter);
	
	public static ISuggestionProvider single(Suggestion value) { return new SingleSuggestion(value); }
	public static ISuggestionProvider array(Suggestion... elements) { return new SimpleSuggestion(ObjectArrayList.wrap(elements)); }
	public static ISuggestionProvider list(List<Suggestion> elements) { return new SimpleSuggestion(elements); }
	public static <E extends Enum<E>> ISuggestionProvider enums(Class<E> type) { return new EnumSuggestion<>(type); }
	public static ISuggestionProvider wrapper(Function<Predicate<Suggestion>, List<Suggestion>> function) { return new Wrapper(function); }
	
	public static class SingleSuggestion implements ISuggestionProvider {
		Suggestion value;
		
		private SingleSuggestion(Suggestion value) {
			this.value = value;
		}
		
		@Override
		public void provideSuggestions(Consumer<Suggestion> output, Predicate<Suggestion> filter) {
			if(filter.test(value)) output.accept(value);
		}
	}
	
	public static class SimpleSuggestion implements ISuggestionProvider {
		List<Suggestion> suggestions;
		
		private SimpleSuggestion(List<Suggestion> suggestions) {
			this.suggestions = suggestions;
		}
		
		@Override
		public void provideSuggestions(Consumer<Suggestion> output, Predicate<Suggestion> filter) {
			for(Suggestion value : suggestions) {
				if(filter.test(value)) output.accept(value);
			}
		}
	}
	
	public static class EnumSuggestion<E extends Enum<E>> implements ISuggestionProvider {
		Class<E> enumClass;
		
		private EnumSuggestion(Class<E> enumClass) {
			this.enumClass = enumClass;
		}
		
		@Override
		public void provideSuggestions(Consumer<Suggestion> output, Predicate<Suggestion> filter) {
			for(E value : enumClass.getEnumConstants()) {
				ISuggestedEnum<E> wrapper = ISuggestedEnum.getWrapper(value);
				Suggestion suggestion = wrapper == null ? Suggestion.value(value.name()) : Suggestion.namedValue(wrapper.getName(value), value.name());
				if(filter.test(suggestion)) {
					output.accept(suggestion);
				}
			}
		}
	}
	
	public static class Wrapper implements ISuggestionProvider {
		Function<Predicate<Suggestion>, List<Suggestion>> function;

		public Wrapper(Function<Predicate<Suggestion>, List<Suggestion>> function) {
			this.function = function;
		}

		@Override
		public void provideSuggestions(Consumer<Suggestion> output, Predicate<Suggestion> filter) {
			function.apply(filter).forEach(output);
		}
	}
	
	public static class Suggestion {
		String name;
		String value;
		Object type;
		
		private Suggestion(String name, String value, Object type) {
			this.name = name;
			this.value = Objects.requireNonNull(value);
			this.type = type;
			if(value.trim().isEmpty()) throw new IllegalArgumentException("Value isn't allowed to be empty");
		}
		
		public static Suggestion namedTypeValue(String name, String value, Object type) {
			return new Suggestion(name, value, type);
		}
		
		public static Suggestion value(String value) {
			return new Suggestion(value, value, null);
		}
		
		public static Suggestion namedValue(String name, String value) {
			return new Suggestion(name, value, null);
		}
		
		public static Suggestion typedValue(String value, Object type) {
			return new Suggestion(value, value, type);
		}
		
		public String getName() { return name; }
		public String getValue() { return value; }
		public Object getType() { return type; }
	}
}
