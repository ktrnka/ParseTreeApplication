package edu.udel.trnka.pta;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** The same as the normal parse tree, but shows comparison to other parse trees */
public class ComparedParseTree extends ParseTree
	{
	/** is this tree the same as the generic tree from the top-down? */
	boolean topDownSame = false;

	/** is this tree the same as the generic tree from the bottom up? */
	boolean bottomUpSame = false;

	/** some nodes can be specially flagged (a semantically-poor way of highlighting something) */
	boolean flagged = false;

	/** this is a set of attribute names that have shared values with all other trees */
	Set<String> commonAttributes;

	// this is omitted to allow the drawing algorithm to work as-is. 
	// We just have to cast a lot this way.
//	ComparedParseTree[] children;

	/**
	  * I don't remember why this exists.
	  */
	public ComparedParseTree()
		{
		super();
		}

	/**
	  * copy the specified ParseTree for comparison. If it's an instance of this class,
	  * the extra data fields will be copied as well.
	  */
	public ComparedParseTree(ParseTree toCopy)
		{
		this(toCopy, false);
		}

	public ComparedParseTree(ParseTree toCopy, boolean shallow)
		{
		this();

		if (toCopy instanceof ComparedParseTree)
			{
			topDownSame = ((ComparedParseTree)toCopy).topDownSame;
			bottomUpSame = ((ComparedParseTree)toCopy).bottomUpSame;
			flagged = ((ComparedParseTree)toCopy).flagged;
			}

		// copy immutable fields and primitive types
		constituent = toCopy.constituent;
		features = toCopy.features; // (this isn't immutable, but we shouldn't ever edit it anyway)
		depth = toCopy.depth;
		maxHeight = toCopy.maxHeight;
		minHeight = toCopy.minHeight;
		leftBound = toCopy.leftBound;
		rightBound = toCopy.rightBound;
		left = toCopy.left;
		right = toCopy.right;
		originalString = toCopy.originalString;

		// copy the children list (this is a deep copy)
		if (shallow == false && toCopy.children != null)
			{
			children = new ComparedParseTree[toCopy.children.length];
			for (int i = 0; i < children.length; i++)
				children[i] = new ComparedParseTree(toCopy.children[i]);
			}
		else
			children = null;
		}

	/**
	  * compare the specified parse trees, filling in their data structures
	  */
	public static void compare(ComparedParseTree[] trees) throws Exception
		{
		// check that there are enough parse trees
		if (trees.length < 2)
			throw new RuntimeException("Must specify at least two parse trees");

		// check that the underlying string is the same for each
		if (!treesReferenceSameString(trees))
			throw new Exception("Parse trees do not have the same underlying string");

		compareTopDown(trees);
		compareBottomUp(trees);
		}

	/**
	  * check that all of the parse trees reference the same underlying string
	  */
	private static boolean treesReferenceSameString(ParseTree[] trees)
		{
		String sentence = trees[0].getUnderlyingString();
		for (int i = 1; i < trees.length; i++)
			if (!sentence.equals(trees[i].getUnderlyingString()))
				return false;

		return true;
		}

	/**
	  * perform top-down recursion on all nodes that match
	  */
	public static void compareTopDown(ComparedParseTree[] trees)
		{
		// check that the array isn't null
		if (trees == null)
			throw new RuntimeException("compareTopDown: trees should not be null");

		// check that no one element of the array is null (if it is, just stop the recursion)
		for (int i = 0; i < trees.length; i++)
			if (trees[i] == null)
				return;

		// check to see if they are all have the same label (if not, just return)
		for (int i = 0; i < trees.length; i++)
			if (!trees[i].constituent.equals(trees[0].constituent))
				return;

		// check to see if they all reference the same sentence (if not, just return)
		for (int i = 0; i < trees.length; i++)
			if (!trees[i].getUnderlyingString().equals(trees[0].getUnderlyingString()))
				return;

		// all of them have the same constituent, so mark them all 
		for (int i = 0; i < trees.length; i++)
			trees[i].topDownSame = true;

		// they all have the same constituent, so compare their features
		compareFeatures(trees);

		// if not all have children, return
		for (int i = 0; i < trees.length; i++)
			if (trees[i].children == null)
				return;

		// build a table of their signatures (which is a mapping of string indices to CompardParseTree nodes)
		ComparedParseTree[][] signatures = new ComparedParseTree[trees.length][trees[0].rightBound + 1];
		for (int tree = 0; tree < signatures.length; tree++)
			for (int child = 0; child < trees[tree].children.length; child++)
				for (int position = trees[tree].children[child].leftBound; position < trees[tree].children[child].rightBound; position++)
					signatures[tree][position] = (ComparedParseTree)trees[tree].children[child];

		// check their signatures and recurse when possible
		boolean match;
		for (int i = 0; i < trees[0].children.length; i++)
			{
			match = true;
			for (int tree = 1; tree < trees.length; tree++)
				{
				// check that the subtrees at the left and inclusive right boundaries are the same
				if (signatures[tree][trees[0].children[i].leftBound] != signatures[tree][trees[0].children[i].rightBound - 1])
					{
					match = false;
					return;
					}
				}

			if (match)
				{
				// create a new array
				ComparedParseTree[] subtrees = new ComparedParseTree[trees.length];

				// fill the array in
				for (int tree = 0; tree < trees.length; tree++)
					subtrees[tree] = signatures[tree][trees[0].children[i].leftBound];

				// perform recursion
				compareTopDown(subtrees);
				}
			}
		}

	/**
	  * compare them from the bottom up
	  */
	public static void compareBottomUp(ComparedParseTree[] trees)
		{
		boolean atLeastOneAgreement = true;

		// look at each level from the bottom
		HeightLoop:
		for (int currentHeight = 0; currentHeight < trees[0].maxHeight && atLeastOneAgreement; currentHeight++)
			{
			// get a list of nodes at this height for each tree
			ArrayList[] nodes = new ArrayList[trees.length];
			for (int tree = 0; tree < trees.length; tree++)
				{
				nodes[tree] = trees[tree].getNodesAtHeight(currentHeight);
				}

			atLeastOneAgreement = false;

			// loop until we're removed everything
			while (nodes[0].size() > 0)
				{
				// move each list forward until they all agree on the left boundaries
				int leftBoundary = ((ComparedParseTree)nodes[0].get(0)).leftBound;

				while (!allAgreeOnLeftBoundary(nodes))
					{
					// move each one forward up to the current leftBoundary
					for (int i = 0; i < nodes.length; i++)
						{
						int currentLB = ((ComparedParseTree)nodes[i].get(0)).leftBound;
						while (nodes[i].size() > 0 && currentLB < leftBoundary)
							{
							nodes[i].remove(0);
							if (nodes[i].size() > 0)
								currentLB = ((ComparedParseTree)nodes[i].get(0)).leftBound;
							}

						if (nodes[i].size() == 0)
							{
							// we're done working at this height
							continue HeightLoop;
							}

						// also, increase the left boundary if we need to
						if (currentLB > leftBoundary)
							leftBoundary = currentLB;
						}

					}

				if (allAgreeOnLeftBoundary(nodes))
					{
					// check that they all agree on the right boundary and constituent
					if (allAgreeOnRightBoundary(nodes) && allAgreeOnLabel(nodes))
						{
						// create an array of the constituents, so we can compare features
						ComparedParseTree[] sameTrees = new ComparedParseTree[trees.length];
						for (int i = 0; i < nodes.length; i++)
							{
							// add to the array and remove from our list
							sameTrees[i] = (ComparedParseTree)nodes[i].remove(0);

							// set the bottomUpSame field
							sameTrees[i].bottomUpSame = true;
							}
						
						// compare features
						compareFeatures(sameTrees);

						atLeastOneAgreement = true;
						}
					else
						{
						// just remove them all
						for (int i = 0; i < nodes.length; i++)
							nodes[i].remove(0);
						}
					}
				}
			}
		}

	/**
	  * This method compares the features for all constituents
	  * given by the roots of the trees. It doesn't perform
	  * recursion, as it's desgined to be called from compareToDown
	  * and compareBottomUp.
	  */
	private static void compareFeatures(ComparedParseTree[] trees)
		{
		// if any trees have no features, just return early
		for (int i = 0; i < trees.length; i++)
			if (trees[i].features == null || trees[i].features.size() == 0)
				return;

		// compute a frequency list of key-value pairs
		HashMap<Map.Entry<String,String>,int[]> frequency = new HashMap<Map.Entry<String,String>,int[]>();
		for (int i = 0; i < trees.length; i++)
			{
			for (Map.Entry<String,String> entry : trees[i].features.entrySet())
				{
				if (frequency.containsKey(entry))
					frequency.get(entry)[0]++;
				else
					frequency.put(entry, new int[] {1} );
				}
			}
		
		// build a set of attribute-value pairs that are common to all, but only store the attributes
		HashSet<String> commonAttributes = new HashSet<String>();
		for (Map.Entry<String,String> entry : frequency.keySet())
			{
			if (frequency.get(entry)[0] == trees.length)
				{
				commonAttributes.add(entry.getKey());
				}
			}
		
		// set the commonAttributes of each tree to this set (also, make the set immutable)
		Set<String> immutableCommonAttributes = Collections.unmodifiableSet(commonAttributes);
		for (int i = 0; i < trees.length; i++)
			trees[i].commonAttributes = immutableCommonAttributes;
		}

	/** The code is setup this funny way to support the bottom-up comparison */
	private static boolean allAgreeOnLeftBoundary(ArrayList[] nodes)
		{
		int boundary = ((ComparedParseTree)nodes[0].get(0)).leftBound;
		for (int i = 1; i < nodes.length; i++)
			if (boundary != ((ComparedParseTree)nodes[i].get(0)).leftBound)
				return false;

		return true;
		}

	/** The code is setup this funny way to support the bottom-up comparison */
	private static boolean allAgreeOnRightBoundary(ArrayList[] nodes)
		{
		int boundary = ((ComparedParseTree)nodes[0].get(0)).rightBound;
		for (int i = 1; i < nodes.length; i++)
			if (boundary != ((ComparedParseTree)nodes[i].get(0)).rightBound)
				return false;

		return true;
		}

	/** The code is setup this funny way to support the bottom-up comparison */
	private static boolean allAgreeOnLabel(ArrayList[] nodes)
		{
		String constituent = ((ComparedParseTree)nodes[0].get(0)).constituent;
		for (int i = 1; i < nodes.length; i++)
			if (!constituent.equals(((ComparedParseTree)nodes[i].get(0)).constituent))
				return false;

		return true;
		}

	/** 
	  * build a difference tree for this compared parse tree. The difference 
	  * tree only shows the differences and some similarities. This method 
	  * calls getDifferenceTreeWorker, then performs a few traversals of the
	  * tree to set the fields correctly.
	  */
	public ComparedParseTree getDifferenceTree()
		{
		ComparedParseTree differenceTree = getDifferenceTreeWorker();

		// compute tree stats
		differenceTree.computeTreeStats();

		// compute the string boundaries
		differenceTree.computeStringBoundaries();

		return differenceTree;
		}

	/** 
	  * build a difference tree for this compared parse tree. The difference 
	  * tree only shows the differences and some similarities. This method 
	  * computes the difference tree recursively. 
	  */
	private ComparedParseTree getDifferenceTreeWorker()
		{
		if (bottomUpSame)
			{
			// create a similar ComparedTree with a single child
			ComparedParseTree copy = new ComparedParseTree(this, true);

			// if it's a non-terminal
			if (maxHeight > 0)
				{
				copy.children = new ParseTree[1];

				copy.children[0] = new ComparedParseTree(this, true);
				copy.children[0].constituent = getUnderlyingString();
				}
			return copy;
			}
		else
			{
			// if only one child is not bottom-up same (and is top-down same), then recurse on that subchild
			int childRecursion = -1;
			if (topDownSame)
				{
				for (int i = 0; i < children.length; i++)
					{
					ComparedParseTree child = (ComparedParseTree)children[i];
					if (child.topDownSame)
						{
						if (!child.bottomUpSame)
							{
							if (childRecursion == -1)
								{
								childRecursion = i;
								}
							else
								{
								childRecursion = -1;
								break;
								}
							}
						}
					else
						{
						childRecursion = -1;
						break;
						}
					}
				}

			if (childRecursion != -1)
				{
				// return just the subtree that has the difference
				ComparedParseTree diffTree = ((ComparedParseTree)children[childRecursion]).getDifferenceTreeWorker();
				diffTree.depth = 0;
				return diffTree;
				}
			else
				{
				// shallow copy the current node
				ComparedParseTree copy = new ComparedParseTree(this, true);
				copy.children = new ComparedParseTree[children.length];
				for (int i = 0; i < copy.children.length; i++)
					copy.children[i] = ((ComparedParseTree)children[i]).getDifferenceTreeWorker();
				return copy;
				}
			}
		}

	/**
	  * saves the comparison parse tree, which includes the needed
	  * parts of the normal parse tree. This is needed for workspace support.
	  */		
	public void saveToWorkspace(DataOutputStream out) throws IOException
		{
		// save the class name
		out.writeUTF("ComparedParseTree");
		
		// save the original string
		out.writeUTF(originalString);
		
		// save the comparison fields
		out.writeBoolean(topDownSame);
		out.writeBoolean(bottomUpSame);
		out.writeBoolean(flagged);
		}

	/**
	  * loads the comparison parse tree. This is needed for workspace support.
	  */		
	public static ParseTree loadFromWorkspace(DataInputStream in) throws IOException
		{
		// load the original string
		String s = in.readUTF();
		
		// load in fields
		boolean topDownSame = in.readBoolean();
		boolean bottomUpSame = in.readBoolean();
		boolean flagged = in.readBoolean();
		try
			{
			ParseTree t = build(s);
			ComparedParseTree ct = new ComparedParseTree(t);
			ct.topDownSame = topDownSame;
			ct.bottomUpSame = bottomUpSame;
			ct.flagged = flagged;
			return t;
			}
		catch (Exception e)
			{
			throw new RuntimeException(e);
			}
		}
	
	/**
	  * get a mapping of lexical ambiguity to sets of parse trees.
	  */
	public static HashMap<ArrayList<String>,ArrayList<ComparedParseTree>> getLexicalAmbiguity(ComparedParseTree[] trees)
		{
		HashMap<ArrayList<String>,ArrayList<ComparedParseTree>> lexicalAmbiguityMap = new HashMap<ArrayList<String>,ArrayList<ComparedParseTree>>();
		
		for (int i = 0; i < trees.length; i++)
			{
			// get the preterminals
			ArrayList<String> preterminals = trees[i].getNonGapPreterminals();
			
			// find out if it's already in the hashtable
			if (lexicalAmbiguityMap.containsKey(preterminals))
				{
				ArrayList<ComparedParseTree> list = lexicalAmbiguityMap.get(preterminals);
				list.add(trees[i]);
				}
			else
				{
				ArrayList<ComparedParseTree> list = new ArrayList<ComparedParseTree>();
				list.add(trees[i]);
				lexicalAmbiguityMap.put(preterminals, list);
				}
			}
		return lexicalAmbiguityMap;
		}
	}
