package jredfox.mcripper.printer;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.mcripper.utils.ArchiveManager;
import jredfox.mcripper.utils.RippedUtils;

public class ArchivePrinter extends MapPrinter{

	public ArchiveManager am;
	
	public ArchivePrinter(ArchiveManager am, File log, int capacity) throws IOException
	{
		super(log, capacity);
		this.am = am;
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
		String hash = arr[0].trim().toLowerCase();
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
		return this.am.getSimplePath(output);
	}
	
	public File getSimpleFile(String path)
	{
		return this.am.getSimpleFile(path);
	}
	
	public File getFileFromHash(String hash)
	{
		String path = this.map.get(hash);
		return path == null ? null : this.getSimpleFile(path);
	}
	
	public void computeHashes() throws IOException 
	{
		this.map.clear();
		this.setPrintWriter();
		long ms = System.currentTimeMillis();
		System.out.println("computing archive hashes this will take a while. Unless it's your first launch");
		List<File> files = DeDuperUtil.getDirFiles(this.am.dir);
		for(File f : files)
		{
			String unsafe = RippedUtils.getUnsafeHash(f);
			String hash =  unsafe != null ? unsafe : RippedUtils.getSHA1(f);//speed up verify hashes dramatically and use verify to determine if it's modified
			if(this.contains(hash))
			{
				System.err.println("skipping duplicate file entry:" + hash + " " + this.getSimplePath(f));
				continue;
			}
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
		Set<String> se = new HashSet<>(this.map.size());
		while(it.hasNext())
		{
			Map.Entry<String, String> p = it.next();
			String h = p.getKey();
			String path = p.getValue();
			if(!se.add(path))
			{
				System.err.println("duplicate index file entry dedected:" + path + " try recomputing your hashes");
				continue;//skip duplicate file entries
			}
			File f = this.getSimpleFile(path);
			String actualHash = RippedUtils.getSHA1(f);
			String unsafeHash = RippedUtils.getUnsafeHash(f);
			boolean modified = !h.equals(actualHash);
			boolean hmodified = unsafeHash != null && !actualHash.equals(unsafeHash);

			if(modified || hmodified)
			{
				System.err.println( (hmodified ? "File hashed form doesn't match actual hash" : "File has been modified") + (delete ? " removing" : "") + ":" + path);
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
			this.save();
			this.setPrintWriter();
		}
		
		//update files out of sync with the ArchivePrinter
		List<File> dirFiles = DeDuperUtil.getDirFiles(this.am.dir);
		for(File f : dirFiles)
		{
			String path = this.getSimplePath(f);
			if(!this.map.containsValue(path))
			{
				String hash = RippedUtils.getSHA1(f);
				if(this.contains(hash))
				{
					System.err.println("duplicate file found" + (delete ? " removing" : "") + ":" + path + " with hash:" + hash);
					if(delete)
						f.delete();
				}
				else
				{
					System.err.println("File missing from index adding:" + path + " with hash:" + hash);
					this.append(hash, f);
				}
				hasErr = true;
			}
		}
		
		if(hasErr)
			System.err.println("Files have been verified WITH ERRORS");
		else
			System.out.println("Files have been verified with NO Errors");
	}

}
