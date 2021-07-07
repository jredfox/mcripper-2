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

public class ArchiveManager implements Closeable{
	
	public File tmp;
	public File root;//the root folder of the archive
	public File dir;//the archive directory
	public File lroot;
	public File cachedDir;
	public ArchivePrinter printer;
	public Map<String, Learner> learners = new HashMap<>(6);
	
	public ArchiveManager(File tmp, File root, File cached, String archivePath, int hInitCapacity) throws IOException
	{
		this.tmp = tmp != null ? tmp.getAbsoluteFile() : this.getSimpleFile("tmp");
		this.root = root.getAbsoluteFile();
		this.dir = this.getSimpleFile(archivePath);
		this.lroot = this.getSimpleFile("learned");
		this.cachedDir = cached;
		this.printer = new ArchivePrinter(this, this.getSimpleFile("index.hash"), hInitCapacity);
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
	}
	
	public void computeHashes() throws IOException 
	{
		this.printer.computeHashes();
		this.clearLearners();
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

}
