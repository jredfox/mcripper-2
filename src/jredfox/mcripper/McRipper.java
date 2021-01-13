package jredfox.mcripper;

import java.io.File;
import java.io.IOException;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.CommandInvalid;
import jredfox.filededuper.command.Commands;
import jredfox.filededuper.config.simple.MapConfig;
import jredfox.mcripper.command.McRipperCommands;
import jredfox.mcripper.utils.McChecker;
import jredfox.selfcmd.SelfCommandPrompt;
import jredfox.selfcmd.util.OSUtil;

public class McRipper {
	
	static
	{
		Command.get("");
		Command.cmds.clear();
		Command.cmds.put("help", Commands.help);
		McRipperCommands.load();
	}
	
	public static final String appId = "Mcripper";
	public static final String version = "rc.2-nightly-1-13-2021-01:54:48.Z";
	public static final String appName = "MC Ripper 2 Build: " + version;
	
	public static void main(String[] args) throws Exception
	{
		args = SelfCommandPrompt.wrapWithCMD("input a command: ", appId, appName, args, true, true);
		System.out.println("starting:" + appName);
		loadCfg();
		args = args.length == 0 ? new String[]{"rip"} : args;
		Command<?> cmd = Command.fromArgs(args);
		if(!(cmd instanceof CommandInvalid) && cmd != McRipperCommands.recomputeHashes)
			McChecker.parseHashes();
		cmd.run();
		McChecker.closePrinters();
	}
	
	public static void loadCfg() throws IOException
	{
		File appdir = new File(OSUtil.getAppData(), McRipper.appId);
		MapConfig cfg = new MapConfig(new File(System.getProperty("user.dir"), McRipper.appId + ".cfg"));
		cfg.load();
		appdir = new File(cfg.get(McRipper.appId + "Dir", appdir.getPath())).getAbsoluteFile();
		cfg.save();
		McChecker.setRoot(appdir);
	}

}
