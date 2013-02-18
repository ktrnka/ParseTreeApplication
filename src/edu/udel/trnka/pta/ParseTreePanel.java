package edu.udel.trnka.pta;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jibble.epsgraphics.EpsGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * The visualization of a parse tree in a JPanel. The Javadocs in this class are
 * lacking and need updating.
 */
public class ParseTreePanel extends JPanel
	{
	/** the tree will have nodes as close to the top as possible */
	public static final int TOP_BIASED = 0;

	/** the tree will have nodes as close to the bottom as possible */
	public static final int BOTTOM_BIASED = 1;

	/** the tree will have nodes somewhere in the middle */
	public static final int AVERAGE_BIASED = 2;

	/** the color to use for nodes that are the same from the top-down (takes priority over bottom-up) */
	public static final Color topDownSameColor = new Color(Color.HSBtoRGB(0F, 0F, .5F));

	/** the color to use for nodes that are the same from the bottom-up */
	public static final Color bottomUpSameColor = new Color(Color.HSBtoRGB(0F, 0F, .5F));

	/** the color to use for nodes that are greyed (anti-highlighted) */
	public static final Color greyedColor = new Color(Color.HSBtoRGB(0F, 0F, .5F));

	/** the color to use for nodes that are extra greyed (super anti-highlighted) */
	public static final Color extraGreyedColor = new Color(Color.HSBtoRGB(0F, 0F, .75F));

	/** the color to use for nodes that are flagged */
	public static final Color flaggedColor = new Color(Color.HSBtoRGB(0F, 1F, .8F));

	/** the string to print when there's a gap */
	public static final String gapString = "*gap*";

	/** sets whether or not this is antialiased */
	public boolean antialiased = true;

	/** the margin between the top of a label and the line to it. Also, the distance between the bottom of a label and the line under it. */
	static final int lineMargin = 2;

	/** the amount of space required around a constituent (in addition to the lineMargin space) */
	static final int constituentMargin = 5;

	/** the extra width to give on both the right and left sides of the underlying string */
	static final int sentenceMargin = 10;

	/** 
	  * an array of the vertical centers of each deck. Well, a deck
	  * by definition is a horizontal line, so there can only be a 
	  * single position associated with it, hence the name deckPositions.
	  */
	private int[] deckPositions;

	/** debug the decks by drawing the deck centers */
	public static boolean drawDeckCenters = false;

	/** the current parse tree to draw */
	ParseTree tree;

	/** whether the parse tree to draw (if it's a comparison tree) is a difference tree */
	JCheckBoxMenuItem differenceItem;
	
	/** the different versions of the parse tree to draw */
	private ParseTree normalTree;
	private ComparedParseTree comparedTree;
	private ComparedParseTree differenceTree;

	/** the x coordinate of the upper-left corner */
	int ulx = 0;

	/** the y coordinate of the upper-left corner */
	int uly = 0;

	/** the selected bias */
	private int bias = BOTTOM_BIASED;

	/** the menu for right-clicking */
	private JPopupMenu menu;

	/**
	  * creates a new panel to display the provided parse tree. This panel uses the BOTTOM_BIASED bias setting.
	  */
	public ParseTreePanel(ParseTree tree)
		{
		this(tree, BOTTOM_BIASED);
		}

	/**
	  * creates a new panel to display the provided parse tree. 
	  * @param bias The bias to use. One of ParseTreePanel.TOP_BIASED, ParseTreePanel.BOTTOM_BIASED, or ParseTreePanel.AVERAGE_BIASED.
	  */
	public ParseTreePanel(ParseTree tree, int bias)
		{
		this.tree = tree;
		normalTree = tree;
		this.bias = bias;

		// allocate space for the deckPosition array
		deckPositions = new int[tree.maxHeight + 1];

		// setup the right-click menu
		setupRightClickMenu();

		// initialize the size
		performCharacterMappingAndSizing();
		
		// set the default background to white
		setBackground(Color.WHITE);
		}
	
	/**
	  * creates the JPopupMenu used for right-clicking the image.
	  * Also populates the menu and instantiates all the menu items.
	  */
	private void setupRightClickMenu()
		{
		// create the menu
		menu = new JPopupMenu();
		addMouseListener(new PopupMenuListener());
		
		// the save action
		menu.add(new SaveAction());
		
		// the print action
		menu.add(new PrintAction());
		
		// the options sub-menu
		JMenu optionsMenu = new JMenu("Options");
		menu.add(optionsMenu);
		
		// add the options
		ButtonGroup orientationButtonGroup = new ButtonGroup();

		// set the bias to TOP
		JRadioButtonMenuItem topOriented = new JRadioButtonMenuItem("Top oriented", bias == TOP_BIASED);
		topOriented.addActionListener(new ActionListener()
			{
			public void actionPerformed(ActionEvent e)
				{
				bias = TOP_BIASED;
				repaint();
				}
			});
		orientationButtonGroup.add(topOriented);
		optionsMenu.add(topOriented);

		// set the bias to AVERAGE
		JRadioButtonMenuItem middleOriented = new JRadioButtonMenuItem("Middle oriented", bias == AVERAGE_BIASED);
		middleOriented.addActionListener(new ActionListener()
			{
			public void actionPerformed(ActionEvent e)
				{
				bias = AVERAGE_BIASED;
				repaint();
				}
			});
		orientationButtonGroup.add(middleOriented);
		optionsMenu.add(middleOriented);
		
		// set the bias to BOTTOM
		JRadioButtonMenuItem bottomOriented = new JRadioButtonMenuItem("Bottom oriented", bias == BOTTOM_BIASED);
		bottomOriented.addActionListener(new ActionListener()
			{
			public void actionPerformed(ActionEvent e)
				{
				bias = BOTTOM_BIASED;
				repaint();
				}
			});
		orientationButtonGroup.add(bottomOriented);
		optionsMenu.add(bottomOriented);

		optionsMenu.add(new JSeparator());
		differenceItem = new JCheckBoxMenuItem("Hide similarities");
		differenceItem.addActionListener(new ActionListener()
			{
			public void actionPerformed(ActionEvent e)
				{
				if (differenceItem.getState())
					{
					// it's checked now, so they're promoting it
					promoteToDifferenceTree();
					}
				else
					{
					restoreToComparisonTree();
					}
				repaint();
				}
			});
		differenceItem.setEnabled(false);
		optionsMenu.add(differenceItem);
		}

	/**
	  * initializeSize sets the preferred size of this component,
	  * using a default FontRenderContext. The number obtained may 
	  * be a little off, but it's worth it to be able to use pack().
	  */
/*
Commented out because it's the old way of sizing the panel.
It isn't appropriate anymore; just double-checking that no
method calls it.
	public void initializeSize()
		{
		// get the string
		string = tree.getUnderlyingString();

		// get the font
		font = getFont();
		context = new FontRenderContext(null, false, false);
		
		// decide the height of the parse tree as the max depth times the string height times some factor
		int deckGap = (int)font.getStringBounds(string, context).getHeight() * 2;
		height = (tree.maxHeight + 1) * deckGap;

		// decide the width of the string
		width = 2 * sentenceMargin + (int)font.getStringBounds(string, context).getWidth();

		// update the space requirements of this panel
		setMinimumSize(new Dimension(width, height));
		setPreferredSize(new Dimension(width, height));
		}
*/

	/** 
	  * converts the existing ParseTree (field tree) into a ComparedParseTree.
	  * This sets the field <i>comparedTree</i> and returns it.
	  * It also tells the rendered that we'll be drawing the ComparedParseTree instead.
	  */
	public ComparedParseTree promoteForComparison()
		{
		comparedTree = new ComparedParseTree(tree);
		tree = comparedTree;
		differenceItem.setEnabled(true);
		return comparedTree;
		}

	/** assuming that the tree is the comparison tree, create a difference tree and make that the one drawn */
	public ComparedParseTree promoteToDifferenceTree()
		{
		if (tree != comparedTree)
			throw new RuntimeException("The active tree isn't the compared tree.");

		differenceTree = comparedTree.getDifferenceTree();
		tree = differenceTree;
		return differenceTree;
		}

	/** this should only work when the difference tree is active */
	public void restoreToComparisonTree()
		{
		if (tree != differenceTree)
			throw new RuntimeException("The difference tree isn't the active tree.");

		tree = comparedTree;
		}
		
	/** this should return comparison and difference trees to normal */
	public void restoreToNormalTree()
		{
		if (normalTree == null)
			throw new RuntimeException("The normal tree is null");

		differenceItem.setEnabled(false);
		differenceItem.setState(false);
		tree = normalTree;
		}

	/** saves the convenience of passing this as an argument */
	private Graphics2D g;
	/** saves the convenience of passing this as an argument */
	private Font font;
	/** saves the convenience of passing this as an argument */
	private FontRenderContext context;
	/** saves the convenience of passing this as an argument */
	private int height;
	/** saves the convenience of passing this as an argument */
	private int deckGap;
	/** saves the convenience of passing this as an argument */
	private int width;
	/** saves the convenience of passing this as an argument */
	private String string;

	/**
	  * draw the parse tree
	  */
	public void paintComponent(Graphics oldGraphics)
		{
		g = (Graphics2D)oldGraphics;

		// paint the panel itself
		super.paintComponent(g);

		// turn on antialiasing
		if (antialiased)
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// change the line width
		g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		performCharacterMappingAndSizing();

		if (drawDeckCenters)
			drawDeckCenters(g);

		paintTree(tree, 0, 0, 0, null);
		}

	/**
	  * draw horizontal, dashed lines to show where the deck vertical centers are.
	  * This function is purely used for debugging.
	  */
	private void drawDeckCenters(Graphics2D g)
		{
		if (!drawDeckCenters)
			return;

		Color oldColor = g.getColor();
		Stroke oldStroke = g.getStroke();

		g.setColor(Color.BLUE);
		g.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 1, new float[] { 10, 3 }, 0));

		for (int deck = 0; deck < deckPositions.length; deck++)
			{
			g.draw(new Line2D.Float(0, deckPositions[deck], width, deckPositions[deck]));
			}
		g.setColor(oldColor);
		g.setStroke(oldStroke);
		}

	/**
	  * recomputes the size of the panel, designed specifically for
	  * the event that feature drawing is turned on or off
	  */
	public void recomputeSize()
		{
		// recompute the size of all label drawings
		recomputeLabelSizes(tree);

		// redo the sizing part
		performCharacterMappingAndSizing();
		}
	
	/**
	  * traverse the tree and "recompute" the labels for each node.
	  * More specifically, recomputation of labels means that the arrange()
	  * method is called.
	  */
	private void recomputeLabelSizes(ParseTree currentTree)
		{
		currentTree.drawing.arrange();

		if (currentTree.children != null)
			for (int i = 0; i < currentTree.children.length; i++)
				recomputeLabelSizes(currentTree.children[i]);
		}

	/**
	  * This method performs a number of seemingly unrelated tasks:
	  * <UL>
	  *		<LI>Setting of the convenience variables: font, context, string
	  *		<LI>Setting of the deckGap variable
	  *		<LI>Building the array that maps characters to x-coordinates
	  *		<LI>Integration of the character position array into the parse tree
	  *		<LI>Spacing adjustment to prevent labels from overlapping
	  *		<LI>Computation of the height and width. This sets the minimum and preferred size of the component.
	  *		<LI>Centering of the parse tree within the panel via the ulx and uly fields
	  * </UL>
	  *
	  * An important note about the way it is coded is that there is a circular
	  * dependency between three computations:
	  * <OL>
	  *		<LI>The character position array depends upon ulx as an offset
	  *		<LI>The width computation depends upon teh respacing of the tree, which depends upon the character position array
	  *		<LI>ulx depends upon the desired width and actual width of the component
	  * </OL>
	  * To resolve this issue, the character position array is incremented by ulx 
	  * after ulx is computed, then some tree statistics and respacing are redone
	  * to reflect the changes in the character positions.<br>
	  */
	private void performCharacterMappingAndSizing()
		{
		// get the font and font rendering context
		font = getFont();
		context = new FontRenderContext(null, false, false);

		// get the underlying string
		string = tree.getUnderlyingString();

		// build the array of character positions
		int[] positionArray = new int[tree.rightBound + 1];
		positionArray[0] = 0;
		for (int i = 0; i < string.length(); i++)
			{
			positionArray[i + 1] = positionArray[i] + (int)font.getStringBounds(string.substring(i, i + 1), context).getWidth();
			}

		// fill in the left and right fields
		tree.computeHorizontalRange(positionArray);

		// make extra spaces if need be
		int spaceAdded = respaceTree(tree, 0, 0);

		// decide the height of the parse tree as the max depth times the string height times some factor
		computeDeckPositions();

		// decide the width of the panel
		width = 2 * sentenceMargin + (spaceAdded + positionArray[positionArray.length - 1] - positionArray[0]);

		// reset ulx, uly to center the tree
		ulx = sentenceMargin + (getSize().width - width) / 2;
		uly = (getSize().height - height) / 2;

		// redo the positioning to reflect this choice of ulx
		for (int i = 0; i < positionArray.length; i++)
			positionArray[i] += ulx;
		tree.computeHorizontalRange(positionArray);
		respaceTree(tree, 0, 0);

		// redo the deck positions to reflect the choice of uly
		for (int i = 0; i < deckPositions.length; i++)
			deckPositions[i] += uly;

		// update the space requirements of this panel
		setMinimumSize(new Dimension(width, height));
		setPreferredSize(new Dimension(width, height));
		}
	
	/**
	  * re-do the spacing of the parse tree. This assumes
	  * that the horizontal range computation has already been performed.
	  * <br><i>Note: it isn't possible to perform this computation in
	  * the ParseTree class because it requires the font and font render
	  * context.</i>
	  * @param currentTree the tree to respace
	  * @param maxWidth the maximum width of the path to child (only when there's one child)
	  * @param offset the offset to add to every pixel location in this tree
	  * @returns the number of pixels that the tree has been expanded by
	  */
	private int respaceTree(ParseTree currentTree, int maxWidth, int offset)
		{
		// initialize the drawing if need be, or refresh if need be
		if (currentTree.drawing == null)
			currentTree.drawing = new ConstituentLabelDrawing(currentTree, this);

		// compute the width of the current label
		int currentLabelWidth = currentTree.drawing.getWidth();
//		int currentLabelWidth = (int)font.getStringBounds(currentTree.constituent, context).getWidth();

/*		// support the fake label for gaps
		if (currentTree.isGap())
			{
			currentLabelWidth = (int)font.getStringBounds(gapString, context).getWidth();
			}
*/
		// shift our range over by the offset
		currentTree.left += offset;
		currentTree.right += offset;

		if (currentTree.children == null)
			{
			// terminal
			if (currentLabelWidth >= maxWidth)
				{
				// if we have enough space, just add the offset
				return 0;
				}
			else
				{
				// if we need some space, just center left and right at the same location
				int allocatedWidth = currentTree.right - currentTree.left;
				int requiredWidth = maxWidth + lineMargin;
				int extraSpaceNeeded = requiredWidth - allocatedWidth;

				int leftExtraSpace = extraSpaceNeeded / 2;
				int rightExtraSpace = extraSpaceNeeded - leftExtraSpace;

				// move the tree over (the return value takes care of the rightExtraSpace)
				currentTree.left += leftExtraSpace;
				currentTree.right += leftExtraSpace;

				return extraSpaceNeeded;
				}
			}
		else
			{
			// update the maximum width
			if (currentLabelWidth > maxWidth)
				maxWidth = currentLabelWidth;

			// subtrees are handled differently for single children and multiple children
			int respacing = 0;
			if (currentTree.children.length == 1)
				{
				// single child
				respacing = respaceTree(currentTree.children[0], maxWidth, offset);
				}
			else
				{
				/* UPDATE CODE: 
				Require that each subtree be at least ceil(maxWidth / children.length ) ???
				*/
				//int propagatedWidth = (int)Math.round(maxWidth / (double) currentTree.children.length);
				int propagatedWidth = 0;

				// many children; don't worry about spacing for now (set maxWidth to zero == don't worry about spaces)
				for (int i = 0; i < currentTree.children.length; i++)
					respacing += respaceTree(currentTree.children[i], propagatedWidth, offset + respacing);

				}

			// this will work because we divide by two to get leftExtraSpace
			currentTree.left += respacing / 2;
			currentTree.right += respacing / 2;

			return respacing;
			}
		}

	/**
	  * determine how many levels to jump between these two nodes.
	  * This simply inspects the bias and calls the appropriate equation.
	  */
	private int getLevelGap(ParseTree root, ParseTree descendant)
		{
		if (bias == TOP_BIASED)
			return descendant.depth - root.depth;
		else if (bias == BOTTOM_BIASED)
			return root.maxHeight - descendant.maxHeight;
		else if (bias == AVERAGE_BIASED)
			return (int)Math.round((descendant.depth - root.depth + root.maxHeight - descendant.maxHeight) / 2.0);
		else
			throw new RuntimeException("Unsupported bias");
		}

	/**
	  * fill in the deckPositions array and set the height parameter
	  */
	private void computeDeckPositions()
		{
		// compute the maximum heights of each deck
		int[] deckHeights = new int[deckPositions.length];
		for (int deck = 0; deck < deckHeights.length; deck++)
			{
			// recursively find the maximum height on each deck
			deckHeights[deck] = maxHeightAtDeck(tree, null, deck, 0);
			}

		// turn these into deck center positions
		int currentPosition = 0;
		for (int deck = 0; deck < deckPositions.length; deck++)
			{
			// add the top half
			currentPosition += constituentMargin + lineMargin + deckHeights[deck] / 2;

			// set the center
			deckPositions[deck] = currentPosition;

			// add the bottom half
			currentPosition += constituentMargin + lineMargin + deckHeights[deck] / 2;
			}
		
		height = currentPosition;
		}
	
	/**
	  * recursively find the maximum required height at the specified deck.
	  * @param tree the root of the tree to search
	  * @param parent the parent node of that tree (or null if none)
	  * @param deck the deck to find the maximum height on
	  * @param parentDeck the deck number of the parent's deck
	  */
	private int maxHeightAtDeck(ParseTree tree, ParseTree parent, int deck, int parentDeck)
		{
		int currentDeck;
		if (parent == null)
			currentDeck = 0;
		else
			currentDeck = parentDeck + getLevelGap(parent, tree);

		if (currentDeck > deck)
			{
			// return a dummy value if this is below the desired deck
			return -1;
			}
		else if (currentDeck == deck)
			{
			// initialize the drawing if need be
			if (tree.drawing == null)
				tree.drawing = new ConstituentLabelDrawing(tree, this);

			return tree.drawing.getHeight();
			}
		else
			{
			if (tree.children == null)
				{
				return -1;
				}
			
			// take the maximum value returned by all children
			int max = -1;
			for (int i = 0; i < tree.children.length; i++)
				{
				max = Math.max(max, maxHeightAtDeck(tree.children[i], tree, deck, currentDeck));
				}
			return max;
			}
		}
	

	/**
	  * draw the specified nodes at the specified level
	  * @param subtree The current node in the recirsive drawing
	  * @param currentDeck The number of the current deck, counting from zero at the top
	  * @param parentAnchorX The south anchor point x-value of the parent node's label
	  * @param parentAnchorY The south anchor point y-value of the parent node's label
	  */
	private void paintTree(ParseTree subtree, int currentDeck, int parentAnchorX, int parentAnchorY, ParseTree parent)
		{
		// initialize the drawing if need be
		if (subtree.drawing == null)
			subtree.drawing = new ConstituentLabelDrawing(subtree, this);

		// get the height and width of this string
		int labelHeight = subtree.drawing.getHeight();
		int labelWidth = subtree.drawing.getWidth();
		
		// load the deck center
		int deckVCenter = deckPositions[currentDeck];

		// compute the horizontal center
		int deckWidth = subtree.right - subtree.left;
		int deckHCenter = subtree.left + deckWidth / 2;

		// compute the upper-left corner of the node drawing
		int anchorX = deckHCenter - labelWidth / 2;
		int anchorY = deckVCenter - labelHeight / 2;

		// force drawing to occur in the specifed part of the diagram
		g.translate(anchorX, anchorY);

		subtree.drawing.paintComponent(g);

		/* undo the translate from before 
		(we have to use negative numbers, because they are in the 
		current coordinate system, which was previously modified)
		*/
		g.translate(-anchorX, -anchorY);

		// compute our anchor points
		int topAnchorX = deckHCenter;
		int topAnchorY = anchorY - lineMargin;
		int bottomAnchorX = deckHCenter;
		int bottomAnchorY = anchorY + labelHeight + lineMargin;

		// draw the line to the parent (unless it's the root or a terminal)
		if (subtree.depth > 0 && (subtree.children != null || (ParseTreeApplication.settings.terminalLinesEnabled && !subtree.getUnderlyingString().matches("^\\s*$"))))
			{
			Color oldColor = g.getColor();
			boolean colorChanged = false;
			if (subtree instanceof ComparedParseTree && parent != null && parent instanceof ComparedParseTree)
				{
				ComparedParseTree cpt = (ComparedParseTree)subtree;
				ComparedParseTree cptParent = (ComparedParseTree)parent;
				if (cpt.flagged && cptParent.flagged)
					{
					g.setColor(flaggedColor);
					colorChanged = true;
					}
				else if (cpt.topDownSame && cptParent.topDownSame)
					{
					g.setColor(topDownSameColor);
					colorChanged = true;
					}
				else if (cpt.bottomUpSame && cptParent.bottomUpSame)
					{
					g.setColor(bottomUpSameColor);
					colorChanged = true;
					}
				}
			
			g.drawLine(topAnchorX, topAnchorY, parentAnchorX, parentAnchorY);


			if (colorChanged)
				g.setColor(oldColor);
			}
		
		if (subtree.maxHeight == 0)
			{
			// otherwise, this is a preterminal
			String underlyingString = subtree.getUnderlyingString();

			if (underlyingString.matches("^.*\\S\\s\\S.*$"))
				{
				// create a triangle
				Point2D bottomLeftPoint = new Point2D.Float(subtree.left + lineMargin, topAnchorY);
				Point2D bottomRightPoint = new Point2D.Float(subtree.right - lineMargin, topAnchorY);
				Point2D topPoint = new Point2D.Float(parentAnchorX, parentAnchorY);

				// save some settings
				Color c = g.getColor();
				Stroke s = g.getStroke();

				// modify them
				g.setColor(extraGreyedColor);
				g.setStroke(new BasicStroke(1));

				// draw the triangle
				GeneralPath path = new GeneralPath();
				path.append(new Line2D.Float(topPoint, bottomLeftPoint), true);
				path.append(new Line2D.Float(bottomLeftPoint, bottomRightPoint), true);
				path.append(new Line2D.Float(bottomRightPoint, topPoint), true);
				g.fill(path);

				// restore settings
				g.setColor(c);
				g.setStroke(s);
				}
			}


		// draw the subtrees
		if (subtree.children != null)
			{
			for (int i = 0; i < subtree.children.length; i++)
				{
				paintTree(subtree.children[i], currentDeck + getLevelGap(subtree, subtree.children[i]), bottomAnchorX, bottomAnchorY, subtree);
				}
			}
		}
	
	/**
	  * PopupMenuListener listens for mouse clicks in the panel and
	  * shows the popup menu accordingly.
	  */
	private class PopupMenuListener extends MouseAdapter
		{
		public void mousePressed(MouseEvent e)
			{
			act(e);
			}
			
		public void mouseReleased(MouseEvent e)
			{
			act(e);
			}

		/**
		  * the popup trigger on Windows and Linux is mouseReleased+right-click.
		  * However, the popup trigger on Solaris is mousePressed_right-click.
		  * So the action is abstracted away from the other part.
		  */
		private void act(MouseEvent e)
			{
			if (e.isPopupTrigger())
				{
				menu.show(ParseTreePanel.this, e.getX(), e.getY());
				}
			}
		}

	/**
	  * SaveAction represents that action for saving the parse tree panel to an image file
	  */
	private class SaveAction extends AbstractAction
		{
		/** temporarily use a static variable for this */
		ParseTreeApplication.Settings ptaSettings = ParseTreeApplication.settings;

		public SaveAction()
			{
			putValue(NAME, "Save image...");
			}

		public void actionPerformed(ActionEvent e)
			{
			// prompt for a filename
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter()
				{
				public boolean accept(File f)
					{
					return f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".png") || f.getName().toLowerCase().endsWith(".eps") || f.getName().toLowerCase().endsWith(".svg") || f.isDirectory();
					}

				public String getDescription()
					{
					return "Image files (JPG, PNG, EPS, SVG)";
					}
				});

			fileChooser.setCurrentDirectory(ptaSettings.workingDirectory);
			if (fileChooser.showSaveDialog(ParseTreePanel.this) == JFileChooser.APPROVE_OPTION)
				{
				ptaSettings.workingDirectory = fileChooser.getCurrentDirectory();
				File f = fileChooser.getSelectedFile();
				try
					{
					// check that the file type is known
					if (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg"))
						saveToFile(f.getAbsolutePath(), "jpeg");
					else if (f.getName().toLowerCase().endsWith(".png"))
						saveToFile(f.getAbsolutePath(), "png");
					else if (f.getName().toLowerCase().endsWith(".eps"))
						saveAsEPS(f);
					else if (f.getName().toLowerCase().endsWith(".svg"))
						saveAsSVG(f);
					else
						JOptionPane.showMessageDialog(ParseTreePanel.this, "Unknown file type", "Error", JOptionPane.ERROR_MESSAGE); 
					}
				catch (IOException exc)
					{
					JOptionPane.showMessageDialog(ParseTreePanel.this, "Unable to save", "Error", JOptionPane.ERROR_MESSAGE); 
					}
				}
			}
		}

	/**
	  * Save a picture of the parse tree to the specified file,
	  * using the specified file format.
	  */
	public void saveToFile(String filename, String format) throws IOException
		{
		// use the current component size, hoping that it is appropriate
		Image image = createImage((int)getSize().getWidth(), (int)getSize().getHeight());

		Graphics imageGraphics = image.getGraphics();

		// store and change the background color
		Color backgroundColor = getBackground();
		setBackground(Color.WHITE);

		// draw it
		paintComponent(imageGraphics);

		// restore the background color
		setBackground(backgroundColor);

		// save it to the file
		ImageIO.write((RenderedImage)image, format, new File(filename));

		// release the graphics resources
		imageGraphics.dispose();
		}

	/**
	  * saves as Encapsulated Postscript (EPS) using the Jibble library.
	  */
	public void saveAsEPS(File file) throws IOException
		{
		EpsGraphics2D epsGraphics = new EpsGraphics2D("Parse Tree", file, 0, 0, width, height);
		paintComponent(epsGraphics);
		epsGraphics.flush();
		epsGraphics.close();
		epsGraphics.dispose();
		}

	/**
	  * save as Scalable Vector Graphics (SVG) using the Batik library.
	  */
	public void saveAsSVG(File file) throws IOException
		{
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
		
		String svgNS = "http://www.w3.org/2000/svg";
		Document document = domImpl.createDocument(svgNS, "svg", null);
		SVGGraphics2D svgGen = new SVGGraphics2D(document);
		
		paintComponent(svgGen);
		
		Writer out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		svgGen.stream(out, true);
		out.close();
		}


	/**
	  * The PrintAction class allows the user to print
	  * the current ParseTreePanel to the printer.
	  */
	private class PrintAction extends AbstractAction implements Printable
		{
		public PrintAction()
			{
			putValue(NAME, "Print...");
			}
			
		public void actionPerformed(ActionEvent e)
			{
			// setup the print job
			PrinterJob job = PrinterJob.getPrinterJob();
			
			job.setPrintable(this);
			
			// print it
			if (job.printDialog())
				{
				try
					{
					job.print();
					}
				catch (PrinterException exc)
					{
					JOptionPane.showMessageDialog(ParseTreePanel.this, "Unable to print", "Error", JOptionPane.ERROR_MESSAGE); 
					System.out.println("Printer error: " + exc);
					}
				}
			}
			
		public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
			{
			if (pageIndex == 0)
				{
				// translate to pageFormat.getImageableX(), pageFormat.getImageableY()
				graphics.translate((int)pageFormat.getImageableX() + 1, (int)pageFormat.getImageableY() + 1);
				paintComponent(graphics);
				return PAGE_EXISTS;
				}
			else
				{
				return NO_SUCH_PAGE;
				}
			}
		}

	/** 
	  * This feature implements saving of workspaces, which is mostly implemented
	  * but it seems like I decided that it wasn't important.
	  */		
	public void saveToWorkspace(DataOutputStream out) throws IOException
		{
		// save the bias
		out.writeInt(bias);
		
		// save the parse tree
		tree.saveToWorkspace(out);
		}
		
	/** 
	  * This feature implements loading of workspaces.
	  */		
	public static ParseTreePanel loadFromWorkspace(DataInputStream in) throws IOException
		{
		// read the bias
		int bias = in.readInt();
		
		// read the tree
		ParseTree tree = ParseTree.loadFromWorkspace(in);
		
		return new ParseTreePanel(tree, bias);
		}
	}
