package carbonconfiglib.api;

import java.util.List;
import java.util.Map;

import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.maps.interfaces.Object2ObjectMap;

/**
 * Copyright 2024 Speiger, Meduris
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
public interface IEntrySettings {
	public default <T> T get(Class<T> clz) { return clz.isInstance(this) ? clz.cast(this) : null; }
	
	
	public static IEntrySettings compound(IEntrySettings... settings) { return new CompoundEntrySettings(ObjectArrayList.wrap(settings)); }
	
	public static class CompoundEntrySettings implements IEntrySettings {
		Map<Class<?>, IEntrySettings> settings = Object2ObjectMap.builder().map();
		
		public CompoundEntrySettings(List<IEntrySettings> settings) {
			for(IEntrySettings setting : settings) {
				this.settings.put(setting.getClass(), setting);
			}
		}
		
		public CompoundEntrySettings(Map<Class<?>, IEntrySettings> settings) {
			this.settings.putAll(settings);
		}
		
		@Override
		public <T> T get(Class<T> clz) {
			IEntrySettings setting = settings.get(clz);
			return setting == null ? null : clz.cast(setting);
		}
	}
}
