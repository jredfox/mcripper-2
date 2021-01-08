package jredfox.mcripper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class DebugCode {
	
	public static void main(String[] args) throws MalformedURLException, IOException
	{
		URLConnection url = new URL("https://www.google.com").openConnection();
		System.out.println(url.getReadTimeout());
//		System.out.println(OffsetDateTime.now().toInstant() + "," + OffsetDateTime.now().toInstant().toEpochMilli());
//		OffsetDateTime offset = OffsetDateTime.parse("2011-04-19T13:03:05.000Z");
//		System.out.println(offset.toInstant().toEpochMilli());
//		Instant instant = Instant.parse("2012-08-01T12:56:33.000Z");
//		System.out.println(instant.toEpochMilli());
		
//		File in = new File("C:/Users/jredf/Desktop/cracked");
//        List<File> files = DeDuperUtil.getDirFiles(in, "java");
//        for(File f : files)
//        {
//            List<String> lines = IOUtils.getFileLines(f);
//            for(String s : lines)
//            {
//                if(s.contains("http") || s.contains("www"))
//                {
//                    System.out.println(s + " " + DeDuperUtil.getRealtivePath(in, f));
//                }
//            }
//        }
//        System.out.println("done");
	}

}
