package jredfox.mcripper.obj.printer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import jredfox.filededuper.util.IOUtils;

public abstract class Printer implements Closeable{

	public File root;
	public String rootPath;
	public boolean dirty;
	public File log;
	public PrintWriter out;
	
	public Printer(File root, File log) 
	{
		this.root = root;
		this.rootPath = root.getPath();
		this.log = log;
	}
	
	protected abstract void parse(String line);
	public abstract void save(BufferedWriter writer);
	
	public void load() throws IOException
	{
		if(!this.log.exists())
			return;
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
		this.out = new PrintWriter(new BufferedWriter(new FileWriter(this.log, true)), true);
	}
	
	public void save() throws FileNotFoundException, IOException
	{
		this.save(IOUtils.getWriter(this.log));
		this.dirty = false;
	}
	
	public File getLog()
	{
		return this.log;
	}
	
	public void append(String line)
	{
		this.parse(line);
		this.println(line);
	}
	
	public void println(Object obj)
	{
		this.println(String.valueOf(obj));
	}
	
	public void println(String line)
	{
		this.out.println(line);
	}

	@Override
	public void close() throws IOException 
	{
		this.out.close();
	}

}
