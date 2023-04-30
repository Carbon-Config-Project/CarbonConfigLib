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
	public default SimpleDataType asDataType() { throw new IllegalStateException("Type isn't a Simple DataEntry"); }
	public default CompoundDataType asCompound() { throw new IllegalStateException("Type isn't a Compound DataEntry"); }
	
	public static class SimpleDataType implements IEntryDataType {
		EntryDataType dataType;
		Class<?> variant;
		
		private SimpleDataType(EntryDataType dataType, Class<?> variant) {
			this.dataType = dataType;
			this.variant = variant;
		}
		
		public static SimpleDataType ofVariant(Class<?> variant) {
			return new SimpleDataType(EntryDataType.CUSTOM, variant);
		}
		
		@Override
		public boolean isCompound() {
			return false;
		}
		
		public SimpleDataType asDataType() {
			return this;
		}
		
		public EntryDataType getType() {
			return dataType;
		}
		
		public Class<?> getVariant() {
			return variant;
		}
	}
	
	public static enum EntryDataType {
		BOOLEAN,
		INTEGER,
		DOUBLE,
		STRING,
		CUSTOM(true);
		
		SimpleDataType owner;
		
		private EntryDataType() {
			this(false);
		}
		
		private EntryDataType(boolean custom) {
			this.owner = custom ? null : new SimpleDataType(this, null);
		}
		
		public SimpleDataType toSimpleType() {
			return owner;
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