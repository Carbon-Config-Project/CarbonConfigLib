package carbonconfiglib.config;

import java.util.Set;

import carbonconfiglib.config.ConfigEntry.ArrayConfigEntry;
import speiger.src.collections.objects.sets.ObjectOpenHashSet;

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
