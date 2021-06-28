package jredfox.mcripper.exception.url;

import java.io.IOException;

public class URLException extends IOException{
	
	public final String protocol;
	public final int errCode;
	
	public URLException(String protocol, int err, String msg)
	{
		super(msg);
		this.protocol = protocol;
		this.errCode = err;
	}

}
