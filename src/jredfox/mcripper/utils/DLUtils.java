package jredfox.mcripper.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.mcripper.McRipper;
import jredfox.mcripper.obj.printer.CSVPrinter;
import jredfox.mcripper.obj.printer.HashPrinter;
import jredfox.mcripper.obj.printer.Printer;
import jredfox.mcripper.obj.printer.SetPrinter;
import jredfox.selfcmd.util.OSUtil;

public class DLUtils {
	
	public static File dl(String url, String path, String hash) throws FileNotFoundException, IOException
	{
		return dl(url, path, System.currentTimeMillis(), hash);
	}
	
	public static File dl(String url, String path, long timestamp, String hash) throws FileNotFoundException, IOException, IllegalArgumentException
	{
		return dl(McRipperChecker.hash, url, path, timestamp, hash);
	}
	
	/**
	 * download a file to the path specified. With timestamp and hashing support. 
	 * The hash is in case the file destination already exists. To allow override pass "override" as the hash
	 */
	public static File dl(HashPrinter printer, String url, String path, long timestamp, String hash) throws FileNotFoundException, IOException, IllegalArgumentException
	{
		url = url.replaceAll(" ", "%20");
		if(hash == null)
			throw new IllegalArgumentException("hash cannot be null!");
		File output = null;
		try
		{
			long time = System.currentTimeMillis();
			boolean hasHash = !hash.equals("override");
			output = OSUtil.toWinFile(new File(path.replaceAll("%20", " "))).getAbsoluteFile();
			if(hasHash)
			{
				if(printer.hashes.containsKey(hash))
					return new File(printer.root, printer.hashes.get(hash));
				else if(output.exists())
				{
					//prevent duplicate downloads
					File hfile = new File(output.getParent(), DeDuperUtil.getTrueName(output) + "-" + hash + DeDuperUtil.getExtensionFull(output));
					boolean hflag = hfile.exists();
					if(hflag || hash.equals(RippedUtils.getSHA1(output)))
					{
						output = hflag ? hfile : output;
						System.err.println("File is out of sync with " + printer.log.getName() + " skipping duplicate download:" + output);
						printer.append(hash, output);
						return output;
					}
					output = hfile;
				}
			}
			
			//speed the process up for libraries as they are extremely slow
			if(path.contains("libraries"))
			{
				File cached = new File(McRipperChecker.mcDir, DeDuperUtil.getRealtivePath(McRipperChecker.mojang, new File(path).getAbsoluteFile()));
				if(cached.exists())
					url = cached.toURI().toURL().toString();
			}
			
			URLConnection con = new URL(url).openConnection();
			con.setConnectTimeout(1000 * 15);
			con.setReadTimeout(Integer.MAX_VALUE / 2);
			InputStream inputStream = con.getInputStream();
			output.getParentFile().mkdirs();
			IOUtils.copy(inputStream, new FileOutputStream(output));
			output.setLastModified(timestamp);
			printer.append(hash, output);
			System.out.println("dl:" + output + " in:" + (System.currentTimeMillis() - time) + "ms");
			return output;
		}
		catch(IOException io)
		{
			if(output.exists())
				output.delete();
			printer.hashes.remove(hash);
			throw io;
		}
	}
	
	public static File dlToFile(String url, File output) throws FileNotFoundException, IOException
	{
		return dlToFile(url, output, System.currentTimeMillis());
	}
	
	public static File dlToFile(String url, File output, long timestamp) throws FileNotFoundException, IOException
	{
		return dlToFile(url, output, timestamp, false);
	}
	
	public static File dlToFile(String url, File output, long timestamp, boolean print) throws FileNotFoundException, IOException
	{
		url = url.replaceAll(" ", "%20");
		output = new File(output.getPath().replaceAll("%20", " "));
		try
		{
			URLConnection con = new URL(url).openConnection();
			con.setConnectTimeout(1000 * 15);
			con.setReadTimeout(Integer.MAX_VALUE / 2);
			InputStream inputStream = con.getInputStream();
			output.getParentFile().mkdirs();
			IOUtils.copy(inputStream, new FileOutputStream(output));
			output.setLastModified(timestamp);
			if(print)
				System.out.println("dl:" + output + " from:" + url);
		}
		catch(IOException io)
		{
			if(output.exists())
				output.delete();
			throw io;
		}
		return output;
	}
	
	public static File dlMove(String url, String path, File saveAs) throws FileNotFoundException, IOException
	{
		File tmpFile = dlToFile(url, new File(McRipperChecker.tmp, path));
		String hash = RippedUtils.getSHA1(tmpFile);
		File moved = dl(RippedUtils.toURL(tmpFile).toString(), saveAs.getPath(), hash);
		tmpFile.delete();
		return moved;
	}
	
	public static File dlFromMc(File mcDir, String url, String path, File saveAs, String hash) throws FileNotFoundException, IOException
	{
		File cached = new File(mcDir, path).getAbsoluteFile();
		cached = cached.exists() ? cached : new File(McRipperChecker.mojang, path);
		boolean exists = cached.exists();
		long timestamp = exists ? cached.lastModified() : System.currentTimeMillis();
		url = exists && hash.equals(RippedUtils.getSHA1(cached)) ? cached.toURI().toURL().toString() : url;
		File f = dlToFile(url, saveAs, timestamp);
		if(!url.startsWith("file:"))
			System.out.println("dl:" + f.getPath() + " from:" + url);
		return f;
	}
	
