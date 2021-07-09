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
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
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
import jredfox.mcripper.url.URLResponse;
import jredfox.mcripper.url.exception.URLException;
import jredfox.selfcmd.util.OSUtil;

public class DLUtils {
	
	public static boolean https = true;
	
	public static URLResponse dlSingleton(ArchiveManager am, String url, File saveAs, String hash)
	{
		return dlSingleton(am, url, saveAs, -1, hash);
	}
	
	public static URLResponse dlSingleton(ArchiveManager am, String url, File saveAs, long timestamp, String hash)
	{
		return dlSingleton(am, url, null, saveAs, timestamp, hash);
	}
	
	public static URLResponse dlSingleton(ArchiveManager am, String url, String cachedPath, File saveAs, String hash)
	{
		return dlSingleton(am, url, cachedPath, saveAs, -1, hash);
	}

	/**
	 * download a file to the path specified file if hash doesn't exist. Requires a HashPrinter
	 * @return the Pair[HTTP Response --> File] NOTE: File will be null if an http error or IOException has occurred
	 */
	public static URLResponse dlSingleton(ArchiveManager am, String url, String cachedPath, File saveAs, long timestamp, String hash)
	{
		if(!RippedUtils.isValidSHA1(hash))
			throw new IllegalArgumentException("invalid sha1 hash:" + hash);
		
		//correct the bad hash to the correct hash to prevent duplicate downloads
		if(am.badHashes.contains(hash))
			hash = am.badHashes.get(hash);
		
		url = getFixedUrl(url);
		saveAs = getFixedFile(saveAs);
		File oldSaveAs = saveAs;
		boolean hashed = false;
		long start = System.currentTimeMillis();
		
		//prevent duplicate downloads
		if(am.contains(hash))
			return new URLResponse(am.getFileFromHash(hash));
		else if(saveAs.exists())
		{
			File hfile = RippedUtils.hashFile(saveAs, hash);
			boolean hExists = hfile.exists();
			if(hExists || hash.equals(RippedUtils.getSHA1(saveAs)))
			{
				saveAs = hExists ? hfile : saveAs;
				System.err.println("File is out of sync with " + am.printer.log.getName() + " skipping duplicate download:" + am.getSimplePath(saveAs).replaceAll("\\\\", "/"));
				am.printer.append(hash, saveAs);
				return new URLResponse(saveAs);
			}
			saveAs = hfile;
			hashed = true;
		}
		
		//pull from cached path whenever possible
		if(cachedPath != null)
		{
			String old = url;
			url = DLUtils.getCachedURL(am.cachedDir, cachedPath, url, hash);
			if(timestamp == -1 && !url.equals(old))
				timestamp = getTime(old);//auto fill the real timestamp to prevent FileURLConnection#getLastModified() from returning the wrong value due to mc launcher not preserving timestamp integrity 
		}
		
		URLResponse reply = null;
		try
		{
			reply = directDL(url, saveAs, timestamp);
			
			//start the dl integrity check
			String actualHash = RippedUtils.getSHA1(saveAs);
			if(!hash.equals(actualHash))
			{
				System.err.println("hash mismatch expected hash:" + hash + " actual:" + actualHash + " from:" + am.getSimplePath(saveAs).replaceAll("\\\\", "/"));
				am.badHashes.append(hash, actualHash);
				hash = actualHash;
				
				//check for dupes in index.hash, out of sync non hashed file. the hashed form of the proper file if it exists will just get overriden so no need to worry about dupes
				if(am.contains(actualHash))
				{
					System.err.println("deleting duplicate file due to hash mismatch:" + am.getSimplePath(saveAs).replaceAll("\\\\", "/"));
					saveAs.delete();
					return new URLResponse(am.getFileFromHash(actualHash));
				}
				else if(hashed && actualHash.equals(RippedUtils.getSHA1(oldSaveAs)))
				{
					System.err.println("File is out of sync with " + am.printer.log.getName() + " deleting hash mismatch download:" + am.getSimplePath(saveAs).replaceAll("\\\\", "/"));
					saveAs.delete();
					am.printer.append(actualHash, oldSaveAs);
					return new URLResponse(oldSaveAs);
				}
				
				//move the file to proper path. do nothing if the name remains the same
				if(hashed)
				{
					File hfile = RippedUtils.hashFile(oldSaveAs, actualHash);
					try
					{
						RippedUtils.move(saveAs, hfile);
					}
					catch(Exception e)
					{
						saveAs.delete();//delete the file if it fails to move. treat it like a failed download
						throw e;
					}
					saveAs = hfile;
				}
			}
		}
		catch(URLException h)
		{
			printWebIO(h);
			return new URLResponse(h);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return new URLResponse(getProtocol(url), -1, null);
		}
		
		System.out.println("dl:" + am.getSimplePath(saveAs).replaceAll("\\\\", "/") + " in:" + (System.currentTimeMillis() - start) + "ms " + " from:" + url);
		am.printer.append(hash, saveAs);
		return reply;
	}

