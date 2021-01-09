package jredfox.mcripper.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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
import jredfox.mcripper.printer.HashPrinter;
import jredfox.selfcmd.util.OSUtil;

public class DLUtils {
	
	public static File dl(String url, File saveAs, String hash) throws FileNotFoundException, IOException
	{
		return dl(url, saveAs, System.currentTimeMillis(), hash);
	}
	
	/**
	 * download a file to the path specified. With timestamp and hashing support. 
	 * The hash is in case the file destination already exists. To allow override pass "override" as the hash
	 */
	public static File dl(String url, File saveAs, long timestamp, String hash) throws FileNotFoundException, IOException, IllegalArgumentException
	{
		HashPrinter printer = McChecker.hash;
		url = getFixedUrl(url);
		if(hash == null)
			throw new IllegalArgumentException("hash cannot be null!");
		
		long time = System.currentTimeMillis();
		saveAs = getFixedFile(saveAs);
		
		try
		{	
			//prevent duplicate downloads
			if(printer.hashes.containsKey(hash))
				return RippedUtils.getSimpleFile(printer.hashes.get(hash));
			else if(saveAs.exists())
			{
				File hfile = new File(saveAs.getParent(), DeDuperUtil.getTrueName(saveAs) + "-" + hash + DeDuperUtil.getExtensionFull(saveAs));
				boolean hflag = hfile.exists();
				if(hflag || hash.equals(RippedUtils.getSHA1(saveAs)))
				{
					saveAs = hflag ? hfile : saveAs;
					System.err.println("File is out of sync with " + printer.log.getName() + " skipping duplicate download:" + saveAs);
					printer.append(hash, saveAs);
					return saveAs;
				}
				saveAs = hfile;
			}
			
			directDL(url, saveAs, timestamp);
			System.out.println("dl:" + RippedUtils.getSimplePath(saveAs) + " in:" + (System.currentTimeMillis() - time) + "ms " + " from:" + url);
			printer.append(hash, saveAs);
			return saveAs;
		}
		catch(IOException io)
		{
			printer.hashes.remove(hash);
			throw io;
		}
	}
	
	public static File extractDL(Class<?> clazz, String path, File saveAs, long timestamp, String hash) throws FileNotFoundException, IOException
	{
		String url = clazz.getClassLoader().getResource(path).toString();
		return dl(url, saveAs, timestamp, hash);
	}
	
	public static File learnExtractDL(Class<?> clazz, String path, File saveAs)
	{
		return DLUtils.learnDl(clazz.getClassLoader().getResource(path).toString(), saveAs);
	}
	
	/**
	 * direct dl with safegards in place to delete corrupted download files
	 */
	public static void directDL(String url, File output, long timestamp) throws MalformedURLException, IOException
	{
		URLConnection con = new URL(url).openConnection();
		con.setConnectTimeout(1000 * 15);
		InputStream inputStream = con.getInputStream();
		directDL(inputStream, output, timestamp);
	}

