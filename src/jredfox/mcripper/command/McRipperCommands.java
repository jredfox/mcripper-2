package jredfox.mcripper.command;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.jml.evilnotch.lib.json.JSONObject;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.CommandOption;
import jredfox.filededuper.command.ParamList;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.JarUtil;
import jredfox.mcripper.utils.McChecker;
import jredfox.mcripper.utils.RippedUtils;
import jredfox.selfcmd.SelfCommandPrompt;

public class McRipperCommands {
	
	private static final String skipSnaps = "skipSnaps";
	private static final String mcDir = "mcDir=value";
	protected static final char clear = 'c';
	protected static final String lboarder = "\n#############################################################################\n#############################################################################\n";
	protected static final String rboarder = "\n#############################################################################\n#############################################################################";
	
	public static RipperCommand checkAll = new RipperCommand(new String[]{"--" + mcDir, "--" + skipSnaps, "-" + clear}, "checkAll")
	{
		@Override
		public void run(ParamList<Object> params) 
		{
			long start = System.currentTimeMillis();
			this.start(params);
			params.options.add(new CommandOption("--internal"));
			System.out.println("CHECKING MOJANG:");
			McRipperCommands.checkMojang.run(params);
			System.out.println("CHECKING THE DISK FOR CUSTOM JSONS:");
			McRipperCommands.checkDisk.run(params);
			System.out.println("CHECKING OLD(LEGACY) MINECRAFT DOMAINS:");
			McRipperCommands.checkOld.run(params);
			System.out.println("CHECKING OMNI-ARCHIVE:");
			McRipperCommands.checkOmni.run(params);
			this.finish(params);
			System.out.println(lboarder + "Finished checkAll in:" + (System.currentTimeMillis() - start) / 1000D + " seconds" + (McChecker.oldMajorCount > 0 ? " oldMajor:" + McChecker.oldMajorCount : "") + " major:" + McChecker.majorCount + (McChecker.oldMinor > 0 ? " oldMinor:" + McChecker.oldMinor : "") + " minor:" + McChecker.minorCount + " assets:" + McChecker.assetsCount + rboarder);
		}
		
		@Override
		public void start(ParamList<?> params)
		{
			this.clearLearners(params);
		}
		
		@Override
		public void finish(ParamList<?> params)
		{
			this.clear(params);
		}
	};
	
