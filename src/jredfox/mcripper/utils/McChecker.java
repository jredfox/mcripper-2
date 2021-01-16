package jredfox.mcripper.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.jml.evilnotch.lib.json.JSONArray;
import com.jml.evilnotch.lib.json.JSONObject;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.mcripper.McRipper;
import jredfox.mcripper.data.FileSet;
import jredfox.mcripper.printer.HashPrinter;
import jredfox.mcripper.printer.Learner;
import jredfox.mcripper.printer.LogPrinter;
import jredfox.selfcmd.util.OSUtil;

public class McChecker {
	
	//global vars
	public static boolean loaded;
	public static final FileSet checkJsons = new FileSet(2 + 533 + 20);
	public static final File tmp =  new File(OSUtil.getAppData(), McRipper.appId + "/tmp");
	public static File root;
	public static File lRoot;
	public static File mcripped;
	public static File mojang;
	public static File jsonDir;
	public static File jsonMajor;
	public static File jsonMinor;
	public static File jsonAssets;
	public static File jsonOldMajor;
	public static File jsonOldMinor;
	
	//counts
	public static int majorCount;
	public static int oldMajorCount;
	public static int minorCount;
	public static int assetsCount;
	public static int oldMinor;
	
	//printers
	public static HashPrinter hash;
	public static LogPrinter logger;// the logger for the program

	//mc dirs
	public static final File mcDefaultDir = RippedUtils.getMinecraftDir();
	public static volatile File mcDir = mcDefaultDir;
	
