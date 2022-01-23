package jredfox.mcripper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.TreeMap;

import com.jml.evilnotch.lib.JavaUtil;
import com.jml.evilnotch.lib.json.JSONObject;

public class DebugCode {
	
	public static void main(String[] args) throws MalformedURLException, IOException
	{
		JSONObject json = JavaUtil.getJSON(new File("C:/Users/jredfox/Desktop/index.json"));
		TreeMap<Long, String> list = new TreeMap<>();
		for(String hash : json.keySet())
		{
			list.put(Long.parseLong(hash), (String)json.get(hash));
		}
		for(Map.Entry<Long, String> map : list.entrySet())
			System.out.println(map);
	}

}
