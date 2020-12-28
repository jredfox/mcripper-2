package jredfox.mcripper;

import java.io.File;
import java.util.List;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;

public class DebugCode {
	
	public static void main(String[] args)
	{
		System.out.println("starting");
		List<File> files = DeDuperUtil.getDirFiles(new File("C:/Users/jredf/Desktop/extract"), "java");
		for(File f : files)
		{
			List<String> lines = IOUtils.getFileLines(f);
			for(String s : lines)
			{
				if(s.contains("http"))
				{
					System.out.println(s + " " + DeDuperUtil.getRealtivePath(new File("C:/Users/jredf/Desktop/extract"), f));
				}
			}
		}
		System.out.println("done:" + files.size());
	}

}
