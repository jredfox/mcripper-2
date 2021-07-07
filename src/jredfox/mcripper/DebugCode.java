package jredfox.mcripper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import jredfox.mcripper.utils.DLUtils;

public class DebugCode {
	
	public static void main(String[] args) throws MalformedURLException, IOException
	{
		DLUtils.dlToFile("https://raw.githubusercontent.com/jredfox/mcripper-2/main/README.MD", new File("test.MD"));
	}

}
