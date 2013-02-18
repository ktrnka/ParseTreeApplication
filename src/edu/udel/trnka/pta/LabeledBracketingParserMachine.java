package edu.udel.trnka.pta;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;

/**
 * LabeledBracketingParserMachine works somewhat like the SAXParserFactory
 * class, in that it controls parsing of labeled bracketing by calling the
 * appropriate methods of the specified LabeledBracketingParser. Certain
 * restrictions are placed on the labeled bracketing. For example, closing
 * parentheses may be attached to a terminal symbol and there may be many of
 * them. However, opening parentheses must be immediately followed by the label
 * (no spaces). There must be whitespace immediately following the label.
 * 
 * @author Keith Trnka
 */
public class LabeledBracketingParserMachine
	{
	/** the string to parse */
	private String data;
	
	public LabeledBracketingParserMachine(String data)
		{
		this.data = data;
		}

	public LabeledBracketingParserMachine(File file) throws IOException
		{
		char[] buffer = new char[256];
		StringWriter writer =  new StringWriter();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		int charsRead;
		while ((charsRead = reader.read(buffer)) > 0)
			writer.write(buffer, 0, charsRead);
		reader.close();

		data = writer.getBuffer().toString();
		}
	
	private boolean isOpeningBracket(char c)
		{
		return c == '(' || c == '[';
		}
	
	private boolean isClosingBracket(char c)
		{
		return c == ')' || c == ']';
		}
		
	/**
	  * Parse the string using the specified parser.
	  */
	public void parse(LabeledBracketingParser parser)
		{
		String[] tokens = data.split("\\s+");
		
		for (int i = 0; i < tokens.length; i++)
			{
			if (isOpeningBracket(tokens[i].charAt(0)))
				parser.startBracket(tokens[i].substring(1));
			else if (tokens[i].indexOf(')') != -1 || tokens[i].indexOf(']') != -1)
				{
				// the location of the first closing paren
				int startingPosition = tokens[i].indexOf(')');
				if (startingPosition == -1 || (tokens[i].indexOf(']') != -1 && tokens[i].indexOf(']') < startingPosition))
					startingPosition = tokens[i].indexOf(']');

				// if there are characters before the first closing paren, they are data
				if (startingPosition > 0)
					parser.data(tokens[i].substring(0, startingPosition));

				// close a bunch of parens
				for (int j = startingPosition; j < tokens[i].length(); j++)
					if (isClosingBracket(tokens[i].charAt(j)))
						parser.endBracket();
					else
						throw new RuntimeException("Invalid labeled bracketing format");
				}
			else
				parser.data(tokens[i]);
			}
		}
	}
