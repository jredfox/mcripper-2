package jredfox.mcripper.command;

import java.io.File;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.ParamList;
import jredfox.filededuper.util.IOUtils;
import jredfox.mcripper.printer.Learner;
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
		if(params.hasFlag("internal"))
			return;
		if(params.hasFlag('c'))
			this.clearLearners(params);
	}
	
	public void finish(ParamList<?> params)
	{
		if(params.hasFlag("internal"))
			return;
		this.clear(params);
		this.print();
	}
	
	public void print() 
	{
		System.out.println(McRipperCommands.lboarder + "Done in:" + (System.currentTimeMillis() - this.ms) / 1000D + " seconds" + (McChecker.oldMajorCount > 0 ? " oldMajor:" + McChecker.oldMajorCount : "") + " major:" + McChecker.majorCount + (McChecker.oldMinor > 0 ? " oldMinor:" + McChecker.oldMinor : "") + " minor:" + McChecker.minorCount + " assets:" + McChecker.assetsCount + McRipperCommands.rboarder);
	}

	public void clearLearners(ParamList<?> params)
	{
		if(params.hasFlag("internal"))
			return;
		if(params.hasFlag(McRipperCommands.clear))
		{
			IOUtils.deleteDirectory(McChecker.hash.learned);
			Learner.learners.clear();
			System.out.println("forgot all learning data");
		}
	}
	
	public void clear(ParamList<?> params) 
	{
		this.setMcDefault();
		McChecker.checkJsons.clear();
		IOUtils.deleteDirectory(McChecker.hash.tmp);
	}

	public void setMcDefault()
	{
		McChecker.mcDir = McChecker.mcDefaultDir;
	}
}
