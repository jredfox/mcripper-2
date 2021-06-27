package jredfox.mcripper.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.mcripper.McRipper;
import jredfox.mcripper.printer.HashPrinter;
import jredfox.mcripper.printer.Learner;
import jredfox.selfcmd.util.OSUtil;

public class DLUtils {
	
	public static File dl(String url, File saveAs, String hash) throws FileNotFoundException, IllegalArgumentException, IOException
	{
		return dl(url, saveAs, -1, hash);
	}
	
	public static File dl(String url, File saveAs, long timestamp, String hash) throws FileNotFoundException, IllegalArgumentException, IOException 
	{
		return dl(url, null, saveAs, timestamp, hash);
	}
	
	public static File dl(String url, String mcPath, File saveAs, String hash) throws FileNotFoundException, IOException, IllegalArgumentException
	{
		return dl(url, mcPath, saveAs, -1, hash);
	}

	/**
	 * download a file to the path specified. With timestamp and hashing support. 
	 * The hash is in case the file destination already exists. To allow override pass "override" as the hash
	 */
	public static File dl(String url, String mcPath, File saveAs, long timestamp, String hash) throws FileNotFoundException, IOException, IllegalArgumentException
	{
		if(hash == null)
			throw new IllegalArgumentException("hash cannot be null!");
		
		HashPrinter printer = McChecker.hash;
		url = getFixedUrl(url);
		saveAs = getFixedFile(saveAs);
		long time = System.currentTimeMillis();
		
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
			
			if(mcPath != null)
			{
				long ms = System.currentTimeMillis();
				String old = url;
				url = DLUtils.getMcURL(McChecker.mcDir, url, mcPath, hash);
				if(timestamp == -1 && !url.equals(old))
					timestamp = RippedUtils.getTime(old);
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
	
	public static File learnExtractDL(Class<?> clazz, String path, File saveAs)
	{
		return DLUtils.learnDl("extraction", McRipper.version, clazz.getClassLoader().getResource(path).toString(), saveAs);
	}
	
	/**
	 * direct dl with safegards in place to delete corrupted download files. setting the timestamp to -1 will be {@link URLConnection#getLastModified()} of the website or current ms
	 */
	public static void directDL(String sURL, File output, long timestamp) throws MalformedURLException, IOException
	{
		URL url = new URL(sURL);
		URLConnection con = url.openConnection();
		if(timestamp == -1)
			timestamp = RippedUtils.getTime(con);
		con.setConnectTimeout(1000 * 15);
		InputStream inputStream = con.getInputStream();
		directDL(inputStream, output, timestamp);
	}

	/**
	 * direct dl with safegaurds of corrupted downloads. it's private so you you call the other method to fix -1 timestamps
	 */
	private static void directDL(InputStream inputStream, File output, long timestamp) throws FileNotFoundException, IOException 
	{
		try
		{
			output.getParentFile().mkdirs();
			RippedUtils.copy(inputStream, new FileOutputStream(output));
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
		return dlToFile(url, output, -1);
	}
	
	public static File dlToFile(String url, File output, boolean print) throws FileNotFoundException, IOException
	{
		return dlToFile(url, output, -1, print);
	}
	
	public static File dlToFile(String url, File output, long timestamp) throws FileNotFoundException, IOException
	{
		return dlToFile(url, output, timestamp, false);
	}
	
	public static File dlToFile(String url, File output, long timestamp, boolean print) throws FileNotFoundException, IOException
	{
		output = getFixedFile(output);
		directDL(getFixedUrl(url), output, timestamp);
		if(print)
			System.out.println("dl:" + output + " from:" + url);
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
		return dlMove(url, path, saveAs, -1);
	}
	
	public static File dlMove(String url, String path, File saveAs, long timestamp) throws FileNotFoundException, IOException
	{
		File tmpFile = dlToFile(url, new File(McChecker.tmp, path), timestamp);
		String hash = RippedUtils.getSHA1(tmpFile);
		File moved = dl(RippedUtils.toURL(tmpFile).toString(), saveAs, tmpFile.lastModified(), hash);
		tmpFile.delete();
		return moved;
	}
	
	/**
	 * get a mc file from the specified path or dl it to the mcDir if not applicable
	 */
	public static File getOrDlFromMc(File mcDir, String url, String path, String hash) throws FileNotFoundException, IOException
	{
		File saveAs = new File(mcDir, path).getAbsoluteFile();
		File cached = saveAs;
		cached = cached.exists() ? cached : McChecker.hash.contains(hash) ? RippedUtils.getFileFromHash(hash) : cached;
		return cached.exists() && hash.equals(RippedUtils.getSHA1(cached)) ? cached : DLUtils.dlToFile(url, saveAs, true);
	}
	
	/**
	 * dl it from the cached mcDir if applicable otherwise download it from the url
	 */
	public static File dlFromMc(File mcDir, String url, File saveAs, String path, String hash) throws FileNotFoundException, IOException
	{
		File cached = new File(mcDir, path).getAbsoluteFile();
		cached = cached.exists() ? cached : McChecker.hash.contains(hash) ? RippedUtils.getFileFromHash(hash) : cached;
		url = cached.exists() && hash.equals(RippedUtils.getSHA1(cached)) ? RippedUtils.toURL(cached).toString() : url;
		File f = dlToFile(url, saveAs);
		if(!url.startsWith("file:"))
			System.out.println("dl:" + f.getPath() + " from:" + url);
		return f;
	}
	
	public static String getMcURL(File mcDir, String url, String path, String hash) throws MalformedURLException
	{
		File cached = new File(mcDir, path);
		String fixedUrl = cached.exists() && RippedUtils.getSHA1(cached).equals(hash) ? RippedUtils.toURL(cached).toString() : url;
		return fixedUrl;
	}
	
	public static File learnDl(String url, File saveAs)
	{
		return learnDl(url, saveAs, -1);
	}
	
	public static File learnDl(String url, File saveAs, long timestamp) 
	{
		return learnDl("global", "null", url, saveAs, timestamp);
	}
	
	public static File learnDl(String index, String indexHash, String url, File saveAs) 
	{
		return learnDl(index, indexHash, url, saveAs, -1);
	}

	private static int[] http404Codes = new int[]{401, 403, 404, 405, 410, 414, 451};
	/**
	 * the main method for learnDl. input the index and indexHash to get the specific learner rather then having everything as global. If the hash is mismatched it won't parse the data and will delete the files
	 */
	public static File learnDl(String index, String indexHash, String url, File saveAs, long timestamp) 
	{
		url = getFixedUrl(url);
		String spath = DeDuperUtil.getRealtivePath(McChecker.mcripped, saveAs.getAbsoluteFile());
		String urlPath = url;
		if(urlPath.contains("file:") || urlPath.contains("jar:"))
			urlPath = spath;
		
		Learner learner = getLearner(index, indexHash);
		if(learner.bad.contains(urlPath))
			return null;
		
		String cachedHash = learner.learner.get(urlPath);
		if(cachedHash != null)
		{
			//recall location of file
			if(McChecker.hash.contains(cachedHash))
				return RippedUtils.getSimpleFile(McChecker.hash.hashes.get(cachedHash));
			
			//re-download if file does not exist
			try
			{
				return dl(url, saveAs, timestamp, cachedHash);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		//learn here
		try
		{	
			File tmpFile = dlToFile(url, new File(McChecker.tmp, spath), timestamp);
			timestamp = tmpFile.lastModified();//use the one on the cached disk first
			String hash = RippedUtils.getSHA1(tmpFile);
			System.out.println("learned:" + url + ", " + hash);
			File moved = dl(RippedUtils.toURL(tmpFile).toString(), saveAs, timestamp, hash);
			tmpFile.delete();
			learner.learner.append(urlPath, hash);
			return moved;
		}
		catch(IOException e)
		{
			int code = RippedUtils.getResponseCode(url);
			if(code != -1)
			{
				if(RippedUtils.containsNum(code, http404Codes))
				{
					System.err.println(e.getMessage());
					learner.bad.append(urlPath);
				}
			}
			else
				e.printStackTrace();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static Learner getLearner(String index, String indexHash) 
	{
		Learner learner = Learner.learners.get(index);
		if(learner == null)
		{
			try
			{
				learner = new Learner(McChecker.lRoot, index, indexHash);
				learner.parse();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return learner;
	}
	
	/**
	 * dl all files from an amazonAws website
	 * @return the xmlIndex if it returns null it did not succeed
	 */
	public static void dlAmazonAws(String url, String path) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException
	{
		dlAmazonAws(url, path, null);
	}

	/**
	 * dl all files from an amazonAws website
	 * @return the xmlIndex if it returns null it did not succeed
	 */
	public static void dlAmazonAws(String url, String path, File extracted) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException
	{
		File baseDir = new File(McChecker.mcripped, path);
		String xname = DeDuperUtil.getTrueName(baseDir) + ".xml";
		File xmlFile = safeDlMove(url, path + "/" + xname, new File(baseDir, xname));
		if(xmlFile == null)
			xmlFile = extracted;
		dlAmazonAws(url, baseDir, xmlFile);
	}
	
	public static void dlAmazonAws(String baseUrl, File xmlFile) throws SAXException, IOException, ParserConfigurationException
	{
		dlAmazonAws(baseUrl, xmlFile.getParentFile(), xmlFile);
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
		if(nlist == null)
		{
			System.err.println("XML file appears to be missing Contents:" + xmlFile.getAbsolutePath() + " from:" + baseUrl);
			return;
		}
		String index = DeDuperUtil.getTrueName(xmlFile);
		String indexHash = RippedUtils.getSHA1(xmlFile);
		for(int i=0; i < nlist.getLength(); i++)
		{
			Node node = nlist.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element = (Element) node;
				String key = RippedUtils.getText(element, "Key");
				if(key.endsWith("/"))
					continue;//skip the directories
				String strTime = RippedUtils.getText(element, "LastModified");
				long timestamp = RippedUtils.parseZTime(strTime);
				String fileUrl = baseUrl + "/" + key;
				File saveAs = new File(baseDir, key);
				learnDl(index, indexHash, fileUrl, saveAs, timestamp);
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
			System.err.println(io.getMessage());
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
		String lowerName = name.toLowerCase();
		Set<String> internals = new HashSet<>(7);
		internals.add(lowerName + "_archive.torrent");
		internals.add(lowerName + "_files.xml");
		internals.add(lowerName + "_meta.sqlite");
		internals.add(lowerName + "_meta.xml");
		internals.add(lowerName + "_reviews.xml");
		internals.add(lowerName + "_rules.conf");
		internals.add("__ia_thumb.jpg");
		
		name = name + "_files.xml";
		File xmlFile = safeDlMove(xmlUrl, dirPath + "/" + name, new File(webDir, name));
		if(xmlFile == null)
		{
			System.err.println("web archive xml index missing for:" + xmlUrl + " skipping");
			return;
		}
		
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
				
				//skip internal files
				if(internals.contains(nodeName.toLowerCase()))
					continue;
				
				String source = element.getAttribute("source");
				//non original files are auto generated by webarchive don't ask me why they do this
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
