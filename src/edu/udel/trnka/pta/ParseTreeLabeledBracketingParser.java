package edu.udel.trnka.pta;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * The ParseTreeLabeledBracketingParser class can load a parse tree as
 * represented using labeled bracketing. The labeled bracketing format is
 * basically a LISP expression. Here is an example:<br>
 * <code>(S (NP (DET The) (NN man)) (VP (V ate) (NP (DET his) (NN food))))</code>
 * <br>
 * 
 * @author Keith Trnka
 */
public class ParseTreeLabeledBracketingParser implements LabeledBracketingParser
	{
	/** keeps track of the list of nodes we're in */
	private Stack treeHistory = new Stack();

	/** keeps track of the child list for each node (needed because we need to use a List instead of any array */
	private Stack childHistory = new Stack();

	/** the parse tree currently being loaded */
	ParseTree currentParseTree;

	/** the list of children found so far for the current parse tree */
	private List currentParseTreeChildren;

	/** the caret position within the string */
	private int characterIndex = 0;
	
	public void startBracket(String name)
		{
		// potentially add a space before the new node created
		if (currentParseTree != null)
			if (currentParseTreeChildren.size() > 0)
				currentParseTreeChildren.add(buildLeaf(" "));

		// create the new parse tree
		currentParseTree = new ParseTree();
		currentParseTree.constituent = name;
		currentParseTree.leftBound = characterIndex;
		currentParseTreeChildren = new ArrayList();

		// push the stack
		treeHistory.push(currentParseTree);
		childHistory.push(currentParseTreeChildren);
		}
		
	public void data(String data)
		{
		if (currentParseTreeChildren.size() > 0)
			currentParseTreeChildren.add(buildLeaf(" "));
		
		currentParseTreeChildren.add(buildLeaf(data));
		}

	private ParseTree buildLeaf(String data)
		{
		ParseTree newOne = new ParseTree();
		newOne.constituent = data;

		newOne.leftBound = characterIndex;
		characterIndex += data.length();
		newOne.rightBound = characterIndex;

		return newOne;
		}
		
	public void endBracket()
		{
		// set the right boundary
		currentParseTree.rightBound = characterIndex;

		// convert the list of children to an array
		currentParseTree.children = new ParseTree[currentParseTreeChildren.size()];
		for (int i = 0; i < currentParseTreeChildren.size(); i++)
			currentParseTree.children[i] = (ParseTree)currentParseTreeChildren.get(i);

		// pop the stack
		treeHistory.pop();
		childHistory.pop();

		if (treeHistory.size() == 0)
			return;

		ParseTree subtree = currentParseTree;

		// reset the current fields
		currentParseTree = (ParseTree)treeHistory.peek();
		currentParseTreeChildren = (List)childHistory.peek();

		// add the child to the list
		currentParseTreeChildren.add(subtree);
		}
	}