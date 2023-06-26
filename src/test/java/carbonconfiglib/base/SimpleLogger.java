package carbonconfiglib.base;

import carbonconfiglib.api.ILogger;

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
public class SimpleLogger implements ILogger {

	
	@Override
	public void debug(String s) {
		System.out.println("[Debug] "+s);
	}

	@Override
	public void debug(String s, Object o) {
		System.out.println("[Debug] "+String.format(s, o));
	}

	@Override
	public void debug(Object o) {
		System.out.println("[Debug] "+o);
	}
	
	@Override
	public void info(String s) {
		System.out.println("[Info] "+s);
	}

	@Override
	public void info(String s, Object o) {
		System.out.println("[Info] "+String.format(s, o));
	}

	@Override
	public void info(Object o) {
		System.out.println("[Info] "+o);
	}
	
	@Override
	public void warn(String s) {
		System.out.println("[Warn] "+s);
	}

	@Override
	public void warn(String s, Object o) {
		System.out.println("[Warn] "+String.format(s, o));
	}

	@Override
	public void warn(Object o) {
		System.out.println("[Warn] "+o);
	}
	
	@Override
	public void error(String s) {
		System.out.println("[Error] "+s);
	}
	
	@Override
	public void error(String s, Object o) {
		System.out.println("[Error] "+String.format(s, o));
	}

	@Override
	public void error(Object o) {
		System.out.println("[Error] "+o);
	}
}
