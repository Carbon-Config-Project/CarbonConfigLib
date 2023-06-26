package carbonconfiglib.utils;

/**
 * Copyright 2023 Speiger, Meduris
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
public class ParseExpection {
	final String value;
	final Exception expection;
	final String message;
	final int index;
	
	public ParseExpection(String value, Exception expection, String message) {
		this(value, expection, message, -1);
	}
	
	public ParseExpection(String value, Exception expection, String message, int index) {
		this.value = value;
		this.expection = expection;
		this.message = message;
		this.index = index;
	}
	
	public String getValue() {
		return value;
	}
	
	public Exception getExpection() {
		return expection;
	}
	
	public String getMessage() {
		return message;
	}
	
	public int getIndex() {
		return index;
	}
	
	public ParseExpection withIndex(int index) {
		return new ParseExpection(value, expection, message, index);
	}
}
