package jredfox.mcripper;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.ParamList;

public class MCRipperCommands {
	
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
	
	public static Command<File> rip = new Command<File>(new String[]{"--mcDir=value"}, "rip")
	{
		@Override
		public String[] displayArgs() 
		{
			return new String[]{"assetsIndex.json"};
		}

		@Override
		public File[] parse(String... inputs)
		{
			return new File[]{new File(inputs[0])};
		}

		@Override
		public void run(ParamList<File> params) 
		{
			//TODO:
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
	
	public static RunableCommand verify = new RunableCommand("verify")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				boolean shouldSave = false;
				Iterator<Map.Entry<String, String>> it = McRipper.hashes.entrySet().iterator();
				while(it.hasNext())
				{
					Map.Entry<String, String> p = it.next();
					String h = p.getKey();
					String path = p.getValue();
					File f = new File(McRipper.root, path);
					if(!f.exists() || !h.equals(RippedUtils.getUnsafeHash(f)))
					{
						System.err.println("file has been modified removing:" + path + " " + h + ", " + RippedUtils.getUnsafeHash(f));
						it.remove();
						f.delete();
						shouldSave = true;
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
