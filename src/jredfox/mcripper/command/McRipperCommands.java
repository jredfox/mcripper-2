package jredfox.mcripper.command;

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
import jredfox.filededuper.util.IOUtils;
import jredfox.filededuper.util.JarUtil;
import jredfox.mcripper.printer.Printer;
import jredfox.mcripper.utils.DLUtils;
import jredfox.mcripper.utils.McChecker;
import jredfox.mcripper.utils.RippedUtils;

public class McRipperCommands {
	
	public static RipperCommand checkDisk = new RipperCommand(new String[]{"--mcDir=value", "--skipSnaps", "--skipOldMajors"}, "checkDisk")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			this.start(params);
			try 
			{
				McChecker.checkDisk(params.hasFlag("skipSnaps"), params.hasFlag("skipOldMajors"));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			this.finish();
		}
	};
	
	public static RipperCommand checkMojang = new RipperCommand(new String[]{"--mcDir=value", "--skipSnaps"}, "checkMojang")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			this.start(params);
			try 
			{
				McChecker.checkMojang(params.hasFlag("skipSnaps"));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			} 
			this.finish();
		}
	};
	
	public static RipperCommand checkOmni = new RipperCommand("checkOmni")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			McChecker.checkOmni();
		}
	};
	
	public static RipperCommand checkOld = new RipperCommand(new String[]{"--mcDir=value"}, "checkOld")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			this.start(params);
			McChecker.checkOldMc();
			this.finish();
		}
	};
	
	public static Command<File> rip = new Command<File>(new String[]{"-s", "-a", "--mcDir=value"}, "rip")
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
			File mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McChecker.mcDir;
			File dir = params.get(0);
			File jarFile = params.get(1);
			File rootOut = params.get(2);
			boolean skip = params.hasFlag('s');
			boolean ripAll = params.hasFlag('a');
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
						this.ripMinor(json, mcDir, out, ripAll);
					else
					{
						File jar = skip ? null : isFile ? jarFile : this.nextFile("input minecraft.jar that uses assetsIndex " + jsonFile.getName() + ":");
						this.ripAssetsIndex(jar, json, mcDir, out, ripAll);
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
			return json.containsKey("libraries");
		}

		public void ripMinor(JSONObject json, File mcDir, File outDir, boolean all) throws FileNotFoundException, IOException 
		{
			String type = json.getString("type");
			
			//fetch the assetsIndex json file
			JSONObject assetsLoc = json.getJSONObject("assetIndex");
			String idAssets = assetsLoc.getString("id");
			String sha1Assets = assetsLoc.getString("sha1");
			String urlAssets = assetsLoc.getString("url");
			File assetsIndexFile = DLUtils.getOrDlFromMc(mcDir, urlAssets, type, "assets/indexes/" + idAssets + ".json", sha1Assets);
			JSONObject assetsIndex = RippedUtils.getJSON(assetsIndexFile);
			
			//fetch the jar
			JSONObject downloads = json.getJSONObject("downloads");
			JSONObject client = downloads.getJSONObject("client");
			String idClient = json.getString("id");
			String sha1Client = client.getString("sha1");
			String urlClient = client.getString("url");
			File jar = DLUtils.getOrDlFromMc(mcDir, urlClient, type, "versions/" + idClient + "/" + idClient + ".jar", sha1Client);
			this.ripAssetsIndex(jar, assetsIndex, mcDir, outDir, all);
		}

		public void ripAssetsIndex(File jar, JSONObject json, File mcDir, File outDir, boolean all) throws ZipException, IOException 
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
					DLUtils.dlFromMc(mcDir, assetUrl, pathBase + hpath, new File(outDir, (isAssetRoot(key) ? "" : "assets/") + key).getAbsoluteFile(), assetSha1);
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
				List<ZipEntry> mcmetas = JarUtil.getEntriesFromDir(zip, "assets/", all ? "*" : "mcmeta");
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
	
	public static RipperCommand recomputeHashes = new RipperCommand("recomputeHashes")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				this.deletePrinter(McChecker.hash);
				this.deletePrinter(McChecker.bad);
				this.deletePrinter(McChecker.learner);
				McChecker.parseHashes();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		private void deletePrinter(Printer p) throws IOException 
		{
			p.close();
			p.log.delete();
		}
	};
	
	public static RipperCommand verify = new RipperCommand(new String[]{"--info"}, "verify")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				boolean delete = !params.hasFlag("info");
				boolean shouldSave = false;
				Iterator<Map.Entry<String, String>> it = McChecker.hash.hashes.entrySet().iterator();
				while(it.hasNext())
				{
					Map.Entry<String, String> p = it.next();
					String h = p.getKey();
					String path = p.getValue();
					File f = new File(McChecker.root, path);
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
				{
					IOUtils.close(McChecker.hash);
					McChecker.hash.save();
					McChecker.hash.setPrintWriter();
				}
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
