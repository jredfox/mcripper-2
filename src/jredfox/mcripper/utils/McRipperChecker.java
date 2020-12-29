package jredfox.mcripper.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jml.evilnotch.lib.json.JSONArray;
import com.jml.evilnotch.lib.json.JSONObject;

import jredfox.filededuper.config.simple.MapConfig;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.mcripper.McRipper;
import jredfox.mcripper.obj.FileSet;
import jredfox.mcripper.obj.printer.CSVPrinter;
import jredfox.mcripper.obj.printer.HashPrinter;
import jredfox.mcripper.obj.printer.Printer;
import jredfox.mcripper.obj.printer.SetPrinter;
import jredfox.selfcmd.util.OSUtil;

public class McRipperChecker {
	
	//global vars
	public static final Set<File> checkJsons = new FileSet(2 + 534 + 11);
	public static final File tmp =  new File(OSUtil.getAppData(), McRipper.appId + "/tmp");
	public static File root;
	public static File mcripped;
	public static File mojang;
	public static File jsonDir;
	public static File jsonMajor;
	public static File jsonMinor;
	public static File jsonAssets;
	public static File jsonOldMajor;
	
	//counts
	public static int majorCount;
	public static int oldMajorCount;
	public static int minorCount;
	public static int assetsCount;
	public static int oldMinor;
	
	//printers
	public static HashPrinter hash;
	public static Printer learner;
	public static Printer bad;

	//mc dirs
	public static final File mcDefaultDir = RippedUtils.getMinecraftDir();
	public static volatile File mcDir = mcDefaultDir;
	
	public static void checkMojang(boolean skipSnaps) throws FileNotFoundException, IOException 
	{
		File[] majors = dlMojang();
		Set<File> minors = new HashSet<>(534);//the default amount of versions is 534 grow if needed to past this
		minors.add(extractAf());//since mojang no longer has this file on their servers we need embed it into the jar and extract it
		for(File major : majors)
		{
			minors.addAll(checkMajor(major, skipSnaps));
		}
		Set<File> assets = new HashSet<>(jsonAssets.exists() ? jsonAssets.listFiles().length + 10 : 20);
		for(File minor : minors)
		{
			Set<File> assetsIndex = checkMinor(minor, skipSnaps, false);
			assets.addAll(assetsIndex);
		}
		for(File asset : assets)
		{
			checkAssets(asset);
		}
	}
	
	public static void checkDisk(boolean skipSnaps, boolean skipOldMajors) throws FileNotFoundException, IOException
	{
		List<File> majors = DeDuperUtil.getDirFiles(jsonMajor);
		Set<File> oldMinors = new FileSet(0);
		for(File major : majors)
		{
			checkMajor(major, skipSnaps);
		}
		if(!skipOldMajors)
		{
			List<File> oldMajors = DeDuperUtil.getDirFiles(jsonOldMajor);
			for(File oldMajor : oldMajors)
				oldMinors = checkOldMajor(oldMajor);
		}
		List<File> minors = DeDuperUtil.getDirFiles(jsonMinor);
		for(File minor : minors)
		{
			checkMinor(minor, skipSnaps, oldMinors.contains(minor));
		}
		List<File> assetsJsons = DeDuperUtil.getDirFiles(jsonAssets);
		for(File assets : assetsJsons)
		{
			checkAssets(assets);
		}
	}
	
	public static void checkOldVersions() throws FileNotFoundException, IOException 
	{
		File oldJson = DLUtils.dlMove("http://s3.amazonaws.com/Minecraft.Download/versions/versions.json", "Minecraft.Download/versions.json", new File(jsonOldMajor, "versions.json"));
		Set<File> oldMinors = checkOldMajor(oldJson);
		Set<File> oldAssets = new FileSet(jsonAssets.exists() ? jsonAssets.listFiles().length + 10 : 20);
		for(File oldMinor : oldMinors)
		{
			Set<File> assets = checkMinor(oldMinor, false, true);
			oldAssets.addAll(assets);//populate anything checking a minor may update
		}
		for(File oldAsset : oldAssets)
		{
			checkAssets(oldAsset);
		}
	}
	
