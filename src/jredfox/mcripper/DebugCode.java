package jredfox.mcripper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import jredfox.common.os.OSUtil;
import jredfox.filededuper.util.DeDuperUtil;

public class DebugCode {
	
	public static void main(String[] args) throws MalformedURLException, IOException
	{
		System.out.println(DeDuperUtil.getDirFiles(new File(OSUtil.getAppData(), "Mcripper/mcripped/mojang/libraries")).toString().length());
	}

}
