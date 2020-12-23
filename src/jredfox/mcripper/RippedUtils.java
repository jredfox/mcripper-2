package jredfox.mcripper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jml.evilnotch.lib.JavaUtil;
import com.jml.evilnotch.lib.json.JSONObject;
import com.jml.evilnotch.lib.json.serialize.JSONSerializer;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;

/**
 * a ripp off of my own utils because I am a noob and don't have a commons jar yet
 */
public class RippedUtils {
	
	
	public static Set<String> getFileLines(BufferedReader reader) 
	{
		Set<String> list = null;
		try
		{
			list = new HashSet<>();
			String s = reader.readLine();
			
			if(s != null)
			{
				list.add(s.trim());
			}
			
			while(s != null)
			{
				s = reader.readLine();
				if(s != null)
				{
					list.add(s.trim());
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(reader != null)
			{
				try 
				{
					reader.close();
				} catch (IOException e) 
				{
					System.out.println("Unable to Close InputStream this is bad");
				}
			}
		}
		return list;
	}
	
	public static void saveFileLines(Collection<String> list, File f, boolean utf8)
	{
		IOUtils.makeParentDirs(f);
		BufferedWriter writer = null;
		try
		{
			if(!utf8)
			{
				writer = new BufferedWriter(new FileWriter(f));
			}
			else
			{
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8 ) );
			}
			
			for(String s : list)
			{
				writer.write(s + System.lineSeparator());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			IOUtils.close(writer);
		}
	}
	
	public static Set<String> getHashes(File dir)
	{
		if(!dir.exists())
			return new HashSet<>(0);
		List<File> files = DeDuperUtil.getDirFiles(dir);
		Set<String> hashes = new HashSet<>(files.size());
		for(File f : files)
			hashes.add(getSHA1(f));
		return hashes;
	}
	
	public static String getSHA1(File f) 
	{
		return DeDuperUtil.getSHA1(f).toLowerCase();
	}

	public static JSONObject getJSON(File file)
	{
		if(!file.exists())
			return null;
		try
		{
			JSONSerializer parser = new JSONSerializer();
			return parser.readJSONObject(JavaUtil.getReader(file));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

}
