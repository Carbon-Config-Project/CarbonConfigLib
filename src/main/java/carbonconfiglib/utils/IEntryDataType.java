package carbonconfiglib.utils;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import carbonconfiglib.api.ISuggestionProvider;
import carbonconfiglib.api.ISuggestionProvider.Suggestion;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.maps.impl.hash.Object2ObjectOpenHashMap;
import speiger.src.collections.objects.utils.ObjectLists;

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
public interface IEntryDataType {
	public boolean isCompound();
	public default SimpleDataType asDataType() { throw new IllegalStateException("Type isn't a Simple DataEntry"); }
	public default CompoundDataType asCompound() { throw new IllegalStateException("Type isn't a Compound DataEntry"); }
	
	public static class SimpleDataType implements IEntryDataType {
		EntryDataType dataType;
		Class<?> variant;
		
		private SimpleDataType(EntryDataType dataType, Class<?> variant) {
			this.dataType = dataType;
			this.variant = variant;
		}
		
		public static SimpleDataType ofVariant(Class<?> variant) {
			return new SimpleDataType(EntryDataType.CUSTOM, variant);
		}
		
		@Override
		public boolean isCompound() {
			return false;
		}
		
		public SimpleDataType asDataType() {
			return this;
		}
		
		public EntryDataType getType() {
			return dataType;
		}
		
		public Class<?> getVariant() {
			return variant;
		}
	}
	
	public static enum EntryDataType {
		BOOLEAN,
		INTEGER,
		DOUBLE,
		STRING,
		ENUM,
		CUSTOM(true);
		
		SimpleDataType owner;
		
		private EntryDataType() {
			this(false);
		}
		
		private EntryDataType(boolean custom) {
			this.owner = custom ? null : new SimpleDataType(this, null);
		}
		
		public SimpleDataType toSimpleType() {
			return owner;
		}
	}
	
	public static final class CompoundDataType implements IEntryDataType {
		List<Map.Entry<String, EntryDataType>> values = new ObjectArrayList<>();
		Map<String, Class<?>> customVariants = new Object2ObjectOpenHashMap<>();
		Map<String, EntryDataType> customDisplay = new Object2ObjectOpenHashMap<>();
		Map<String, List<ISuggestionProvider>> providers = new Object2ObjectOpenHashMap<>();
		
		public CompoundDataType with(String name, EntryDataType type) {
			return withSuggestion(name, type);
		}

		public CompoundDataType withSuggestion(String name, EntryDataType type, ISuggestionProvider... provider) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(type);
			if(type == EntryDataType.CUSTOM) throw new IllegalStateException("Use withCustom to add custom entries!");
			values.add(new AbstractMap.SimpleEntry<>(name, type));
			if(provider != null && provider.length > 0) providers.computeIfAbsent(name, T -> new ObjectArrayList<>()).addAll(ObjectArrayList.wrap(provider));
			return this;
		}
		
		public CompoundDataType withCustom(String name, Class<?> type, EntryDataType displayType) {
			return withCustomSuggestion(name, type, displayType);
		}
		
		public CompoundDataType withCustomSuggestion(String name, Class<?> type, EntryDataType displayType, ISuggestionProvider... provider) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(type);
			values.add(new AbstractMap.SimpleEntry<>(name, EntryDataType.CUSTOM));
			customVariants.put(name, type);
			customDisplay.put(name, displayType);
			if(provider != null && provider.length > 0) providers.computeIfAbsent(name, T -> new ObjectArrayList<>()).addAll(ObjectArrayList.wrap(provider));
			return this;
		}
				
		public List<Map.Entry<String, EntryDataType>> getCompound() {
			return Collections.unmodifiableList(values);
		}
		
		public Class<?> getVariant(String name) {
			return customVariants.get(name);
		}
		
		public EntryDataType getDisplay(String name) {
			return customDisplay.get(name);
		}
		
		public List<ISuggestionProvider> getProviders(String name) {
			return providers.getOrDefault(name, ObjectLists.empty());
		}
		
		public List<Suggestion> getSuggestions(String name, Predicate<Suggestion> filter) {
			List<Suggestion> suggestions = new ObjectArrayList<>();
			for(ISuggestionProvider provider : providers.getOrDefault(name, ObjectLists.empty())) {
				provider.provideSuggestions(suggestions::add, filter);
			}
			return suggestions;
		}
		
		@Override
		public CompoundDataType asCompound() {
			return this;
		}

		@Override
		public boolean isCompound() {
			return true;
		}
	}
}