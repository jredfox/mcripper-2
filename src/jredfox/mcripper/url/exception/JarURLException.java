package jredfox.mcripper.url.exception;

import java.io.IOException;
import java.net.URL;

public class JarURLException extends FileURLException{

	private static final long serialVersionUID = 6605984203257257484L;

	public JarURLException(IOException io, URL url, String msg) 
	{
		super(io, url, msg);
	}

}
