package jredfox.mcripper.command;

import java.io.File;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.ParamList;
import jredfox.filededuper.util.IOUtils;
import jredfox.mcripper.utils.McChecker;
import jredfox.mcripper.utils.RippedUtils;

public abstract class RipperCommand extends Command<Object>{
	
	public long ms;
	
	public RipperCommand(String[] options, String... ids)
	{
		super(options, ids);
	}
	
	public RipperCommand(String... ids)
	{
		super(ids);
	}

	@Override
	public String[] displayArgs() {
		return new String[]{""};
	}

	@Override
	public Object[] parse(ParamList<Object> optionAcess, String... inputs) {
		return null;
	}
	
	public void start(ParamList<?> params)
	{
		this.ms = System.currentTimeMillis();
		McChecker.am.cachedDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McChecker.am.cachedDir;
		if(params.hasFlag("internal"))
			return;
		this.clearLearners(params);
	}
	
	public void finish(ParamList<?> params)
	{
		if(params.hasFlag("internal"))
			return;
		this.clear(params);
		this.print(params);
	}
	
	public void print(ParamList<?> params) 
	{
		String msg = this.getDoneMsg(params);
		int size = msg.length();
		String fill = RippedUtils.fillString("#", size + 10);
		String lboarder = "\n" + fill + "\n" + fill + "\n";
		String rboarder = "\n" + fill + "\n" + fill;
		System.out.println(lboarder + msg + rboarder);
	}
	
	public String getDoneMsg(ParamList<?> params)
	{
		return "Finished " + this.name + (params.options.isEmpty() ? "" : " " + params.options) + " in:" + (System.currentTimeMillis() - this.ms) / 1000D + " seconds" + (McChecker.oldMajorCount > 0 ? " oldMajor:" + McChecker.oldMajorCount : "") + " major:" + McChecker.majorCount + (McChecker.oldMinor > 0 ? " oldMinor:" + McChecker.oldMinor : "") + " minor:" + McChecker.minorCount + " assets:" + McChecker.assetsCount;
	}

	public void clearLearners(ParamList<?> params)
	{
		if(params.hasFlag("internal"))
			return;
		if(params.hasFlag(McRipperCommands.clear))
		{
			McChecker.am.clearLearners();
			System.out.println("forgot all learning data");
		}
	}
	
	public void clear(ParamList<?> params) 
	{
		this.setMcDefault();
		McChecker.checkJsons.clear();
		McChecker.am.localCache.clear();
		IOUtils.deleteDirectory(McChecker.am.tmp);
	}

	public void setMcDefault()
	{
		McChecker.am.cachedDir = McChecker.mcDefaultDir;
	}
}
