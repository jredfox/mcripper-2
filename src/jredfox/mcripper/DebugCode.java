package jredfox.mcripper;

import java.time.Instant;

public class DebugCode {
	
	public static void main(String[] args)
	{
//		System.out.println(OffsetDateTime.now().toInstant() + "," + OffsetDateTime.now().toInstant().toEpochMilli());
//		OffsetDateTime offset = OffsetDateTime.parse("2011-04-19T13:03:05.000Z");
//		System.out.println(offset.toInstant().toEpochMilli());
		Instant instant = Instant.parse("2012-08-01T12:56:33.000Z");
		System.out.println(instant.toEpochMilli());
	}

}
