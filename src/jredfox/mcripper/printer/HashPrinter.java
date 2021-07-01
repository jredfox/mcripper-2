package jredfox.mcripper.printer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.mcripper.utils.RippedUtils;

public class HashPrinter extends Printer {

	public volatile Map<String, String> hashes;
	public File archiveRoot;
	public File tmp;
	public File archiveDir;
	public File learned;
	
	public HashPrinter(File root, File tmp, String archive, File log, int capacity) throws IOException 
	{
		super(log);
		this.archiveRoot = root;
		this.tmp = tmp != null ? tmp : new File(root, "tmp");
		this.archiveDir = new File(root, archive);
		this.learned = new File(root, "learned");
		this.hashes = new LinkedHashMap<>(capacity);
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
		if(!this.getSimpleFile(fname).exists())
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
	
	//don't re-parse them as parsing verifies file's location
	public void append(String hash, File out)
	{
		String path = this.getSimplePath(out);
		this.hashes.put(hash, path);
		this.println(hash + "," + path);
	}

	public void computeHashes() throws IOException 
	{
		this.setPrintWriter();
		long ms = System.currentTimeMillis();
		System.out.println("computing hashes this will take a while. Unless it's your first launch");
		List<File> files = DeDuperUtil.getDirFiles(this.archiveDir);
		for(File f : files)
		{
			String hash = RippedUtils.getSHA1(f);
			this.append(hash, f);
		}
		System.out.println("finished computing & saving hashes in:" + ((System.currentTimeMillis() - ms) / 1000D) + " seconds");
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
	
	public String getSimplePath(File output)
	{
		return DeDuperUtil.getRealtivePath(this.root, output.getAbsoluteFile());
	}

	public File getFileFromHash(String hash)
	{
		String path = this.hashes.get(hash);
		return path == null ? null : this.getSimpleFile(path);
	}
	
	public File getSimpleFile(String path)
	{
		return new File(this.root, path);
	}
	
	public Learner getLearner(String index, String indexHash) 
	{
		Learner learner = Learner.learners.get(index);
		if(learner == null)
		{
			try
			{
				learner = new Learner(this.learned, index, indexHash);
				learner.parse();
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return learner;
	}
	
}
