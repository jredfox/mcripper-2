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
import jredfox.mcripper.printer.Learner;
import jredfox.mcripper.url.exception.URLException;
import jredfox.selfcmd.util.OSUtil;

public class DLUtils {
	
	public static boolean https = true;
	
	public static Pair<Integer, File> dlSingleton(ArchiveManager am, String url, File saveAs, String hash)
	{
		return dlSingleton(am, url, saveAs, -1, hash);
	}
	
	public static Pair<Integer, File> dlSingleton(ArchiveManager am, String url, File saveAs, long timestamp, String hash)
	{
		return dlSingleton(am, url, null, saveAs, timestamp, hash);
	}
	
	public static Pair<Integer, File> dlSingleton(ArchiveManager am, String url, String mcPath, File saveAs, String hash)
	{
		return dlSingleton(am, url, mcPath, saveAs, -1, hash);
	}

	/**
	 * download a file to the path specified file if hash doesn't exist. Requires a HashPrinter
	 * @return the Pair[HTTP Response --> File] NOTE: File will be null if an http error or IOException has occurred
	 */
	public static Pair<Integer, File> dlSingleton(ArchiveManager am, String url, String mcPath, File saveAs, long timestamp, String hash)
	{
		if(!RippedUtils.isValidSHA1(hash))
			throw new IllegalArgumentException("invalid sha1 hash:" + hash);
		
		url = getFixedUrl(url);
		saveAs = getFixedFile(saveAs);
		long start = System.currentTimeMillis();
		
		//prevent duplicate downloads
		if(am.contains(hash))
			return new Pair<>(304, am.getFileFromHash(hash));
		else if(saveAs.exists())
		{
			File hfile = new File(saveAs.getParent(), DeDuperUtil.getTrueName(saveAs) + "-" + hash + DeDuperUtil.getExtensionFull(saveAs));
			boolean hflag = hfile.exists();
			if(hflag || hash.equals(RippedUtils.getSHA1(saveAs)))
			{
				saveAs = hflag ? hfile : saveAs;
				System.err.println("File is out of sync with " + am.printer.log.getName() + " skipping duplicate download:" + saveAs);
				am.printer.append(hash, saveAs);
				return new Pair<>(304, saveAs);
			}
			saveAs = hfile;
		}
		
		//TODO: separate from dlSingleton method
		if(mcPath != null)
		{
			String old = url;
			url = DLUtils.getMcURL(McChecker.mcDir, url, mcPath, hash);
			if(timestamp == -1 && !url.equals(old))
				timestamp = getTime(old);//auto fill the real timestamp to prevent the file from dictating it from mc dir as it's always wrong
		}
		
		try
		{
			directDL(url, saveAs, timestamp);
		}
		catch(URLException h)
		{
			printWebIO(h);
			return new Pair<>(h.errCode, null);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return new Pair<>(403, (File) null);
		}
		
		System.out.println("dl:" + am.getSimplePath(saveAs).replaceAll("\\\\", "/") + " in:" + (System.currentTimeMillis() - start) + "ms " + " from:" + url);
		am.printer.append(hash, saveAs);
		return new Pair<>(200, saveAs);
	}
	
	public static void printWebIO(Exception io) 
	{
		if(io instanceof URLException && ((URLException)io).isWeb())
			System.err.println(io.getMessage());
		else
			io.printStackTrace();
	}

	public static File learnExtractDL(ArchiveManager am, String version, Class<?> clazz, String path, File saveAs)
	{
		return DLUtils.learnDl(am, "extraction", version, clazz.getClassLoader().getResource(path).toString(), saveAs);
	}
	
