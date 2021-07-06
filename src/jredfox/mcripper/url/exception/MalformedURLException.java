package jredfox.mcripper.url.exception;

public class MalformedURLException extends URLException{

	private static final long serialVersionUID = -1962150616948168767L;

	public MalformedURLException(String attempted, String msg) 
	{
		super(msg);
		this.url = attempted;
	}

	@Override
	public boolean isSupported(String p)
	{
		return true;
	}

}
