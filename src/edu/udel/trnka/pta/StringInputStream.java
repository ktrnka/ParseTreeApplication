package edu.udel.trnka.pta;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * Reads an input stream (binary data) from a string (character data). This
 * class is needed to support loading XML from a string.
 */
public class StringInputStream extends InputStream
	{
	private StringReader reader;

	public StringInputStream(String dataSource)
		{
		reader = new StringReader(dataSource);
		}

	public int read() throws IOException
		{
		return reader.read();
		}
	}
