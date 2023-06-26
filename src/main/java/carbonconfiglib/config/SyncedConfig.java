package carbonconfiglib.config;

import java.util.UUID;
import java.util.function.Supplier;

import carbonconfiglib.api.buffer.IReadBuffer;
import speiger.src.collections.objects.maps.impl.hash.Object2ObjectLinkedOpenHashMap;
import speiger.src.collections.objects.maps.interfaces.Object2ObjectMap;

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
public class SyncedConfig<T extends ConfigEntry<?>>
{
	Object2ObjectMap<UUID, T> mappedEntries = new Object2ObjectLinkedOpenHashMap<>();
	Supplier<T> creator;
	
	public SyncedConfig(Supplier<T> creator, T defaultValue) {
		this.creator = creator;
		mappedEntries.setDefaultReturnValue(defaultValue);
	}

	public boolean isPresent(UUID id) {
		return mappedEntries.containsKey(id);
	}
	
	public T get(UUID id) {
		return mappedEntries.get(id);
	}
	
	public void onSync(IReadBuffer buffer, UUID owner) {
		mappedEntries.computeIfAbsent(owner, T -> creator.get()).deserializeValue(buffer);
	}
}
