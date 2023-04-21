package carbonconfiglib.utils;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface IEntryDataType {
	public boolean isCompound();
	public default EntryDataType asDataType() { throw new IllegalStateException("Type isn't a Simple DataEntry"); }
	public default CompoundDataType asCompound() { throw new IllegalStateException("Type isn't a Compound DataEntry"); }
	
	public static enum EntryDataType implements IEntryDataType {
		BOOLEAN,
		INTEGER,
		DOUBLE,
		STRING,
		CUSTOM;
		
		@Override
		public boolean isCompound() {
			return false;
		}
		
		@Override
		public EntryDataType asDataType() {
			return this;
		}
	}
	
	public static final class CompoundDataType implements IEntryDataType {
		List<Map.Entry<String, EntryDataType>> values = new ObjectArrayList<>();
		Map<String, Class<?>> customVariants = new Object2ObjectOpenHashMap<>();
		
		public CompoundDataType with(String name, EntryDataType type) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(type);
			if(type == EntryDataType.CUSTOM) throw new IllegalStateException("Use withCustom to add custom entries!");
			values.add(new AbstractMap.SimpleEntry<>(name, type));
			return this;
		}
		
		public CompoundDataType withCustom(String name, Class<?> type) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(type);
			values.add(new AbstractMap.SimpleEntry<>(name, EntryDataType.CUSTOM));
			customVariants.put(name, type);
			return this;
		}
		
		public List<Map.Entry<String, EntryDataType>> getCompound() {
			return Collections.unmodifiableList(values);
		}
		
		public Class<?> getVariant(String name) {
			return customVariants.get(name);
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