	public static void checkMojang(boolean skipSnaps) throws FileNotFoundException, IOException, URISyntaxException 
	{
		File[] majors = dlMojang();
		Set<File> minors = new FileSet(534);//the default amount of versions is 534 grow if needed to past this
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
	
	public static void checkDisk(boolean skipSnaps) throws FileNotFoundException, IOException
	{
		//do majors
		List<File> majors = DeDuperUtil.getDirFiles(jsonMajor);
		for(File major : majors)
		{
			checkMajor(major, skipSnaps);
		}
		
		//do oldMajors
		List<File> oldMajors = DeDuperUtil.getDirFiles(jsonOldMajor);
		for(File oldMajor : oldMajors)
		{
			checkOldMajor(oldMajor, skipSnaps);
		}
		
		//do minors
		List<File> minors = DeDuperUtil.getDirFiles(jsonMinor);
		for(File minor : minors)
		{
			checkMinor(minor, skipSnaps, false);
		}
		
		//do oldMinors
		List<File> ominors = DeDuperUtil.getDirFiles(jsonOldMinor);
		for(File ominor : ominors)
		{
			checkMinor(ominor, skipSnaps, true);
		}
		
		//do assets
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
			DLUtils.dlAmazonAws("http://s3.amazonaws.com/MinecraftResources", "old/MinecraftResources");
			DLUtils.dlAmazonAws("http://s3.amazonaws.com/Minecraft.Resources", "old/Minecraft.Resources");
			DLUtils.dlAmazonAws("http://assets.minecraft.net", "old/assets_minecraft_net", extractAssetsXml());
			DLUtils.dlAmazonAws("http://s3.amazonaws.com/MinecraftDownload", "old/MinecraftDownload");
			checkOldVersions(skipSnaps);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static File extractAssetsXml() 
	{
		return DLUtils.learnExtractDL(McChecker.class, "resources/mcripper/aws/assets_minecraft_net-2016-11-06.xml", new File(McChecker.mcripped, "old/assets_minecraft_net/assets_minecraft_net.xml"));
	}

	public static void checkOldVersions(boolean skipSnaps) throws FileNotFoundException, IOException 
	{
		File oldJson = DLUtils.safeDlMove("http://s3.amazonaws.com/Minecraft.Download/versions/versions.json", "old/Minecraft.Download/versions.json", new File(jsonOldMajor, "versions.json"));
		if(oldJson == null)
		{
			System.err.println("Old Major is missing index skipping");
			return;
		}
		Set<File> oldMinors = checkOldMajor(oldJson, skipSnaps);
		Set<File> oldAssets = new FileSet(jsonAssets.exists() ? jsonAssets.listFiles().length + 10 : 20);
		for(File oldMinor : oldMinors)
		{
			Set<File> assets = checkMinor(oldMinor, skipSnaps, true);
			oldAssets.addAll(assets);//populate anything checking a minor may update
		}
		
		//manually download missing versions
		if(!skipSnaps)
		{
			List<String> list = IOUtils.getFileLines(IOUtils.getReader("resources/mcripper/snapshots.txt"));
			File oldMcDir = new File(mcripped, "old/Minecraft.Download");
			for(String snapId : list)
			{
				McChecker.dlOldMinor(oldMcDir, "snapshot", snapId, snapId, -1);
			}
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
			long time = RippedUtils.parseOffsetTime(jsonVersion.getString("time"));
			File minor = DLUtils.dl(url, "versions/" + version + "/" + version + ".json", new File(jsonMinor, type + "/" + version + ".json"), time, minorHash);
			minors.add(minor.getAbsoluteFile());
		}
		majorCount++;
		return minors;
	}
	
	@SuppressWarnings("rawtypes")
	public static Set<File> checkOldMajor(File oldJson, boolean skipSnaps)
	{
		if(!checkJsons.add(oldJson))
			return Collections.emptySet();
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
			long time = RippedUtils.parseOffsetTime(versionEntry.getString("time"));
			String clientPath = type + "/" + version + ".json";
			File minorFile = new File(jsonOldMinor, clientPath);
			File dlMinor = DLUtils.learnDl(urlBase + "versions/" + version + "/" + version + ".json", minorFile, time);
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
		if(fCheckOld || !json.containsKey("assetIndex") || !json.containsKey("downloads"))
			assets.addAll(checkOldMinor(json));
		
		//download the asset indexes
		if(json.containsKey("assetIndex"))
		{
			JSONObject aIndex = json.getJSONObject("assetIndex");
			String id = aIndex.getString("id");
			String sha1 = aIndex.getString("sha1").toLowerCase();
			String url = aIndex.getString("url");
			assets.add(DLUtils.dl(url, "assets/indexes/" + id + ".json", new File(jsonAssets, id + ".json"), sha1));
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
				String logPath = "assets/log_configs/" + logId;
				DLUtils.dl(logUrl, logPath, new File(mojang, logPath), logSha1);
			}
		}
		
		//download the client data versions, mappings, servers
		if(json.containsKey("downloads"))
		{
			JSONObject clientData = json.getJSONObject("downloads");
			for(String key : clientData.keySet())
			{
				JSONObject data = clientData.getJSONObject(key);
				long time = key.equals("client") ? RippedUtils.parseOffsetTime(json.getString("releaseTime")) : -1;
				String dataSha1 = data.getString("sha1").toLowerCase();
				String dataUrl = data.getString("url");
				String[] dataUrlSplit = dataUrl.replace("\\", "/").split("/");
				String name = dataUrlSplit[dataUrlSplit.length - 1];
				DLUtils.dl(dataUrl, "versions/" + versionName + "/" + versionName + RippedUtils.getExtensionFull(name), new File(mojang, "versions/" + type + "/" + versionName + "/" + versionName + "-" + name), time, dataSha1);
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
						DLUtils.dl(libUrl, "libraries/" + libPath, new File(mojang, "libraries/" + libPath), libSha1);
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
							DLUtils.dl(clUrl, "libraries/" + clPath, new File(mojang, "libraries/" + clPath), clSha1);
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
						DLUtils.learnDl(libBaseUrl, new File(mojang, "libraries/" + lpath));
					}
					else if(!entry.containsKey("natives"))
					{
						DLUtils.learnDl(lUrl, new File(mojang, "libraries/" + lpath));
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
								DLUtils.learnDl(libURL32, new File(mojang, "libraries/" + path32));
								DLUtils.learnDl(libURL64, new File(mojang, "libraries/" + path64));
							}
							else
							{	
								String npath = lBasePath + "-" + nvalue + ".jar";
								String nUrl = libBaseUrl + npath;
								DLUtils.learnDl(nUrl, new File(mojang, "libraries/" + npath));
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
		File oldMcDir = new File(mcripped, "old/Minecraft.Download");
		String assetsId = json.getString("assets");
		String version = json.getString("id");
		long clientTime = RippedUtils.parseOffsetTime(json.getString("releaseTime"));
		String type = json.getString("type");
		Set<File> assets = dlOldMinor(oldMcDir, type, version, assetsId, clientTime);
		oldMinor++;
		return assets;
	}
	
	public static Set<File> dlOldMinor(File oldMcDir, String type, String version, String assetsId, long clientTime)
	{
		Set<File> assets = new FileSet(2);
		String urlBase = "http://s3.amazonaws.com/Minecraft.Download/";
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
		
		//dl the files
		File dlClient = DLUtils.learnDl(urlBase + "versions/" + version + "/" + version + ".jar", jarFile, clientTime);
		if(dlClient == null)
			return assets;//no need to continue here the rest should return 404
		
		DLUtils.learnDl(urlBase + "versions/" + version + "/" + "minecraft_server." + version + ".jar", serverJarFile);
		DLUtils.learnDl(urlBase + "versions/" + version + "/" + "minecraft_server." + version + ".exe", serverExeFile);
		
		//dl the assetIndexes
		assets.add(DLUtils.learnDl(urlBase + "indexes/" + checkPath, checkFile));
		assets.add(DLUtils.learnDl(urlBase + "indexes/" + assetsPath, assetsFile));
		
		//dl the json in case it's being invoked directly instead of calling checkOldMajor
		DLUtils.learnDl(urlBase + "versions/" + version + "/" + version + ".json", new File(jsonOldMinor, type + "/" + version + ".json"));
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
			String twoChar = assetSha1.substring(0, 2);
			String assetUrl = "https://resources.download.minecraft.net/" + twoChar + "/" + assetSha1;
			DLUtils.dl(assetUrl, new File(mojang, "assets/objects/" + twoChar + "/" + assetSha1), assetSha1);//runs actually faster just dling it from online because the server is really fast
		}
		assetsCount++;
	}
	
	public static void extractJsons() throws FileNotFoundException, IOException, URISyntaxException 
	{
		String base = "resources/mcripper/jsons";
		Set<String> resources = RippedUtils.getPathsFromDir(McChecker.class, base);
		File dir = new File(base);//create a virtual file
		for(String r : resources)
		{
			String path = DeDuperUtil.getRealtivePath(dir, new File(r));//get a relative path based on virtual files
			DLUtils.learnExtractDL(McChecker.class, r, new File(jsonDir, path));
		}
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
		File v1 = dlMajor("version_manifest.json");
		File v2 = dlMajor("version_manifest_v2.json");
		
		if(v1 == null)
			System.err.println("missing mojang v1 index from: https://launchermeta.mojang.com/mc/game/version_manifest.json report to https://github.com/jredfox/mcripper-2/issues");
		if(v2 == null)
			System.err.println("missing mojang v2 index from: https://launchermeta.mojang.com/mc/game/version_manifest_v2.json report to https://github.com/jredfox/mcripper-2/issues");
		
		FileSet set = new FileSet(2);
		set.add(v1);
		set.add(v2);
		return DeDuperUtil.toArray(set, File.class);
	}
	
	private static File dlMajor(String vname) throws FileNotFoundException, MalformedURLException, IOException
	{ 
		File saveAs = new File(jsonMajor, vname);
		return DLUtils.safeDlMove("https://launchermeta.mojang.com/mc/game/" + vname, vname, saveAs);
	}
	
	public static void setRoot(File appDir) throws IOException
	{
		root = appDir;
		lRoot = new File(root, "learned");
		mcripped = new File(root, "mcripped");
		mojang = new File(mcripped, "mojang");
		jsonDir = new File(mcripped, "jsons");
		jsonMajor = new File(jsonDir, "major");
		jsonMinor = new File(jsonDir, "minor");
		jsonAssets = new File(jsonDir, "assets");
		jsonOldMajor = new File(jsonDir, "oldmajor");
		jsonOldMinor = new File(jsonDir, "oldminor");
		
		IOUtils.close(logger);
		hash = new HashPrinter(root, new File(root, "index.hash"), 23000);
		logger = new LogPrinter(new File(root, "log.txt"), System.out, System.err, false, true);
		logger.load();
	}

	public static void parseHashes(boolean extract) throws IOException, URISyntaxException
	{
		long ms = System.currentTimeMillis();
		hash.load();
		System.out.println("parsed hashes in:" + (System.currentTimeMillis() - ms) + "ms");
		if(extract)
			extractJsons();
		loaded = true;
	}

	public static void closePrinters()
	{
		IOUtils.close(hash);
		for(Learner learner : Learner.learners.values())
			IOUtils.close(learner);
	}

}
