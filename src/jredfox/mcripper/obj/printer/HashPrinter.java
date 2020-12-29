package jredfox.mcripper.obj.printer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.mcripper.McRipper;
import jredfox.mcripper.utils.RippedUtils;

public class HashPrinter extends Printer {

	public Map<String, String> hashes;
	
	public HashPrinter(File root, File log, int capacity) throws IOException 
	{
		super(root, log);
		this.hashes = new LinkedHashMap<>(capacity);
	}

	@Override
	protected void parse(String line) 
	{
		String[] arr = line.split(",");
		String hash = arr[0].trim();
		String fname = arr[1].trim();
		if(!new File(fname).exists())
		{
			System.out.println("deleting hash:" + hash + "," + fname);
			this.dirty = true;
			return;
		}
		this.hashes.put(hash, fname);
	}
	
	public void append(String hash, File out)
	{
		String path = DeDuperUtil.getRealtivePath(this.root, out);
		this.hashes.put(hash, path);
		this.println(hash + "," + path);
	}
	
	@Override
	public void load() throws IOException
	{
		if(!this.log.exists())
		{
			this.computeHashes();
		}
		else
			super.load();
	}

	public void computeHashes() 
	{
		List<File> files = DeDuperUtil.getDirFiles(this.root);
		Map<String, String> hashes = new LinkedHashMap<String, String>(files.size());
		for(File f : files)
		{
			String hash = RippedUtils.getUnsafeHash(f);
			hashes.put(hash, DeDuperUtil.getRealtivePath(this.root, f));
		}
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
