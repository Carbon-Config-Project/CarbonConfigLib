package carbonconfiglib.utils.structure;

import carbonconfiglib.utils.structure.StructureCompound.CompoundData;
import carbonconfiglib.utils.structure.StructureList.ListData;

public interface IStructuredData
{
	public StructureType getDataType();
	public default SimpleData asSimple() { throw new IllegalStateException("Type isn't a Simple Structure"); }
	public default CompoundData asCompound() { throw new IllegalStateException("Type isn't a Compound Structure"); };
	public default ListData asList() { throw new IllegalStateException("Type isn't a List Structure"); }
	
	public static class SimpleData implements IStructuredData {
		EntryDataType dataType;
		Class<?> variant;
		
		private SimpleData(EntryDataType dataType, Class<?> variant) {
			this.dataType = dataType;
			this.variant = variant;
		}
		
		public static SimpleData variant(Class<?> variant) { return new SimpleData(EntryDataType.CUSTOM, variant); }
		private static SimpleData of(EntryDataType type) { return new SimpleData(type, null); }
		
		@Override
		public StructureType getDataType() {return StructureType.SIMPLE; }
		public SimpleData asSimple() { return this; }
		public EntryDataType getType() { return dataType; }
		public Class<?> getVariant() { return variant; }
	}
	
	public static enum StructureType {
		SIMPLE,
		COMPOUND,
		LIST;
	}
	
	public static enum EntryDataType {
		BOOLEAN,
		INTEGER,
		DOUBLE,
		STRING,
		ENUM,
		CUSTOM(true);
		
		SimpleData simple;
		
		private EntryDataType() { this(false); }
		private EntryDataType(boolean custom) {
			simple = custom ? null : SimpleData.of(this);
		}
		
		public SimpleData toSimpleType() {
			return simple;
		}
	}
}
