package jredfox.mcripper.command;

import java.io.File;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.ParamList;
import jredfox.mcripper.utils.McChecker;

public abstract class RipperCommand extends Command<Object>{
	
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
	public Object[] parse(String... inputs) {
		return null;
	}
	
	public void setMc(ParamList<?> params)
	{
		McChecker.mcDir = params.hasFlag("mcDir") ? new File(params.getValue("mcDir")).getAbsoluteFile() : McChecker.mcDir;
	}
	
	public void setMcDefault()
	{
		McChecker.mcDir = McChecker.mcDefaultDir;
	}
	
	public void finish()
	{
		this.setMcDefault();
		McChecker.checkJsons.clear();
	}

}
