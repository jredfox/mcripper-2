package jredfox.mcripper.url.exception;

import java.net.URL;

public class HTTPException extends URLException{
	
	private static final long serialVersionUID = 58925012983410284L;
	
	protected HTTPException(URL url, int err, String msg)
	{
		super(url, err, msg);
		if(this.errCode < 400)
			throw new IllegalArgumentException("http error code must be 400-499!");
	}
	
	@Override
	public boolean isSupported(String p)
	{
		return p.equals("http") || p.equals("https");
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
