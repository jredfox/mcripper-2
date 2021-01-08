package jredfox.mcripper.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.jml.evilnotch.lib.json.JSONArray;
import com.jml.evilnotch.lib.json.JSONObject;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.mcripper.McRipper;
import jredfox.mcripper.data.FileSet;
import jredfox.mcripper.printer.CSVPrinter;
import jredfox.mcripper.printer.HashPrinter;
import jredfox.mcripper.printer.SetPrinter;
import jredfox.selfcmd.util.OSUtil;

public class McChecker {
	
	//global vars
	public static final FileSet checkJsons = new FileSet(2 + 534 + 11);
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
	public static CSVPrinter learner;
	public static SetPrinter bad;

	//mc dirs
	public static final File mcDefaultDir = RippedUtils.getMinecraftDir();
	public static volatile File mcDir = mcDefaultDir;
	
	public static void checkMojang(boolean skipSnaps) throws FileNotFoundException, IOException 
	{
		File[] majors = dlMojang();
		Set<File> minors = new FileSet(534);//the default amount of versions is 534 grow if needed to past this
		minors.add(extractAf());//since mojang no longer has this file on their servers we need embed it into the jar and extract it
		for(File major : majors)
		{
			minors.addAll(checkMajor(major, skipSnaps));
		}
		Set<File> assets = new FileSet(jsonAssets.exists() ? jsonAssets.listFiles().length + 10 : 20);
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
				oldMinors = checkOldMajor(oldMajor, skipSnaps);
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

	public static void checkOldMc(boolean skipSnaps)
	{
		try
		{
//			DLUtils.dlAmazonAws("http://s3.amazonaws.com/MinecraftResources", "old/MinecraftResources");
//			DLUtils.dlAmazonAws("http://s3.amazonaws.com/Minecraft.Resources", "old/Minecraft.Resources");
//			DLUtils.dlAmazonAws("http://assets.minecraft.net", "old/Assets_Minecraft_Net");
			DLUtils.dlAmazonAws("http://s3.amazonaws.com/MinecraftDownload", "old/MinecraftDownload");
//			checkOldVersions(skipSnaps);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void checkOldVersions(boolean skipSnaps) throws FileNotFoundException, IOException 
	{
		File oldJson = DLUtils.dlMove("http://s3.amazonaws.com/Minecraft.Download/versions/versions.json", "old/Minecraft.Download/versions.json", new File(jsonOldMajor, "versions.json"));
		Set<File> oldMinors = checkOldMajor(oldJson, skipSnaps);
		Set<File> oldAssets = new FileSet(jsonAssets.exists() ? jsonAssets.listFiles().length + 10 : 20);
		for(File oldMinor : oldMinors)
		{
			Set<File> assets = checkMinor(oldMinor, skipSnaps, true);
			oldAssets.addAll(assets);//populate anything checking a minor may update
		}
		for(File oldAsset : oldAssets)
		{
			checkAssets(oldAsset);
		}
	}
	
	public static Set<File> checkMajor(File master, boolean skipSnaps) throws FileNotFoundException, IOException
	{
		if(!checkJsons.add(master))
			return Collections.emptySet();
		JSONObject mjson = RippedUtils.getJSON(master);
		JSONArray arr = (JSONArray) mjson.get("versions");
		Set<File> minors = new FileSet(arr.size());
		for(Object obj : arr)
		{
			JSONObject jsonVersion = (JSONObject)obj;
			String url = jsonVersion.getString("url");
			String minorHash = getMinorHash(jsonVersion, url);
			String version = jsonVersion.getString("id");
			String type = jsonVersion.getString("type");
			if(skipSnaps && type.startsWith("snapshot"))
				continue;
			String time = jsonVersion.getString("time");
			File minor = DLUtils.dl(url, new File(jsonMinor, type + "/" + version + ".json"), minorHash);
			minors.add(minor.getAbsoluteFile());
		}
		majorCount++;
		return minors;
	}
	
	@SuppressWarnings("rawtypes")
	public static Set<File> checkOldMajor(File oldJson, boolean skipSnaps)
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
			if(skipSnaps && type.startsWith("snapshot"))
				continue;
			String time = versionEntry.getString("time");
			
			String clientPath = type + "/" + version + ".json";
			File minorFile = new File(jsonMinor, clientPath);
			File dlMinor = DLUtils.learnDl(urlBase + "versions/" + version + "/" + version + ".json", "old/Minecraft.Download/jsons/minor/" + clientPath, minorFile);
			oldMinors.add(dlMinor);
		}
		oldMajorCount++;
		return oldMinors;
	}
	
	public static Set<File> checkMinor(File version, boolean skipSnaps, boolean fCheckOld) throws FileNotFoundException, IOException 
	{
		if(!checkJsons.add(version))
			return Collections.emptySet();
		Set<File> assets = new FileSet(2);
		JSONObject json = RippedUtils.getJSON(version);
		String versionName = json.getString("id");
		String type = json.getString("type");
		if(skipSnaps && type.startsWith("snapshot"))
		{
			return null;
		}
		//check legacy assetsIndex
		if(fCheckOld || !json.containsKey("assetIndex"))
			assets.addAll(checkOldMinor(json));
		
		//download the asset indexes
		if(json.containsKey("assetIndex"))
		{
			JSONObject aIndex = json.getJSONObject("assetIndex");
			String id = aIndex.getString("id");
			String sha1 = aIndex.getString("sha1").toLowerCase();
			String url = aIndex.getString("url");
			assets.add(DLUtils.dl(url, new File(jsonAssets, id + ".json"), sha1));
		}
		
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
				DLUtils.dl(logUrl, new File(mojang, "assets/log_configs/" + logId), logSha1);
			}
		}
		
