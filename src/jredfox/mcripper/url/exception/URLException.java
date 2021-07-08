package jredfox.mcripper.url.exception;

import java.io.IOException;
import java.net.URL;

import jredfox.mcripper.utils.RippedUtils;

public abstract class URLException extends IOException{
	
	private static final long serialVersionUID = 4489831757280655388L;
	public String protocol;
	public String url;
	public int errCode;
	
	public static final int UNKNOWNHOST = 472;
	
	public URLException(URL url, int err, String msg)
	{
		super(msg);
		this.protocol = url.getProtocol();
		if(!this.isSupported(this.protocol))
			throw new IllegalArgumentException("unsupported http protocal:" + this.protocol);
		this.url = url.toString();
		this.errCode = err;
	}
	
	protected URLException(String msg)
	{
		this.errCode = -1;
		this.protocol = "";
		this.url = "";
	}

	public abstract boolean isSupported(String p);

	/**
	 * returns whether or not the exception is from the network rather then the current device / USB
	 */
	public boolean isWeb() 
	{
		return RippedUtils.isWeb(this.protocol);
	}
	
	public static URLException create(IOException io, URL url, int code) 
	{
		return create(io, url, code, null);
	}

	public static URLException create(IOException org, URL url, int code, String msg)
	{
		String p = url.getProtocol();
		if(msg == null)
			msg = p.toUpperCase() + " error code:" + code + " for URL:" + url;
		if(p.equals("https") && code >= 400 && code < 600)
			return new HTTPSException(url, code, msg);
		else if(p.equals("http") && code >= 400 && code < 600)
			return new HTTPException(url, code, msg);
		else if(p.equals("file") && org != null)
			return new FileURLException(org, url, msg);
		else if(p.equals("jar") && org != null)
			return new JarURLException(org, url, msg);
		return null;
	}

}
