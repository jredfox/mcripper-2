package jredfox.mcripper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jml.evilnotch.lib.JavaUtil;
import com.jml.evilnotch.lib.json.JSONArray;
import com.jml.evilnotch.lib.json.JSONObject;
import com.jml.evilnotch.lib.json.serialize.JSONSerializer;

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
	public static volatile Set<String> hashes = new HashSet<>();
	
	public static void main(String[] args) throws Exception
	{
		args = SelfCommandPrompt.runWithCMD(appId, appName, args, true, true);
		long ms = System.currentTimeMillis();
		File workingDir = new File(System.getProperty("user.dir"), "mcripped/mojang");
		hashes = getHashes(workingDir);
		System.out.println("computed Hashes in:" + (System.currentTimeMillis() - ms) + "ms");
		File master = dlMojang(workingDir);
		JSONObject mjson = getJSON(master);
		JSONArray arr = (JSONArray) mjson.get("versions");
		for(Object obj : arr)
		{
			JSONObject jsonVersion = (JSONObject)obj;
			String url = jsonVersion.getString("url");
			String[] urlArr = url.replace("\\", "/").split("/");
			String minorHash = urlArr[urlArr.length - 2].toLowerCase();
			if(hashes.contains(minorHash))
			{
				continue;
			}
			String version = jsonVersion.getString("id");
			String type = jsonVersion.getString("type");
			String time = jsonVersion.getString("time");
			File minorVersion = dl(url, workingDir.getPath() + "/" + type + "/" + version + "/" + version + ".json", minorHash);
			hashes.add(getSHA1(minorVersion));
		}
		System.out.println("Done in:" + (System.currentTimeMillis() - ms) / 1000L + " seconds");
	}
	
	public static Set<String> getHashes(File dir)
	{
		if(!dir.exists())
			return new HashSet<>(0);
		List<File> files = DeDuperUtil.getDirFiles(dir);
		Set<String> hashes = new HashSet<>(files.size());
		for(File f : files)
			hashes.add(getSHA1(f));
		return hashes;
	}

	public static String getSHA1(File f) 
	{
		return DeDuperUtil.getSHA1(f).toLowerCase();
	}

	public static JSONObject getJSON(File file)
	{
		if(!file.exists())
			return null;
		try
		{
			JSONSerializer parser = new JSONSerializer();
			return parser.readJSONObject(JavaUtil.getReader(file));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
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
	    File output = OSUtil.toWinFile(new File(path.replaceAll("%20", " "))).getAbsoluteFile();
	    if(output.exists() && !hash.equals("override"))
	    	output = new File(output.getParent(), DeDuperUtil.getTrueName(output) + "-" + hash + DeDuperUtil.getExtension(output));
		InputStream inputStream = new URL(url).openStream();
        output.getParentFile().mkdirs();
        IOUtils.copy(inputStream, new FileOutputStream(output));
        output.setLastModified(timestamp);
        System.out.println("downloaded:" + output + " in:" + (System.currentTimeMillis() - time) + "ms");
        return output;
	}

}
