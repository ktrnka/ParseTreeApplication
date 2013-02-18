package edu.udel.trnka.pta;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Represents a single parse tree. Does not represent the rendering in any way;
 * only the data.
 * 
 * @author keith.trnka
 */
public class ParseTree
	{
	/** the root consitutent label */
	String constituent;

	/** 
	  * the features (if any) associated with this node. 
	  * If there are no features, it's null. A LinkedHashMap is used
	  * to display them in the same order as they are specified in 
	  * the original format.
	  */
	LinkedHashMap<String,String> features;

	/** children constituents; null if this is a terminal symbol */
	ParseTree[] children;

	/** the drawing of this node in the tree (saved for efficiency) */
	ConstituentLabelDrawing drawing;

	// assorted things about the tree structure 
	// (populated by computeTreeStats)
	int depth, maxHeight, minHeight;

	// the range of character array values for this constituent (rightBound is exclusive)
	// (populated by computeStringBoundaries)
	int leftBound, rightBound;

	// the range of x-values allocated to this constituent
	// (populated by computeHorizontalRange)
	int left, right;

	/** rather than performing the O(n) traversal each time to get the string, save it here and just return this if not null */
	private String memoizedUnderlyingString = null;
	
	/** the original string passed in - the textual representation of the parse tree */
	protected String originalString;

	/**
	  * perform a traversal of this tree, computing the values about the height/depth of each node.
	  */
	protected void computeTreeStats()
		{
		computeTreeStats(0);
		}

	/**
	  * perform a traversal of this tree, computing the values about the height/depth of each node.
	  * Sets the depth field to the parameter.
	  */
	private void computeTreeStats(int depth)
		{
		this.depth = depth;
		if (children != null)
			{
			// compute on children first
			for (int i = 0; i < children.length; i++)
				{
				// set the depth field
				children[i].depth = depth + 1;

				// compute stats
				children[i].computeTreeStats(depth + 1);
				}

			// compute the maxHeight and minHeight
			maxHeight = children[0].maxHeight + 1;
			minHeight = children[0].minHeight + 1;
			for (int i = 1; i < children.length; i++)
				{
				maxHeight = Math.max(maxHeight, children[i].maxHeight + 1);
				minHeight = Math.min(minHeight, children[i].minHeight + 1);
				}
			}
		else
			{
			maxHeight = 0;
			minHeight = 0;
			}
		}

	/**
	  * perform a traversal of this tree and compute the left and right boundaries.
	  * Some methods of building the tree already perform this task, but more 
	  * efficiently, because they perform the task in the middle of another traversal.
	  */
	protected void computeStringBoundaries()
		{
		computeStringBoundaries(0);
		}
		
	/**
	  * perform a traversal of this tree and compute the left and right boundaries.
	  * Some methods of building the tree already perform this task, but more 
	  * efficiently, because they perform the task in the middle of another traversal.
	  * @param leftBoundary the caret position
	  */
	protected void computeStringBoundaries(int leftBoundary)
		{
		// compute the left and right boundaries
		leftBound = leftBoundary;
		String underlyingString = getUnderlyingString();
		rightBound = leftBoundary + underlyingString.length();

		// compute boundaries for all children
		if (children != null)
			{
			int currentLeft = leftBoundary;
			for (int i = 0; i < children.length; i++)
				{
				children[i].computeStringBoundaries(currentLeft);
				currentLeft = children[i].rightBound;
				}
			}
		}

	/**
	  * computing the values about the height/depth of each node, assuming that it has alreadyu been done for children
	  */
	protected void computeTreeStatsNoRecurse()
		{
		if (children != null)
			{
			// compute the maxHeight and minHeight
			maxHeight = children[0].maxHeight + 1;
			minHeight = children[0].minHeight + 1;
			for (int i = 1; i < children.length; i++)
				{
				maxHeight = Math.max(maxHeight, children[i].maxHeight + 1);
				minHeight = Math.min(minHeight, children[i].minHeight + 1);
				}
			}
		else
			{
			maxHeight = 0;
			minHeight = 0;
			}
		}

	/**
	  * given the array of pixel coordinates for the underlying string, 
	  * compute the left and right boundaries of this constituent in pixels
	  */
	public void computeHorizontalRange(int[] positionArray)
		{
		// compute it for us first
		left = positionArray[leftBound];
		right = positionArray[rightBound];

		if (children != null)
			for (int i = 0; i < children.length; i++)
				children[i].computeHorizontalRange(positionArray);
		}

	/**
	  * build a parse tree from labeled bracketing
	  */
	public static ParseTree buildFromLabeledBracketing(String parseString)
		{
		LabeledBracketingParserMachine machine = new LabeledBracketingParserMachine(parseString);
		ParseTreeLabeledBracketingParser parser = new ParseTreeLabeledBracketingParser();
		machine.parse(parser);
		parser.currentParseTree.computeTreeStats();
		return parser.currentParseTree;
		}

	/**
	  * build a parse tree from XML representation
	  */
	public static ParseTree buildFromXML(String parseString) throws Exception
		{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		return build(builder.parse(new StringInputStream(parseString)).getChildNodes().item(0));
		}

	/**
	  * builds a parse tree from the specified string. This checks
	  * to see whether the string is XML or labeled bracketing by 
	  * checking the first non-whitespace character. If it's a '(', 
	  * then labeled bracketing is used. Otherwise, XML is used.
	  */
	public static ParseTree build(String s) throws Exception
		{
		ParseTree tree;
		// determine the mode
		
		if (s.matches("^\\s*[\\(\\[].*"))
			{
			// then it's labeled bracketing
			tree = buildFromLabeledBracketing(s);
			}
		else
			{
			// it must be XML
			tree = buildFromXML(s);
			}
			
		tree.originalString = s;
		return tree;
		}

	/**
	  * build a parse tree structure from the XML-DOM node
	  */
	public static ParseTree build(Node node)
		{
		ParseTree root = build(node, 0);
		root.computeTreeStats();
		return root;
		}

	/**
	  * build a parse tree structure from the XML-DOM node, given the starting position in the underlying character array
	  */
	protected static ParseTree build(Node node, int leftBound)
		{
		ParseTree subtree = new ParseTree();
		subtree.rightBound = leftBound;
		subtree.leftBound = leftBound;

		if (node.getNodeName().equals("#text"))
			{
			// this is a terminal node, so determination of right and left boundaries and such is easy
			subtree.constituent = node.getNodeValue();
			subtree.rightBound += subtree.constituent.length();
			subtree.children = null;
			}
		else
			{
			// get the constituent label
			subtree.constituent = node.getNodeName();

			// allow parse trees to have constituents with escaped labels
			if (subtree.constituent.equals("constituent"))
				subtree.constituent = ((Element)node).getAttributeNode("label").getValue();

			// allow parse trees to have features
			NamedNodeMap attributes = node.getAttributes();
			if (attributes.getLength() > 0)
				{
				subtree.features = new LinkedHashMap<String,String>();
				for (int i = 0; i < attributes.getLength(); i++)
					{
					Node attributeNode = attributes.item(i);
					if (!attributeNode.getNodeName().equals("constituent"))
						{
//						System.out.println("Adding feature " + attributeNode.getNodeName() + " = " + attributeNode.getNodeValue());
						subtree.features.put(attributeNode.getNodeName(), attributeNode.getNodeValue());
						}
					}
				}

			// loop over the children, moving the boundary leftwards
			NodeList nodes = node.getChildNodes();

			if (nodes.getLength() == 0)
				{
				// if the node isn't a terminal node (it's an XML element with no data), create an empty child
				// create a dummy child node
				ParseTree childTree = new ParseTree();
				childTree.constituent = "";
				childTree.leftBound = childTree.rightBound = leftBound;
				childTree.children = null;

				// create the array for this node
				subtree.children = new ParseTree[1];
				subtree.children[0] = childTree;
				}
			else
				{
				// the node is a normal non-terminal element
				ArrayList<ParseTree> childList = new ArrayList<ParseTree>();
				for (int i = 0; i < nodes.getLength(); i++)
					{
					Node child = nodes.item(i);

					// call recursively
					ParseTree childSubtree = build(child, subtree.rightBound);

					// update our right boundary
					subtree.rightBound = childSubtree.rightBound;

					childList.add(childSubtree);
					}

				// generate the array of children for this node
				subtree.children = new ParseTree[childList.size()];
				for (int i = 0; i < subtree.children.length; i++)
					{
					subtree.children[i] = childList.get(i);
					}
				}
			}

		return subtree;
		}

	/** get the string that this is a parse of */
	public String getUnderlyingString()
		{
		if (children == null)
			return constituent;
		else if (memoizedUnderlyingString != null)
			return memoizedUnderlyingString;
		else
			{
			String retval = "";
			for (int i = 0; i < children.length; i++)
				{
				retval += children[i].getUnderlyingString();
				}
			memoizedUnderlyingString = retval;
			return retval;
			}
		}

	/** for debugging, display the constituent and statistics. */
	public String toString()
		{
		return "Constituent: " + constituent + "\n\tdepth: " + depth + "\n\tmaxHeight: " + maxHeight + "\n\tminHeight: " + minHeight + "\n";
		}

	/** for debugging, print the tree out recursively using the specified indent */
	public void printTree(String indent)
		{
		System.out.println(indent + constituent + " [" + leftBound + ", " + rightBound + ")" + " [" + left + ", " + right + ")" + " depth=" + depth);
		if (children != null)
			for (int i = 0; i < children.length; i++)
				children[i].printTree(indent + "\t");
		}
	
	/**
	  * save this parse tree to the specified DataOutputStream. This feature is
	  * required for workspace support.
	  */
	public void saveToWorkspace(DataOutputStream out) throws IOException
		{
		// save the class
		out.writeUTF("ParseTree");
		
		// save the original string
		out.writeUTF(originalString);
		}
	
	/**
	  * load the parse tree from the specified workspace. This feature
	  * is required for workspace support.
	  */
	public static ParseTree loadFromWorkspace(DataInputStream in) throws IOException
		{
		String type = in.readUTF();
		
		if (type.equals("ComparedParseTree"))
			return ComparedParseTree.loadFromWorkspace(in);
			
		// load the original string
		String s = in.readUTF();
		
		try
			{
			ParseTree t = build(s);
			return t;
			}
		catch (Exception e)
			{
			throw new RuntimeException(e);
			}
		}
	
	/** returns true if the parse trees are equal (same constituents and structure) */
	public boolean equals(ParseTree other)
		{
		// if the names mismatch, they aren't equal
		if (!constituent.equals(other.constituent))
			return false;
			
		if (children == null)
			{
			// the base case -- if the constituent has matched, then it must be cool
			return true;
			}
		else
			{
			// check that the number of children is correct
			if (other.children == null || children.length != other.children.length)
				return false;
			
			// check that all children align correctly
			for (int i = 0; i < children.length; i++)
				if (!children[i].equals(other.children[i]))
					return false;
			
			// all tests passed
			return true;
			}
		}
	
	/**
	  * write a signature of thise parse tree, which is a matrix.
	  * Each row represents a level of the parse tree in bottom-up
	  * form. Each column represents each character.
	  */
	public String[][] getSignature()
		{
		String[][] signature = new String[maxHeight][rightBound];
		
		populateSignature(signature);
		
		return signature;
		}
	
	/**
	  * fills in the part of the signature for this node in the graph.
	  * So each character that this constituent covers is labeled with
	  * this constituent at the maxDepth level in the signature.
	  */
	private void populateSignature(String[][] signature)
		{
		// fill in the parts for this constituent
		for (int i = leftBound; i < rightBound; i++)
			signature[maxHeight][i] = constituent;
		
		// do the recursion
		if (children != null)
			for (int i = 0; i < children.length; i++)
				children[i].populateSignature(signature);
		}
	
	/**
	  * get a list of the preterminals of this parse tree, not including things just for gaps
	  */
	public ArrayList<String> getNonGapPreterminals()
		{
		ArrayList<String> preterminalList = new ArrayList<String>();
		
		if (maxHeight == 0)
			{
			// this will occur for terminal symbols under the root (like whitespace)
			// the string level
			}
		else if (maxHeight == 1 && !getUnderlyingString().equals(""))
			{
			// the preterminal level
			// this IS a preterminal, so the list only has one thing in it
			preterminalList.add(constituent);
			}
		else
			{
			// any nonterminal level >= 2
			// add the lists from all children
			for (int i = 0; i < children.length; i++)
				preterminalList.addAll(children[i].getNonGapPreterminals());
			}
		
		return preterminalList;
		}

	/** return a list of the nodes at the specified height. Height is measured from the bottom.  */
	protected ArrayList<ParseTree> getNodesAtHeight(int height)
		{
		if (maxHeight == height)
			{
			ArrayList<ParseTree> returnValue = new ArrayList<ParseTree>();
			returnValue.add(this);
			return returnValue;
			}
		else if (maxHeight < height)
			{
			return null;
			}
		else
			{
			if (children != null)
				{
				// recurse on children
				ArrayList<ParseTree> returnValue = new ArrayList<ParseTree>();
				for (int i = 0; i < children.length; i++)
					{
					ArrayList<ParseTree> temp = children[i].getNodesAtHeight(height);
					if (temp != null)
						returnValue.addAll(temp);
					}
				if (returnValue.size() == 0)
					returnValue = null;

				return returnValue;
				}
			else
				return null;
			}
		}

	/** return a list of the nodes at the specified height that aren't just gaps. Height is measured from the bottom.  */
	protected ArrayList<ParseTree> getNonGapNodesAtHeight(int height)
		{
		if (maxHeight == height && !getUnderlyingString().equals(""))
			{
			ArrayList<ParseTree> returnValue = new ArrayList<ParseTree>();
			returnValue.add(this);
			return returnValue;
			}
		else if (maxHeight < height)
			{
			return null;
			}
		else
			{
			if (children != null)
				{
				// recurse on children
				ArrayList<ParseTree> returnValue = new ArrayList<ParseTree>();
				for (int i = 0; i < children.length; i++)
					{
					ArrayList<ParseTree> temp = children[i].getNodesAtHeight(height);
					if (temp != null)
						returnValue.addAll(temp);
					}
				if (returnValue.size() == 0)
					returnValue = null;

				return returnValue;
				}
			else
				return null;
			}
		}

	/**
	  * returns true if this constituent is a gap
	  */
	public boolean isGap()
		{
		return constituent.equals("");
		}
	}
