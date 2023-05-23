package carbonconfiglib.config;

import java.util.UUID;
import java.util.function.Supplier;

import carbonconfiglib.api.buffer.IReadBuffer;
import speiger.src.collections.objects.maps.impl.hash.Object2ObjectLinkedOpenHashMap;
import speiger.src.collections.objects.maps.interfaces.Object2ObjectMap;

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