	/**
	 * direct dl with safegards in place to delete corrupted download files. setting the timestamp to -1 will be {@link URLConnection#getLastModified()} of the website or current ms
	 * @throws URLException if an exception occurs and is not standard Exceptions
	 */
	public static URLResponse directDL(String sURL, File output, long timestamp) throws URLException, IOException, Exception
	{
		URL url = null;
		URLConnection con = null;
		try
		{
			url = new URL(sURL);
			con = url.openConnection();
			con.setConnectTimeout(1000 * 15);
			if(timestamp == -1)
				timestamp = getTime(con);
			InputStream inputStream = con.getInputStream();
			directDL(inputStream, output, timestamp);
			return new URLResponse(url.getProtocol(), getCode(con), output);
		}
		catch(MalformedURLException m)
		{
			throw new MalformedURLException(sURL);
		}
		catch(IOException io)
		{
			URLException uException = URLException.create(io, url, getCode(con));
			if(uException != null)
				throw uException;
			else
				throw io;
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
		catch(Exception e)
		{
			if(output.exists())
				output.delete();
			throw e;
		}
	}
	
	public static URLResponse dlToFile(String url, File output)
	{
		return dlToFile(url, output, -1);
	}
	
	public static URLResponse dlToFile(String url, File output, boolean print)
	{
		return dlToFile(url, output, -1, print);
	}
	
	public static URLResponse dlToFile(String url, File output, long timestamp)
	{
		return dlToFile(url, output, timestamp, false);
	}
	
	public static URLResponse dlToFile(String url, File output, long timestamp, boolean print)
	{
		url = getFixedUrl(url);
		output = getFixedFile(output);
		try
		{
			URLResponse reply = directDL(url, output, timestamp);
			if(print)
				System.out.println("dl:" + output.getPath().replaceAll("\\\\", "/") + " from:" + url);
			return reply;
		}
		catch(URLException ue)
		{
			printWebIO(ue);
			return new URLResponse(ue);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return new URLResponse(getProtocol(url), -1, null);
		}
	}
	
	public static String getFixedUrl(String url) 
	{
		if(https)
			url = url.replaceAll("http:", "https:");
		return url.replaceAll(" ", "%20");
	}
	
	public static File getFixedFile(File saveAs) 
	{
		return OSUtil.toWinFile(new File(getUnfixedURL(saveAs.getPath()))).getAbsoluteFile();
	}
	
	public static String getUnfixedURL(String url)
	{
		return url.replaceAll("%20", " ");
	}
	
	public static URLResponse dlMove(ArchiveManager am, String url, String path, File saveAs)
	{
		return dlMove(am, url, path, saveAs, -1);
	}
	
	public static URLResponse dlMove(ArchiveManager am, String url, String path, File saveAs, long timestamp)
	{
		URLResponse r1 = dlToFile(url, new File(am.tmp, path), timestamp);
		File tmpFile = r1.file;
		if(tmpFile == null)
			return r1;
		String hash = RippedUtils.getSHA1(tmpFile);
		URLResponse reply = dlSingleton(am, RippedUtils.toURL(tmpFile).toString(), saveAs, tmpFile.lastModified(), hash);
		File moved = reply.file;
		if(moved == null)
			System.err.println("moved file is null this should never happen! " + getFixedFile(saveAs) + " report to github issues");
		tmpFile.delete();
		return reply;
	}
	
	public static String getCachedURL(File baseDir, String path, String url, String hash)
	{
		File cached = new File(baseDir, path);
		return cached.exists() && RippedUtils.getSHA1(cached).equals(hash) ? RippedUtils.toURL(cached).toString() : url;
	}
	
	private static int[] http404Codes = new int[]{401, 403, 404, 405, 410, 414, 451};
	
	public static URLResponse learnExtractDL(ArchiveManager am, String version, Class<?> clazz, String path, File saveAs)
	{
		return DLUtils.learnDl(am, "extraction", version, clazz.getClassLoader().getResource(path).toString(), saveAs);
	}
	
	public static URLResponse learnDl(ArchiveManager am, String url, File saveAs)
	{
		return learnDl(am, url, saveAs, -1);
	}
	
	public static URLResponse learnDl(ArchiveManager am, String url, File saveAs, long timestamp) 
	{
		return learnDl(am, "global", "null", url, saveAs, timestamp);
	}
	
	public static URLResponse learnDl(ArchiveManager am, String index, String indexHash, String url, File saveAs) 
	{
		return learnDl(am, index, indexHash, url, saveAs, -1);
	}
	
	/**
	 * the main method for learnDl. input the index and indexHash to get the specific learner rather then having everything as global. If the hash is mismatched it won't parse the data and will delete the files
	 */
	public static URLResponse learnDl(ArchiveManager am, String index, String indexHash, String url, File saveAs, long timestamp) 
	{
		url = getFixedUrl(url);
		String spath = DeDuperUtil.getRealtivePath(am.dir, saveAs.getAbsoluteFile());
		String urlPath = url;
		if(!RippedUtils.isWeb(getProtocol(urlPath)))
			urlPath = spath;
		
		Learner learner = am.getLearner(index, indexHash);
		if(learner.bad.contains(urlPath))
		{
			String p = getProtocol(url);
			return new URLResponse(p, RippedUtils.isHTTP(p) ? 404 : -1, null);
		}
		
		String cachedHash = learner.learner.get(urlPath);
		if(cachedHash != null)
		{
			//recall location of file
			if(am.contains(cachedHash))
				return new URLResponse(am.getFileFromHash(cachedHash));
			
			//re-download if file does not exist
			return dlSingleton(am, url, saveAs, timestamp, cachedHash);
		}
		
		//learn here
		URLResponse replyTmp = dlToFile(url, new File(am.tmp, spath), timestamp);
		File tmpFile = replyTmp.file;
		if(tmpFile == null)
		{
			if(replyTmp.isHTTP() && RippedUtils.containsNum(replyTmp.code, http404Codes))
				learner.bad.append(urlPath);
			return replyTmp;
		}
		timestamp = tmpFile.lastModified();//use the one on the cached disk first
		String hash = RippedUtils.getSHA1(tmpFile);
		System.out.println("learned:" + urlPath + ", " + hash);
		URLResponse response = dlSingleton(am, RippedUtils.toURL(tmpFile).toString(), saveAs, timestamp, hash);
		
		if(response.file != null)
			learner.learner.append(urlPath, hash);
		
		tmpFile.delete();
		return response;
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
		File root = new File(am.dir, path);
		File indexDir = new File(root, "indexes");
		File fileDir = new File(root, "files");
		
		String xname = DeDuperUtil.getTrueName(root) + ".xml";
		File xmlFile = dlMove(am, url, path + "/indexes/" + xname, new File(indexDir, xname)).file;
		if(xmlFile == null)
			xmlFile = extracted;
		dlAmazonAws(am, url, fileDir, xmlFile);
	}
	
	public static void dlAmazonAws(ArchiveManager am, String baseUrl, File xmlFile)
	{
		dlAmazonAws(am, baseUrl, xmlFile != null ? xmlFile.getParentFile() : null, xmlFile);
	}
	
	public static void dlAmazonAws(ArchiveManager am, String baseUrl, File fileDir, File xmlFile) 
	{
		if(xmlFile == null)
		{
			System.err.println("Unable to dl index from:" + baseUrl + " to path:" + fileDir);
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
				File saveAs = new File(fileDir, key);
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
		File root = new File(am.dir, dirPath);
		File indexDir = new File(root, "indexes");
		File webDir = new File(root, "files");
		
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
		File xmlFile = dlMove(am, xmlUrl, dirPath + "/indexes/" + name, new File(indexDir, name)).file;
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
	
	/**
	 * sun.net.www.protocol.file.FileURLConnection doesn't contain error codes yet
	 */
	public static int getCode(URLConnection con)
	{
		if(con instanceof HttpURLConnection)
		{
			try 
			{
				return ((HttpURLConnection)con).getResponseCode();
			}
			catch(UnknownServiceException um)
			{
				return 415;//unsupported media
			}
			catch(UnknownHostException uh)
			{
				return URLException.UNKNOWNHOST;
			}
			catch (IOException e)
			{
				System.err.println("critical error while retrieving url error code. Report issue to github!");
				e.printStackTrace();
			}
		}
		else if(con.getClass().getName().equals("sun.net.www.protocol.ftp.FtpURLConnection"))
			System.err.println("JRE 8 doesn't support FTP codes report to java!");
		return -1;
	}
	
	public static String getProtocol(String url) 
	{
		try
		{
			return new URL(url).getProtocol();
		}
		catch(Exception e)
		{
			return "";
		}
	}

	public static void printWebIO(Exception io) 
	{
		if(io instanceof URLException && ((URLException)io).isWeb())
			System.err.println(io.getMessage());
		else
			io.printStackTrace();
	}
}
