package jredfox.mcripper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jml.evilnotch.lib.json.JSONArray;
import com.jml.evilnotch.lib.json.JSONObject;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.selfcmd.SelfCommandPrompt;
import jredfox.selfcmd.util.OSUtil;

public class McRipper {
	
	static
	{
		Command.get("");
		Command.cmds.clear();
		MCRipperCommands.load();
	}
	
	public static final String appId = "Mcripper";
	public static final String version = "b.1.0.0";
	public static final String appName = "MC Ripper 2 Build: " + version;
	public static volatile Map<String, String> hashes;
	public static volatile Set<File> checkJsons = new HashSet<>(100);
	public static File root = new File(OSUtil.getAppData(), McRipper.appId);
	public static File mcripped = new File(root, "mcripped");
	public static File mojang = new File(mcripped, "mojang");
	public static File jsonDir = new File(mojang, "jsons");
	public static File jsonMajor = new File(jsonDir, "major");
	public static File jsonMinor = new File(jsonDir, "minor");
	public static File jsonAssets = new File(jsonDir, "assets");
	public static File hashFile;
	public static PrintWriter hashWriter;
	public static int majorCount;
	public static int minorCount;
	public static int assetsCount;
	
	public static void main(String[] args)
	{
		args = SelfCommandPrompt.wrapWithCMD("input a command: ", appId, appName, args, false, true);
		System.out.println("starting:" + appName);
		try
		{
			long ms = System.currentTimeMillis();
			parseHashes();
			System.out.println("computed Hashes in:" + (System.currentTimeMillis() - ms) + "ms");
			Command.run(args.length == 0 ? new String[]{"checkMojang"} : args);
			System.out.println("Done in:" + (System.currentTimeMillis() - ms) / 1000D + " seconds" + " major:" + majorCount + " minor:" + minorCount + " assets:" + assetsCount);
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		hashWriter.close();
	}
	
	public static void checkCustom(boolean diskOnly) throws FileNotFoundException, IOException
	{
		if(!diskOnly)
			dlMojang();
		checkDisk();
	}
	
	public static void checkMojang() throws FileNotFoundException, IOException 
	{
		File major = dlMojang();
		Set<File> minors = checkMajor(major);
		Set<File> assets = new HashSet<>(minors.size());
		for(File minor : minors)
		{
			assets.add(checkMinor(minor));			
		}
		for(File asset : assets)
		{
			checkAssets(asset);
		}
	}

	public static void checkDisk() throws FileNotFoundException, IOException
	{
		List<File> majors = DeDuperUtil.getDirFiles(jsonMajor);
		for(File major : majors)
		{
			checkMajor(major);
		}
		List<File> minors = DeDuperUtil.getDirFiles(jsonMinor);
		for(File minor : minors)
		{
			checkMinor(minor);
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

	public static Set<File> checkMajor(File master) throws FileNotFoundException, IOException
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
			String[] urlArr = url.replace("\\", "/").split("/");
			//download the minor versions from the version_manifest.json
			String minorHash = urlArr[urlArr.length - 2].toLowerCase();
			String version = jsonVersion.getString("id");
			String type = jsonVersion.getString("type");
			String time = jsonVersion.getString("time");
			File minor = dl(url, jsonMinor + "/" + type + "/" + version + ".json", minorHash);
			minors.add(minor.getAbsoluteFile());
		}
		majorCount++;
		return minors;
	}

	public static File checkMinor(File version) throws FileNotFoundException, IOException 
	{
		if(!checkJsons.add(version.getAbsoluteFile()))
			return version;
		JSONObject json = RippedUtils.getJSON(version);
		String versionName = json.getString("id");
		String type = json.containsKey("type") ? json.getString("type") : DeDuperUtil.getTrueName(version.getParentFile());
		
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
			dl(dataUrl, new File(mojang, type + "/" + versionName + "/" + versionName + "-" + name).getPath(), dataSha1);
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

	private static void checkAssets(File assetsIndexFile) throws FileNotFoundException, IOException
	{
		if(!checkJsons.add(assetsIndexFile.getAbsoluteFile()))
			return;
		JSONObject objects = RippedUtils.getJSON(assetsIndexFile).getJSONObject("objects");
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
		hashFile = new File(root, "index.hash");
		if(!hashFile.exists())
			computeHashes(mcripped);
		else
			hashes = RippedUtils.parseHashFile(IOUtils.getReader(hashFile));
		RippedUtils.saveFileLines(hashes, hashFile, true);
		hashWriter = new PrintWriter(new BufferedWriter(new FileWriter(hashFile, true)), true);
	}
	
	private static void computeHashes(File dir)
	{
		System.out.println("Computing hashes This may take a while");
		hashes = RippedUtils.getHashes(dir);
	}
	
	public static void checkHashes() 
	{
		Iterator<Map.Entry<String, String>> it = hashes.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry<String, String> p = it.next();
			String h = p.getKey();
			String path = p.getValue();
			File f = new File(path);
			if(!f.exists() || !RippedUtils.getSHA1(f).equals(h))
			{
				System.err.println("file has been modified removing:" + path);
				it.remove();
				f.delete();
			}
		}
		RippedUtils.saveFileLines(hashes, hashFile, true);
	}
	
	public static void add(String hash, File output) 
	{
		hashes.put(hash, output.getPath());
    	hashWriter.println(hash + "," + DeDuperUtil.getRealtivePath(root, output));
	}
	
	/**
	 * get the default minecraft folder supports all os's
	 */
	public static File getMinecraftDir()
	{
		return new File(OSUtil.getAppData(), OSUtil.isMac() ? "minecraft" : ".minecraft");
	}

	private static File dlMojang() throws FileNotFoundException, IOException 
	{
		File master = dl("https://launchermeta.mojang.com/mc/game/version_manifest.json", new File(OSUtil.getAppData() + "/" + McRipper.appId, "mojang-versions.json").getPath(), "override");
		String sha1 = RippedUtils.getSHA1(master);
		return dl(master.toURI().toURL().toString(), new File(jsonMajor, "version_manifest.json").getPath(), sha1);
	}

	public static File dl(String url, String path, String hash) throws FileNotFoundException, IOException
	{
		return dl(url, path, System.currentTimeMillis(), hash);
	}
	
	public static File dlFromMC(String url, String path, String hash) throws FileNotFoundException, IOException
	{
		File minecraft = getMinecraftDir();
		File cached = new File(minecraft, DeDuperUtil.getRealtivePath(mojang, new File(path).getAbsoluteFile()));
		//dl will automatically handle libraries but, the rest has to be delt with by this method to prefer the disk
		return !path.contains("libraries") && cached.exists() ? dl(cached.toURI().toURL().toString(), path, hash) : dl(url, path, hash);
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
				File minecraft = getMinecraftDir();
				File cached = new File(minecraft, DeDuperUtil.getRealtivePath(mojang, new File(path).getAbsoluteFile()));
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
			System.out.println("downloaded:" + output + " in:" + (System.currentTimeMillis() - time) + "ms");
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

}
