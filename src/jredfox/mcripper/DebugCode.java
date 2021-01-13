package jredfox.mcripper;

import java.io.File;

import jredfox.mcripper.printer.LogPrinter;

public class DebugCode {
	
	public static void main(String[] args) throws Exception
	{
		try
		{
			LogPrinter log = new LogPrinter(new File("log.txt").getAbsoluteFile(), System.out, System.err);
			System.out.println("printing to file?????");
			System.out.println("testing");
			System.err.print("a b c");
			System.err.print("d\n");
			System.err.println("err 404");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

}
