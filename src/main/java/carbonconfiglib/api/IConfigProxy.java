package carbonconfiglib.api;

import java.nio.file.Path;
import java.util.List;

public interface IConfigProxy
{
	public List<Path> getBasePaths();
	public boolean isDynamicProxy();
}
