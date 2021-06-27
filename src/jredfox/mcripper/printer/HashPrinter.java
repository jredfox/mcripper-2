package jredfox.mcripper.printer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.mcripper.utils.McChecker;
import jredfox.mcripper.utils.RippedUtils;

public class HashPrinter extends Printer {

	public volatile Map<String, String> hashes;
	
	public HashPrinter(File log, int capacity) throws IOException 
	{
		super(log);
		this.hashes = new LinkedHashMap<>(capacity);
	}
	
	@Override
	public void load() throws IOException
	{
		if(!this.log.exists())
		{
			this.computeHashes();
			this.setPrintWriter();
		}
		else
			super.load();
	}

	@Override
	public void parse(String line) 
	{
		String[] arr = line.split(",");
		String hash = arr[0].trim();
		String fname = arr[1].trim();
		if(!RippedUtils.getSimpleFile(fname).exists())
		{
			System.out.println("deleting hash:" + hash + "," + fname);
			this.dirty = true;
			return;
		}
		this.hashes.put(hash, fname);
	}
	
	@Override
	public boolean contains(String key)
	{
		return this.hashes.containsKey(key);
	}
	
	public void append(String hash, File out)
	{
		String path = RippedUtils.getSimplePath(out);
		this.hashes.put(hash, path);
		this.println(hash + "," + path);
	}

	public void computeHashes() 
	{
		long ms = System.currentTimeMillis();
		System.out.println("computing hashes this will take a while. Unless it's your first launch");
		List<File> files = DeDuperUtil.getDirFiles(McChecker.mcripped);
		for(File f : files)
		{
			String hash = RippedUtils.getSHA1(f);
			this.hashes.put(hash, DeDuperUtil.getRealtivePath(this.root, f));
		}
		this.save();
		System.out.println("finished computing & saving hashes in:" + (System.currentTimeMillis() - ms) / 1000D + " seconds");
	}
	
	@Override
	public void save(BufferedWriter writer) 
	{
		try
		{
			for(Map.Entry<String, String> entry : this.hashes.entrySet())
			{
				writer.write(entry.getKey() + "," + entry.getValue() + System.lineSeparator());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			IOUtils.close(writer);
		}
	}
}
