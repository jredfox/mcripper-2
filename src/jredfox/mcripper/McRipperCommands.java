package jredfox.mcripper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.jml.evilnotch.lib.json.JSONObject;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.ParamList;

public class McRipperCommands {
	
	public static RunableCommand checkDisk = new RunableCommand(new String[]{"--mcDir=value", "--diskOnly", "--skipSnaps"}, "checkDisk")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				McRipper.mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
				McRipper.checkCustom(params.hasFlag("diskOnly"), params.hasFlag("skipSnaps"));
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
	
	public static RunableCommand checkOldMc = new RunableCommand(new String[]{"--mcDir=value"}, "checkOldMc", "checkOld")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			McRipper.mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McRipper.mcDir;
			this.dlOldMC();
			McRipper.mcDir = McRipper.mcDefaultDir;
		}

		public void dlOldMC()
		{
			try
			{
				McRipper.dlAmazonAws("http://s3.amazonaws.com/MinecraftDownload", "MinecraftDownload");
				McRipper.dlAmazonAws("http://s3.amazonaws.com/MinecraftResources", "MinecraftResources");
				McRipper.dlOldVersions();
			}
			catch(Exception e) 
			{
				e.printStackTrace();
			}
		}
	};
	
	public static Command<File> rip = new Command<File>(new String[]{"--mcDir=value"}, "rip")
	{
		@Override
		public String[] displayArgs() 
		{
			return new String[]{"assetsIndex.json & outputDir"};
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
			File assetsIndex = params.get(0);
			File outDir = params.get(1);
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
					McRipper.dlFromMc(mcDir, assetUrl, pathBase + hpath, new File(outDir, key).getAbsoluteFile(), assetSha1);
				} 
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	};
	
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
