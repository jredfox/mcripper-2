package jredfox.mcripper.url.exception;

import java.net.URL;

public class HTTPSException extends HTTPException{

	public HTTPSException(URL url, int err, String msg) 
	{
		super(url, err, msg);
	}

}
