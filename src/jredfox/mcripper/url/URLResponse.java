package jredfox.mcripper.url;

import java.io.File;

public class URLResponse {
	
	public int code;
	public String protocol;
	public File file;
	
	public URLResponse(String p, int c, File f)
	{
		this.protocol = p;
		this.code = c;
		this.file = f;
	}

}
