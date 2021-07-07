package jredfox.mcripper.printer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import jredfox.filededuper.util.IOUtils;

public class MapPrinter extends Printer{

	public Map<String, String> map;
	
	public MapPrinter(File log, int capacity) throws IOException 
	{
		super(log);
		this.map = new LinkedHashMap<>(capacity);
	}

	@Override
	public void parse(String line) 
	{
		String[] arr = line.split(",");
		this.map.put(arr[0], arr[1]);
	}

	@Override
	public void save(BufferedWriter writer) 
	{
		try
		{
			for(Map.Entry<String, String> entry : this.map.entrySet())
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

	@Override
	public boolean contains(String key) 
	{
		return this.map.containsKey(key);
	}
	
	public void append(String key, String value)
	{
		this.map.put(key, value);
		this.println(key + "," + value);
	}

	public String get(String key) 
	{
		return this.map.get(key);
	}

}