		//download the client data versions, mappings, servers
		if(json.containsKey("downloads"))
		{
			JSONObject clientData = json.getJSONObject("downloads");
			for(String key : clientData.keySet())
			{
				JSONObject data = clientData.getJSONObject(key);
				String dataSha1 = data.getString("sha1").toLowerCase();
				String dataUrl = data.getString("url");
				String[] dataUrlSplit = dataUrl.replace("\\", "/").split("/");
				String name = dataUrlSplit[dataUrlSplit.length - 1];
				DLUtils.dl(dataUrl, new File(mojang, "versions/" + type + "/" + versionName + "/" + versionName + "-" + name), dataSha1);
			}
		}
		
		//download the libs classifier's
		if(json.containsKey("libraries"))
		{
			JSONArray libs = json.getJSONArray("libraries");
			for(Object obj : libs)
			{
				JSONObject entry = (JSONObject)obj;
				if(json.containsKey("downloads"))
				{
					JSONObject downloads = entry.getJSONObject("downloads");
					//download the artifacts
					if(downloads.containsKey("artifact"))
					{
						JSONObject artifact = downloads.getJSONObject("artifact");
						String libPath = artifact.getString("path");
						String libSha1 = artifact.getString("sha1");
						String libUrl = artifact.getString("url");
						DLUtils.dl(libUrl, new File(mojang, "libraries/" + libPath), libSha1);
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
							DLUtils.dl(clUrl, new File(mojang, "libraries/" + clPath), clSha1);
						}
					}
				}
				else
				{
					//start legacy library download support with machine learning dl to speed up the process for the next launch
					String libBaseUrl = entry.containsKey("url") ? entry.getString("url") : "https://libraries.minecraft.net/";
					String name = entry.getString("name");
					String[] arr = name.split(":");
					arr[0] = arr[0].replaceAll("\\.", "/");
					String lBasePath = arr[0] + "/" + arr[1] + "/" + arr[2] + "/" + arr[1] + "-" + arr[2];
					String lpath = lBasePath + ".jar";
					String lUrl = libBaseUrl + lpath;
					
					if(libBaseUrl.trim().isEmpty())
					{
						System.err.println("Library URL is empty skipping:" + lpath);
						continue;
					}
					
					if(!libBaseUrl.endsWith("/"))
					{
						DLUtils.learnDl(libBaseUrl, "libraries/" + lpath, new File(mojang, "libraries/" + lpath));
					}
					else if(!entry.containsKey("natives"))
					{
						DLUtils.learnDl(lUrl, "libraries/" + lpath, new File(mojang, "libraries/" + lpath));
					}
					else
					{
						JSONObject natives = entry.getJSONObject("natives");
						for(String nname : natives.keySet())
						{
							String nvalue = natives.getString(nname);
							boolean bits = false;
							if(nvalue.contains("${arch}"))
							{
								nvalue = nvalue.replaceAll("\\$\\{arch\\}", "");
								bits = true;
							}
							if(bits)
							{
								String path32 = lBasePath + "-" + nvalue + "32.jar";
								String path64 = lBasePath + "-" + nvalue + "64.jar";
								String libURL32 = libBaseUrl + path32;
								String libURL64 = libBaseUrl + path64;
								DLUtils.learnDl(libURL32, "libraries/" + path32, new File(mojang, "libraries/" + path32));
								DLUtils.learnDl(libURL64, "libraries/" + path64, new File(mojang, "libraries/" + path64));
							}
							else
							{	
								String npath = lBasePath + "-" + nvalue + ".jar";
								String nUrl = libBaseUrl + npath;
								DLUtils.learnDl(nUrl, "libraries/" + npath, new File(mojang, "libraries/" + npath));
							}
						}
					}
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
		File oldMcDir = new File(mcripped, "old/Minecraft.Download");
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
		assets.add(DLUtils.learnDl(urlBase + "indexes/" + checkPath, "old/Minecraft.Download/jsons/assets/" + checkPath, checkFile));
		assets.add(DLUtils.learnDl(urlBase + "indexes/" + assetsPath, "old/Minecraft.Download/jsons/assets/" + assetsPath, assetsFile));
		
		DLUtils.learnDl(urlBase + "versions/" + version + "/" + version + ".jar", "old/Minecraft.Download/" + jarPath, jarFile);
		DLUtils.learnDl(urlBase + "versions/" + version + "/" + "minecraft_server." + version + ".jar", "old/Minecraft.Download/" + serverPath, serverJarFile);
		DLUtils.learnDl(urlBase + "versions/" + version + "/" + "minecraft_server." + version + ".exe", "old/Minecraft.Download/" + serverExePath, serverExeFile);
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
			DLUtils.dl(assetUrl, new File(mojang, "assets/objects/" + assetSha1Char + "/" + assetSha1), assetSha1);
		}
		assetsCount++;
	}
	
	public static File extractAf() throws FileNotFoundException, IOException 
	{
		return DLUtils.learnExtractDL(McChecker.class, "resources/mcripper/jsons/minor/1.12.2-af-minor.json", new File(jsonMinor, "release/1.12.2-af-minor.json"));
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
		jsonDir = new File(mcripped, "jsons");
		jsonOldMajor = new File(jsonDir, "oldmajors");
		jsonMajor = new File(jsonDir, "major");
		jsonMinor = new File(jsonDir, "minor");
		jsonAssets = new File(jsonDir, "assets");
	}

	public static void parseHashes() throws IOException
	{
		long ms = System.currentTimeMillis();
		hash = new HashPrinter(root, new File(root, "index.hash"), 10000);
		learner = new CSVPrinter(root, new File(root, "learned.rhash"), 600);
		bad = new SetPrinter(root, new File(root, "bad.paths"), 300);
		hash.load();
		learner.load();
		bad.load();
		extractAf();
		System.out.println("parsed hashes & data in:" + (System.currentTimeMillis() - ms) + "ms");
	}

	public static void closePrinters()
	{
		IOUtils.close(hash);
		IOUtils.close(learner);
		IOUtils.close(bad);
	}

}
