package jredfox.mcripper.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.jml.evilnotch.lib.JavaUtil;
import com.jml.evilnotch.lib.json.JSONObject;
import com.jml.evilnotch.lib.json.serialize.JSONSerializer;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.filededuper.util.JarUtil;
import jredfox.mcripper.url.URLResponse;
import jredfox.selfcmd.util.OSUtil;

/**
 * a ripp off of my own utils because I am a noob and don't have a commons jar yet
 */
public class RippedUtils {
	
	public static Document parseXML(File xmlFile)
	{
		try
		{
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setNamespaceAware(true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			return doc;
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
	
	public static boolean isValidMD5(String s) {
	    return s.matches("^[a-fA-F0-9]{32}$");
	}
	
	public static boolean isValidSHA1(String s) {
	    return s.matches("^[a-fA-F0-9]{40}$");
	}
	
	public static boolean isValidSHA256(String s) {
	    return s.matches("^[a-fA-F0-9]{64}$");
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
	 * returns null if the unsafeHash is maulformed or non existent
	 */
	public static String getUnsafeHash(File f) 
	{
		String name = DeDuperUtil.getTrueName(f);
		if(name.contains("-"))
		{
			String[] splited = DeDuperUtil.split(name, '-', '?', '?');
			String possibleHash = splited[splited.length - 1];
			return isValidSHA1(possibleHash) ? possibleHash.toLowerCase() : null;
		}
		else if(isValidSHA1(name))
			return name;//if it's a flat archive or it's literally the hash it's suppose to be
		return null;
	}
	
	public static BufferedWriter getWriter(File file)
	{
		try
		{
			return IOUtils.getWriter(file);
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
	
	/**
	 * returns null if it's maulformed
	*/
	public static URL toURL(File file)
	{
		try
		{
			return file.toURI().toURL();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
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
	
	@SuppressWarnings("unused")
	private <K,V> Map.Entry<K, V> getLast(LinkedHashMap<K, V> map) 
	{
		Map.Entry<K, V> e = null;
		for(Map.Entry<K, V> entry : map.entrySet())
		{
			e = entry;
		}
		return e;
	}
	
	public static NodeList getElementSafely(Document d, String... keys) 
	{
		for(String key : keys)
		{
			NodeList l = d.getElementsByTagName(key);
			if(isEmpty(l))
				l = d.getElementsByTagName(key.toLowerCase());
			
			if(!isEmpty(l))
				return l;
		}
		return null;
	}
	
	private static boolean isEmpty(NodeList nl)
	{
		return nl == null || nl.getLength() == 0;
	}

	public static NodeList getElementSafely(Element elment, String... keys) 
	{
		for(String key : keys)
		{
			NodeList l = elment.getElementsByTagName(key);
			if(isEmpty(l))
				l = elment.getElementsByTagName(key.toLowerCase());
			
			if(!isEmpty(l))
				return l;
		}
		return null;
	}
	
	public static String getText(Element e, String key)
	{
		return getElementSafely(e, key).item(0).getTextContent();
	}
	
	public static String getText(NodeList l)
	{
		return l.item(0).getTextContent();
	}
	
	public static InputStream getInputStreamFromJar(Class<?> clazz, String path)
	{
		return clazz.getClassLoader().getResourceAsStream(path);
	}
	
	public static Set<String> getPathsFromDir(Class<?> clazz, String path)
	{
		return getPathsFromDir(clazz, path, "*");
	}

	/**
	 * getResource Paths from a jar/IDE from a directory
	 */
	public static Set<String> getPathsFromDir(Class<?> clazz, String path, String ext)
	{
		Set<String> paths = new HashSet<>();
		try
		{
			File jarFile = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
			if(!jarFile.isDirectory())
			{
				ZipFile jar = JarUtil.getZipFile(jarFile);
				List<ZipEntry> li = JarUtil.getEntriesFromDir(jar, path, ext);
				for(ZipEntry e : li)
					paths.add(e.getName());
			}
			else
			{
				for(File f : DeDuperUtil.getDirFiles(new File(jarFile, path), ext))
				{
					paths.add(DeDuperUtil.getRealtivePath(jarFile, f));
				}
			}
			return paths;
		}
		catch(Throwable t)
		{
			return Collections.emptySet();
		}
	}
	
	public static void copy(InputStream in, OutputStream out) throws IOException
	{
		copy(in, out, true);
	}
	
	/**
	 * needed to copy input to output with closing stream safely
	 */
	public static void copy(InputStream in, OutputStream out, boolean close) throws IOException
	{
		try
		{
			int length;
   	 		while ((length = in.read(IOUtils.buffer)) > 0)
   	 		{
   	 			out.write(IOUtils.buffer, 0, length);
   	 		}
		}
		catch(Exception e)
		{
			throw e;
		}
		finally
		{
   	 		if(close)
   	 		{
   	 			IOUtils.close(in);
   	 			IOUtils.close(out);
   	 		}
		}
	}

	/**
	 * parse them zula times. example: 2010-09-18T21:35:15.000Z
	 */
	public static long parseZTime(String strTime) 
	{
		return Instant.parse(strTime).toEpochMilli();
	}
	
	/**
	 * parse the offset times. example: 2015-11-10T16:43:29-0500 and 2014-05-14T17:29:23+00:00. returns -1 if not found
	 */
	public static long parseOffsetTime(String strTime) throws DateTimeParseException
	{
		try {
			return OffsetDateTime.parse(strTime).toInstant().toEpochMilli();
		}
		catch(DateTimeParseException e){}
		
		return -1;
	}

	public static String getExtensionFull(String fname) 
	{
		String ext = DeDuperUtil.getExtension(fname);
		return ext.isEmpty() ? "" : "." + ext;
	}

	public static boolean containsNum(int code, int[] is) 
	{
		for(int i : is)
			if(code == i)
				return true;
		return false;
	}
	
	/**
	 * get a file from mcDir or dl it to the specified path if non existent
	 */
	public static URLResponse getOrDlFromMc(File mcDir, String url, String path, String hash)
	{
		return dlFromMc(mcDir, url, new File(mcDir, path), path, hash);
	}
	
	/**
	 * dl it from the cached mcDir if applicable otherwise download it from the url
	 */
	public static URLResponse dlFromMc(File mcDir, String url, File saveAs, String path, String hash)
	{
		File cached = new File(mcDir, path).getAbsoluteFile();
		cached = cached.exists() ? cached : McChecker.am.contains(hash) ? McChecker.am.getFileFromHash(hash) : cached;
		url = cached.exists() && hash.equals(RippedUtils.getSHA1(cached)) ? RippedUtils.toURL(cached).toString() : url;
		URLResponse reply = DLUtils.dlToFile(url, saveAs);
		if(reply.file != null && !url.startsWith("file:"))
			System.out.println("dl:" + reply.file.getPath().replaceAll("\\\\", "/") + " from:" + url);
		return reply;
	}

	/**
	 * copy a file. preserve date modified, and create dirs if non existent
	 */
	public static void copy(File input, File output) throws FileNotFoundException, IOException
	{
		//TODO: copy os file attributes
		output.getParentFile().mkdirs();
		RippedUtils.copy(new FileInputStream(input), new FileOutputStream(output));
		output.setLastModified(input.lastModified());
	}

	public static boolean isWeb(String protocol)
	{
		 return protocol != null && !protocol.isEmpty() && !protocol.equals("file") && !protocol.equals("jar") && !protocol.equals("ap");
	}
}
