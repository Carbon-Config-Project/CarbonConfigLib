package carbonconfiglib.api;

import java.nio.file.Path;
import java.util.List;

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
public interface IConfigProxy
{
	public List<Path> getBasePaths();
	public List<? extends IPotentialTarget> getPotentialConfigs();
	public boolean isDynamicProxy();
	
	public static interface IPotentialTarget {
		public Path getFolder();
		public String getName();
	}
}
