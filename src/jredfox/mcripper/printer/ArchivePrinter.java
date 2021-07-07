package jredfox.mcripper.printer;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.mcripper.utils.McChecker;
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
	
	/**
	 * verify the integrity of the ArchivePrinter
	 */
	public void verify(boolean delete) throws IOException
	{
		boolean hasErr = false;
		boolean shouldSave = false;
		
		Iterator<Map.Entry<String, String>> it = this.map.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry<String, String> p = it.next();
			String h = p.getKey();
			String path = p.getValue();
			File f = this.getSimpleFile(path);
			if(!h.equals(RippedUtils.getSHA1(f)))
			{
				System.err.println("file has been modified" + (delete ? " removing" : "") + ":" + path);
				hasErr = true;
				if(delete)
				{
					it.remove();
					f.delete();
					shouldSave = true;
				}
			}
		}
		if(shouldSave)
		{
			IOUtils.close(McChecker.am.printer);
			this.save();
			this.setPrintWriter();
		}
		else if(hasErr)
			System.err.println("Files have been verified WITH ERRORS");
		else
			System.out.println("Files have been verified with NO Errors");
	}

}
