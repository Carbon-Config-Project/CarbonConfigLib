package carbonconfiglib.config;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import carbonconfiglib.utils.ParseResult;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.sets.ObjectOpenHashSet;

/**
 * Copyright 2026 Speiger, Meduris
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
public interface IConfigSelector<T>
{
	public ParseResult<Boolean> isValid(T value);
	public List<T> getValidValues();
	public String getLimitation();
	
	@SafeVarargs
	public static <T extends Comparable<T>> IConfigSelector<T> simple(T... values) { return new SimpleValue<>(ObjectArrayList.wrap(values), Comparator.naturalOrder()); }
	@SafeVarargs
	public static <T> IConfigSelector<T> simple(Comparator<T> sorter, T... values) { return new SimpleValue<>(ObjectArrayList.wrap(values), sorter); }
	public static <T extends Comparable<T>> IConfigSelector<T> simple(List<T> values) { return new SimpleValue<>(values, Comparator.naturalOrder()); }
	public static <T> IConfigSelector<T> simple(Comparator<T> sorter, List<T> values) { return new SimpleValue<>(values, sorter); }	
	
	@SafeVarargs
	public static <T extends Comparable<T>> IConfigSelector<T[]> array(T... values) { return new ArrayValue<>(ObjectArrayList.wrap(values), Comparator.naturalOrder()); }
	@SafeVarargs
	public static <T> IConfigSelector<T[]> array(Comparator<T> sorter, T... values) { return new ArrayValue<>(ObjectArrayList.wrap(values), sorter); }
	public static <T extends Comparable<T>> IConfigSelector<T[]> array(List<T> values) { return new ArrayValue<>(values, Comparator.naturalOrder()); }
	public static <T> IConfigSelector<T[]> array(Comparator<T> sorter, List<T> values) { return new ArrayValue<>(values, sorter); }
	
	@SafeVarargs
	public static <T extends Comparable<T>> IConfigSelector<List<T>> list(T... values) { return new ListValue<>(ObjectArrayList.wrap(values), Comparator.naturalOrder()); }
	@SafeVarargs
	public static <T> IConfigSelector<List<T>> list(Comparator<T> sorter, T... values) { return new ListValue<>(ObjectArrayList.wrap(values), sorter); }
	public static <T extends Comparable<T>> IConfigSelector<List<T>> list(List<T> values) { return new ListValue<>(values, Comparator.naturalOrder()); }
	public static <T> IConfigSelector<List<T>> list(Comparator<T> sorter, List<T> values) { return new ListValue<>(values, sorter); }
	
	
	public static class SimpleValue<T> implements IConfigSelector<T> {
		Comparator<T> sorter;
		Set<T> values = new ObjectOpenHashSet<>();
		
		public SimpleValue(List<T> list, Comparator<T> sorter) {
			this.values.addAll(list);
			this.sorter = sorter;
			if(values.contains(null)) throw new IllegalArgumentException("Provided Values contain a null. Which is not allowed");
		}

		@Override
		public ParseResult<Boolean> isValid(T value) {
			return ParseResult.result(values.contains(value), IllegalArgumentException::new, "Value isn't in the valid list valid");
		}

		@Override
		public List<T> getValidValues() {
			List<T> values = new ObjectArrayList<>(this.values);
			values.sort(sorter);
			return values;
		}
		
		@Override
		public String getLimitation() {
			return "Must be one of "+getValidValues().toString();
		}
	}
	
	public static class ArrayValue<T> implements IConfigSelector<T[]> {
		Comparator<T> sorter;
		Set<T> values = new ObjectOpenHashSet<>();
		
		public ArrayValue(List<T> list, Comparator<T> sorter) {
			this.values.addAll(list);
			this.sorter = sorter;
			if(values.contains(null)) throw new IllegalArgumentException("Provided Values contain a null. Which is not allowed");
		}
		
		@Override
		public ParseResult<Boolean> isValid(T[] value) {
			return ParseResult.result(values.containsAll(ObjectArrayList.wrap(value)), IllegalArgumentException::new, "Not all Values are inside the valid value list");
		}
		
		@Override
		public List<T[]> getValidValues() {
			return values.stream().sorted(sorter).map(this::toArray).toList();
		}
		
		@SuppressWarnings("unchecked")
		T[] toArray(T input) {
			T[] result = (T[])Array.newInstance(input.getClass(), 1);
			result[0] = input;
			return result;
		}
		
		@Override
		public String getLimitation() {
			List<T> values = new ObjectArrayList<>(this.values);
			values.sort(sorter);
			return "Must be one of "+values.toString();
		}
	}
	
	public static class ListValue<T> implements IConfigSelector<List<T>> {
		Comparator<T> sorter;
		Set<T> values = new ObjectOpenHashSet<>();
		
		public ListValue(List<T> list, Comparator<T> sorter) {
			this.values.addAll(list);
			this.sorter = sorter;
			if(values.contains(null)) throw new IllegalArgumentException("Provided Values contain a null. Which is not allowed");
		}
		
		@Override
		public ParseResult<Boolean> isValid(List<T> value) {
			return ParseResult.result(values.containsAll(value), IllegalArgumentException::new, "Not all Values are inside the valid value list");
		}
		
		@Override
		public List<List<T>> getValidValues() {
			return values.stream().sorted(sorter).map(Collections::singletonList).toList();
		}
		
		@Override
		public String getLimitation() {
			List<T> values = new ObjectArrayList<>(this.values);
			values.sort(sorter);
			return "Must be one of "+values.toString();
		}
	}
}