	public static RipperCommand checkDisk = new RipperCommand(new String[]{"--" + mcDir, "--" + skipSnaps, "-" + clear}, "checkDisk")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			this.start(params);
			try 
			{
				McChecker.checkDisk(params.hasFlag("skipSnaps"));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			this.finish(params);
		}
	};
	
	public static RipperCommand checkMojang = new RipperCommand(new String[]{"--" + mcDir, "--" + skipSnaps, "-" + clear}, "checkMojang")
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
			this.finish(params);
		}
	};
	
	public static RipperCommand checkOmni = new RipperCommand(new String[]{"-" + clear}, "checkOmni")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			this.start(params);
			McChecker.checkOmni();
			this.finish(params);
		}
	};
	
	public static RipperCommand checkOld = new RipperCommand(new String[]{"--" + mcDir, "--" + skipSnaps, "-" + clear}, "checkOld")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			this.start(params);
			McChecker.checkOldMc(params.hasFlag("skipSnaps"));
			this.finish(params);
		}
	};
	
	public static Command<File> rip = new Command<File>(new Object[]{"-s", new CommandOption(new String[]{"-s"}, "-a"), "--" + mcDir}, "rip")
	{
		@Override
		public String[] displayArgs() 
		{
			return new String[]{"dir/version.json/assetsIndex.json & outputDir", "assetsIndex.json & minecraft.jar & outputDir"};
		}

		@Override
		public File[] parse(ParamList<File> paramOptions, String... inputs)
		{
			boolean skip = paramOptions.hasFlag('s');
			if(this.hasScanner(inputs))
			{
				File jsonFile = this.nextFile("input the dir/version.json/assetsIndex.json:");
				File jarFile = skip || jsonFile.isDirectory() || this.isMinor(RippedUtils.getJSON(jsonFile)) ? null : this.nextFile("input minecraft.jar:");
				File outDir = this.nextFile("input the directory of the output:");
				return new File[]{jsonFile, jarFile, outDir};
			}
			
			File jsonFile = new File(inputs[0]);
			File jarFile = skip || jsonFile.isDirectory() || this.isMinor(RippedUtils.getJSON(jsonFile)) ? null : new File(inputs[1]);
			File outDir = new File(inputs[jarFile != null ? 2 : 1]);
			return new File[]{jsonFile, jarFile, outDir};
		}

		@Override
		public void run(ParamList<File> params) 
		{
			long ms = System.currentTimeMillis();
			File mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McChecker.am.cachedDir;
			File dir = params.get(0);
			File jarFile = params.get(1);
			File rootOut = ((File) params.get(2)).getAbsoluteFile();
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
			//fetch the assetsIndex json file
			JSONObject assetsLoc = json.getJSONObject("assetIndex");
			String idAssets = assetsLoc.getString("id");
			String sha1Assets = assetsLoc.getString("sha1");
			String urlAssets = assetsLoc.getString("url");
			String assetsPath = "assets/indexes/" + idAssets + ".json";
			File assetsIndexFile = RippedUtils.getOrDlFromMc(mcDir, urlAssets, assetsPath, sha1Assets).file;
			JSONObject assetsIndex = RippedUtils.getJSON(assetsIndexFile);
			
			//fetch the jar
			JSONObject downloads = json.getJSONObject("downloads");
			JSONObject client = downloads.getJSONObject("client");
			String idClient = json.getString("id");
			String sha1Client = client.getString("sha1");
			String urlClient = client.getString("url");
			String jarPath = "versions/" + idClient + "/" + idClient + ".jar";
			File jar = RippedUtils.getOrDlFromMc(mcDir, urlClient, jarPath, sha1Client).file;
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
				String twoChar = assetSha1.substring(0, 2);
				String hpath = twoChar + "/" + assetSha1;
				String assetUrl = "https://resources.download.minecraft.net/" + hpath;
				RippedUtils.dlFromMc(mcDir, assetUrl, new File(outDir, (isAssetRoot(key) ? "" : "assets/") + key).getAbsoluteFile(), pathBase + hpath, assetSha1);
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
	
	public static Command<Object> recomputeHashes = new RipperCommand(new String[]{"-" + clear}, "recomputeHashes", "computeHashes")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			this.start(params);
			try 
			{
				McChecker.am.computeHashes();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			this.finish(params);
		}
		
		@Override
		public void print()
		{
			McRipperCommands.printDefault(this.ms);
		}
	};
	
	public static RipperCommand verify = new RipperCommand(new String[]{"--info"}, "verify")
	{
		@Override
		public void run(ParamList<Object> params)
		{
			this.start(params);
			try 
			{
				McChecker.am.printer.verify(!params.hasFlag("info"));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			this.finish(params);
		}
		
		@Override
		public void start(ParamList<?> params)
		{
			this.ms = System.currentTimeMillis();
		}
		
		@Override
		public void print()
		{
			McRipperCommands.printDefault(this.ms);
		}
	};
	
	public static Command<File> hasher = new Command<File>("hasher")
	{
		@Override
		protected void register() 
		{
			if(!SelfCommandPrompt.isCompiled())
				super.register();//do not allow users to use this command unless they are in a dev enviorment
		}

		@Override
		public String[] displayArgs() 
		{
			return new String[]{"Dir/File"};
		}

		@Override
		public File[] parse(ParamList<File> params, String... inputs) 
		{
			return new File[]{this.hasScanner(inputs) ? this.nextFile("Input the Directory to Hash:") : DeDuperUtil.newFile(inputs[0])};
		}

		@Override
		public void run(ParamList<File> params)
		{
			File dir = params.get(0);
			File hdir = new File(dir.getParent(), "hashed-" + DeDuperUtil.getTrueName(dir));
			hdir.mkdirs();
			List<File> files = DeDuperUtil.getDirFiles(dir);
			for(File f : files)
			{
				try
				{
					RippedUtils.copy(f, new File(hdir, RippedUtils.getSHA1(f) + DeDuperUtil.getExtensionFull(f)));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	};
	
	public static void printDefault(long ms)
	{
		System.out.println(McRipperCommands.lboarder + "Done in:" + (System.currentTimeMillis() - ms) / 1000D + " seconds" + McRipperCommands.rboarder);
	}

	/**
	 * enforces that the static anomous command classes are instantiated
	 */
	public static void load() {}

}