	/**
	 * direct dl with safegards in place to delete corrupted download files
	 */
	public static void directDL(InputStream inputStream, File output, long timestamp) throws FileNotFoundException, IOException 
	{
		try
		{
			output.getParentFile().mkdirs();
			IOUtils.copy(inputStream, new FileOutputStream(output));
			output.setLastModified(timestamp);
		}
		catch(IOException io)
		{
			if(output.exists())
				output.delete();
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
		long time = System.currentTimeMillis();
		url = url.replaceAll(" ", "%20");
		output = new File(output.getPath().replaceAll("%20", " "));
		directDL(url, output, timestamp);
		if(print)
			System.out.println("dl:" + RippedUtils.getSimplePath(output) + " in:" + (System.currentTimeMillis() - time) + "ms " + " from:" + url);
		return output;
	}
	
	public static String getFixedUrl(String url) 
	{
		return url.replaceAll(" ", "%20");
	}
	
	public static File getFixedFile(File saveAs) 
	{
		return OSUtil.toWinFile(new File(saveAs.getPath().replaceAll("%20", " "))).getAbsoluteFile();
	}
	
	public static File dlMove(String url, String path, File saveAs) throws FileNotFoundException, IOException
	{
		File tmpFile = dlToFile(url, new File(McChecker.tmp, path));
		String hash = RippedUtils.getSHA1(tmpFile);
		File moved = dl(RippedUtils.toURL(tmpFile).toString(), saveAs, hash);
		tmpFile.delete();
		return moved;
	}
	
	public static File dlFromMc(File mcDir, String url, File saveAs, String path, String hash) throws FileNotFoundException, IOException
	{
		File cached = new File(mcDir, path).getAbsoluteFile();
		cached = cached.exists() ? cached : new File(McChecker.mojang, path);
		boolean exists = cached.exists();
		long timestamp = exists ? cached.lastModified() : System.currentTimeMillis();
		url = exists && hash.equals(RippedUtils.getSHA1(cached)) ? RippedUtils.toURL(cached).toString() : url;
		File f = dlToFile(url, saveAs, timestamp);
		if(!url.startsWith("file:"))
			System.out.println("dl:" + f.getPath() + " from:" + url);
		return f;
	}

	public static File learnDl(String url, File saveAs) 
	{
		String spath = DeDuperUtil.getRealtivePath(McChecker.mcripped, saveAs.getAbsoluteFile());
		String urlPath = getFixedUrl(url);
		if(urlPath.contains("file:") || urlPath.contains("jar:"))
		{
			urlPath = spath;
		}
		if(McChecker.bad.contains(urlPath))
			return null;
		String cachedHash = McChecker.learner.get(urlPath, 1);
		//recall learning
		if(cachedHash != null && McChecker.hash.contains(cachedHash))
		{
			return new File(McChecker.root, McChecker.hash.hashes.get(cachedHash));
		}
		else if(cachedHash != null)
		{
			//if file doesn't exist recall hash and direct dl it here
			try
			{
				return dl(url, saveAs, cachedHash);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		//learn here
		try
		{
			File tmpFile = dlToFile(url, new File(McChecker.tmp, spath));
			String hash = RippedUtils.getSHA1(tmpFile);
			File moved = dl(RippedUtils.toURL(tmpFile).toString(), saveAs, hash);
			tmpFile.delete();
			McChecker.learner.append(urlPath, hash);
			System.out.println("dl tmp:" + spath + " from:" + url);
			return moved;
		}
		catch(IOException e)
		{
			String msg = e.getMessage();
			if(e instanceof FileNotFoundException || msg.contains("HTTP response code:"))
			{
				System.err.println(msg);
				McChecker.bad.append(urlPath);
			}
			else
			{
				e.printStackTrace();
				McChecker.bad.parse(urlPath);
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
		File baseDir = new File(McChecker.mcripped, path);
		String xname = DeDuperUtil.getTrueName(baseDir) + ".xml";
		File xmlFile = safeDlMove(url, path + "/" + xname, new File(baseDir, xname));
		dlAmazonAws(url, baseDir, xmlFile);
	}
	
	public static void dlAmazonAws(String baseUrl, File baseDir, File xmlFile) throws SAXException, IOException, ParserConfigurationException
	{
		if(xmlFile == null)
		{
			System.err.println("Unable to dl index from:" + baseUrl + " to path:" + baseDir);
			return;
		}
		Document doc = RippedUtils.parseXML(xmlFile);
		NodeList nlist = RippedUtils.getElementSafely(doc, "Contents");
		for(int i=0; i < nlist.getLength(); i++)
		{
			Node node = nlist.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element = (Element) node;
				String key = RippedUtils.getText(element, "Key");
				if(key.endsWith("/"))
					continue;//skip the directories
				String timestamp = RippedUtils.getText(element, "LastModified");
				String fileUrl = baseUrl + "/" + key;
				File saveAs = new File(baseDir, key);
				learnDl(fileUrl, saveAs);
			}
		}
	}
	
	public static File safeDlMove(String url, String path, File saveAs) 
	{
		try
		{
			return DLUtils.dlMove(url, path, saveAs);
		}
		catch(IOException io)
		{
			if(!(io instanceof FileNotFoundException))
				System.err.println(io.getMessage());
			else
				System.err.println("HTTP 404:" + url);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
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
		File webDir = new File(McChecker.mcripped, dirPath);
		
		//dl the index file
		String xmlUrl = baseUrl + "/" + name + "_files.xml";
		name = name + "_files.xml";
		File xmlFile = dlMove(xmlUrl, dirPath + "/" + name, new File(webDir, name));
		
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
					dl(baseUrl + "/" + nodeName, new File(webDir, nodeName), ms, sha1);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}
