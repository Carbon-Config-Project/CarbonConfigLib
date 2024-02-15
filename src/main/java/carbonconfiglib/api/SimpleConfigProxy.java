package carbonconfiglib.api;

import java.nio.file.Path;
import java.util.List;

import carbonconfiglib.utils.Helpers;
import speiger.src.collections.objects.utils.ObjectLists;

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
public class SimpleConfigProxy implements IConfigProxy
{
	Path path;
	
	public SimpleConfigProxy(Path path) {
		this.path = path;
	}
	
	@Override
	public boolean isDynamicProxy() {
		return false;
	}
	
	@Override
	public Path getBasePaths(Path relativeFile) {
		return path;
	}
	
	@Override
	public List<SimpleTarget> getPotentialConfigs() {
		return ObjectLists.singleton(new SimpleTarget(path, Helpers.firstLetterUppercase(path.getFileName().toString())));
	}
	
	public static class SimpleTarget implements IPotentialTarget {
		Path folder;
		String name;
		
		public SimpleTarget(Path folder, String name) {
			this.folder = folder;
			this.name = name;
		}
		
		@Override
		public Path getFolder() {
			return folder;
		}
		
		@Override
		public String getName() {
			return name;
		}
	}
}
