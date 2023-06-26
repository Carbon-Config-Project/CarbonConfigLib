package carbonconfiglib.config;

import java.util.Set;

import carbonconfiglib.config.ConfigEntry.ArrayConfigEntry;
import speiger.src.collections.objects.sets.ObjectOpenHashSet;

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
public class HashSetCache<T> {
    public final ArrayConfigEntry<T> configEntry;
    private final Set<T> cache = new ObjectOpenHashSet<>();
    
    private HashSetCache(ArrayConfigEntry<T> configEntry, ConfigHandler configHandler) {
        this.configEntry = configEntry;
        configHandler.addLoadedListener(this::reload);
    }
    
    private void reload() {
        cache.clear();
        for (T val : configEntry.getValue()) {
            cache.add(val);
        }
    }
    
    public boolean contains(T value) {
        return cache.contains(value);
    }
    
    public static <T> HashSetCache<T> create(ArrayConfigEntry<T> configEntry, ConfigHandler configHandler) {
        return new HashSetCache<>(configEntry, configHandler);
    }
}
