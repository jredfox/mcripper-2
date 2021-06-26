package jredfox.mcripper.printer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import jredfox.filededuper.util.IOUtils;
import jredfox.mcripper.utils.RippedUtils;

public abstract class Printer implements Closeable{

	//Object vars
	public File root;
	public String rootPath;
	public boolean dirty;
	
	//what matters
	public File log;
	public PrintWriter out;
	
	public Printer(File log) throws IOException 
	{
		log = log.getAbsoluteFile();
		this.root = log.getParentFile();
		this.rootPath = root.getPath();
		this.log = log;
		this.sanityCheck();
	}
	
	public abstract void parse(String line);
	public abstract void save(BufferedWriter writer);
	public abstract boolean contains(String key);
	
	public void load() throws IOException
	{
		this.setPrintWriter();
		BufferedReader reader = IOUtils.getReader(this.log);
		try
		{
			String s = reader.readLine();	
			if(s != null)
			{
				s = s.trim();
				if(!s.isEmpty())
					this.parse(s);
			}
			while(s != null)
			{
				s = reader.readLine();
				if(s != null)
				{
					s = s.trim();
					if(!s.isEmpty())
						this.parse(s);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			IOUtils.close(reader);
		}
		try
		{
			if(this.dirty) 
				this.save();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void save()
	{
		this.save(RippedUtils.getWriter(this.log));
		this.dirty = false;
	}
	
	public void setPrintWriter() throws IOException 
	{
		this.sanityCheck();
		this.out = new PrintWriter(new BufferedWriter(new FileWriter(this.log, true)), true);
	}

	public void sanityCheck() throws IOException 
	{
		if(!this.root.exists() && !this.root.mkdirs())
		{
			throw new IOException("Log Directory cannot be found nor created for:" + this.log);
		}
	}

	public File getLog()
	{
		return this.log;
	}
	
	/**
	 * appends it in memory and prints it to the file
	 */
	public void append(String line)
	{
		this.parse(line);
		this.println(line);
	}
	
	public void println(Object obj)
	{
		this.println(String.valueOf(obj));
	}
	
	public void print(String line)
	{
		this.out.print(line);
    	this.out.flush();
	}
	
	public void println(String line)
	{
		this.out.println(line);
	}

	@Override
	public void close() throws IOException 
	{
		IOUtils.close(this.out);
	}

}
