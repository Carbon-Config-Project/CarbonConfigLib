package carbonconfiglib.api;

import java.nio.file.Path;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectLists;

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
}
