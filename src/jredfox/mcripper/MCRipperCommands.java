package jredfox.mcripper;

import java.io.File;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.ParamList;

public class MCRipperCommands {
	
	public static RunableCommand checkCustom = new RunableCommand(new String[]{"--mcDir=value", "--diskOnly", "--skipSnaps"}, "checkCustom")
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
				McRipper.hashFile.delete();
				McRipper.parseHashes();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			} 
		}
	};
	
	public static RunableCommand checkSelfIntegrity = new RunableCommand("checkSelfIntegrity")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			try 
			{
				McRipper.checkHashes();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			} 
		}
	};

	public static void load() {}

}
