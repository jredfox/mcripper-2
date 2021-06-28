package jredfox.mcripper.exception.url;

public class HTTPException extends URLException{
	
	private static final long serialVersionUID = 58925012983410284L;
	
	public HTTPException(int err, String msg)
	{
		this(false, err, msg);
	}
	
	protected HTTPException(boolean https, int err, String msg)
	{
		super(https ? "https" : "http", err, msg);
		if(this.errCode < 400)
			throw new IllegalArgumentException("http error code must be 400-499!");
	}
	
	public boolean isClientErr()
	{
		return this.errCode > 400 && this.errCode < 500;
	}
	
	public boolean isServerErr()
	{
		return this.errCode >= 500 && this.errCode < 600;
	}

}
