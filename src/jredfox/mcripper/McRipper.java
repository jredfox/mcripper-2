package jredfox.mcripper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
	public static final String version = "2-0.0.0";
	public static final String appName = "MC Ripper " + version;
	public static volatile Set<String> hashes;
	
	public static void main(String[] args) throws Exception
	{
		args = SelfCommandPrompt.runWithCMD(appId, appName, args, true, true);
		System.out.println("starting:" + appName);
		long ms = System.currentTimeMillis();
		File workingDir = new File(System.getProperty("user.dir"), "mcripped/mojang");
		parseHashes();
		System.out.println("computed Hashes in:" + (System.currentTimeMillis() - ms) + "ms");
		File master = dlMojang(workingDir);
		JSONObject mjson = RippedUtils.getJSON(master);
		JSONArray arr = (JSONArray) mjson.get("versions");
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
			checkVersion(workingDir, minorVersion);
		}
		System.out.println("saving hashes");
		saveHashes();
		System.out.println("Done in:" + (System.currentTimeMillis() - ms) / 1000L + " seconds");
	}
	
	public static void parseHashes()
	{
		File hashFile = new File(System.getProperty("user.dir"),"index.sha1");
		if(!hashFile.exists())
			computeHashes(hashFile.getParentFile());
		else
			hashes = RippedUtils.getFileLines(IOUtils.getReader(hashFile));
	}
	
	private static void computeHashes(File dir)
	{
		hashes = RippedUtils.getHashes(dir);
	}

	public static void saveHashes() 
	{
		File hashFile = new File(System.getProperty("user.dir"),"index.sha1");
		RippedUtils.saveFileLines(hashes, hashFile, true);
	}

	public static void checkVersion(File workingDir, File version) throws FileNotFoundException, IOException 
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
		JSONObject objects = RippedUtils.getJSON(assetsIndexFile).getJSONObject("objects");
		for(String key : objects.keySet())
		{
			JSONObject assetJson = objects.getJSONObject(key);
			String assetSha1 = assetJson.getString("hash");
			String assetSha1Char = assetSha1.substring(0, 2);
			String assetUrl = "https://resources.download.minecraft.net/" + assetSha1Char + "/" + assetSha1;
			dl(assetUrl, new File(workingDir, "assets/objects/" + assetSha1Char + "/" + assetSha1).getPath(), assetSha1);
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
		
		//TODO: download the libs
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
		if(hasHash && !hashes.add(hash))
			return output;
	    if(hasHash && output.exists())
	    	output = new File(output.getParent(), DeDuperUtil.getTrueName(output) + "-" + hash + DeDuperUtil.getExtensionFull(output));
		InputStream inputStream = new URL(url).openStream();
        output.getParentFile().mkdirs();
        IOUtils.copy(inputStream, new FileOutputStream(output));
        output.setLastModified(timestamp);
        System.out.println("downloaded:" + output + " in:" + (System.currentTimeMillis() - time) + "ms");
        return output;
	}

}
