package jredfox.mcripper.exception.url;

import java.io.IOException;
import java.net.URL;

public class URLException extends IOException{
	
	public final String protocol;
	public final String url;
	public final int errCode;
	
	public URLException(URL url, int err, String msg)
	{
		super(msg);
		this.protocol = url.getProtocol();
		this.url = url.toString();
		this.errCode = err;
	}

	/**
	 * returns whether or not the exception is from the network rather then the current device / USB
	 */
	public boolean isWeb() 
	{
		return !this.protocol.equals("file") && !this.protocol.equals("jar");
	}

}
