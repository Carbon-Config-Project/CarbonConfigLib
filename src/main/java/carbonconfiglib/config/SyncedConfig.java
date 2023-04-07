package carbonconfiglib.config;

import java.util.UUID;
import java.util.function.Supplier;

import carbonconfiglib.api.buffer.IReadBuffer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

public class SyncedConfig<T extends ConfigEntry<?>>
{
	Object2ObjectMap<UUID, T> mappedEntries = new Object2ObjectLinkedOpenHashMap<>();
	Supplier<T> creator;
	
	public SyncedConfig(Supplier<T> creator, T defaultValue) {
		this.creator = creator;
		mappedEntries.defaultReturnValue(defaultValue);
	}

	public boolean isPresent(UUID id) {
		return mappedEntries.containsKey(id);
	}
	
	public T get(UUID id) {
		return mappedEntries.get(id);
	}
	
	public void onSync(IReadBuffer buffer) {
		mappedEntries.computeIfAbsent(buffer.readUUID(), T -> creator.get()).deserialize(buffer);
	}
}
