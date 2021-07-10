package jredfox.mcripper;

import java.io.IOException;
import java.net.MalformedURLException;

import jredfox.mcripper.utils.RippedUtils;

public class DebugCode {
	
	public static void main(String[] args) throws MalformedURLException, IOException
	{
		System.out.println(RippedUtils.fillString("#", 10));
	}

}
