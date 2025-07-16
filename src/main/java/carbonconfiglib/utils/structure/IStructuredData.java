package carbonconfiglib.utils.structure;

import java.util.function.Function;

import carbonconfiglib.api.IEntrySettings;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.structure.StructureCompound.CompoundData;
import carbonconfiglib.utils.structure.StructureList.ListData;

/**
 * Copyright 2024 Speiger, Meduris
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
public interface IStructuredData
{
	public StructureType getDataType();
	public IEntrySettings getSettings();
	public default SimpleData asSimple() { throw new IllegalStateException("Type isn't a Simple Structure"); }
	public default CompoundData asCompound() { throw new IllegalStateException("Type isn't a Compound Structure"); };
	public default ListData asList() { throw new IllegalStateException("Type isn't a List Structure"); }
	public void appendFormat(StringBuilder builder, boolean start);
	public String generateDefaultValue(Function<SimpleData, String> defaultFactory);
	
	public static class SimpleData implements IStructuredData {
		EntryDataType dataType;
		Class<?> variant;
		IEntrySettings settings;
		
		private SimpleData(EntryDataType dataType, Class<?> variant, IEntrySettings settings) {
			this.dataType = dataType;
			this.variant = variant;
			this.settings = settings;
		}
		
		public static SimpleData variant(EntryDataType type, Class<?> variant) { return new SimpleData(type, variant, null); }
		private static SimpleData of(EntryDataType type) { return new SimpleData(type, null, null); }
		
		@Override
		public StructureType getDataType() {return StructureType.SIMPLE; }
		@Override
		public IEntrySettings getSettings() { return settings; }
		public SimpleData asSimple() { return this; }
		public EntryDataType getType() { return dataType; }
		public Class<?> getVariant() { return variant; }
		public boolean isVariant() { return variant != null; }
		public SimpleData addSettings(IEntrySettings settings) { return new SimpleData(dataType, variant, IEntrySettings.merge(this.settings, settings)); }
		public SimpleData withSettings(IEntrySettings settings) { return new SimpleData(dataType, variant, settings); }
		
		@Override
		public void appendFormat(StringBuilder builder, boolean start) {
			builder.append(Helpers.firstLetterUppercase(dataType.name().toLowerCase()));
		}
		
		@Override
		public String generateDefaultValue(Function<SimpleData, String> defaultFactory) {
			return defaultFactory.apply(this);
		}
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
		
		final SimpleData simple;
		
		private EntryDataType() { this(false); }
		private EntryDataType(boolean custom) {
			simple = custom ? null : SimpleData.of(this);
		}
		
		public SimpleData toSimpleType() { return simple; }
		public SimpleData toConfiguredType(IEntrySettings settings) { return settings == null ? simple : simple.withSettings(settings); }
	}
}
