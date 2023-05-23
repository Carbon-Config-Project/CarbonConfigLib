package carbonconfiglib.api;

import java.nio.file.Path;
import java.util.List;

import carbonconfiglib.utils.Helpers;
import speiger.src.collections.objects.utils.ObjectLists;

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
	public List<Path> getBasePaths() {
		return ObjectLists.singleton(path);
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
