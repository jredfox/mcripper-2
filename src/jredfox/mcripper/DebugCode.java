package jredfox.mcripper;

import jredfox.mcripper.utils.RippedUtils;

public class DebugCode {
	
	public static void main(String[] args)
	{
		System.out.println(RippedUtils.getResponseCode("https://google.com/lbs"));
		System.out.println(RippedUtils.getResponseCode("https://s3.amazonaws.com/MinecraftResources/music/calm3.ogg"));
	}

}
