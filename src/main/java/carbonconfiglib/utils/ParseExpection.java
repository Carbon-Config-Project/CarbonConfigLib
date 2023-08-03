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
	
	public ParseExpection(String value, Exception expection, String message) {
		this.value = value;
		this.expection = expection;
		this.message = message;
	}
	
	public ParseExpection appendMessage(String message) {
		return new ParseExpection(value, expection, message+"\n"+this.message);
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
}
