package edu.udel.trnka.pta;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
  * LexicalAmbiguitySelector allows a user to visually
  * select the preterminals for a given string. The formatting
  * is not as precise as the parse tree formatting, because 
  * it's a user interface, not a visualization.
  */
public class CondensedLexicalAmbiguitySelector extends JPanel
	{
	/** the initial data */
	private ComparedParseTree[] trees;
	
	/** the tokens */
	private ArrayList tokens;
	
	/** mapping of preterminal assignments to sets */
	private HashMap ambiguityMap;
	
	/** an ordered list of keys, because I don't depend on the HashMap giving the same order all the time */
	private ArrayList ambiguityKeys;

	/** the currently selected key */
	private ArrayList selectedKey;
	
	/** the field that describes how many parses match the selection */
	private JTextField matchingParses;
	
	/** a double-list: tokens are mapped to Collections of choices */
	private ArrayList ambiguityChoices;

	/** if this is run within the ParseTreeApplication context, this variable will be set */
	public ParseTreeApplication application;
	
	public CondensedLexicalAmbiguitySelector(ComparedParseTree[] trees)
		{
		super();
		
		this.trees = trees;
		
		initializeData();
		
		initializeGUI();
		}
		
	/**
	  * perform processing on the parse trees
	  */
	private void initializeData()
		{
		// figure out the tokens from one parse tree
		ArrayList nodes = trees[0].getNonGapNodesAtHeight(1);
		
		tokens = new ArrayList();
		for (int i = 0; i < nodes.size(); i++)
			tokens.add(((ParseTree)nodes.get(i)).getUnderlyingString());
		
		// get the mapping of lexical assignments to sets of trees
		ambiguityMap = ComparedParseTree.getLexicalAmbiguity(trees);
		
		// get a list of keys
		ambiguityKeys = new ArrayList(ambiguityMap.keySet());
		
		// figure out the ambiguity choices
		ambiguityChoices = new ArrayList(tokens.size());
		for (int i = 0; i < tokens.size(); i++)
			ambiguityChoices.add(null);
		Iterator i = ambiguityKeys.iterator();
		while (i.hasNext())
			{
			ArrayList key = (ArrayList)i.next();

			for (int tagIndex = 0; tagIndex < key.size(); tagIndex++)
				{
				String tag = (String)key.get(tagIndex);

				TreeSet tagSet = (TreeSet)ambiguityChoices.get(tagIndex);
				if (tagSet != null)
					{
					tagSet.add(tag);
					}
				else
					{
					tagSet = new TreeSet();
					tagSet.add(tag);
					ambiguityChoices.set(tagIndex, tagSet);
					}
				}
			}

		selectedKey = new ArrayList();
		i = ambiguityChoices.iterator();
		while (i.hasNext())
			{
			TreeSet treeSet = (TreeSet)i.next();
			Iterator j = treeSet.iterator();
			String tag = (String)j.next();
			selectedKey.add(tag);
			}
		}
	
	/**
	  * setup the GUI
	  */
	private void initializeGUI()
		{
		// use a GridBagLayout
		JPanel selectionPanel = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		selectionPanel.setLayout(gridbag);
		
		// setup the default constraints
		GridBagConstraints c = new GridBagConstraints();
		
		// external padding
		c.insets = new Insets(2, 2, 2, 2);

		// Terminals don't consume excess vertical space
		c.weighty = 0;
		
		// add the row of terminal symbols
		c.gridy = 0;
		for (int i = 0; i < tokens.size(); i++)
			{
			String token = (String)tokens.get(i);
			c.gridx = i;
			
			JLabel label = new JLabel(token);
			gridbag.setConstraints(label, c);
			selectionPanel.add(label);
			}

		// add the selection area
		c.gridy = 1;
		c.gridx = 0;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;

		// build the radio button groups
		Iterator ambiguityIterator = ambiguityChoices.iterator();
		while (ambiguityIterator.hasNext())
			{
			TreeSet tagSet = (TreeSet)ambiguityIterator.next();

			// create a panel to put everything in
			Box tagPanel = new Box(BoxLayout.Y_AXIS);

			if (tagSet.size() == 1)
				{
				Iterator tagIterator = tagSet.iterator();
				String tag = (String)tagIterator.next();
				tagPanel.add(new JLabel(tag));
				}
			else
				{
				// build the list of radio buttons
				ButtonGroup buttonGroup = new ButtonGroup();

				Iterator tagIterator = tagSet.iterator();
				JRadioButton firstButton = null;
				while (tagIterator.hasNext())
					{
					String tag = (String)tagIterator.next();

					// create the button
					JRadioButton button = new JRadioButton(tag);
					button.addItemListener(new ButtonStateChangeListener(button, c.gridx, tag));
					buttonGroup.add(button);

					// store the first one
					if (firstButton == null)
						firstButton = button;

					// add it
					tagPanel.add(button);
					}

				firstButton.setSelected(true);
				}
			
			// add a border to the panel
			tagPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK), BorderFactory.createEmptyBorder(2, 2, 2, 2)));

			// add the panel
			gridbag.setConstraints(tagPanel, c);
			selectionPanel.add(tagPanel);

			// move right
			c.gridx++;
			}


		// create the area including the number of matching parses
		JPanel matchingParsesPanel = new JPanel();
		matchingParsesPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		matchingParsesPanel.add(new JLabel("Matching parses: "));
		matchingParses = new JTextField();
		matchingParses.setEditable(false);
		matchingParsesPanel.add(matchingParses);
		updateMatchingParses();

		// create the area for the "Show me" button
		JPanel showMePanel = new JPanel();
		showMePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton showMeButton = new JButton("Show trees");
		showMeButton.addActionListener(new ShowTreesListener());
		showMePanel.add(showMeButton);

		// add the two bottom panels
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add(matchingParsesPanel, BorderLayout.WEST);
		bottomPanel.add(showMePanel, BorderLayout.EAST);

		// setup the layout
		setLayout(new BorderLayout());

		// add the table
		add(selectionPanel, BorderLayout.NORTH);

		// add the bottom part
		add(bottomPanel, BorderLayout.SOUTH);
		}

	/**
	  * does a hashtable lookup and class cast
	  */
	private ArrayList getMatchingParses(Object key)
		{
		return (ArrayList)ambiguityMap.get(key);
		}

	/**
	  * does a hashtable lookup and class cast
	  */
	private ArrayList getSelectedParses()
		{
		return getMatchingParses(selectedKey);
		}

	/**
	  * updates the matchingParses field by checking the table's selection
	  */
	private void updateMatchingParses()
		{
		ArrayList selectedParses = getSelectedParses();
		int number = (selectedParses != null)?(selectedParses.size()):(0);

		// the selection field is created before the matching parses
		if (matchingParses != null)
			matchingParses.setText(String.valueOf(number));
		}
		
	/**
	  * Unit testing
	  */
	public static void main(String[] args) throws Exception
		{
		// build two dummy parses
		ComparedParseTree tree1 = new ComparedParseTree(ParseTree.build("<NP><DET>the</DET> <ADJ>lazy</ADJ> <NN>dog</NN></NP>"));
		ComparedParseTree tree2 = new ComparedParseTree(ParseTree.build("<NP><DET>the</DET> <ADV>lazy</ADV> <NN>dog</NN></NP>"));
		ComparedParseTree tree3 = new ComparedParseTree(ParseTree.build("<NP><DET>the</DET> <ADV>lazy</ADV> <NN>dog</NN></NP>"));
		ComparedParseTree tree4 = new ComparedParseTree(ParseTree.build("<NP><DT>the</DT> <ADV>lazy</ADV> <NN>dog</NN></NP>"));
		ComparedParseTree[] array = new ComparedParseTree[] { tree1, tree2, tree3, tree4 };
		
		// compare them
		ComparedParseTree.compare(array);
		
		CondensedLexicalAmbiguitySelector las = new CondensedLexicalAmbiguitySelector(array);
		
		// setup a GUI
		JFrame window = new JFrame("Lexical Ambiguity Selector Demo");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		window.setContentPane(las);
		
		// show it
		window.setLocation(new Point(100, 100));
		window.pack();
		window.show();
		}
	
	/**
	  * display the parse trees that match the selected lexical ambiguity
	  */
	private class ShowTreesListener implements ActionListener
		{
		public void actionPerformed(ActionEvent e)
			{
			ArrayList parses = getSelectedParses();
			Iterator i = parses.iterator();
			int x = 10, y = 10;
			ParseTreePanel[] panelArray = new ParseTreePanel[parses.size()];
			int index = 0;
			while (i.hasNext())
				{
				ComparedParseTree tree = (ComparedParseTree)i.next();

				if (application == null)
					{
					// create the new window and add it to the list
					JFrame newWindow = new JFrame("Parse Tree");
					panelArray[index] = new ParseTreePanel(tree);
					newWindow.setContentPane(panelArray[index]);
					newWindow.pack();
					newWindow.setLocation(new Point(x, y));
					newWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					newWindow.show();
					x += 10;
					y += 10;
					}
				else
					{
					ParseTreeWindow newWindow = application.popupNewWindow(tree);
					panelArray[index] = newWindow.panel;
					}
				index++;
				}
			
			// now, compare all of the parse trees
			ComparedParseTree[] promotedTrees = new ComparedParseTree[panelArray.length];
			for (int tree = 0; tree < promotedTrees.length; tree++)
				promotedTrees[tree] = panelArray[tree].promoteForComparison();

			try
				{
				ComparedParseTree.compare(promotedTrees);
				}
			catch (Exception exc)
				{
				// oh well, I guess we didn't want them anyway
				}
			}
		}

	/**
	  * this class implements functionality to simplify selection of parse trees
	  */
	private class ButtonStateChangeListener implements ItemListener
		{
		private AbstractButton button;
		private int keyIndex;
		private String tag;

		public ButtonStateChangeListener(AbstractButton button, int keyIndex, String tag)
			{
			this.button = button;
			this.keyIndex = keyIndex;
			this.tag = tag;
			}

		public void itemStateChanged(ItemEvent e)
			{
			if (button.isSelected())
				{
				selectedKey.set(keyIndex, tag);
				updateMatchingParses();
				}
			}
		}
	}
