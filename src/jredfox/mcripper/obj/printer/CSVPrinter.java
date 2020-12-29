package jredfox.mcripper.obj.printer;

import java.io.BufferedWriter;
import java.io.File;

import jredfox.filededuper.config.csv.CSV;
import jredfox.filededuper.util.DeDuperUtil;

public class CSVPrinter extends Printer{

	public CSV csv;
	public CSVPrinter(File root, File log, int capacity) 
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
	public void save(BufferedWriter writer) 
	{
		this.csv.save();
	}

	@Override
	public boolean contains(String key) 
	{
		return this.csv.getLine(key, 0) != null;
	}
	
	public String get(String key, int colum)
	{
		String[] line = this.csv.getLine(key, colum);
		return line == null ? null : line[colum];
	}

	public void append(Object... objs) 
	{
		this.csv.add(DeDuperUtil.toString(objs, ","));
	}

}
