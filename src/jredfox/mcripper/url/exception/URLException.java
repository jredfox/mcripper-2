package jredfox.mcripper.url.exception;

import java.io.IOException;
import java.net.URL;

public abstract class URLException extends IOException{
	
	private static final long serialVersionUID = 4489831757280655388L;
	public final String protocol;
	public final String url;
	public final int errCode;
	
	public URLException(URL url, int err, String msg)
	{
		super(msg);
		this.protocol = url.getProtocol();
		if(!this.isSupported(this.protocol))
			throw new IllegalArgumentException("unsupported http protocal:" + this.protocol);
		this.url = url.toString();
		this.errCode = err;
	}

	public abstract boolean isSupported(String p);

	/**
	 * returns whether or not the exception is from the network rather then the current device / USB
	 */
	public boolean isWeb() 
	{
		return !this.protocol.equals("file") && !this.protocol.equals("jar");
	}

}
