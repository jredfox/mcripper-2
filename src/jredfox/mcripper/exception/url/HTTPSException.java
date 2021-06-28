package jredfox.mcripper.exception.url;

public class HTTPSException extends HTTPException{

	public HTTPSException(int err, String msg) 
	{
		super(true, err, msg);
	}

}
