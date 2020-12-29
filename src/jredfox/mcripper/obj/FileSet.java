package jredfox.mcripper.obj;

import java.io.File;
import java.util.HashSet;

/**
 * create a set of files and ensure no null and no duplicates by converting to absolute files before adding, contains, removing
 */
public class FileSet extends HashSet<File>{
	
	private static final long serialVersionUID = -350432434229912L;

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
