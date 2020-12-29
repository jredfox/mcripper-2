package jredfox.mcripper.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.jml.evilnotch.lib.JavaUtil;
import com.jml.evilnotch.lib.json.JSONObject;
import com.jml.evilnotch.lib.json.serialize.JSONSerializer;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.selfcmd.util.OSUtil;

/**
 * a ripp off of my own utils because I am a noob and don't have a commons jar yet
 */
public class RippedUtils {
	
	public static Document parseXML(File xmlFile) throws SAXException, IOException, ParserConfigurationException
	{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
	    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    Document doc = dBuilder.parse(xmlFile);
	    doc.getDocumentElement().normalize();
	    return doc;
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
	
	public static boolean isValidMD5(String s) {
	    return s.matches("^[a-fA-F0-9]{32}$");
	}
	
	public static boolean isValidSHA1(String s) {
	    return s.matches("^[a-fA-F0-9]{40}$");
	}
	
	public static boolean isValidSHA256(String s) {
	    return s.matches("^[a-fA-F0-9]{64}$");
	}
	
	public static String getUnsafeHash(File f) 
	{
		String hash = null;
		String name = DeDuperUtil.getTrueName(f);
		if(name.contains("-"))
		{
			String[] splited = DeDuperUtil.split(name, '-', '?', '?');
			String possibleHash = splited[splited.length - 1];
			hash = isValidSHA1(possibleHash) ? possibleHash.toLowerCase() : getSHA1(f);
		}
		else
		{
			hash = getSHA1(f);
		}
		return hash;
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
	
	/**
	 * no directory support use at your own risk
	 */
	public static String getExtension(char c, String name) 
	{
		int index = name.lastIndexOf(c);
		return index != -1 ? name.substring(index + 1) : "";
	}
	
	/**
	 * get the default minecraft folder supports all os's
	 */
	public static File getMinecraftDir()
	{
		return new File(OSUtil.getAppData(), OSUtil.isMac() ? "minecraft" : ".minecraft");
	}
	
	public static URL toURL(File file) throws MalformedURLException
	{
		return file.toURI().toURL();
	}
	
	/**
	 * returns if the k,v was added into the hash map. adapted from HashSet impl
	 */
	public static <K, V> boolean add(Map<K,V> map, K k, V v)
	{
		return map.put(k, v) == null;
	}
	
	public static String getLastSplit(String str, String sep) 
	{
		String[] arr = str.split(sep);
		return arr[arr.length - 1];
	}

}
