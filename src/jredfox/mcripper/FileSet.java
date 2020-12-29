package jredfox.mcripper;

import java.io.File;
import java.util.HashSet;

/**
 * create a set of files and ensure no duplicates by converting to absolute files before adding, contains, removing
 */
public class FileSet extends HashSet<File>{
	
	public FileSet()
	{
		super();
	}
	
	public FileSet(int capacity)
	{
		super(capacity);
	}
	
	@Override
	public boolean add(File f)
	{
		if(f == null)
			return false;
		return super.add(f.getAbsoluteFile());
	}
	
	@Override
	public boolean remove(Object f)
	{
		if(f == null)
			return false;
		return super.remove(((File)f).getAbsoluteFile());
	}
	
	@Override
	public boolean contains(Object f)
	{
		if(!(f instanceof File))
			return false;
		return super.contains(((File)f).getAbsoluteFile());
	}

}
