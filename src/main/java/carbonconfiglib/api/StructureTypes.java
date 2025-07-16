package carbonconfiglib.api;

import java.util.function.Function;

import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.ParsedCollections.ParsedMap;
import carbonconfiglib.utils.structure.IStructuredData.EntryDataType;
import carbonconfiglib.utils.structure.IStructuredData.SimpleData;
import carbonconfiglib.utils.structure.StructureCompound.CompoundBuilder;
import carbonconfiglib.utils.structure.StructureCompound.CompoundData;
import carbonconfiglib.utils.structure.StructureList.ListBuilder;
import carbonconfiglib.utils.structure.StructureList.ListData;

public class StructureTypes {

    //Compounds
    public static CompoundBuilder compound() { return new CompoundBuilder(); }
    
    //Lists
    public static ListBuilder listSimple(EntryDataType type) { return ListBuilder.of(type); }
    public static <T extends Enum<T>> ListBuilder listEnum(Class<T> type) { return ListBuilder.enums(type); }
    public static <T> ListBuilder listVariant(EntryDataType displayType, Class<T> type, Function<String, ParseResult<T>> parse, Function<T, String> serialize) { return ListBuilder.variants(displayType, type, parse, serialize); }
    public static ListBuilder listCopy(ListData data) { return ListBuilder.list(data); }
    public static <T> ListBuilder listCompound(CompoundData data, Function<ParsedMap, ParseResult<T>> parse, Function<T, ParsedMap> serialize) { return ListBuilder.object(data, parse, serialize); }
    
    //Simple Types
    public static SimpleData simple(EntryDataType type) { return type.toSimpleType(); }
    public static SimpleData simpleVariant(EntryDataType displayType, Class<?> type) { return SimpleData.variant(displayType, type); }
    
    

}
