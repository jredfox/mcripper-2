package jredfox.mcripper.url;

import java.io.File;

import jredfox.mcripper.url.exception.URLException;
import jredfox.mcripper.utils.RippedUtils;

public class URLResponse {
	
	public int code;
	public String protocol;
	public File file;
	
	public URLResponse(String p, int c, File f)
	{
		this.protocol = p.toLowerCase();
		this.code = c;
		this.file = f;
	}
	
	public URLResponse(File f)
	{
		this.protocol = "file";
		this.code =  f == null ? 404 : 200;
		this.file = f;
	}
	
	public URLResponse(URLException e)
	{
		this(e.protocol, e.errCode, null);
	}

	public boolean isWeb()
	{
		return RippedUtils.isWeb(this.protocol);
	}

	public boolean isHTTP() 
	{
		return RippedUtils.isHTTP(this.protocol);
	}

}
