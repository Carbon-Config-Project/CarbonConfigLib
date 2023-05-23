package carbonconfiglib.api;

import java.nio.file.Path;
import java.util.List;

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
