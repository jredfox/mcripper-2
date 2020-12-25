package jredfox.mcripper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.digest.DigestUtils;

import com.jml.evilnotch.lib.JavaUtil;
import com.jml.evilnotch.lib.json.JSONObject;
import com.jml.evilnotch.lib.json.serialize.JSONSerializer;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;

/**
 * a ripp off of my own utils because I am a noob and don't have a commons jar yet
 */
public class RippedUtils {
	
	/**
	 * returns if the k,v was added into the hash map. adapted from HashSet impl
	 */
	public static <K, V> boolean add(Map<K,V> map, K k, V v)
	{
		return map.put(k, v) == null;
	}
	
	public static Map<String, String> parseHashFile(BufferedReader reader) 
	{
		String path = McRipper.root.getPath();
		Map<String, String> list = null;
		try
		{
			list = new HashMap<>();
			String s = reader.readLine();
			
			if(s != null)
			{
				parse(list, path, s);
			}
			
			while(s != null)
			{
				s = reader.readLine();
				if(s != null)
				{
					parse(list, path, s);
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
	
	private static void parse(Map<String, String> list, String root, String s) 
	{
		String[] arr = s.split(",");
		String fname = arr[1].trim();
		if(!new File(root, fname).exists())
		{
			System.out.println("deleting hash:" + s);
			return;
		}
		if(!s.isEmpty())
			list.put(arr[0].trim(), fname);
	}

	public static void saveFileLines(Map<String, String> map, File f, boolean utf8)
	{
		IOUtils.makeParentDirs(f);
		BufferedWriter writer = null;
		try
		{
			writer = utf8 ? new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8 ) ) : new BufferedWriter(new FileWriter(f));
			Iterator<Entry<String, String>> it = map.entrySet().iterator();
			while(it.hasNext())
			{
				Map.Entry<String, String> pair = it.next();
				writer.write(pair.getKey() + "," + pair.getValue() + System.lineSeparator());
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
	
	public static Map<String, String> getHashes(File dir)
	{
		if(!dir.exists())
			return new HashMap<>(0);
		List<File> files = DeDuperUtil.getDirFiles(dir);
		Map<String, String> hashes = new HashMap<>(files.size());
		for(File f : files)
			hashes.put(getSHA1(f), DeDuperUtil.getRealtivePath(McRipper.root, f));
		return hashes;
	}
	
	public static String getSHA1(File f) 
	{
		try
		{
			InputStream input = new FileInputStream(f);
			String hash = DigestUtils.sha1Hex(input);
			input.close();
			return hash;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
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
