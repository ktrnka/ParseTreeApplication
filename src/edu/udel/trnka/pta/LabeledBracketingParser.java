package edu.udel.trnka.pta;

/**
 * LabeledBracketingParser is analogous to SAXParser; an implementation provides
 * methods to implement the events generated during parsing.
 * 
 * @author Keith Trnka
 */
public interface LabeledBracketingParser
	{
	/**
	  * called when a bracket is opened
	  */
	void startBracket(String name);
	
	/**
	  * called for a string found in the middle
	  */
	void data(String data);
	
	/**
	  * called when the most recently opened bracket was closed
	  */
	void endBracket();
	}
