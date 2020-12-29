package jredfox.mcripper.obj.printer;

import java.io.BufferedWriter;
import java.io.File;

import jredfox.filededuper.config.csv.CSV;

public class CSVPrinter extends Printer{

	public CSV csv;
	public CSVPrinter(File root, File log, int capacity) 
	{
		super(root, log);
		this.csv = new CSV(this.log, capacity);
	}

	@Override
	protected void parse(String line) 
	{
		this.csv.add(line);
	}

	@Override
	public void save(BufferedWriter writer) 
	{
		this.csv.save();
	}

}
