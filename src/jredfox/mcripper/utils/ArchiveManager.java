package jredfox.mcripper.utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.mcripper.printer.ArchivePrinter;
import jredfox.mcripper.printer.Learner;
import jredfox.mcripper.printer.MapPrinter;

public class ArchiveManager implements Closeable {
	
	public File tmp;
	public File root;//the root folder of the archive
	public File dir;//the archive directory
	public File lroot;
	public File cachedDir;
	public ArchivePrinter printer;
	public MapPrinter badHashes;
	public Map<String, Learner> learners = new HashMap<>(6);
	public Map<String, String> localCache = new HashMap<>(500);//map of file > sha1 for checking out of sync files from the index
	
	public ArchiveManager(File tmp, File root, File cached, String archivePath, int hInitCapacity, int bInitCapacity) throws IOException
	{
		this.tmp = tmp != null ? tmp.getAbsoluteFile() : this.getSimpleFile("tmp");
		this.root = root.getAbsoluteFile();
		this.dir = this.getSimpleFile(archivePath);
		this.lroot = this.getSimpleFile("learned");
		this.cachedDir = cached;
		this.printer = new ArchivePrinter(this, this.getSimpleFile("index.hash"), hInitCapacity);
		this.badHashes = new MapPrinter(this.getSimpleFile("badhashes.hash"), bInitCapacity);
	}
	
	public boolean contains(String hash)
	{
		return this.printer.map.containsKey(hash);
	}
	
	public String getSimplePath(File output)
	{
		return DeDuperUtil.getRealtivePath(this.root, output.getAbsoluteFile());
	}
	
	public File getSimpleFile(String path)
	{
		return new File(this.root, path);
	}
	
	public File getFileFromHash(String hash)
	{
		return this.printer.getFileFromHash(hash);
	}
	
	public Learner getLearner(String index, String indexHash) 
	{
		Learner learner = this.learners.get(index);
		if(learner == null)
		{
			try
			{
				learner = new Learner(this.lroot, index, indexHash);
				learner.parse();
				this.learners.put(index, learner);
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return learner;
	}
	
	public void clearLearners()
	{
		this.closeLearners();
		this.learners.clear();
		IOUtils.deleteDirectory(this.lroot);//delete any machine learned data
		
		//clear the bad hashes to as it's technically a learned event
		IOUtils.close(this.badHashes);
		this.badHashes.map.clear();
		this.badHashes.log.delete();
	}
	
	public void computeHashes() throws IOException 
	{
		this.printer.computeHashes();
	}

	@Override
	public void close() throws IOException
	{
		IOUtils.close(this.printer);
		this.closeLearners();
	}
	
	public void closeLearners() 
	{
		for(Learner l : this.learners.values())
			IOUtils.close(l);
	}

	public void load() throws IOException
	{
		this.badHashes.load();
		this.printer.load();
	}

	/**
	 * speed up dlSingleton integrity checker for searching out of sync files from the hash index
	 */
	public String getLocalCacheHash(File saveAs) 
	{
		String path = saveAs.getPath();
		if(!this.localCache.containsKey(path))
			this.localCache.put(path, RippedUtils.getSHA1(saveAs));
		return this.localCache.get(path);
	}

}
