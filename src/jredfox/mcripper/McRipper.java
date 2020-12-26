package jredfox.mcripper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.jml.evilnotch.lib.json.JSONArray;
import com.jml.evilnotch.lib.json.JSONObject;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.config.simple.MapConfig;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.selfcmd.SelfCommandPrompt;
import jredfox.selfcmd.util.OSUtil;

public class McRipper {
	
	static
	{
		Command.get("");
		Command.cmds.clear();
		McRipperCommands.load();
	}
	
	public static final String appId = "Mcripper";
	public static final String version = "1.0.0-pre.1";
	public static final String appName = "MC Ripper 2 Build: " + version;
	public static volatile Map<String, String> hashes;
	public static volatile Set<File> checkJsons = new HashSet<>(100);
	public static final File mcDefaultDir = getMinecraftDir();
	public static volatile File mcDir = mcDefaultDir;
	public static File root;
	public static File mcripped;
	public static File mojang;
	public static File jsonDir;
	public static File jsonMajor;
	public static File jsonMinor;
	public static File jsonAssets;
	public static File hashFile;
	public static PrintWriter hashWriter;
	public static int majorCount;
	public static int minorCount;
	public static int assetsCount;
	
	public static void main(String[] args) throws Exception
	{
		args = SelfCommandPrompt.wrapWithCMD("input a command: ", appId, appName, args, true, true);
		System.out.println("starting:" + appName);
		try
		{
			long ms = System.currentTimeMillis();
			loadCfg();
			Command<?> cmd = Command.fromArgs(args.length == 0 ? new String[]{"checkMojang"} : args);
			if(cmd != McRipperCommands.recomputeHashes)
			{
				parseHashes();
				System.out.println("computed Hashes in:" + (System.currentTimeMillis() - ms) + "ms");
			}
			cmd.run();
			System.out.println("Done in:" + (System.currentTimeMillis() - ms) / 1000D + " seconds" + " major:" + majorCount + " minor:" + minorCount + " assets:" + assetsCount);
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		hashWriter.close();
	}

	public static void loadCfg()
	{
		File appdir = new File(OSUtil.getAppData(), McRipper.appId);
		MapConfig cfg = new MapConfig(new File(System.getProperty("user.dir"), McRipper.appId + ".cfg"));
		cfg.load();
		appdir = new File(cfg.get(McRipper.appId + "Dir", appdir.getPath())).getAbsoluteFile();
		cfg.save();
		setRoot(appdir);
	}
	
	public static void setRoot(File appDir)
	{
		root = appDir;
		hashFile = new File(root, "index.hash");
		mcripped = new File(root, "mcripped");
		mojang = new File(mcripped, "mojang");
		jsonDir = new File(mojang, "jsons");
		jsonMajor = new File(jsonDir, "major");
		jsonMinor = new File(jsonDir, "minor");
		jsonAssets = new File(jsonDir, "assets");
	}

	public static void checkCustom(boolean diskOnly, boolean skipSnap) throws FileNotFoundException, IOException
	{
		if(!diskOnly)
			dlMojang();
		checkDisk(skipSnap);
	}
	
	public static void checkMojang(boolean skipSnaps) throws FileNotFoundException, IOException 
	{
		File[] majors = dlMojang();
		Set<File> minors = new HashSet<>();
		for(File major : majors)
		{
			minors.addAll(checkMajor(major, skipSnaps));
		}
		Set<File> assets = new HashSet<>(jsonAssets.listFiles().length);
		for(File minor : minors)
		{
			File assetsIndex = checkMinor(minor, skipSnaps);
			if(assetsIndex != null)
				assets.add(assetsIndex);
		}
		for(File asset : assets)
		{
			checkAssets(asset);
		}
	}

	public static void checkDisk(boolean skipSnaps) throws FileNotFoundException, IOException
	{
		List<File> majors = DeDuperUtil.getDirFiles(jsonMajor);
		for(File major : majors)
		{
			checkMajor(major, skipSnaps);
		}
		List<File> minors = DeDuperUtil.getDirFiles(jsonMinor);
		for(File minor : minors)
		{
			checkMinor(minor, skipSnaps);
		}
		List<File> assetsJsons = DeDuperUtil.getDirFiles(jsonAssets);
		for(File assets : assetsJsons)
		{
			checkAssets(assets);
		}
	}
	
	/**
	 * TODO: checkOldMc(check older mojang sites like amazon aws)
	 * TODO: dl checkOmni
	 * TODO: dl checkBetacraft
	 */
	public static void checkOptional()
	{
		
	}

	public static Set<File> checkMajor(File master, boolean skipSnap) throws FileNotFoundException, IOException
	{
		if(!checkJsons.add(master.getAbsoluteFile()))
			return Collections.emptySet();
		JSONObject mjson = RippedUtils.getJSON(master);
		JSONArray arr = (JSONArray) mjson.get("versions");
		Set<File> minors = new HashSet<>(arr.size());
		for(Object obj : arr)
		{
			JSONObject jsonVersion = (JSONObject)obj;
			String url = jsonVersion.getString("url");
			String minorHash = getHash(jsonVersion, url);
			String version = jsonVersion.getString("id");
			String type = jsonVersion.getString("type");
			if(skipSnap && type.startsWith("snapshot"))
				continue;
			String time = jsonVersion.getString("time");
			File minor = dl(url, jsonMinor + "/" + type + "/" + version + ".json", minorHash);
			minors.add(minor.getAbsoluteFile());
		}
		majorCount++;
		return minors;
	}

	private static String getHash(JSONObject jsonVersion, String url) 
	{
		if(jsonVersion.containsKey("sha1"))
			return jsonVersion.getString("sha1");
		String[] urlArr = url.replace("\\", "/").split("/");
		String minorHash = urlArr[urlArr.length - 2].toLowerCase();
		return minorHash;
	}

	public static File checkMinor(File version, boolean skipSnap) throws FileNotFoundException, IOException 
	{
		if(!checkJsons.add(version.getAbsoluteFile()))
			return version;
		JSONObject json = RippedUtils.getJSON(version);
		String versionName = json.getString("id");
		String type = json.containsKey("type") ? json.getString("type") : DeDuperUtil.getTrueName(version.getParentFile());
		if(skipSnap && type.startsWith("snapshot"))
		{
			return null;
		}
		//download the asset indexes
		JSONObject aIndex = json.getJSONObject("assetIndex");
		String id = aIndex.getString("id");
		String sha1 = aIndex.getString("sha1").toLowerCase();
		String url = aIndex.getString("url");
		File aIndexFile = dl(url, new File(jsonAssets, id + ".json").getPath(), sha1);
		
		//download the logging
		if(json.containsKey("logging"))
		{
			JSONObject logging = json.getJSONObject("logging");
			for(String key : logging.keySet())
			{
				JSONObject logEntry = logging.getJSONObject(key);
				JSONObject logFile = logEntry.getJSONObject("file");
				String logId = logFile.getString("id");
				String logSha1 = logFile.getString("sha1");
				String logUrl = logFile.getString("url");
				dl(logUrl, new File(mojang, "assets/log_configs/" + logId).getPath(), logSha1);
			}
		}
		
		//download the client data versions, mappings, servers
		JSONObject clientData = json.getJSONObject("downloads");
		for(String key : clientData.keySet())
		{
			JSONObject data = clientData.getJSONObject(key);
			String dataSha1 = data.getString("sha1").toLowerCase();
			String dataUrl = data.getString("url");
			String[] dataUrlSplit = dataUrl.replace("\\", "/").split("/");
			String name = dataUrlSplit[dataUrlSplit.length - 1];
			dl(dataUrl, new File(mojang, "versions/" + type + "/" + versionName + "/" + versionName + "-" + name).getPath(), dataSha1);
		}
		
		//download the libs classifier's
		JSONArray libs = json.getJSONArray("libraries");
		for(Object obj : libs)
		{
			JSONObject entry = (JSONObject)obj;
			JSONObject downloads = entry.getJSONObject("downloads");
			
			//download the artifacts
			if(downloads.containsKey("artifact"))
			{
				JSONObject artifact = downloads.getJSONObject("artifact");
				String libPath = artifact.getString("path");
				String libSha1 = artifact.getString("sha1");
				String libUrl = artifact.getString("url");
				dl(libUrl, new File(mojang, "libraries/" + libPath).getPath(), libSha1);
			}
			
			//download the classifiers
			if(downloads.containsKey("classifiers"))
			{
				JSONObject classifiers = downloads.getJSONObject("classifiers");
				for(String key : classifiers.keySet())
				{
					JSONObject cl = classifiers.getJSONObject(key);
					String clPath = cl.getString("path");
					String clSha1 = cl.getString("sha1");
					String clUrl = cl.getString("url");
					dl(clUrl, new File(mojang, "libraries/" + clPath).getPath(), clSha1);
				}
			}
		}
		minorCount++;
		return aIndexFile.getAbsoluteFile();
	}
	
	/**
	 * NOTE: there is nothing to differentiate a snapshot only assets index and a non snapshot one. As types are not specified.
	 * I got it working with checkMojang --skipSnaps but, that's because it uses only the return files it will fail with checkCustom --skipSnaps
	 */
	public static void checkAssets(File assetsIndexFile) throws FileNotFoundException, IOException
	{
		if(!checkJsons.add(assetsIndexFile.getAbsoluteFile()))
			return;
		JSONObject json = RippedUtils.getJSON(assetsIndexFile);
		JSONObject objects = json.getJSONObject("objects");
		for(String key : objects.keySet())
		{
			JSONObject assetJson = objects.getJSONObject(key);
			String assetSha1 = assetJson.getString("hash");
			String assetSha1Char = assetSha1.substring(0, 2);
			String assetUrl = "https://resources.download.minecraft.net/" + assetSha1Char + "/" + assetSha1;
			dl(assetUrl, new File(mojang, "assets/objects/" + assetSha1Char + "/" + assetSha1).getPath(), assetSha1);
		}
		assetsCount++;
	}

	public static void parseHashes() throws IOException
	{
		if(!hashFile.exists())
			computeHashes(mcripped);
		else
			hashes = RippedUtils.parseHashFile(IOUtils.getReader(hashFile));
		hashWriter = new PrintWriter(new BufferedWriter(new FileWriter(hashFile, true)), true);
	}
	
	private static void computeHashes(File dir)
	{
		System.out.println("Computing hashes This may take a while");
		hashes = RippedUtils.getHashes(dir);
		RippedUtils.saveFileLines(hashes, hashFile, true);
	}
	
	public static void add(String hash, File output) 
	{
		String path = DeDuperUtil.getRealtivePath(root, output);
		hashes.put(hash, path);
    	hashWriter.println(hash + "," + path);
	}
	
	/**
	 * get the default minecraft folder supports all os's
	 */
	public static File getMinecraftDir()
	{
		return new File(OSUtil.getAppData(), OSUtil.isMac() ? "minecraft" : ".minecraft");
	}

	public static File[] dlMojang() throws FileNotFoundException, IOException 
	{
		return new File[]{dlMajor("version_manifest.json"), dlMajor("version_manifest_v2.json")};
	}
	
	private static File dlMajor(String vname) throws FileNotFoundException, MalformedURLException, IOException
	{
		File master = dlToFile("https://launchermeta.mojang.com/mc/game/" + vname, new File(mojang, vname));
		String sha1 = RippedUtils.getSHA1(master);
		return dl(toURL(master).toString(), new File(jsonMajor, vname).getPath(), sha1);
	}

	public static URL toURL(File file) throws MalformedURLException
	{
		return file.toURI().toURL();
	}

	public static File dl(String url, String path, String hash) throws FileNotFoundException, IOException
	{
		return dl(url, path, System.currentTimeMillis(), hash);
	}
	
	public static File dlFromMc(File mcDir, String url, String path, File saveAs, String hash) throws FileNotFoundException, IOException
	{
		File cached = new File(mcDir, path).getAbsoluteFile();
		url = cached.exists() && hash.equals(RippedUtils.getSHA1(cached)) ? cached.toURI().toURL().toString() : url;
		return dlToFile(url, saveAs);
	}
	
	public static File dlToFile(String url, File output) throws FileNotFoundException, IOException
	{
		return dlToFile(url, output, System.currentTimeMillis());
	}
	
	public static File dlToFile(String url, File output, long timestamp) throws FileNotFoundException, IOException
	{
		URLConnection con = new URL(url).openConnection();
		con.setConnectTimeout(1000 * 15);
		con.setReadTimeout(Integer.MAX_VALUE / 2);
		InputStream inputStream = con.getInputStream();
		output.getParentFile().mkdirs();
		IOUtils.copy(inputStream, new FileOutputStream(output));
		output.setLastModified(timestamp);
		System.out.println("dl:" + url + " to:" + output);
		return output;
	}
	
	/**
	 * download a file to the path specified. With timestamp and hashing support. 
	 * The hash is in case the file destination already exists. To allow override pass "override" as the hash
	 */
	public static File dl(String url, String path, long timestamp, String hash) throws FileNotFoundException, IOException, IllegalArgumentException
	{
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
				if(hashes.containsKey(hash))
					return new File(root, hashes.get(hash));
				else if(output.exists())
				{
					//prevent duplicate downloads
					File hfile = new File(output.getParent(), DeDuperUtil.getTrueName(output) + "-" + hash + DeDuperUtil.getExtensionFull(output));
					boolean hflag = hfile.exists();
					if(hflag || hash.equals(RippedUtils.getSHA1(output)))
					{
						output = hflag ? hfile : output;
						System.err.println("File is out of sync with " + hashFile.getName() + " skipping duplicate download:" + output);
						add(hash, output);
						return output;
					}
					output = hfile;
				}
				add(hash, output);
			}
			
			//speed the process up for libraries as they are extremely slow
			if(path.contains("libraries"))
			{
				File cached = new File(mcDir, DeDuperUtil.getRealtivePath(mojang, new File(path).getAbsoluteFile()));
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
			System.out.println("dl:" + output + " in:" + (System.currentTimeMillis() - time) + "ms");
			return output;
		}
		catch(IOException io)
		{
			if(output.exists())
				output.delete();
			hashes.remove(hash);
			throw io;
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
		String name = getLastSplit(baseUrl, "/");
		File webDir = new File(mcripped, dirPath);
		
		//dl the index file
		String xmlUrl = baseUrl + "/" + name + "_files.xml";
		name = name + "_files.xml";
		File xmlFile = dlMove(xmlUrl, name, new File(webDir, name));
		
		//start the dl process
		Document doc = parseXML(xmlFile);
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
					System.err.println("skipping Node Element:" + nodeName + " reasons:" + timeErr  + (nodeHash == null ? (timeErr.isEmpty() ? "hash" : ",hash") : ""));
					continue;
				}
				long ms = Long.parseLong(nodeStamp.getTextContent()) * 1000L;
				String sha1 = nodeHash.getTextContent();
				McRipper.dl(baseUrl + "/" + nodeName, new File(webDir, nodeName).getPath(), ms, sha1);
			}
		}
	}
	
	public static Document parseXML(File xmlFile) throws SAXException, IOException, ParserConfigurationException
	{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
	    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    Document doc = dBuilder.parse(xmlFile);
	    doc.getDocumentElement().normalize();
	    return doc;
	}
	
	public static File dlMove(String url, String path, File saveAs) throws FileNotFoundException, IOException
	{
		File tmp = new File(root, "tmp");
		File tmpFile = McRipper.dlToFile(url, new File(tmp, path));
		String hash = RippedUtils.getSHA1(tmpFile);
		File moved = McRipper.dl(url, saveAs.getPath(), hash);
		tmpFile.delete();
		return moved;
	}

	private static String getLastSplit(String str, String sep) 
	{
		String[] arr = str.split(sep);
		return arr[arr.length - 1];
	}

}
