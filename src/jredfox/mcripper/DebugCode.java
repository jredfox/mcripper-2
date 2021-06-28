package jredfox.mcripper;

import java.io.File;
import java.io.IOException;

import jredfox.mcripper.utils.DLUtils;
import jredfox.mcripper.utils.RippedUtils;

public class DebugCode {
	
	public static void main(String[] args)
	{
		try {
			DLUtils.directDL("https://files.minecraftforge.net/maven/com/typesafe/akka/akka-actor_2.11/2.3.3/akka-actor_2.11-2.3.3.000.jar", new File("forge.jar").getAbsoluteFile(), -1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println();
		System.out.println(RippedUtils.getResponseCode("https://s3.amazonaws.com/MinecraftResources/music/calm3.ogg"));
	}

}
