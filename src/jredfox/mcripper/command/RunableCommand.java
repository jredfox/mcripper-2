package jredfox.mcripper.command;

import jredfox.filededuper.command.Command;

public abstract class RunableCommand extends Command<Object>{
	
	public RunableCommand(String[] options, String... ids)
	{
		super(options, ids);
	}
	
	public RunableCommand(String... ids)
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

}
