package carbonconfiglib.config;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import carbonconfiglib.config.ConfigEntry.ArrayConfigEntry;
import carbonconfiglib.config.ConfigEntry.CollectionConfigEntry;
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
    private final Set<T> cache = new ObjectOpenHashSet<>();
    private final Consumer<Consumer<T>> provider;

    private HashSetCache(ConfigHandler configHandler, Consumer<Consumer<T>> provider) {
        this.provider = provider;
        configHandler.addLoadedListener(this::reload);
    }
    
    private void reload() {
        cache.clear();
        provider.accept(cache::add);
    }
    
    public boolean contains(T value) {
        return cache.contains(value);
    }
    
    public static <T> HashSetCache<T> create(ArrayConfigEntry<T> configEntry, ConfigHandler configHandler) {
        return new HashSetCache<>(configHandler, T -> {
            for (T val : configEntry.getValue()) {
                T.accept(val);
            }
        });
    }

    public static <T, E extends Collection<T>> HashSetCache<T> create(CollectionConfigEntry<T, E> configEntry, ConfigHandler configHandler) {
        return new HashSetCache<>(configHandler, T -> configEntry.getValue().forEach(T));
    }
}
