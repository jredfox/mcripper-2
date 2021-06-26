package jredfox.mcripper.printer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import jredfox.filededuper.config.csv.CSV;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;

public class CSVPrinter extends Printer{

	public CSV csv;
	public CSVPrinter(File root, File log, int capacity) throws IOException 
	{
		super(root, log);
		this.csv = new CSV(this.log, capacity);
	}

	@Override
	public void parse(String line) 
	{
		this.csv.add(line);
	}

	@Override
	public void save()
	{
		this.save(null);
		this.dirty = false;
	}
	
	@Override
	public void save(BufferedWriter writer) 
	{
		this.csv.save();
		IOUtils.close(writer);
	}

	@Override
	public boolean contains(String key) 
	{
		return this.csv.getLine(key, 0) != null;
	}
	
	public String get(String key, int section)
	{
		String[] line = this.csv.getLine(key, 0);
		return line == null ? null : line[section];
	}

	public void append(Object... objs) 
	{
		String line = DeDuperUtil.toString(objs, ",");
		this.csv.add(line);
		this.println(line);
	}

}
