package carbonconfiglib.api;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

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
