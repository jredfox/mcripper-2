package jredfox.mcripper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import jredfox.mcripper.utils.RippedUtils;

public class DebugCode {
	
	public static void main(String[] args) throws MalformedURLException, IOException
	{
		File file = new File("index - Copy.hash").getAbsoluteFile();
		file.createNewFile();
		File f2 = new File("test/d/b");
		f2.mkdirs();
		long ms = System.currentTimeMillis();
		RippedUtils.move(file, new File(f2, "abcd.txt"));
		System.out.println("ms:" + (System.currentTimeMillis() - ms));
	}

}
