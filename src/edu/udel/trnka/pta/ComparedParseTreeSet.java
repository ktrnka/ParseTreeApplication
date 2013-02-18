package edu.udel.trnka.pta;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ComparedParseTreeSet
	{
	public ComparedParseTree[] trees;

	/**
	  * loads a set of parse trees and compares them, based on the file input
	  * @throws Exception when the file type is unsupported, based on the file extension
	  */
	public static ComparedParseTreeSet loadFromFile(File file) throws Exception
		{
		// figure out the file type
		String filename = file.getName();
		if (filename.matches(".*\\.xml"))
			{
			// load using XML
			return loadFromXML(file);
			}
		else if (filename.matches(".*\\.lbk"))
			{
			// load using labeled bracketing
			return loadFromLabeledBracketing(file);
			}
		else
			throw new Exception("Unsupported file type");
		}
	
	private static ComparedParseTreeSet loadFromXML(File file) throws Exception
		{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Element rootElement = builder.parse(file).getDocumentElement();

		// make sure that the root element is "trees"
		if (!rootElement.getNodeName().equals("trees"))
			throw new SAXException("Root element must be names \"trees\"");


		// get the child elements
		NodeList treeNodeList = rootElement.getChildNodes();

		// put trees in here
		ArrayList treeList = new ArrayList();

		// loop over each tree
		for (int i = 0; i < treeNodeList.getLength(); i++)
			{
			Node treeNode = treeNodeList.item(i);
			if (treeNode instanceof Element)
				try
					{
					treeList.add(new ComparedParseTree(ParseTree.build(treeNode)));
					}
				catch (Exception e)
					{
					System.out.println("The tree that messed it up:\n " + treeNode);
					throw e;
					}
			}

		// convert the treelist into the ComparedParseTreeSet
		ComparedParseTreeSet treeSet = new ComparedParseTreeSet();
		treeSet.trees = new ComparedParseTree[treeList.size()];
		for (int i = 0; i < treeSet.trees.length; i++)
			treeSet.trees[i] = (ComparedParseTree)treeList.get(i);

		// compare them
//		ComparedParseTree.compare(treeSet.trees);

		// return the set
		return treeSet;
		}
	
	private static ComparedParseTreeSet loadFromLabeledBracketing(File file) throws IOException
		{
		LabeledBracketingParserMachine machine = new LabeledBracketingParserMachine(file);
		ComparedParseTreeSetLabeledBracketingParser parser = new ComparedParseTreeSetLabeledBracketingParser();
		machine.parse(parser);
		return parser.treeSet;
		}
	
	public static void main(String[] args) throws Exception
		{
		if (args.length < 1)
			{
			System.err.println("Specify a filename");
			return;
			}
		
		File file = new File(args[0]);
		ComparedParseTreeSet set = loadFromFile(file);
		for (int i = 0; i < set.trees.length; i++)
			{
			set.trees[i].printTree("");
			}
		}
	}
