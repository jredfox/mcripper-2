package jredfox.mcripper.command;

import java.io.File;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.ParamList;
import jredfox.mcripper.utils.McChecker;

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
		McChecker.mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McChecker.mcDir;
	}
	
	public void finish(ParamList<?> params)
	{
		if(params.hasFlag("internal"))
			return;
		this.clear();
		System.out.println("Done in:" + (System.currentTimeMillis() - ms) / 1000D + " seconds" + (McChecker.oldMajorCount > 0 ? " oldMajor:" + McChecker.oldMajorCount : "") + " major:" + McChecker.majorCount + (McChecker.oldMinor > 0 ? " oldMinor:" + McChecker.oldMinor : "") + " minor:" + McChecker.minorCount + " assets:" + McChecker.assetsCount);
	}
	
	public void clear() 
	{
		this.setMcDefault();
		McChecker.checkJsons.clear();
	}

	public void setMcDefault()
	{
		McChecker.mcDir = McChecker.mcDefaultDir;
	}
}
