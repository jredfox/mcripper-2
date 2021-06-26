package jredfox.mcripper.printer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;

public class Learner implements Closeable{
	
	public File dir;//the root reference file cached
	public SetPrinter bad;
	public MapPrinter learner;
	public String index;//the index id as a string
	public File sha1File;//the sha1 file
	public String sha1;//the last known sha1 of the index
	
	/**
	 * the registry for all the learners
	 */
	public static Map<String, Learner> learners = new HashMap<>(3);
	
	public Learner(File lRoot, String index, String indexHash) throws IOException
	{
		this(lRoot, index, indexHash, true);
	}
	
	public Learner(File lRoot, String index, String indexHash, boolean register) throws IOException
	{
		this.dir = new File(lRoot, index);
		this.index = index;
		this.sha1 = indexHash;
		this.sha1File = new File(this.dir, "checksum.sha1");
		this.bad =  new SetPrinter(new File(this.dir, "bad.paths"), 300);
		this.learner = new MapPrinter(new File(this.dir, "learned.rhash"), 600);
		if(register)
			Learner.register(index, this);
	}
	
	public void parse() throws IOException
	{
		if(this.sha1File.exists())
		{
			String cachedSha1 = IOUtils.getFileLines(IOUtils.getReader(this.sha1File)).get(0).trim();
			if(!this.sha1.equals(cachedSha1))
			{
				System.out.println("resetting machine learning index mismatch expected:" + this.sha1 + " actual:" + cachedSha1);
				this.bad.log.delete();
				this.learner.log.delete();
				this.saveSha1();
			}
		}
		else
			this.saveSha1();
		
		this.bad.load();
		this.learner.load();
	}
	
	public void save()
	{
		this.bad.save();
		this.learner.save();
		this.saveSha1();
	}
	
	public void saveSha1() 
	{
		IOUtils.saveFileLines(DeDuperUtil.asList(new String[]{this.sha1}), this.sha1File, false);
	}

	public static void register(String index, Learner l)
	{
		learners.put(index, l);
	}

	@Override
	public void close() throws IOException 
	{
		IOUtils.close(this.bad);
		IOUtils.close(this.learner);
	}

}
