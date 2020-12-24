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
import java.util.HashSet;
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
	}
	
	public static final String appId = "mcripper";
	public static final String version = "0.0.1-alpha";
	public static final String appName = "MC Ripper 2:" + version;
	public static volatile Map<String, String> hashes;
	public static File root = new File(System.getProperty("user.dir"));
	public static File mcripped = new File(root, "mcripped");
	public static File hashFile;
	public static PrintWriter hashWriter;
	
	public static void main(String[] args)
	{
		args = SelfCommandPrompt.runWithCMD(appId, appName, args, true, true);
		System.out.println("starting:" + appName);
		long ms = System.currentTimeMillis();
		try
		{
		File workingDir = new File(mcripped, "mojang");
		parseHashes();
		System.out.println("computed Hashes in:" + (System.currentTimeMillis() - ms) + "ms");
		File master = dlMojang(workingDir);
		JSONObject mjson = RippedUtils.getJSON(master);
		JSONArray arr = (JSONArray) mjson.get("versions");
		Set<File> checkFiles = new HashSet<>(30);
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
			File minorVersion = dl(url, workingDir.getPath() + "/" + type + "/" + version + "/" + version + ".json", minorHash);
			//check the minor version jsons
			checkVersion(checkFiles, workingDir, minorVersion);
		}
		System.out.println("saving hashes");
		saveHashes();
		System.out.println("Done in:" + (System.currentTimeMillis() - ms) / 1000D + " seconds");
		}
		catch(Throwable t)
		{
			t.printStackTrace();
			saveHashes();
		}
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
		hashes = RippedUtils.getHashes(dir);
	}

	public static void saveHashes() 
	{
		hashWriter.close();
	}

	public static void checkVersion(Set<File> checkedAssets, File workingDir, File version) throws FileNotFoundException, IOException 
	{
		JSONObject json = RippedUtils.getJSON(version);
		String versionName = json.getString("id");
		
		//download the asset indexes
		JSONObject assetsIndex = json.getJSONObject("assetIndex");
		String id = assetsIndex.getString("id");
		String sha1 = assetsIndex.getString("sha1").toLowerCase();
		String url = assetsIndex.getString("url");
		File assetsIndexFile = dl(url, new File(workingDir, "assets/indexes/" + id + ".json").getPath(), sha1);
		
		//download the assetsIndex data
		if(!checkedAssets.contains(assetsIndexFile))
		{
			JSONObject objects = RippedUtils.getJSON(assetsIndexFile).getJSONObject("objects");
			for(String key : objects.keySet())
			{
				JSONObject assetJson = objects.getJSONObject(key);
				String assetSha1 = assetJson.getString("hash");
				String assetSha1Char = assetSha1.substring(0, 2);
				String assetUrl = "https://resources.download.minecraft.net/" + assetSha1Char + "/" + assetSha1;
				dl(assetUrl, new File(workingDir, "assets/objects/" + assetSha1Char + "/" + assetSha1).getPath(), assetSha1);
			}
			checkedAssets.add(assetsIndexFile);
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
				dl(logUrl, new File(workingDir, "assets/log_configs/" + logId).getPath(), logSha1);
			}
		}
		else
		{
//			System.out.println("missing logging:" + version);
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
			dl(dataUrl, new File(version.getParentFile(), versionName + "-" + name).getPath(), dataSha1);
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
				String libName = artifact.getString("path");
				String libSha1 = artifact.getString("sha1");
				String libUrl = artifact.getString("url");
				dl(libUrl, new File(workingDir, "libraries/" + libName).getPath(), libSha1);
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
					dl(clUrl, new File(workingDir, "libraries/" + clPath).getPath(), clSha1);
				}
			}
		}
	}
	
	/**
	 * get the default minecraft folder supports all os's
	 */
	public static File getMinecraftFolder()
	{
		return new File(OSUtil.getAppData(), OSUtil.isMac() ? "minecraft" : ".minecraft");
	}

	private static File dlMojang(File dir) throws FileNotFoundException, IOException 
	{
		return dl("https://launchermeta.mojang.com/mc/game/version_manifest.json", new File(dir, "version_manifest.json").getPath(), "override");
	}

	public static File dl(String url, String path, String hash) throws FileNotFoundException, IOException
	{
		return dl(url, path, System.currentTimeMillis(), hash);
	}
	
	/**
	 * download a file to the path specified. With timestamp and hashing support. 
	 * The hash is in case the file destination already exists. To allow override pass "override" as the hash
	 */
	public static File dl(String url, String path, long timestamp, String hash) throws FileNotFoundException, IOException, IllegalArgumentException
	{
		if(hash == null)
			throw new IllegalArgumentException("hash cannot be null!");
		long time = System.currentTimeMillis();
		boolean hasHash = !hash.equals("override");
	    File output = OSUtil.toWinFile(new File(path.replaceAll("%20", " "))).getAbsoluteFile();
	    if(hasHash)
	    {	
	    	if(hashes.containsKey(hash))
	    		return new File(hashes.get(hash));
	    	else if(output.exists())
	    	{
	    		//prevent duplicate downloads
	    		String tocheckHash = RippedUtils.getSHA1(output);
	    		if(hash.equals(tocheckHash))
	    		{
	    			System.err.println("Skipping duplicate file:" + output);
	    			add(hash, output);
	    			return output;
	    		}
		    	output = new File(output.getParent(), DeDuperUtil.getTrueName(output) + "-" + hash + DeDuperUtil.getExtensionFull(output));
		    	
		    	//if the hashed version exists on the disk skip the download
		    	if(output.exists())
		    		return output;
	    	}
	    	add(hash, output);
	    }
		InputStream inputStream = new URL(url).openStream();
        output.getParentFile().mkdirs();
        IOUtils.copy(inputStream, new FileOutputStream(output));
        output.setLastModified(timestamp);
        System.out.println("downloaded:" + output + " in:" + (System.currentTimeMillis() - time) + "ms");
        return output;
	}

	public static void add(String hash, File output) 
	{
		hashes.put(hash, output.getPath());
    	hashWriter.println(hash + "," + DeDuperUtil.getRealtivePath(root, output));
	}

}