	/**
	 * direct dl with safegards in place to delete corrupted download files. setting the timestamp to -1 will be {@link URLConnection#getLastModified()} of the website or current ms
	 */
	public static void directDL(String sURL, File output, long timestamp) throws MalformedURLException, IOException
	{
		if(https)
			sURL = sURL.replaceAll("http:", "https:");
		URL url = new URL(sURL);
		URLConnection con = null;
		try
		{
			con = url.openConnection();
			if(timestamp == -1)
				timestamp = getTime(con);
			con.setConnectTimeout(1000 * 15);
			InputStream inputStream = con.getInputStream();
			directDL(inputStream, output, timestamp);
		}
		catch(Exception e)
		{
			throw e;
		}
		finally
		{
			if(con instanceof HttpURLConnection)
				((HttpURLConnection)con).disconnect(); 
		}
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
	
	public static File dlToFile(String url, File output)
	{
		return dlToFile(url, output, -1);
	}
	
	public static File dlToFile(String url, File output, boolean print)
	{
		return dlToFile(url, output, -1, print);
	}
	
	public static File dlToFile(String url, File output, long timestamp)
	{
		return dlToFile(url, output, timestamp, false);
	}
	
	public static File dlToFile(String url, File output, long timestamp, boolean print)
	{
		output = getFixedFile(output);
		try
		{
			directDL(getFixedUrl(url), output, timestamp);
		}
		catch(IOException io)
		{
			printWebIO(io);
			return null;
		}
		if(print)
			System.out.println("dl:" + output.getPath().replaceAll("\\\\", "/") + " from:" + url);
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
	
	public static File dlMove(ArchiveManager am, String url, String path, File saveAs)
	{
		return dlMove(am, url, path, saveAs, -1);
	}
	
	public static File dlMove(ArchiveManager am, String url, String path, File saveAs, long timestamp)
	{
		File tmpFile = dlToFile(url, new File(am.tmp, path), timestamp);
		if(tmpFile == null)
			return null;
		String hash = RippedUtils.getSHA1(tmpFile);
		File moved = dlSingleton(am, RippedUtils.toURL(tmpFile).toString(), saveAs, tmpFile.lastModified(), hash).getRight();
		if(moved == null)
			System.err.println("moved file is null this should never happen! " + getFixedFile(saveAs) + " report to github issues");
		tmpFile.delete();
		return moved;
	}
	
	/**
	 * get a mc file from the specified path or dl it to the mcDir if not applicable
	 */
	public static File getOrDlFromMc(File mcDir, String url, String path, String hash)
	{
		File saveAs = new File(mcDir, path).getAbsoluteFile();
		File cached = saveAs;
		cached = cached.exists() ? cached : McChecker.am.contains(hash) ? McChecker.am.getFileFromHash(hash) : cached;
		return cached.exists() && hash.equals(RippedUtils.getSHA1(cached)) ? cached : DLUtils.dlToFile(url, saveAs, true);
	}
	
	/**
	 * dl it from the cached mcDir if applicable otherwise download it from the url
	 */
	public static File dlFromMc(File mcDir, String url, File saveAs, String path, String hash)
	{
		File cached = new File(mcDir, path).getAbsoluteFile();
		cached = cached.exists() ? cached : McChecker.am.contains(hash) ? McChecker.am.getFileFromHash(hash) : cached;
		url = cached.exists() && hash.equals(RippedUtils.getSHA1(cached)) ? RippedUtils.toURL(cached).toString() : url;
		File f = dlToFile(url, saveAs);
		if(f == null)
			return null;
		if(!url.startsWith("file:"))
			System.out.println("dl:" + f.getPath().replaceAll("\\\\", "/") + " from:" + url);
		return f;
	}
	
	public static String getMcURL(File mcDir, String url, String path, String hash)
	{
		File cached = new File(mcDir, path);
		String fixedUrl = cached.exists() && RippedUtils.getSHA1(cached).equals(hash) ? RippedUtils.toURL(cached).toString() : url;
		return fixedUrl;
	}
	
	public static File learnDl(ArchiveManager am, String url, File saveAs)
	{
		return learnDl(am, url, saveAs, -1);
	}
	
	public static File learnDl(ArchiveManager am, String url, File saveAs, long timestamp) 
	{
		return learnDl(am, "global", "null", url, saveAs, timestamp);
	}
	
	public static File learnDl(ArchiveManager am, String index, String indexHash, String url, File saveAs) 
	{
		return learnDl(am, index, indexHash, url, saveAs, -1);
	}

	private static int[] http404Codes = new int[]{401, 403, 404, 405, 410, 414, 451};
	/**
	 * the main method for learnDl. input the index and indexHash to get the specific learner rather then having everything as global. If the hash is mismatched it won't parse the data and will delete the files
	 */
	public static File learnDl(ArchiveManager am, String index, String indexHash, String url, File saveAs, long timestamp) 
	{
		url = getFixedUrl(url);
		String spath = DeDuperUtil.getRealtivePath(am.dir, saveAs.getAbsoluteFile());
		String urlPath = url;
		if(urlPath.contains("file:") || urlPath.contains("jar:"))
			urlPath = spath;
		
		Learner learner = am.getLearner(index, indexHash);
		if(learner.bad.contains(urlPath))
			return null;
		
		String cachedHash = learner.learner.get(urlPath);
		if(cachedHash != null)
		{
			//recall location of file
			if(am.contains(cachedHash))
				return am.getFileFromHash(cachedHash);
			
			//re-download if file does not exist
			return dlSingleton(am, url, saveAs, timestamp, cachedHash).getRight();
		}
		
		//learn here
		File tmpFile = dlToFile(url, new File(am.tmp, spath), timestamp);
		timestamp = tmpFile.lastModified();//use the one on the cached disk first
		String hash = RippedUtils.getSHA1(tmpFile);
		System.out.println("learned:" + url + ", " + hash);
		Pair<Integer, File> responce = dlSingleton(am, RippedUtils.toURL(tmpFile).toString(), saveAs, timestamp, hash);
		int code = responce.getLeft();
		File moved = responce.getRight();
		//if an error has occurred do not continue and add it to the bad paths if it can be added
		if(moved == null)
		{
			if(RippedUtils.containsNum(code, http404Codes))
				learner.bad.append(urlPath);
			return null;
		}
		tmpFile.delete();
		learner.learner.append(urlPath, hash);
		return moved;
	}
	
	/**
	 * dl all files from an amazonAws website
	 * @return the xmlIndex if it returns null it did not succeed
	 */
	public static void dlAmazonAws(ArchiveManager am, String url, String path)
	{
		dlAmazonAws(am, url, path, null);
	}

	/**
	 * dl all files from an amazonAws website
	 * @return the xmlIndex if it returns null it did not succeed
	 */
	public static void dlAmazonAws(ArchiveManager am, String url, String path, File extracted)
	{
		File baseDir = new File(am.dir, path);
		String xname = DeDuperUtil.getTrueName(baseDir) + ".xml";
		File xmlFile = dlMove(am, url, path + "/" + xname, new File(baseDir, xname));
		if(xmlFile == null)
			xmlFile = extracted;
		dlAmazonAws(am, url, baseDir, xmlFile);
	}
	
	public static void dlAmazonAws(ArchiveManager am, String baseUrl, File xmlFile)
	{
		dlAmazonAws(am, baseUrl, xmlFile.getParentFile(), xmlFile);
	}
	
	public static void dlAmazonAws(ArchiveManager am, String baseUrl, File baseDir, File xmlFile) 
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
				learnDl(am, index, indexHash, fileUrl, saveAs, timestamp);
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
	public static void dlWebArchive(ArchiveManager am, String baseUrl, String dirPath)
	{
		String name = RippedUtils.getLastSplit(baseUrl, "/");
		File webDir = new File(am.dir, dirPath);
		
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
		File xmlFile = dlMove(am, xmlUrl, dirPath + "/" + name, new File(webDir, name));
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
				dlSingleton(am, baseUrl + "/" + nodeName, new File(webDir, nodeName), ms, sha1);
			}
		}
	}
	
	public static long getTime(String url)
	{
		URLConnection con = null;
		try
		{
			con = new URL(url).openConnection();
			con.setConnectTimeout(15000);
			long time = getTime(con);
			return time;
		}
		catch(Exception e)
		{
			DLUtils.printWebIO(e);
		}
		finally
		{
			if(con instanceof HttpURLConnection)
				((HttpURLConnection)con).disconnect();
		}
		return -1;
	}
	
	public static long getTime(URLConnection con)
	{
		long ms = con.getLastModified();
		return ms != 0 ? ms : System.currentTimeMillis();
	}
}
