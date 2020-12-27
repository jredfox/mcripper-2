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
import java.util.HashMap;
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
import jredfox.filededuper.command.CommandInvalid;
import jredfox.filededuper.command.Commands;
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
		Command.cmds.put("help", Commands.help);
		McRipperCommands.load();
	}
	
	public static final String appId = "Mcripper";
	public static final String version = "b.1.3.0";
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
	public static File jsonOldMajor;
	public static File hashFile;
	public static PrintWriter hashWriter;
	public static int majorCount;
	public static int oldMajorCount;
	public static int minorCount;
	public static int assetsCount;
	
	public static void main(String[] args) throws Exception
	{
		args = SelfCommandPrompt.wrapWithCMD("input a command: ", appId, appName, args, false, true);
		System.out.println("starting:" + appName);
		try
		{
			long ms = System.currentTimeMillis();
			loadCfg();
			Command<?> cmd = Command.fromArgs(args.length == 0 ? new String[]{"checkMojang"} : args);
			boolean isNormal = cmd != McRipperCommands.recomputeHashes && cmd != Commands.help && cmd != McRipperCommands.rip && !(cmd instanceof CommandInvalid);
			if(isNormal)
				parseHashes();
			cmd.run();
			if(isNormal)
				System.out.println("Done in:" + (System.currentTimeMillis() - ms) / 1000D + " seconds" + (oldMajorCount > 0 ? " oldMajor:" + oldMajorCount : "") + " major:" + majorCount + " minor:" + minorCount + " assets:" + assetsCount);
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		IOUtils.close(hashWriter);
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
		jsonOldMajor = new File(jsonDir, "oldmajors");
		jsonMajor = new File(jsonDir, "major");
		jsonMinor = new File(jsonDir, "minor");
		jsonAssets = new File(jsonDir, "assets");
	}
	
	public static void checkMojang(boolean skipSnaps) throws FileNotFoundException, IOException 
	{
		File[] majors = dlMojang();
		Set<File> minors = new HashSet<>(534);//the default amount of versions is 534 grow if needed to past this
		minors.add(extractAf());//since mojang no longer has this file on their servers we need embed it into the jar and extract it
		for(File major : majors)
		{
			minors.addAll(checkMajor(major, skipSnaps));
		}
		Set<File> assets = new HashSet<>(jsonAssets.exists() ? jsonAssets.listFiles().length : 0);
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

	public static File extractAf() throws FileNotFoundException, IOException 
	{
		InputStream in = McRipper.class.getResourceAsStream("/1.12.2-af-minor.json");
		File jsonaf = new File(tmp, "release/1.12.2-af-minor.json").getAbsoluteFile();
		jsonaf.getParentFile().mkdirs();
		IOUtils.copy(in, new FileOutputStream(jsonaf));
		String sha1 = RippedUtils.getSHA1(jsonaf);
		File actualaf = McRipper.dl(toURL(jsonaf).toString(), new File(jsonMinor, "release/1.12.2-af-minor.json").getAbsolutePath(), sha1);
		jsonaf.delete();
		return actualaf;
	}

	public static void checkDisk(boolean skipSnaps, boolean skipOldMajors, boolean forceDlCheck) throws FileNotFoundException, IOException
	{
		List<File> majors = DeDuperUtil.getDirFiles(jsonMajor);
		for(File major : majors)
		{
			checkMajor(major, skipSnaps);
		}
		if(!skipOldMajors)
		{
			List<File> oldMajors = DeDuperUtil.getDirFiles(jsonOldMajor);
			for(File oldMajor : oldMajors)
				checkOldMajor(oldMajor, forceDlCheck);
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
		long ms = System.currentTimeMillis();
		if(!hashFile.exists())
			computeHashes(mcripped);
		else
			hashes = RippedUtils.parseHashFile(IOUtils.getReader(hashFile));
		hashWriter = new PrintWriter(new BufferedWriter(new FileWriter(hashFile, true)), true);
		System.out.println("computed Hashes in:" + (System.currentTimeMillis() - ms) + "ms");
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
		File saveAs = new File(jsonMajor, vname);
		return McRipper.dlMove("https://launchermeta.mojang.com/mc/game/" + vname, vname, saveAs);
	}

	public static URL toURL(File file) throws MalformedURLException
	{
		return file.toURI().toURL();
	}

	public static File dl(String url, String path, String hash) throws FileNotFoundException, IOException
	{
		return dl(url, path, System.currentTimeMillis(), hash);
	}
	
	public static File dlFromMc(File altMcDir, File mcDir, String url, String path, File saveAs, String hash) throws FileNotFoundException, IOException
	{
		File cached = new File(mcDir, path).getAbsoluteFile();
		cached = cached.exists() ? cached : new File(altMcDir, path);
		boolean exists = cached.exists();
		long timestamp = exists ? cached.lastModified() : System.currentTimeMillis();
		url = exists && hash.equals(RippedUtils.getSHA1(cached)) ? cached.toURI().toURL().toString() : url;
		File f = dlToFile(url, saveAs, timestamp);
		if(!url.startsWith("file:"))
			System.out.println("dl:" + f.getPath() + " from:" + url);
		return f;
	}
	
	public static File dlToFile(String url, File output) throws FileNotFoundException, IOException
	{
		return dlToFile(url, output, System.currentTimeMillis());
	}
	
	public static File dlToFile(String url, File output, long timestamp) throws FileNotFoundException, IOException
	{
		url = url.replaceAll(" ", "%20");
		output = new File(output.getPath().replaceAll("%20", " "));
		URLConnection con = new URL(url).openConnection();
		con.setConnectTimeout(1000 * 15);
		con.setReadTimeout(Integer.MAX_VALUE / 2);
		InputStream inputStream = con.getInputStream();
		output.getParentFile().mkdirs();
		IOUtils.copy(inputStream, new FileOutputStream(output));
		output.setLastModified(timestamp);
		return output;
	}
	
	/**
	 * download a file to the path specified. With timestamp and hashing support. 
	 * The hash is in case the file destination already exists. To allow override pass "override" as the hash
	 */
	public static File dl(String url, String path, long timestamp, String hash) throws FileNotFoundException, IOException, IllegalArgumentException
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
			add(hash, output);
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
					System.err.println("skipping Node Element:" + nodeName + " reasons:" + timeErr  + (nodeHash == null ? (timeErr.isEmpty() ? "hash" : ", hash") : ""));
					continue;
				}
				long ms = Long.parseLong(nodeStamp.getTextContent()) * 1000L;
				String sha1 = nodeHash.getTextContent();
				try
				{
					McRipper.dl(baseUrl + "/" + nodeName, new File(webDir, nodeName).getPath(), ms, sha1);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * dl all files from an amazonAws website
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static void dlAmazonAws(String url, String path, boolean forceDlCheck) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException
	{
		File oldMcDir = new File(mcripped, path);
		File xmlFile = dlMove(url, path + "/" + path + ".xml", new File(oldMcDir, path + ".xml"));
		Document doc = parseXML(xmlFile);
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
				if(!saveAs.exists() || forceDlCheck)
					McRipper.safeDlMove(fileUrl, path + "/" + key, saveAs);
			}
		}
	}
	
	public static void dlOldVersions(boolean forceDlCheck) throws FileNotFoundException, IOException 
	{
		File oldJson = McRipper.dlMove("http://s3.amazonaws.com/Minecraft.Download/versions/versions.json", "Minecraft.Download/versions.json", new File(jsonOldMajor, "versions.json"));
		checkOldMajor(oldJson, forceDlCheck);
	}
	
	public static void checkOldMajor(File oldJson, boolean forceDlCheck)
	{
		String urlBase = "http://s3.amazonaws.com/Minecraft.Download/";
		File oldMcDir = new File(mcripped, "Minecraft.Download");
		JSONObject json = RippedUtils.getJSON(oldJson);
		JSONArray arr = json.getJSONArray("versions");
		for(Object obj : arr)
		{
			JSONObject versionEntry = (JSONObject)obj;
			String version = versionEntry.getString("id");
			String type = versionEntry.getString("type");
			String time = versionEntry.getString("time");
			
			String jsonPath = "versions/" + type + "/" + version + ".json";
			String jarPath ="versions/" + type + "/" + version + "/" + version + ".jar";
			String serverPath = "versions/" + type + "/" + version + "/" + "minecraft_server." + version + ".jar";
			String serverExePath = "versions/" + type + "/" + version + "/" + "minecraft_server." + version + ".exe";
			
			File jsonFile = new File(jsonMinor, jsonPath);
			File jarFile = new File(oldMcDir, jarPath);
			File serverJarFile = new File(oldMcDir, serverPath);
			File serverExeFile = new File(oldMcDir, serverExePath);
			
			//don't stop the json dlMove check as json are expected to get modified and re-uploaded
			McRipper.safeDlMove(urlBase + "versions/" + version + "/" + version + ".json", "Minecraft.Download/" + jsonPath, jsonFile);
			if(!jarFile.exists() || forceDlCheck)
				McRipper.safeDlMove(urlBase + "versions/" + version + "/" + version + ".jar", "Minecraft.Download/" + jarPath, jarFile);
			if(!serverJarFile.exists() || forceDlCheck)
				McRipper.safeDlMove(urlBase + "versions/" + version + "/" + "minecraft_server." + version + ".jar", "Minecraft.Download/" + serverPath, serverJarFile);
			if(!serverExeFile.exists() || forceDlCheck)
				McRipper.safeDlMove(urlBase + "versions/" + version + "/" + "minecraft_server." + version + ".exe", "Minecraft.Download/" + serverExePath, serverExeFile);
		}
		oldMajorCount++;
	}

	private static File safeDlMove(String url, String path, File saveAs) 
	{
		try
		{
			File file = McRipper.dlMove(url, path, saveAs);
			System.out.println("dl" + url + " to tmp:" + path);
			return file;
		}
		catch(IOException io)
		{
			String msg = io.getMessage();
			if(msg.contains("code: 403") || msg.contains("code: 404"))
			{
				System.err.println(msg);
			}
			else
				io.printStackTrace();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return saveAs;
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
	
	private static final File tmp = new File(OSUtil.getAppData(), McRipper.appId + "/tmp");
	public static File dlMove(String url, String path, File saveAs) throws FileNotFoundException, IOException
	{
		File tmpFile = McRipper.dlToFile(url, new File(tmp, path));
		String hash = RippedUtils.getSHA1(tmpFile);
		File moved = McRipper.dl(toURL(tmpFile).toString(), saveAs.getPath(), hash);
		tmpFile.delete();
		return moved;
	}

	private static String getLastSplit(String str, String sep) 
	{
		String[] arr = str.split(sep);
		return arr[arr.length - 1];
	}

	public static void checkOmni() 
	{
		try 
		{
			McRipper.dlWebArchive("https://archive.org/download/Minecraft-JE-Pre-Classic", "Omniarchive/Pre-Classic");
			McRipper.dlWebArchive("https://archive.org/download/Minecraft-JE-Classic", "Omniarchive/JE-Classic");
			McRipper.dlWebArchive("https://archive.org/download/Minecraft-JE-Indev", "Omniarchive/JE-Indev");
			McRipper.dlWebArchive("https://archive.org/download/Minecraft-JE-Infdev", "Omniarchive/JE-Infdev");
			McRipper.dlWebArchive("https://archive.org/download/Minecraft-JE-Alpha", "Omniarchive/JE-Alpha");
			McRipper.dlWebArchive("https://archive.org/download/Minecraft-JE-Beta", "Omniarchive/JE-Beta");
			McRipper.dlWebArchive("https://archive.org/download/Minecraft-JE-Sounds", "Omniarchive/JE-Sounds");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void checkOldMc(boolean forceDlCheck)
	{
		try
		{
//			McRipper.dlAmazonAws("http://s3.amazonaws.com/MinecraftDownload", "MinecraftDownload");
//			McRipper.dlAmazonAws("http://s3.amazonaws.com/MinecraftResources", "MinecraftResources");
//			McRipper.dlAmazonAws("http://s3.amazonaws.com/MinecraftResources", "Minecraft.Resources");
			McRipper.dlOldVersions(forceDlCheck);
		}
		catch(Exception e) 
		{
			e.printStackTrace();
		}
	}
}