	/**
	 * get a file from mc if it doesn't exist dl it to mc directory
	 */
	public static File getOrDlFromMc(File mcDir, String url, String type, String path, String hash) throws FileNotFoundException, IOException
	{
		File cached = getFromMc(mcDir, type, path);
		boolean exists = cached.exists();
		long timestamp = exists ? cached.lastModified() : System.currentTimeMillis();
		return exists && hash.equals(RippedUtils.getSHA1(cached)) ? cached : dlToFile(url, new File(mcDir, path), timestamp, true);
	}

	public static File learnDl(String url, String path, File saveAs) 
	{
		if(McRipperChecker.bad.contains(path))
			return null;
		String cachedHash = McRipperChecker.learner.get(path, 0);
		//recall learning
		if(cachedHash != null && McRipperChecker.hash.contains(cachedHash))
		{
			return new File(McRipperChecker.root, McRipperChecker.hash.hashes.get(cachedHash));
		}
		else if(cachedHash != null)
		{
			//if file doesn't exist recall hash and direct dl it here
			try
			{
				return dl(url, saveAs.getPath(), cachedHash);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		//learn here
		try
		{
			File tmpFile = dlToFile(url, new File(McRipperChecker.tmp, path));
			String hash = RippedUtils.getSHA1(tmpFile);
			File moved = dl(RippedUtils.toURL(tmpFile).toString(), saveAs.getPath(), hash);
			tmpFile.delete();
			McRipperChecker.learner.append(path, hash, moved.lastModified());
			System.out.println("dl:" + url + " from path:" + path);
			return moved;
		}
		catch(IOException e)
		{
			String msg = e.getMessage();
			if(msg.contains("HTTP response code:"))
			{
				System.err.println(msg);
				McRipperChecker.bad.append(path);
			}
			else
			{
				e.printStackTrace();
				McRipperChecker.bad.parse(path);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * dl all files from an amazonAws website
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static void dlAmazonAws(String url, String path) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException
	{
		File oldMcDir = new File(McRipperChecker.mcripped, path);
		File xmlFile = dlMove(url, path + "/" + path + ".xml", new File(oldMcDir, path + ".xml"));
		Document doc = RippedUtils.parseXML(xmlFile);
		NodeList nlist = doc.getElementsByTagName("Contents");
		for(int i=0; i < nlist.getLength(); i++)
		{
			Node node = nlist.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element = (Element) node;
				String key = element.getElementsByTagName("Key").item(0).getTextContent();
				//skip directories
				if(key.endsWith("/"))
					continue;
				String timestamp = element.getElementsByTagName("LastModified").item(0).getTextContent();
				String fileUrl = url + "/" + key;
				File saveAs = new File(oldMcDir, key);
				learnDl(fileUrl, path + "/" + key, saveAs);
			}
		}
	}
	
	/**
	 * dl an entire webArchive to the archive directory
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static void dlWebArchive(String baseUrl, String dirPath) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException 
	{
		String name = RippedUtils.getLastSplit(baseUrl, "/");
		File webDir = new File(McRipperChecker.mcripped, dirPath);
		
		//dl the index file
		String xmlUrl = baseUrl + "/" + name + "_files.xml";
		name = name + "_files.xml";
		File xmlFile = dlMove(xmlUrl, name, new File(webDir, name));
		
		//start the dl process
		Document doc = RippedUtils.parseXML(xmlFile);
		NodeList nlist = doc.getElementsByTagName("file");
		for(int i=0; i < nlist.getLength(); i++)
		{
			Node node = nlist.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element = (Element) node;
				String nodeName = element.getAttribute("name");
				String source = element.getAttribute("source");
				if(!source.equals("original"))
					continue;
				Node nodeHash = element.getElementsByTagName("sha1").item(0);
				Node nodeStamp = element.getElementsByTagName("mtime").item(0);
				if(nodeHash == null || nodeStamp == null)
				{
					String timeErr = nodeStamp == null ? "time" : "";
					System.err.println("skipping Node Element:" + nodeName + " reasons:" + timeErr  + (nodeHash == null ? (timeErr.isEmpty() ? "hash" : ", hash") : ""));
					continue;
				}
				long ms = Long.parseLong(nodeStamp.getTextContent()) * 1000L;
				String sha1 = nodeHash.getTextContent();
				try
				{
					dl(baseUrl + "/" + nodeName, new File(webDir, nodeName).getPath(), ms, sha1);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public static File getFromMc(File mcDir, String type, String path) 
	{
		File cached = new File(mcDir, path);
		return cached.exists() ? cached : new File(McRipperChecker.mojang, toRipperPath(type, path));
	}
	
	/**
	 * doesn't support server paths, or server mappings as mojang doesn't have a specified path for them as of yet
	 */
	public static String toRipperPath(String type, String path)
	{
		if(path.startsWith("versions/") && !path.startsWith("versions/" + type))
		{
			path = path.replaceFirst("versions/", "versions/" + type + "/");
			String ext = DeDuperUtil.getExtension(path);
			path = path.substring(0, path.length() - ext.length() -1) + "-client." + ext;
		}
		else if(path.startsWith("assets/indexes/"))
		{
			path = path.replaceFirst("assets/indexes/", "jsons/assets/");
		}
		return path;
	}
}
