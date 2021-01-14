package jredfox.mcripper.printer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;

import jredfox.filededuper.util.IOUtils;

public class LogPrinter extends Printer {

	public PrintStream out;
	public PrintStream err;

	public LogPrinter(File log, PrintStream out, PrintStream err) throws IOException
	{
		super(log.getParentFile(), log);

		// reset the log file
		if (log.exists())
			log.delete();

		// set the printer
		super.setPrintWriter();

		// set the streams
		if (out != null)
			System.setOut(new LogPrinter.LogWrapper(this, out, false));
		if (err != null)
			System.setErr(new LogPrinter.LogWrapper(this, err, true));
	}

	@Override
	public void parse(String line) {}
	@Override
	public void save(BufferedWriter writer) {IOUtils.close(writer);}
	@Override
	public boolean contains(String key) {return false;}

	public class LogWrapper extends PrintStream {
		
		public Printer printer;
		public PrintStream child;
		public final boolean isErr;

		public LogWrapper(Printer printer, PrintStream out, boolean isErr)
		{
			super(out, true);
			this.printer = printer;
			this.child = out;
			this.isErr = isErr;
		}

		@Override
		public void print(boolean b)
		{
			this.print(String.valueOf(b));
		}

		@Override
		public void print(char c)
		{
			this.print(String.valueOf(c));
		}

		@Override
		public void print(int i)
		{
			this.print(String.valueOf(i));
		}

		@Override
		public void print(long l) 
		{
			this.print(String.valueOf(l));
		}

		@Override
		public void print(float f)
		{
			this.print(String.valueOf(f));
		}

		@Override
		public void print(double d) 
		{
			this.print(String.valueOf(d));
		}

		@Override
		public void print(Object obj)
		{
			this.print(String.valueOf(obj));
		}

		@Override
		public void print(char[] s) 
		{
			super.print(s);
			listen(String.valueOf(s));
		}

		@Override
		public PrintStream append(CharSequence csq, int start, int end)
		{
			listen(String.valueOf(csq));
			return super.append(csq, start, end);
		}

		@Override
		public void println()
		{
			super.println();
			this.listenln("");
		}

		@Override
		public void println(boolean x)
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(char x)
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(int x) 
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(long x) 
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(float x)
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(double x)
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(char[] x) 
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(Object x)
		{
			this.println(String.valueOf(x));
		}
		
		@Override
		public void print(String s) 
		{
			this.child.print(s);
			listen(s);
		}

		@Override
		public void println(String x)
		{
			this.child.println(x);
			listenln(x);
		}

		public void listen(String str) 
		{
			this.printer.print(str);
		}

		public void listenln(String line) 
		{
			String info = "[" + Instant.now() + "]" + " [" + (this.isErr ? "Err" : "STD") + "]" + ": ";
			this.printer.println(info + line.replaceAll("\n", "\n" + info));
		}
	}

}