	public static void checkOmni() 
	{
		try 
		{
			DLUtils.dlWebArchive("https://archive.org/download/Minecraft-JE-Pre-Classic", "Omniarchive/Pre-Classic");
			DLUtils.dlWebArchive("https://archive.org/download/Minecraft-JE-Classic", "Omniarchive/JE-Classic");
			DLUtils.dlWebArchive("https://archive.org/download/Minecraft-JE-Indev", "Omniarchive/JE-Indev");
			DLUtils.dlWebArchive("https://archive.org/download/Minecraft-JE-Infdev", "Omniarchive/JE-Infdev");
			DLUtils.dlWebArchive("https://archive.org/download/Minecraft-JE-Alpha", "Omniarchive/JE-Alpha");
			DLUtils.dlWebArchive("https://archive.org/download/Minecraft-JE-Beta", "Omniarchive/JE-Beta");
			DLUtils.dlWebArchive("https://archive.org/download/Minecraft-JE-Sounds", "Omniarchive/JE-Sounds");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * NOTE: http://s3.amazonaws.com/Minecraft.Resources hasn't existed since 2013
	 */
	public static void checkOldMc()
	{
		try
		{
			DLUtils.dlAmazonAws("http://s3.amazonaws.com/MinecraftDownload", "MinecraftDownload");
			DLUtils.dlAmazonAws("http://s3.amazonaws.com/MinecraftResources", "MinecraftResources");
			checkOldVersions();
		}
		catch(Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public static Set<File> checkMajor(File master, boolean skipSnap) throws FileNotFoundException, IOException
	{
		if(!checkJsons.add(master))
			return Collections.emptySet();
		JSONObject mjson = RippedUtils.getJSON(master);
		JSONArray arr = (JSONArray) mjson.get("versions");
		Set<File> minors = new HashSet<>(arr.size());
		for(Object obj : arr)
		{
			JSONObject jsonVersion = (JSONObject)obj;
			String url = jsonVersion.getString("url");
			String minorHash = getMinorHash(jsonVersion, url);
			String version = jsonVersion.getString("id");
			String type = jsonVersion.getString("type");
			if(skipSnap && type.startsWith("snapshot"))
				continue;
			String time = jsonVersion.getString("time");
			File minor = DLUtils.dl(url, jsonMinor + "/" + type + "/" + version + ".json", minorHash);
			minors.add(minor.getAbsoluteFile());
		}
		majorCount++;
		return minors;
	}
	
	@SuppressWarnings("rawtypes")
	public static Set<File> checkOldMajor(File oldJson)
	{
		String urlBase = "http://s3.amazonaws.com/Minecraft.Download/";
		JSONObject json = RippedUtils.getJSON(oldJson);
		JSONArray arr = json.getJSONArray("versions");
		Set<File> oldMinors = new FileSet(json.size());
		for(Object obj : arr)
		{
			JSONObject versionEntry = (JSONObject)obj;
			String version = versionEntry.getString("id");
			String type = versionEntry.getString("type");
			String time = versionEntry.getString("time");
			
			String clientPath = type + "/" + version + ".json";
			File minorFile = new File(jsonMinor, clientPath);
			File dlMinor = DLUtils.learnDl(urlBase + "versions/" + version + "/" + version + ".json", "Minecraft.Download/jsons/minor/" + clientPath, minorFile);
			oldMinors.add(dlMinor);
		}
		oldMajorCount++;
		return oldMinors;
	}
	
	public static Set<File> checkMinor(File version, boolean skipSnap, boolean fCheckOld) throws FileNotFoundException, IOException 
	{
		if(!checkJsons.add(version))
			return Collections.emptySet();
		Set<File> assets = new FileSet(2);
		JSONObject json = RippedUtils.getJSON(version);
		String versionName = json.getString("id");
		String type = json.getString("type");
		if(skipSnap && type.startsWith("snapshot"))
		{
			return null;
		}
		//check legacy assetsIndex
		if(fCheckOld || !json.containsKey("assetIndex"))
			assets.addAll(checkOldMinor(json));
		
		//download the asset indexes
		JSONObject aIndex = json.getJSONObject("assetIndex");
		String id = aIndex.getString("id");
		String sha1 = aIndex.getString("sha1").toLowerCase();
		String url = aIndex.getString("url");
		assets.add(DLUtils.dl(url, new File(jsonAssets, id + ".json").getPath(), sha1));
		
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
				DLUtils.dl(logUrl, new File(mojang, "assets/log_configs/" + logId).getPath(), logSha1);
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
			DLUtils.dl(dataUrl, new File(mojang, "versions/" + type + "/" + versionName + "/" + versionName + "-" + name).getPath(), dataSha1);
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
				DLUtils.dl(libUrl, new File(mojang, "libraries/" + libPath).getPath(), libSha1);
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
					DLUtils.dl(clUrl, new File(mojang, "libraries/" + clPath).getPath(), clSha1);
				}
			}
		}
		minorCount++;
		return assets;
	}
	
	public static Set<File> checkOldMinor(JSONObject json) 
	{
		Set<File> assets = new FileSet(2);
		String urlBase = "http://s3.amazonaws.com/Minecraft.Download/";
		File oldMcDir = new File(mcripped, "Minecraft.Download");
		String assetsId = json.getString("assets");
		String version = json.getString("id");
		String type = json.getString("type");
		
		String checkPath = version + ".json";
		String assetsPath = assetsId + ".json";
		String jarPath ="versions/" + type + "/" + version + "/" + version + ".jar";
		String serverPath = "versions/" + type + "/" + version + "/" + "minecraft_server." + version + ".jar";
		String serverExePath = "versions/" + type + "/" + version + "/" + "minecraft_server." + version + ".exe";
		
		File checkFile = new File(jsonAssets, checkPath);
		File assetsFile = new File(jsonAssets, assetsPath);
		File jarFile = new File(oldMcDir, jarPath);
		File serverJarFile = new File(oldMcDir, serverPath);
		File serverExeFile = new File(oldMcDir, serverExePath);
		
		//dl the assetIndexes
		assets.add(DLUtils.learnDl(urlBase + "indexes/" + checkPath, "Minecraft.Download/" + checkPath, checkFile));
		assets.add(DLUtils.learnDl(urlBase + "indexes/" + assetsPath, "Minecraft.Download/jsons/assets/" + assetsPath, assetsFile));
		
		DLUtils.learnDl(urlBase + "versions/" + version + "/" + version + ".jar", "Minecraft.Download/" + jarPath, jarFile);
		DLUtils.learnDl(urlBase + "versions/" + version + "/" + "minecraft_server." + version + ".jar", "Minecraft.Download/" + serverPath, serverJarFile);
		DLUtils.learnDl(urlBase + "versions/" + version + "/" + "minecraft_server." + version + ".exe", "Minecraft.Download/" + serverExePath, serverExeFile);
		oldMinor++;
		return assets;
	}
	
	/**
	 * NOTE: there is nothing to differentiate a snapshot only assets index and a non snapshot one. As types are not specified.
	 * I got it working with checkMojang --skipSnaps but, that's because it uses only the return files it will fail with checkCustom --skipSnaps
	 */
	public static void checkAssets(File assetsIndexFile) throws FileNotFoundException, IOException
	{
		if(!checkJsons.add(assetsIndexFile))
			return;
		JSONObject json = RippedUtils.getJSON(assetsIndexFile);
		JSONObject objects = json.getJSONObject("objects");
		for(String key : objects.keySet())
		{
			JSONObject assetJson = objects.getJSONObject(key);
			String assetSha1 = assetJson.getString("hash");
			String assetSha1Char = assetSha1.substring(0, 2);
			String assetUrl = "https://resources.download.minecraft.net/" + assetSha1Char + "/" + assetSha1;
			DLUtils.dl(assetUrl, new File(mojang, "assets/objects/" + assetSha1Char + "/" + assetSha1).getPath(), assetSha1);
		}
		assetsCount++;
	}
	
	public static File extractAf() throws FileNotFoundException, IOException 
	{
		InputStream in = McRipper.class.getResourceAsStream("/1.12.2-af-minor.json");
		String path = "release/1.12.2-af-minor.json";
		File jsonaf = new File(tmp, path).getAbsoluteFile();
		jsonaf.getParentFile().mkdirs();
		IOUtils.copy(in, new FileOutputStream(jsonaf));
		String sha1 = RippedUtils.getSHA1(jsonaf);
		File actualaf = DLUtils.dl(RippedUtils.toURL(jsonaf).toString(), new File(jsonMinor, path).getAbsolutePath(), sha1);
		jsonaf.delete();
		return actualaf;
	}
	
	/**
	 * used by major jsons that are not v2 to grab the hash
	 */
	private static String getMinorHash(JSONObject json, String url) 
	{
		if(json.containsKey("sha1"))
			return json.getString("sha1");
		String[] urlArr = url.replace("\\", "/").split("/");
		String minorHash = urlArr[urlArr.length - 2].toLowerCase();
		return minorHash;
	}

	public static File[] dlMojang() throws FileNotFoundException, IOException 
	{
		return new File[]{dlMajor("version_manifest.json"), dlMajor("version_manifest_v2.json")};
	}
	
	private static File dlMajor(String vname) throws FileNotFoundException, MalformedURLException, IOException
	{ 
		File saveAs = new File(jsonMajor, vname);
		return DLUtils.dlMove("https://launchermeta.mojang.com/mc/game/" + vname, vname, saveAs);
	}
	
	public static void setRoot(File appDir)
	{
		root = appDir;
		mcripped = new File(root, "mcripped");
		mojang = new File(mcripped, "mojang");
		jsonDir = new File(mojang, "jsons");
		jsonOldMajor = new File(jsonDir, "oldmajors");
		jsonMajor = new File(jsonDir, "major");
		jsonMinor = new File(jsonDir, "minor");
		jsonAssets = new File(jsonDir, "assets");
	}

	public static void parseHashes() throws IOException
	{
		long ms = System.currentTimeMillis();
		hash = new HashPrinter(root, new File(root, "index.hash"), 10000);
		learner = new SetPrinter(root, new File(root, "learned.rhash"), 250);
		bad = new CSVPrinter(root, new File(root, "bad.paths"), 250);
		hash.load();
		learner.load();
		bad.load();
		System.out.println("parsed hashes & data in:" + (System.currentTimeMillis()-ms) + "ms");
	}

}
