package jredfox.mcripper;

public class DebugCode {
	
	public static void main(String[] args) throws Exception
	{
		try
		{
			Integer.parseInt("a");
		}
		catch(Exception e)
		{
			System.out.println("throwing exception");
			throw e;
		}
		finally
		{
			System.out.println("here");
		}
	}

}
