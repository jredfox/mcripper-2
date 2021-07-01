package jredfox.mcripper.printer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.mcripper.utils.RippedUtils;

public class ArchivePrinter extends MapPrinter{

	public File root;
	public File dir;
	
	public ArchivePrinter(File root, File dir, File log, int capacity) throws IOException
	{
		super(log, capacity);
		this.root = root;
		this.dir = dir;
	}
	
	@Override
	public void load() throws IOException
	{
		if(!this.log.exists())
			this.computeHashes();
		else
			super.load();
	}

	@Override
	public void parse(String line) 
	{
		String[] arr = line.split(",");
		String hash = arr[0].trim();
		String fname = arr[1].trim();
		if(this.isLoading && !this.getSimpleFile(fname).exists())
		{
			System.out.println("deleting hash:" + hash + "," + fname);
			this.dirty = true;
			return;
		}
		this.map.put(hash, fname);
	}
	
	public void append(String hash, File out)
	{
		String path = this.getSimplePath(out);
		this.append(hash, path);
	}

	public void computeHashes() throws IOException 
	{
		this.setPrintWriter();
		long ms = System.currentTimeMillis();
		System.out.println("computing hashes this will take a while. Unless it's your first launch");
		List<File> files = DeDuperUtil.getDirFiles(this.dir);
		for(File f : files)
		{
			String hash = RippedUtils.getSHA1(f);
			this.append(hash, f);
		}
		System.out.println("finished computing & saving hashes in:" + ((System.currentTimeMillis() - ms) / 1000D) + " seconds");
	}
	
	public String getSimplePath(File output)
	{
		return DeDuperUtil.getRealtivePath(this.root, output.getAbsoluteFile());
	}
	
	public File getFileFromHash(String hash)
	{
		String path = this.map.get(hash);
		return path == null ? null : this.getSimpleFile(path);
	}
	
	public File getSimpleFile(String path)
	{
		return new File(this.root, path);
	}

}
