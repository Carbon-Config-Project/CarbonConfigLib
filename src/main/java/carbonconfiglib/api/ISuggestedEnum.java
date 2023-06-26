package carbonconfiglib.api;

import java.util.Map;

import speiger.src.collections.objects.maps.impl.hash.Object2ObjectOpenHashMap;
import speiger.src.collections.objects.utils.maps.Object2ObjectMaps;

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
public interface ISuggestedEnum<T extends Enum<T>>
{
	public String getName(T value);
	
	public static void registerWrapper(Class<? extends Enum<?>> clz, ISuggestedEnum<?> wrapper) {
		EnumStorage.WRAPPERS.put(clz, wrapper);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> ISuggestedEnum<T> getWrapper(T value) {
		if(value instanceof ISuggestedEnum) return (ISuggestedEnum<T>)value;
		return (ISuggestedEnum<T>)EnumStorage.WRAPPERS.get(value.getClass());
	}
	
	static class EnumStorage
	{
		private static final Map<Class<? extends Enum<?>>, ISuggestedEnum<?>> WRAPPERS = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
	}
}
