package edu.udel.trnka.pta;
import java.util.ArrayList;

import org.w3c.dom.Node;

/**
 * A list of parses for a given string.
 * 
 * @author keith.trnka
 * 
 */
public class SentenceParses
	{
	/** the sentence that these are all parses of */
	String sentence;

	/**
	  * a list of parses. Note that these are not ParseTree
	  * instances, because we're trying to cut down on
	  * wasted CPU time.
	  */
	ArrayList<Node> parses;

	public String toString()
		{
		return sentence;
		}
	}
