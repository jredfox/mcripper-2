package jredfox.mcripper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;

import jredfox.mcripper.url.exception.URLException;

public class DebugCode {
	
	public static void main(String[] args) throws MalformedURLException, IOException
	{
		URLConnection con = new URL("https://gooble.com").openConnection();
		try
		{
			con.getInputStream();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("sss\n");
		System.out.println("is a 404????:" + getCode(con));
	}
	
	public static int getCode(URLConnection con)
	{
		if(con instanceof HttpURLConnection)
		{
			try 
			{
				return ((HttpURLConnection)con).getResponseCode();
			}
			catch(UnknownServiceException um)
			{
				return 415;//unsupported media
			}
			catch(UnknownHostException uh)
			{
				return URLException.UNKNOWNHOST;
			}
			catch (IOException e) 
			{
				System.err.println("critical error while retrieving url error code. Report issue to github!");
				e.printStackTrace();
			}
		}
		else if(con.getClass().getName().equals("sun.net.www.protocol.ftp.FtpURLConnection"))
			System.err.println("JRE 8 doesn't support FTP codes report to java!");
		return -1;
	}

}
