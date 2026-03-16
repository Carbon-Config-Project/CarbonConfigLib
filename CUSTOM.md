## How to expand the Library

If you want to expand the library with your own functionality.    
You will find here a few things that you need to know.    

### How to add Custom Entries

There are 3 types of Entries:    
- Basic (BasicConfigEntry)     
	Which is for Single Parsed Values of Any Kind
- Arrays (ArrayConfigEntry)    
	Which is for Config Entries that will result in an Array
- Collections (CollectionConfigEntry)     
	Which is for Config Entries that will result in a Collection.

These 3 types define the whole library.   
And you pick them based on the type you are trying to develop.    

When you extend your type the following functions will have to be implemented:    
- **copy**:    
	This function handles if the configentry is cloned. All Required values to make the entry operate have to be cloned.     
- **set**:    
	Handles the setting of the value. Used by the parser and code setting. So you have to sanitize it.     
- **canSet**:    
	Tests if the value provided fits within the spec you want it to be present. (for example if you only provide a certain range, for ints for example)     
- **getLimitations**:    
	(Optional) Defines limitations to the user. Stuff like a range is needed. Multiline text is supported.     
- **getPrefix**:    
	Used for Lazy Loading but is still required. Use a dedicated character for your type.     
- **getDataType**:    
	Mainly used for the Gui allowing to detect what data is dealt with.    
	For simple Types use: EntryDataType.     
	For collections use: ListBuilder.    
	For Complex Objects use: CompoundBuilder   
- **parseValue**:    
	Your String -> Data function. It only does basic Data sanitization.      
	It only ensures that the Parsed Data makes logical sense. For example if you insert a double string and try to parse an Integer it will fail.    
	But if the Integer is negative and you only allow positive values that will not be caught here. canSet and set are used for that level of sanitization.    
- **serializedValue**:   
	Uses by Default: Object#toString. But this function is handling serializing the stored value itself. No sanitization needed as all setting functions ensure sanitization already.    
	For Arrays and Collections you should also use the provided: serializeArray function. Which requires a String array. It handles everything you might need.    
- **serialize**:    
	Mainly for network sync. Writes the data into a buffer.    
	No string serialization needed.     
	Note for people who are new to networking: First in First out (FIFO) rules apply on the deserializeValue    
- **deserializeValue(IReadBuffer)**:    
	Mainly for network sync. Reads the data from a buffer.
	No string deserialization needed.     
	Note for people who are new to networking: First in First out (FIFO) rules apply on the serialize    

Once you created your own ConfigEntry you can simlpy add it into a ConfigSection using the add function.    
This supports almost everything the config library has to offer.   

The lazy loading feature needs one more step.    
Which can be approached in 2 ways.    
- ConfigHandler#**addParser**(Prefix, IConfigParser):    
	This function basically adds a basic parser over the config entries for lazy loading.    
	That way you can ensure the Objects already exist when the config is created.    
- ConfigHandler#**addTempParser**(Prefix):    
	This one is a bit more simple. It simple adds a "TempString" object into the config. Which just caches the value.    
	When you insert your ConfigEntry it will detect the "TempString" and serialize it as you register your ConfigEntry.     
	This is mainly done as some ConfigEntries can't be parsed immediately and have other dependencies.

After either of these are implemented you can use them instantly.    

### Custom Entry Example    

Here is a example of how to implement "Long" support.   
It is only one implementation and it uses the Object#toString for serialization.    
So it misses that. But its a perfect example Template.    


<details>
<summary>LongValue</summary>
<p>

```java

	public static class LongValue extends BasicConfigEntry<Long> {
		private long min = Long.MIN_VALUE;
		private long max = Long.MAX_VALUE;
		
		public LongValue(String key, Long defaultValue, String... comment) {
			super(key, defaultValue, comment);
		}
		
		public LongValue(String key, Long defaultValue) {
			super(key, defaultValue);
		}
		
		public LongValue setMin(long min) {
			this.min = min;
			return this;
		}
		
		public LongValue setMax(long max) {
			this.max = max;
			return this;
		}
		
		public LongValue setRange(long min, long max) {
			this.min = min;
			this.max = max;
			return this;
		}
		
		public long getMin() {
			return min; 
		}
		
		public long getMax() {
			return max;
		}
		
		@Override
		protected LongValue copy() {
			return new LongValue(getKey(), getDefault(), getComment()).setRange(min, max);
		}
		
		@Override
		public LongValue set(Long value) {
			super.set(Helpers.clamp(value, min, max));
			return this;
		}
		
		@Override
		public ParseResult<Boolean> canSet(Long value) {
			ParseResult<Boolean> result = super.canSet(value);
			if(result.hasError()) return result;
			return ParseResult.result(value >= min && value <= max, IllegalArgumentException::new, "Value ["+value+"] has to be within ["+min+" ~ "+max+"]");
		}
		
		@Override
		public String getLimitations() {
			if(min == Long.MIN_VALUE) {
				if(max == Long.MAX_VALUE) return "";
				return "Range: < "+max;
			}
			if(max == Long.MAX_VALUE) {
				return "Range: > "+min;
			}
			return "Range: "+min+" ~ "+max;
		}
		
		@Override
		public char getPrefix() {
			return 'L';
		}
		
		@Override
		public SimpleData getDataType() {
			return EntryDataType.LONG.withRange(new LongRange(min, max));
		}
		
		public long get() {
			return getValue().longValue();
		}
		
		@Override
		public ParseResult<Long> parseValue(String value) {
			return Helpers.parseLong(value);
		}
		
		public static ParseResult<LongValue> parse(String key, String value, String... comment) {
			ParseResult<Long> result = Helpers.parseLong(value);
			if (result.hasError()) return result.withDefault(new LongValue(key, 0L, comment));
			return ParseResult.success(new LongValue(key, result.getValue(), comment));
		}

		@Override
		public void serialize(IWriteBuffer buffer) {
			buffer.writeLong(get());
		}

		@Override
		public void deserializeValue(IReadBuffer buffer) {
			set(buffer.readLong());
		}
	}
```

</p>
</details>