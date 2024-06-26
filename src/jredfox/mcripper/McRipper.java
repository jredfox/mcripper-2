package jredfox.mcripper;

import java.io.File;

import com.jml.evilnotch.lib.JavaUtil;

import jredfox.common.os.OSUtil;
import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.Commands;
import jredfox.filededuper.config.simple.MapConfig;
import jredfox.mcripper.command.McRipperCommands;
import jredfox.mcripper.command.RipperCommand;
import jredfox.mcripper.utils.DLUtils;
import jredfox.mcripper.utils.McChecker;
import jredfox.selfcmd.SelfCommandPrompt;

public class McRipper {

	static
	{
		Command.get("");
		Command.cmds.clear();
		Command.cmds.put("help", Commands.help);
		McRipperCommands.load();
	}
	
	public static final String appId = "Mcripper";
	public static final String version = "1.0.0";
	public static final String appName = "MC Ripper 2 Build: " + version;
	
	public static void main(String[] args) throws Exception
	{
		args = SelfCommandPrompt.wrapWithCMD("input a command: ", appId, appName, args, true, true);
		loadCfg(args);
		args = args.length == 0 ? new String[]{"help"} : args;
		Command<?> cmd = Command.fromArgs(args);
		if(shouldParseHashes(cmd))
			McChecker.parseHashes();
		cmd.run();
		McChecker.closePrinters();
	}
	
	public static boolean shouldParseHashes(Command<?> cmd)
	{
		return cmd instanceof RipperCommand && cmd != McRipperCommands.recomputeHashes && cmd != McRipperCommands.rip && !McChecker.loaded;
	}
	
	public static void loadCfg(String[] args) throws Exception
	{
		File appdir = new File(OSUtil.getAppData(), McRipper.appId);
		MapConfig cfg = new MapConfig(new File(System.getProperty("user.dir"), McRipper.appId + ".cfg"));
		cfg.load();
		appdir = new File(cfg.get(McRipper.appId + "Dir", appdir.getPath())).getAbsoluteFile();
		DLUtils.https = cfg.get("alwaysHTTPS", true);
		cfg.save();
		
		//sanity check for custom appdir
		if(!appdir.exists() && !appdir.mkdirs())
			throw new RuntimeException("appdata \"" + appdir + "\" doesn't exist and cannot be created. Please reconfigure Mc Ripper 2 to a valid path!");
		
		McChecker.setRoot(appdir);
		System.out.println("starting:" + appName + " args:" + JavaUtil.asStringList(args));
	}

}
