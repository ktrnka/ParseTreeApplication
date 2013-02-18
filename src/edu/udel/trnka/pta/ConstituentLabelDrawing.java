package edu.udel.trnka.pta;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
  * ConstituentLabelDrawing represents the drawing of a node in a parse tree.
  * As such, it can compute the height and width of the drawing of the node,
  * which is of crucial importance to accomodating parse trees with features.
  */
public class ConstituentLabelDrawing extends JPanel
	{
	/** the height of this drawing */
	private int height;

	/** the width of this drawing */
	private int width;

	/** the width of the drawn constituent name */
	private double labelWidth;

	/** the height of the drawn constituent name */
	private double labelHeight;

	/** the width of the feature section of this drawing */
	private double featureWidth;

	/** the height of the feature section of this drawing */
	private double featureHeight;

	/** a list of attribute-value pairs for the left column */
	private ArrayList<Map.Entry<String,String>> leftFeatureColumn;

	/** a list of attribute-value pairs for the right column */
	private ArrayList<Map.Entry<String,String>> rightFeatureColumn;

	/** the required width of the left column */
	private double leftColumnWidth; 

	/** the required width of the right column */
	private double rightColumnWidth;
	
	/** the required height of the left column */
	private double leftColumnHeight;
	
	/** the required height of the right column */
	private double rightColumnHeight;

	/** give each string this margin of blank space around it */
	private static final int stringMargin = 2;

	/** the panel to take settings from */
	private ParseTreePanel panel;

	/** the constituent being drawn */
	private ParseTree tree;

	/** the font rendering context; set to default */
	private FontRenderContext context;

	/** the font to display the label in. Taken from the ParseTreePanel */
	private Font labelFont;

	/**
	  * the font to display the features in. If the labelFont size > 6,
	  * it's 2 less than that. Otherwise, it's the same. This choice was
	  * made to avoid drawing too small if the user prefers small fonts.
	  */
	private Font featureFont;

	/** debugging option */
	public static final boolean drawBoundingBox = false;

	/**
	  * construct a drawing of the constituent given by tree. Some
	  * settings are obtained from the ParseTreePanel, so that's why it's
	  * required.
	  */
	public ConstituentLabelDrawing(ParseTree tree, ParseTreePanel drawingPanel)
		{
		super();

		this.tree = tree;
		panel = drawingPanel;

		// everything will use the same redering context
		context = new FontRenderContext(null, false, false);

		// setup the fonts
		labelFont = drawingPanel.getFont();
		int featureFontSize = (labelFont.getSize() <= 6)?(labelFont.getSize()):(labelFont.getSize() - 2);
		featureFont = new Font (labelFont.getName(), labelFont.getStyle(), featureFontSize);

		// compute the height and width of everything
		arrange();
		}

	/**
	  * computes the size of everything and then sets the size of this component
	  */
	public void arrange()
		{
		arrangeLabel();
		arrangeFeatures();

		height = (int)(labelHeight + featureHeight);
		width = (int)(Math.max(labelWidth, featureWidth));

		setPreferredSize(new Dimension(width, height));
		setSize(new Dimension(width, height));
		}

	/**
	  * arrangeLabel will compute the width and height of the constituent label
	  */
	private void arrangeLabel()
		{
		Rectangle2D d = getStringBounds(tree.constituent, labelFont);
		labelHeight = d.getHeight();
		labelWidth = d.getWidth();
		}

	/**
	  * arrangeFeatures will setup the two-column layout of the 
	  * features, and compute the required width and height of
	  * each, then compute the overall require height and width
	  * of the features section of this node drawing
	  */
	private void arrangeFeatures()
		{
		// do nothing if there aren't any features or if features are disabled
		if (tree.features == null || tree.features.size() == 0 || ParseTreeApplication.settings.featuresEnabled == false)
			{
			featureWidth = featureHeight = 0;
			return;
			}

		// arrange them into a list for the left column and the right column
		leftFeatureColumn = new ArrayList<Map.Entry<String,String>>();
		rightFeatureColumn = new ArrayList<Map.Entry<String,String>>();
		leftColumnWidth = leftColumnHeight = rightColumnWidth = rightColumnHeight = 0;
		boolean leftColumn = true;
		for (Map.Entry<String,String> entry : tree.features.entrySet())
			{
			String drawString = getAttributeValuePairString(entry);
			Rectangle2D bounds = getStringBounds(drawString, featureFont);

			if (leftColumn)
				{
				leftFeatureColumn.add(entry);
				leftColumnWidth = Math.max(leftColumnWidth, 2 * stringMargin + bounds.getWidth());
				leftColumnHeight += 2 * stringMargin + bounds.getHeight();

				leftColumn = false;
				}
			else
				{
				rightFeatureColumn.add(entry);
				rightColumnWidth = Math.max(rightColumnWidth, 2 * stringMargin + bounds.getWidth());
				rightColumnHeight += 2 * stringMargin + bounds.getHeight();

				leftColumn = true;
				}
			}
		
		featureWidth = leftColumnWidth + rightColumnWidth;
		featureHeight = Math.max(leftColumnHeight, rightColumnHeight);
		}
	
	/** convert an attribute-value pair to a string, for drawing */
	private String getAttributeValuePairString(Map.Entry<String,String> avpair)
		{
		return avpair.getKey() + "=" + avpair.getValue();
		}

	/** get the required height of this component */
	public int getRequiredHeight()
		{
		return height;
		}

	/** get the required width of this component */
	public int getRequiredWidth()
		{
		return width;
		}
	
	/** draw this node label to the specified graphics */
	public void paintComponent(Graphics oldGraphics)
		{
		Graphics2D g = (Graphics2D)oldGraphics;

		// compute the label's offset
		int labelX = (int)(width / 2 - labelWidth / 2),
			labelY = (int)labelHeight;

		// draw the label
		drawLabel(g, labelX, labelY);

		// draw the features
		drawFeatures(g, labelX, (int)labelHeight);

		// optionally draw a bounding box (if there are any features)
		if (tree.features != null && tree.features.size() > 0 && ParseTreeApplication.settings.featuresEnabled)
			drawBoundingBox(g);
		}

	/**
	  * draw the label of this constituent with the given x and y offset.
	  * The x and y offset are anchored at the lower left corner of the string.
	  */
	private void drawLabel(Graphics2D g, int xoffset, int yoffset)
		{
		// if this is a comparison parse tree that is a non-terminal, check the color
		Color oldColor = g.getColor();
		boolean colorChanged = false;
		if (tree instanceof ComparedParseTree && tree.children != null)
			{
			ComparedParseTree cpt = (ComparedParseTree)tree;
			if (cpt.flagged)
				{
				g.setColor(ParseTreePanel.flaggedColor);
				colorChanged = true;
				}
			else if (cpt.topDownSame)
				{
				g.setColor(ParseTreePanel.topDownSameColor);
				colorChanged = true;
				}
			else if (cpt.bottomUpSame)
				{
				g.setColor(ParseTreePanel.bottomUpSameColor);
				colorChanged = true;
				}
			}

		// draw the string
		g.drawString(tree.constituent, xoffset, yoffset);

		// restore the old color
		if (colorChanged)
			g.setColor(oldColor);
		}

	/**
	  * draw the features, given the bottom anchor point of the label
	  */
	private void drawFeatures(Graphics2D g, int topCenterX, int topCenterY)
		{
		// don't do anything if there are no features
		if (tree.features == null || tree.features.size() == 0 || ParseTreeApplication.settings.featuresEnabled == false)
			return;

		Font oldFont = g.getFont();
		g.setFont(featureFont);

		// draw the left column
		int currentX;
		if (labelWidth > featureWidth)
			{
			// then center the feature width
			currentX = (int)(width - featureWidth / 2) + stringMargin;
			}
		else
			{
			// just leave it left-align (which is the same as centered)
			currentX = stringMargin;
			}

		int currentY = topCenterY;
		for (Map.Entry<String,String> avpair : leftFeatureColumn)
			{
			String string = getAttributeValuePairString(avpair);
			Rectangle2D bounds = getStringBounds(string, featureFont);

			currentY += stringMargin + bounds.getHeight();

			drawAttributeValuePair(g, avpair, string, currentX, currentY);

			currentY += stringMargin;
			}
		
		// draw the right column
		if (labelWidth > featureWidth)
			{
			// then center the feature width
			currentX = width - (int)((width - featureWidth / 2) + rightColumnWidth) + stringMargin;
			}
		else
			{
			// rightColumnWidth incorporates two strnig margins, so negate one of them

			currentX = width - (int)rightColumnWidth + stringMargin;
			}

		currentY = topCenterY;
		for (Map.Entry<String,String> avpair : rightFeatureColumn)
			{
			String string = getAttributeValuePairString(avpair);
			Rectangle2D bounds = getStringBounds(string, featureFont);

			currentY += stringMargin + bounds.getHeight();

			drawAttributeValuePair(g, avpair, string, currentX, currentY);

			currentY += stringMargin;
			}

		g.setFont(oldFont);
		}

	/**
	  * draw a single attribute-value pair at the particular x and y offset. 
	  * The string is what is actually drawn; the attribute-value pair itself
	  * is only passed in so that we can check if it's a common attribute-value
	  * pair, s we can color it differently for compared parse trees.
	  */
	private void drawAttributeValuePair(Graphics2D g, Map.Entry<String,String> avpair, String string, int xoffset, int yoffset)
		{
		// if this is a comparison parse tree that is a non-terminal, check the color
		Color oldColor = g.getColor();
		boolean colorChanged = false;
		if (tree instanceof ComparedParseTree && tree.children != null)
			{
			ComparedParseTree cpt = (ComparedParseTree)tree;
			if (cpt.flagged)
				{
				g.setColor(ParseTreePanel.flaggedColor);
				colorChanged = true;
				}
			else if (cpt.topDownSame && cpt.commonAttributes.contains(avpair.getKey()))
				{
				g.setColor(ParseTreePanel.topDownSameColor);
				colorChanged = true;
				}
			else if (cpt.bottomUpSame && cpt.commonAttributes.contains(avpair.getKey()))
				{
				g.setColor(ParseTreePanel.bottomUpSameColor);
				colorChanged = true;
				}
			}

		// draw the string
		g.drawString(string, xoffset, yoffset);

		// restore the old color
		if (colorChanged)
			g.setColor(oldColor);
		}
	
	private void drawBoundingBox(Graphics2D g)
		{
		Color oldColor = g.getColor();
		Stroke oldStroke = g.getStroke();

		g.setColor(Color.GRAY);
		g.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new Rectangle2D.Float(0, 0, width, height));

		g.setColor(oldColor);
		g.setStroke(oldStroke);
		}
	
	/**
	  * get the string boundaries of the string in the specified font, 
	  * using the FontRenderContext of this drawing
	  */
	private Rectangle2D getStringBounds(String s, Font f)
		{
		return f.getStringBounds(s, context);
		}
	
	/**
	  * unit testing
	  */
	public static void main(String[] args) throws Exception
		{
		// create a parse tree constituent
		ParseTree tree = ParseTree.build("<NP NUMBER=\"s\" PERSON=\"3\" VC=\"NOM\" KONG=\"true\" KING=\"no\" >the dog</NP>");
		ParseTreePanel panel = new ParseTreePanel(tree);
		ConstituentLabelDrawing drawing = new ConstituentLabelDrawing(tree, panel);

		JFrame frame = new JFrame("feature drawing example");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Container container = frame.getContentPane();
		container.setLayout(new BorderLayout());

		// show the whole tree
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 1), "Tree"));
		container.add(panel, BorderLayout.WEST);

		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 1), "Constituent"));
		p.add(drawing);
		container.add(p, BorderLayout.CENTER);

		frame.pack();
		frame.setVisible(true);
		}
	}
