package jredfox.mcripper;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import com.jml.evilnotch.lib.json.JSONObject;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.ParamList;
import jredfox.filededuper.util.DeDuperUtil;

public class McRipperCommands {
	
	public static RunableCommand checkDisk = new RunableCommand(new String[]{"--mcDir=value", "--skipSnaps", "--skipOldMajors", "--forceDlCheck"}, "checkDisk")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				McRipper.mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
				McRipper.checkDisk(params.hasFlag("skipSnaps"), params.hasFlag("skipOldMajors"), params.hasFlag("forceDlCheck"));
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
	
	public static RunableCommand checkOld = new RunableCommand(new String[]{"--forceDlCheck","--mcDir=value"}, "checkOld")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			McRipper.mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
			McRipper.checkOldMc(params.hasFlag("forceDlCheck"));
			McRipper.mcDir = McRipper.mcDefaultDir;
		}
	};
	
	public static Command<File> rip = new Command<File>(new String[]{"--mcDir=value"}, "rip")
	{
		@Override
		public String[] displayArgs() 
		{
			return new String[]{"assetsIndex.json", "assetsIndex.json & outputDir", "assetsIndex.json & minecraft.jar & outputDir"};
		}

		@Override
		public File[] parse(String... inputs)
		{
			if(this.hasScanner(inputs))
			{
				File assetsIndex = this.nextFile("input the assetsIndex.json:");
				File outDir = this.nextFile("input the directory of the output:");
				return new File[]{assetsIndex, outDir};
			}
			return new File[]{new File(inputs[0]), new File(inputs[1])};
		}

		@Override
		public void run(ParamList<File> params) 
		{
			long ms = System.currentTimeMillis();
			File assetsIndex = params.get(0);
			File outDir = new File(((File)params.get(1)).getPath(), DeDuperUtil.getTrueName(assetsIndex));
			File mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
			
			JSONObject json = RippedUtils.getJSON(assetsIndex);
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
					McRipper.dlFromMc(McRipper.mojang, mcDir, assetUrl, pathBase + hpath, new File(outDir, (isAssetRoot(key) ? "" : "assets/") + key).getAbsoluteFile(), assetSha1);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			System.out.println("completed ripping assets in:" + (System.currentTimeMillis() - ms) / 1000D + " seconds");
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
