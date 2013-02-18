package edu.udel.trnka.pta;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Loads a ComparedParseTreeSet from a file using labeled bracketing. It
 * doesn't compare the parse trees though, because it can't handle the
 * associated exception.
 * 
 * @author Keith Trnka
 */
public class ParseTreeSetLabeledBracketingParser implements LabeledBracketingParser
	{
	/** keeps track of the list of nodes we're in */
	private Stack treeHistory = new Stack();

	/** keeps track of the child list for each node (needed because we need to use a List instead of any array */
	private Stack childHistory = new Stack();

	/** the list of parse trees loaded so far */
	private List parseTreeList;

	/** the set of parse trees loaded */
	public ParseTreeSet treeSet;

	/** the parse tree currently being loaded */
	private ParseTree currentParseTree;

	/** the list of children found so far for the current parse tree */
	private List currentParseTreeChildren;

	/** the caret position within the string */
	private int characterIndex = 0;
	
	public void startBracket(String name)
		{
		// potentially begin the parse tree list
		if (name.equals("") && parseTreeList == null)
			{
			parseTreeList = new ArrayList();
			return;
			}

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
		// ignore data that isn't inside a parse tree
		if (currentParseTree == null)
			return;

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
		// check if this is the end of the parse tree list
		if (currentParseTree == null)
			{
			// create the treeSet
			treeSet = new ParseTreeSet();
			treeSet.trees = new ParseTree[parseTreeList.size()];
			for (int i = 0; i < parseTreeList.size(); i++)
				treeSet.trees[i] = (ParseTree)parseTreeList.get(i);
			return;
			}

		// set the right boundary
		currentParseTree.rightBound = characterIndex;

		// convert the list of children to an array
		currentParseTree.children = new ParseTree[currentParseTreeChildren.size()];
		for (int i = 0; i < currentParseTreeChildren.size(); i++)
			currentParseTree.children[i] = (ParseTree)currentParseTreeChildren.get(i);

		// pop the stack
		treeHistory.pop();
		childHistory.pop();

		// if we're back up to the root of the parse tree, store it and move on
		if (treeHistory.size() == 0)
			{
			currentParseTree.computeTreeStats();
			parseTreeList.add(currentParseTree);
			currentParseTree = null;
			characterIndex = 0;
			return;
			}

		ParseTree subtree = currentParseTree;

		// reset the current fields
		currentParseTree = (ParseTree)treeHistory.peek();
		currentParseTreeChildren = (List)childHistory.peek();

		// add the child to the list
		currentParseTreeChildren.add(subtree);
		}
	}