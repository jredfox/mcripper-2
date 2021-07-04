package jredfox.mcripper.url.exception;

import java.io.IOException;
import java.net.URL;

public class FileURLException extends URLException {
	
	public IOException io;
	
	public FileURLException(IOException io, URL url, String msg)
	{
		super(url, -1, msg);
		this.io = io;
	}

	private static final long serialVersionUID = 89898294829111134L;

	@Override
	public boolean isSupported(String p) 
	{
		return p.equals("file");
	}
}
