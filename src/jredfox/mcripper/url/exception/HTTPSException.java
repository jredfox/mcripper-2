package jredfox.mcripper.url.exception;

import java.net.URL;

public class HTTPSException extends HTTPException{
	
	private static final long serialVersionUID = 6007475658693100729L;

	public HTTPSException(URL url, int err, String msg) 
	{
		super(url, err, msg);
	}

}
