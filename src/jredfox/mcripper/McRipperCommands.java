package jredfox.mcripper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.jml.evilnotch.lib.json.JSONObject;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.ParamList;
import jredfox.filededuper.command.exception.CommandParseException;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.JarUtil;

public class McRipperCommands {
	
	public static RunableCommand checkDisk = new RunableCommand(new String[]{"--mcDir=value", "--skipSnaps", "--skipOldMajors"}, "checkDisk")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				McRipper.mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
				McRipper.checkDisk(params.hasFlag("skipSnaps"), params.hasFlag("skipOldMajors"));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			McRipper.checkJsons.clear();
			McRipper.mcDir = McRipper.mcDefaultDir;
		}
	};
	
	public static RunableCommand checkMojang = new RunableCommand(new String[]{"--mcDir=value", "--skipSnaps"}, "checkMojang")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				McRipper.mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
				McRipper.checkMojang(params.hasFlag("skipSnaps"));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			} 
			McRipper.checkJsons.clear();
			McRipper.mcDir = McRipper.mcDefaultDir;
		}
	};
	
	public static RunableCommand checkOmni = new RunableCommand("checkOmni")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			McRipper.checkOmni();
		}
	};
	
	public static RunableCommand checkOld = new RunableCommand(new String[]{"-r","--mcDir=value"}, "checkOld")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			McRipper.mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
			McRipper.checkOldMc(params.hasFlag('r'));
			McRipper.mcDir = McRipper.mcDefaultDir;
		}
	};
	
	public static Command<File> rip = new Command<File>(new String[]{"-s", "--mcDir=value"}, "rip")
	{
		@Override
		public String[] displayArgs() 
		{
			return new String[]{"dir/version.json/assetsIndex.json & outputDir", "assetsIndex.json & minecraft.jar & outputDir"};
		}

		@Override
		public File[] parse(String... inputs)
		{
			if(this.hasScanner(inputs))
			{
				File jsonFile = this.nextFile("input the dir/version.json/assetsIndex.json:");
				File jarFile = jsonFile.isDirectory() || this.isMinor(RippedUtils.getJSON(jsonFile)) ? null : this.nextFile("input minecraft.jar:");
				File outDir = this.nextFile("input the directory of the output:");
				return new File[]{jsonFile, jarFile, outDir};
			}
			boolean hasJar = inputs.length == 3;
			File jsonFile = new File(inputs[0]);
			File jarFile =  hasJar ? new File(inputs[1]) : null;
			File outDir = new File(inputs[hasJar ? 2 : 1]);
			if(jarFile != null && jsonFile.isDirectory())
				throw new CommandParseException("illegal state minecraft.jar cannot be assigned to a directory of assetsIndexes!");
			return new File[]{jsonFile, jarFile, outDir};
		}

		@Override
		public void run(ParamList<File> params) 
		{
			long ms = System.currentTimeMillis();
			File mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
			File dir = params.get(0);
			File jarFile = params.get(1);
			File rootOut = params.get(2);
			boolean skip = params.hasFlag('s');
			boolean isFile = !dir.isDirectory();
			if(skip)
				System.out.println("WARNING: assetsIndex.json extraction will be missing .mcmetafiles which will break if you apply this to an rp pack");
			List<File> files = DeDuperUtil.getDirFiles(dir, "json");
			for(File jsonFile : files)
			{
				JSONObject json = RippedUtils.getJSON(jsonFile);
				File out = new File(rootOut, DeDuperUtil.getTrueName(jsonFile));
				try
				{
					if(this.isMinor(json))
						this.ripMinor(json, mcDir, out);
					else
					{
						File jar = skip ? null : isFile ? jarFile : this.nextFile("input minecraft.jar that uses assetsIndex " + jsonFile.getName() + ":");
						this.ripAssetsIndex(jar, json, mcDir, out);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			System.out.println("completed ripping assets in:" + (System.currentTimeMillis() - ms) / 1000D + " seconds");
		}

		private boolean isMinor(JSONObject json) 
		{
			return json.containsKey("assetIndex");
		}

		public void ripMinor(JSONObject json, File mcDir, File outDir) throws FileNotFoundException, IOException 
		{
			String type = json.getString("type");
			
			//fetch the assetsIndex json file
			JSONObject assetsLoc = json.getJSONObject("assetIndex");
			String idAssets = assetsLoc.getString("id");
			String sha1Assets = assetsLoc.getString("sha1");
			String urlAssets = assetsLoc.getString("url");
			File assetsIndexFile = McRipper.getOrDlFromMc(mcDir, urlAssets, type, "assets/indexes/" + idAssets + ".json", sha1Assets);
			JSONObject assetsIndex = RippedUtils.getJSON(assetsIndexFile);
			
			//fetch the jar
			JSONObject downloads = json.getJSONObject("downloads");
			JSONObject client = downloads.getJSONObject("client");
			String idClient = json.getString("id");
			String sha1Client = client.getString("sha1");
			String urlClient = client.getString("url");
			File jar = McRipper.getOrDlFromMc(mcDir, urlClient, type, "versions/" + idClient + "/" + idClient + ".jar", sha1Client);
			this.ripAssetsIndex(jar, assetsIndex, mcDir, outDir);
		}

		public void ripAssetsIndex(File jar, JSONObject json, File mcDir, File outDir) throws ZipException, IOException 
		{
			System.out.println("ripping assetsIndex to: " + outDir);
			JSONObject objects = json.getJSONObject("objects");
			String pathBase = "assets/objects/";
			for(String key : objects.keySet())
			{
				JSONObject assetJson = objects.getJSONObject(key);
				String assetSha1 = assetJson.getString("hash");
				String assetSha1Char = assetSha1.substring(0, 2);
				String hpath = assetSha1Char + "/" + assetSha1;
				String assetUrl = "https://resources.download.minecraft.net/" + hpath;
				try
				{
					McRipper.dlFromMc(mcDir, assetUrl, pathBase + hpath, new File(outDir, (isAssetRoot(key) ? "" : "assets/") + key).getAbsoluteFile(), assetSha1);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			
			if(jar != null)
			{
				System.out.println("extracting missing files");
				ZipFile zip = new ZipFile(jar);
				List<ZipEntry> mcmetas = JarUtil.getEntriesFromDir(zip, "assets/", "mcmeta");
				for(ZipEntry mcmeta : mcmetas)
				{
					String pathMeta = mcmeta.getName();
					File file = new File(outDir, pathMeta);
					if(!file.exists())
					{
						JarUtil.unzip(zip, mcmeta, file);
						System.out.println("extracted:" + pathMeta + " to:" + file);
					}
				}
				zip.close();
			}
		}
	};
	
	public static boolean isAssetRoot(String key) 
	{
		return key.equals("pack.mcmeta") || key.equals("pack.png");
	}
	
	public static RunableCommand recomputeHashes = new RunableCommand("recomputeHashes")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				if(McRipper.hashWriter != null)
					McRipper.hashWriter.close();
				if(McRipper.hashFile.exists())
					McRipper.hashFile.delete();
				McRipper.parseHashes();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	};
	
	public static RunableCommand verify = new RunableCommand(new String[]{"--info"}, "verify")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				boolean delete = !params.hasFlag("info");
				boolean shouldSave = false;
				Iterator<Map.Entry<String, String>> it = McRipper.hashes.entrySet().iterator();
				while(it.hasNext())
				{
					Map.Entry<String, String> p = it.next();
					String h = p.getKey();
					String path = p.getValue();
					File f = new File(McRipper.root, path);
					if(!h.equals(RippedUtils.getSHA1(f)))
					{
						System.err.println("file has been modified removing:" + path);
						if(delete)
						{
							it.remove();
							f.delete();
							shouldSave = true;
						}
					}
				}
				if(shouldSave)
					RippedUtils.saveFileLines(McRipper.hashes, McRipper.hashFile, true);
				else
					System.out.println("All files have been verified with no errors");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	};

	public static void load() {}

}
