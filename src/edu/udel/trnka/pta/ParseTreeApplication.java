package edu.udel.trnka.pta;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * Main application class.
 * 
 * @author keith.trnka
 * 
 */
public class ParseTreeApplication
	{
	public static void main(String[] args) 
		{
		ParseTreeApplication app = new ParseTreeApplication();
		app.start();
		}

	/** the main window */
	private JFrame frame;
	
	/** the dialog to input new parse trees */
	private JDialog inputDialog;

	/** the setting for parse tree input to keep the previous value */
	private JCheckBox keepContentBox;

	/** the "desktop pane" or thing to add internal frames to */
	private JDesktopPane desktopPane;

	/*& the menu bar for the application */
	private JMenuBar menuBar;

	/** the Window menu is given a special home here, because we add to and delete from it */
	private JMenu windowMenu;
	
	/** 
	  * the settings. They are package private and static because the 
	  * save option in the right-click menu for the ParseTreePanel
	  * needs to access the workingDirectory field to provide a 
	  * better user interface. Although I don't want to assume that
	  * there is only a single instance per virtual machine, it seems 
	  * like a big hassle for every class to know which application
	  * instance it belongs to.
	  */
	static Settings settings;

	/** windows are numbered, so this is the next number to use */
	private int nextWindowNumber = 1;

	/** the corner location of the next window opened */
	private Point nextWindowCorner = new Point(0, 0);
	
	/** the x and y space to increment the window corners by */
	public static final int positionOffset = 20;

	/** the space between windows when tiling windows */
	public static final int tileCellSpacing = 2;

	/** the folder containing the help file */
	public static final String helpPath = "help";
	/** the help file filename */
	public static final String helpFile = "help.html";

	public ParseTreeApplication()
		{
		// load settings
		settings = new Settings();
		settings.load();
		
		// create the frame
		frame = new JFrame("Parse Tree Application");
		frame.addWindowListener(new WindowAdapter()
			{
			public void windowClosing(WindowEvent e)
				{
				exit();
				}
			});

		// create the desktop
		desktopPane = new JDesktopPane();
		frame.setContentPane(desktopPane);

		// set the size and location of the main window
		settings.apply();

		// create a menuBar
		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		menuBar.add(createFileMenu());
		menuBar.add(createCompareMenu());
		menuBar.add(createDemoMenu());
		menuBar.add(createSettingsMenu());
		menuBar.add(createWindowMenu());
		menuBar.add(createHelpMenu());
		}
		
	public JMenu createFileMenu()
		{
		JMenu fileMenu = new JMenu("File");

		// add the new parse tree action
		fileMenu.add(new NewParseTreeAction());

		// add the load parse trees for comparison action
		fileMenu.add(new LoadTreesAction());

		// add the load parse trees for lexical ambiguity selection action
		fileMenu.add(new LoadTreesForLASAction());

		// add a separator and exit button
		fileMenu.add(new JSeparator());

		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(new ActionListener()
			{
			public void actionPerformed(ActionEvent e)
				{
				frame.setVisible(false);
				exit();
				}
			});
		fileMenu.add(exitItem);
		
		return fileMenu;
		}
	
	public JMenu createSettingsMenu()
		{
		JMenu settingsMenu = new JMenu("Settings");
		
		JCheckBoxMenuItem featureToggle = new JCheckBoxMenuItem("Draw features", settings.featuresEnabled);
		featureToggle.addActionListener(new ActionListener()
			{
			public void actionPerformed(ActionEvent e)
				{
				// toggle the settings value
				settings.featuresEnabled = ((JCheckBoxMenuItem)e.getSource()).getState();

				// refresh all windows
				JInternalFrame[] windows = desktopPane.getAllFrames();
				for (int i = 0; i < windows.length; i++)
					{
					if (windows[i] instanceof ParseTreeWindow)
						{
						// the commented code is the original code
						// It's been so many years that I don't know what I was doing.
//						((ParseTreeWindow)windows[i]).refreshPanel();
						((ParseTreeWindow)windows[i]).repaint();
						}
					}

				}
			});
		settingsMenu.add(featureToggle);
		

		JCheckBoxMenuItem terminalLinesToggle = new JCheckBoxMenuItem("Draw terminal lines", settings.terminalLinesEnabled);
		terminalLinesToggle.addActionListener(new ActionListener()
			{
			public void actionPerformed(ActionEvent e)
				{
				// toggle the settings value
				settings.terminalLinesEnabled = ((JCheckBoxMenuItem)e.getSource()).getState();

				// refresh all windows
				JInternalFrame[] windows = desktopPane.getAllFrames();
				for (int i = 0; i < windows.length; i++)
					{
					if (windows[i] instanceof ParseTreeWindow)
						{
						((ParseTreeWindow)windows[i]).repaint();
						}
					}

				}
			});
		settingsMenu.add(terminalLinesToggle);
		
		return settingsMenu;
		}
		
	public JMenu createCompareMenu()
		{
		JMenu compareMenu = new JMenu("Compare");
		
		// actions
		compareMenu.add(new CompareNonMinimizedAction());
		compareMenu.add(new UncompareAllAction());
		
		return compareMenu;
		}
		
	public JMenu createDemoMenu()
		{
		JMenu demoMenu = new JMenu("Demo");

		// add some demonstration parse trees
		demoMenu.add(new DemoParseTreeAction("the man", "<NP><DET>the</DET> <NN>man</NN></NP>"));
		demoMenu.add(new DemoParseTreeAction("the dog", "<NP><DET>the</DET> <NN>dog</NN></NP>"));
		demoMenu.add(new DemoParseTreeAction("telescope - low attachment", "<S><NP><NNP>Keith</NNP></NP> <VP><V>saw</V> <NP><NP><DT>the</DT> <NN>man</NN></NP> <PP><P>with</P> <NP><DT>the</DT> <NN>telescope</NN></NP></PP></NP></VP>.</S>"));
		demoMenu.add(new DemoParseTreeAction("telescope - high attachment", "<S><NP><NNP>Keith</NNP></NP> <VP><VP><V>saw</V> <NP><DT>the</DT> <NN>man</NN></NP></VP> <PP><P>with</P> <NP><DT>the</DT> <NN>telescope</NN></NP></PP></VP>.</S>"));
		demoMenu.add(new DemoParseTreeAction("tree with a gap", "<S><SSUB><SSUB><ADV>there</ADV> <VP><AUX>was</AUX> <DP><DP><DETP><DET>a</DET></DETP> <NP><N><N>bike</N></N></NP></DP> <REL><VP><JUNK-VP-INF><VP><V><V>route</V></V> <DP></DP></VP></JUNK-VP-INF></VP></REL></DP></VP></SSUB> <CONJ>and</CONJ> <SSUB><DP><PRON>we</PRON></DP> <VP><VP><V><V>rode</V></V></VP> <ADVP><PP><P>in</P> <N>line</N></PP></ADVP></VP></SSUB></SSUB></S>"));
		demoMenu.add(new DemoParseTreeAction("shallow parse", "<S><NP>The dog</NP> <VP><V>ate</V> <NP>the cat</NP></VP></S>"));
		demoMenu.add(new DemoParseTreeAction("shallow tree with features", "<S><NP AGR=\"3s\" ROLE=\"SUBJ\">The dog</NP> <VP><V AGR=\"3s\">ate</V> <NP AGR=\"3s\" ROLE=\"OBJ\">the cat</NP></VP></S>"));
		demoMenu.add(new DemoParseTreeAction("BFT", "<S VFORM = \"PAST\"><SSUB VFORM = \"PAST\" ROOT = \"-\" GAP = \"-\"><SSUB VFORM = \"PAST\" ROOT = \"-\" ERROR-FEATURE = \"+\" GAP = \"-\"><PP ROOT = \"THERE\" PFORM = \"LOC\" GAP = \"-\"><ADV ROOT = \"THERE\" POSITION = \"END\" PFORM = \"LOC\" LF = \"THERE\" LEX = \"THERE\" INPUT = \"THERE\">there</ADV></PP> <SSUB VFORM = \"PAST\" AGR = \"3S\" INV = \"+\" GAP = \"-\"><AUX COMPFORM = \"-\" VFORM = \"PAST\" AGR = \"3S\" ROOT = \"BE-AUX\" LF = \"WAS\" LEX = \"WAS\" INPUT = \"WAS\">was</AUX> <DP ROOT = \"BIKE\" DEF = \"?D2540\" CASE = \"NOMACC\" COUNTABLE = \"COUNT\" AGR = \"3S\" WH = \"-\" TYPE = \"-\" GAP = \"-\"><DETP DEF = \"-\" NO-COUNT = \"-\" NO-SING = \"-\" COMPFORM = \"COUNT\" AGR = \"3S\" NO-PLURAL = \"-\" WH = \"-\" NO-MASS = \"+\" GAP = \"-\"><DET COMPFORM = \"COUNT\" AGR = \"3S\" ROOT = \"A\" LF = \"A\" NO-MASS = \"+\" LEX = \"A\" INPUT = \"A\">a</DET></DETP> <NP ROOT = \"BIKE\" AGR = \"3S\" COUNTABLE = \"COUNT\" TYPE = \"-\" CASE = \"NOMACC\" SUBC = \"-\" GAP = \"-\"><N NO-PLURAL = \"-\" AGR = \"3S\" ROOT = \"BIKE\" COUNTABLE = \"COUNT\" TYPE = \"-\" CASE = \"NOMACC\" SUBC = \"-\"><N COUNTABLE = \"COUNT\" AGR = \"3S\" ROOT = \"BIKE\" LF = \"BIKE\" LEX = \"BIKE\" INPUT = \"BIKE\">bike</N></N></NP></DP> <DP AGR = \"3S\" ROOT = \"ROUTE\" CASE = \"NOMACC\" ERROR-FEATURE = \"+\" SUBC = \"-\" GAP = \"-\"><NP ROOT = \"ROUTE\" AGR = \"3S\" COUNTABLE = \"COUNT\" TYPE = \"-\" CASE = \"NOMACC\" SUBC = \"-\" GAP = \"-\"><N NO-PLURAL = \"-\" AGR = \"3S\" ROOT = \"ROUTE\" COUNTABLE = \"COUNT\" TYPE = \"-\" CASE = \"NOMACC\" SUBC = \"-\"><N COUNTABLE = \"COUNT\" AGR = \"3S\" ROOT = \"ROUTE\" LF = \"ROUTE\" LEX = \"ROUTE\" PPROOT = \"OF\" INPUT = \"ROUTE\">route</N></N></NP></DP></SSUB></SSUB> <CONJ AGR = \"3P\" CASE = \"?C84288\" TYPE = \"?T84287:(PERSON ANIMAL INANIMATE MEASURE QUANTITY COLLECTIVE LOCATION TIME)\" LF = \"AND\" LEX = \"AND\" CONJ-TYPE = \"COORD\" INPUT = \"AND\">and</CONJ> <SSUB VFORM = \"PAST\" ROOT = \"-\" WH = \"-\" INV = \"-\" GAP = \"-\"><DP ROOT = \"WE\" AGR = \"1P\" TYPE = \"-\" CASE = \"NOM\" DEF = \"+\" WH = \"-\" PRON = \"+\" GAP = \"-\" LEX = \"WE\"><PRON AGR = \"1P\" ROOT = \"WE\" CASE = \"NOM\" LF = \"WE\" LEX = \"WE\" INPUT = \"WE\">we</PRON></DP> <VP VFORM = \"PAST\" SUBCAT = \"INTRANS\" ROOT = \"-\" SUBJ = \"-\" AGR = \"1P\" GAP = \"-\"><VP VFORM = \"PAST\" SUBCAT = \"INTRANS\" MAIN = \"+\" AGR = \"1P\" ROOT = \"-\" GAP = \"-\"><V SUBCAT = \"INTRANS\" VFORM = \"PAST\" PFORM = \"?P144\" PROOT = \"-\" FEATURES = \"VMOTION\" PPROOT = \"-\" AROOT = \"-\" AGR = \"1P\" NOPASS = \"-\" PROOT1 = \"-\" LEX = \"RODE\"><V SUBCAT = \"INTRANS\" VFORM = \"PAST\" ROOT = \"RIDE\" LF = \"RODE\" LEX = \"RODE\" FEATURES = \"VMOTION\" INPUT = \"RODE\">rode</V></V></VP> <ADVP AGR = \"?A3204\" WH = \"-\" PFORM = \"-\" GAP = \"-\" PP = \"+\"><PP ROOT = \"IN\" GAP = \"-\"><P ROOT = \"IN\" PFORM = \"?P107603:(TIME LOC P-DIR)\" LF = \"IN\" LEX = \"IN\" INPUT = \"IN\">in</P> <N ROOT = \"LINE\" LF = \"LINE\" PPROOT = \"IN\" AGR = \"3S\" COUNTABLE = \"COUNT\" TYPE = \"AGGREGATE\" LEX = \"LINE\" INPUT = \"LINE\">line</N></PP></ADVP></VP></SSUB></SSUB></S>"));
		
		return demoMenu;
		}
		
	public JMenu createWindowMenu()
		{
		windowMenu = new JMenu("Window");
		menuBar.add(windowMenu);
		
		// add the tile feature
		JMenuItem tileMenuItem = new JMenuItem("Tile windows");
		tileMenuItem.addActionListener(new ActionListener()
			{
			public void actionPerformed(ActionEvent e)
				{
				tileWindows();
				}
			});
		windowMenu.add(tileMenuItem);

		// add the close all windows feature
		JMenuItem closeWindowsMenuItem = new JMenuItem("Close windows");
		closeWindowsMenuItem.addActionListener(new ActionListener()
			{
			public void actionPerformed(ActionEvent e)
				{
				closeWindows();
				}
			});
		windowMenu.add(closeWindowsMenuItem);
		

		// add a separator
		windowMenu.add(new JSeparator());
		
		return windowMenu;
		}
		
		
	public JMenu createHelpMenu()
		{
		JMenu helpMenu = new JMenu("Help");
		helpMenu.add(new HelpContentsAction());
		helpMenu.add(new JSeparator());
		helpMenu.add(new AboutAction());
		
		return helpMenu;
		}

	public void start()
		{
		frame.show();
		}

	public void exit()
		{
		settings.save();
		System.exit(0);
		}
		
	public void tileWindows()
		{
		tileWindows(desktopPane.getAllFrames());
		}

	public void tileWindows(JInternalFrame[] windows)
		{
		if (windows.length <= 1)
			return;
			
		// figure out the maximum size of the frames
		Dimension currentSize = windows[0].getSize();
		int horizontalSize = (int)currentSize.getWidth();
		int verticalSize = (int)currentSize.getHeight();
		for (int i = 1; i < windows.length; i++)
			{
			currentSize = windows[i].getSize();
			if (currentSize.getWidth() > horizontalSize)
				horizontalSize = (int)currentSize.getWidth();
				
			if (currentSize.getHeight() > verticalSize)
				verticalSize = (int)currentSize.getHeight();
			}
			
		horizontalSize += tileCellSpacing;
		verticalSize += tileCellSpacing;
		
		// figure out the maximum number of fully-drawn rows and columns
		int cols = (int)frame.getSize().getWidth() / horizontalSize;
		int rows = (int)frame.getSize().getHeight() / verticalSize;
		
		// tile the windows
		int window = 0;
		for (int y = 0, j = 0; j < cols && window < windows.length; j++, y += verticalSize)
			for (int x = 0, i = 0; i < cols && window < windows.length; i++, x += horizontalSize, window++)
				windows[window].setLocation(new Point(x, y));
		}

	/**
	  * closes all windows
	  */
	public void closeWindows()
		{
		// close them all
		JInternalFrame[] windows = desktopPane.getAllFrames();
		for (int i = 0; i < windows.length; i++)
			{
			windows[i].setVisible(false);
			try
				{
				windows[i].setClosed(true);
				}
			catch (java.beans.PropertyVetoException e)
				{
				if (windows[i] instanceof ParseTreeWindow)
					windowMenu.remove(((ParseTreeWindow)windows[i]).windowToFrontMenuItem);
				}
			windows[i].dispose();
			}
		}

	public void saveWorkspace(String filename) throws IOException
		{
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));

		// save each one
		JInternalFrame[] frames = desktopPane.getAllFrames();
		for (int i = 0; i < frames.length; i++)
			if (frames[i] instanceof ParseTreeWindow)
				((ParseTreeWindow)frames[i]).saveToWorkspace(out);

		out.close();
		}

	public void loadWorkspace(String filename) throws IOException
		{
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
		
		
		
		in.close();
		}

	public ParseTreeWindow popupNewWindow(ParseTree tree)
		{
		String title = "Parse Tree " + nextWindowNumber;
		nextWindowNumber++;
		
		ParseTreeWindow newWindow = new ParseTreeWindow(desktopPane, tree, title);
		desktopPane.add(newWindow);
		
		windowMenu.add(newWindow.windowToFrontMenuItem);
		
		newWindow.addInternalFrameListener(new InternalFrameAdapter()
			{
			public void internalFrameClosing(InternalFrameEvent e)
				{
				// remove the window from the menu
				windowMenu.remove(((ParseTreeWindow)e.getInternalFrame()).windowToFrontMenuItem);
				}
			});
		
		// move the window to a nicer location

		// the coodinates of the bottom right corner if we choose the specified point
		int desiredBRX = nextWindowCorner.x + newWindow.getSize().width;
		int desiredBRY = nextWindowCorner.y + newWindow.getSize().height;

		// reset the corner position if the window would hang off
		if (desiredBRX >= frame.getSize().width || desiredBRY >= frame.getSize().height)
			nextWindowCorner = new Point(0, 0);

		// move the window
		newWindow.setLocation(nextWindowCorner);

		// update for the next window
		nextWindowCorner = new Point(nextWindowCorner.x + positionOffset, nextWindowCorner.y + positionOffset);
		newWindow.show();

		return newWindow;
		}
	
	/**
	  * center the specified internal frame in the window
	  */
	public void centerInWindow(JInternalFrame internalFrame)
		{
		Dimension outerSize = frame.getSize();
		Dimension innerSize = internalFrame.getSize();
		Point newCorner = new Point( (outerSize.width - innerSize.width) / 2, (outerSize.height - innerSize.height) / 2 );
		internalFrame.setLocation(newCorner);
		}

	/**
	  * center inside the window.
	  * This version is for non-internal frames
	  */
	public void centerInWindow(Window externalFrame)
		{
		Dimension outerSize = frame.getSize();
		Dimension innerSize = externalFrame.getSize();
		Point newCorner = new Point( frame.getLocation().x + (outerSize.width - innerSize.width) / 2, frame.getLocation().y + (outerSize.height - innerSize.height) / 2 );
		externalFrame.setLocation(newCorner);
		}

	private class NewParseTreeAction extends AbstractAction
		{
		private JTextArea inputArea;
		
		public NewParseTreeAction()
			{
			putValue(NAME, "New parse tree...");
			
			// initialize the input dialog window
			inputDialog = new JDialog(frame, "Enter a parse tree", false);
			inputDialog.setResizable(true);
			Container dp = inputDialog.getContentPane();
			dp.setLayout(new BorderLayout());
			
			// explanation (maybe later)
			
			// the text area
			inputArea = new JTextArea(5, 50);
			inputArea.setLineWrap(true);
			inputArea.setWrapStyleWord(true);
			JScrollPane scroll = new JScrollPane(inputArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			
			// set a border
			scroll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			
			dp.add(scroll, BorderLayout.CENTER);
			
			// ok/cancel boxes
			JPanel okPanel = new JPanel();
			JButton okButton = new JButton("OK");
			okPanel.add(okButton);
			
			JButton cancelButton = new JButton("Cancel");
			okPanel.add(cancelButton);
			
			keepContentBox = new JCheckBox("Keep window content", settings.inputKeepText);
			okPanel.add(keepContentBox);
			
			dp.add(okPanel, BorderLayout.SOUTH);
			
			okButton.addActionListener(new ActionListener()
				{
				public void actionPerformed(ActionEvent e)
					{
					// in this case, check the junk
					okAction();
					}
				});
			
			cancelButton.addActionListener(new ActionListener()
				{
				public void actionPerformed(ActionEvent e)
					{
					if (!keepContentBox.isSelected())
						inputArea.setText("");
					inputDialog.setVisible(false);
					}
				});
			
			// set size and position
			if (settings.inputLocation.x < 0)
				{
				inputDialog.pack();
				centerInWindow(inputDialog);
				}
			else
				{
				inputDialog.setLocation(settings.inputLocation);
				inputDialog.setSize(settings.inputSize);
				}
			}
		
		private void okAction()
			{
			try
				{
				ParseTree tree = ParseTree.build(inputArea.getText());
				popupNewWindow(tree);
				
				if (!keepContentBox.isSelected())
					inputArea.setText("");
				inputDialog.setVisible(false);
				}
			catch (Exception exc)
				{
				JOptionPane.showMessageDialog(frame, "Tree format error", "Error", JOptionPane.ERROR_MESSAGE); 
				}
			}

		public void actionPerformed(ActionEvent e)
			{
			inputDialog.show();

			// prompt the user to enter a parse tree
/*			String parseTree = JOptionPane.showInputDialog(frame, "Enter a parse tree");
			if (parseTree != null)
				{
				try
					{
					ParseTree tree = ParseTree.build(parseTree);
					popupNewWindow(tree);
					}
				catch (Exception exc)
					{
					JOptionPane.showMessageDialog(frame, "Unable to load parse tree", "Error", JOptionPane.ERROR_MESSAGE); 
					}
				}*/
			}
		}

	/**
	  * LoadTreesForComparisonAction allows a user to load parse trees
	  * from a specified file. These trees will be automatically compared.
	  * by the way things are implemented, they are non un-comparable.
	  */
	private class LoadTreesAction extends AbstractAction
		{
		public LoadTreesAction()
			{
			putValue(NAME, "Load trees from file...");
			}
		
		public void actionPerformed(ActionEvent e)
			{
			// prompt the user for a file
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter()
				{
				public boolean accept(File f)
					{
					return (f.isDirectory() || f.getName().endsWith(".xml") || f.getName().endsWith(".lbk"));
					}

				public String getDescription()
					{
					return "Parse tree set files (*.xml, *.lbk)";
					}
				});
			fileChooser.setCurrentDirectory(settings.workingDirectory);
			if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
				{
				settings.workingDirectory = fileChooser.getCurrentDirectory();
				try
					{
					// load the set
					ComparedParseTreeSet treeSet = ComparedParseTreeSet.loadFromFile(fileChooser.getSelectedFile());
					
					// display the set
					for (int i = 0; i < treeSet.trees.length; i++)
						popupNewWindow(treeSet.trees[i]);
					}
				catch (Exception exc)
					{
					JOptionPane.showMessageDialog(frame, "Unable to load parse trees", "Error", JOptionPane.ERROR_MESSAGE); 
					}
				}
			}
		}

	/**
	  * LoadTreesForLASAction allows a user to load parse trees
	  * from a specified file. These trees will be automatically compared.
	  * by the way things are implemented, they are non un-comparable. The
	  * CondensedLexicalAmbiguitySelector is displayed with the results.
	  */
	private class LoadTreesForLASAction extends AbstractAction
		{
		public LoadTreesForLASAction()
			{
			putValue(NAME, "Load trees for lexical ambiguity selection...");
			}
		
		public void actionPerformed(ActionEvent e)
			{
			// prompt the user for a file
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter()
				{
				public boolean accept(File f)
					{
					return (f.isDirectory() || f.getName().endsWith(".xml") || f.getName().endsWith(".lbk"));
					}

				public String getDescription()
					{
					return "Parse tree set files (*.xml, *.lbk)";
					}
				});
			fileChooser.setCurrentDirectory(settings.workingDirectory);
			if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
				{
				settings.workingDirectory = fileChooser.getCurrentDirectory();
				try
					{
					// load the set
					ComparedParseTreeSet treeSet = ComparedParseTreeSet.loadFromFile(fileChooser.getSelectedFile());
					
					// load the comparator
					LASWindow lasWindow = new LASWindow(treeSet);
					desktopPane.add(lasWindow);
					lasWindow.las.application = ParseTreeApplication.this;
					lasWindow.pack();
					centerInWindow(lasWindow);
					lasWindow.show();
					}
				catch (Exception exc)
					{
					JOptionPane.showMessageDialog(frame, "Unable to load parse trees", "Error", JOptionPane.ERROR_MESSAGE); 
					}
				}
			}
		}	

	private class DemoParseTreeAction extends NewParseTreeAction
		{
		private String demoParse;

		public DemoParseTreeAction(String title, String demo)
			{
			putValue(NAME, title);
			demoParse = demo;
			}

		public void actionPerformed(ActionEvent e)
			{
			try
				{
				ParseTree tree = ParseTree.build(demoParse);
				popupNewWindow(tree);
				}
			catch (Exception exc)
				{
				JOptionPane.showMessageDialog(frame, "Unable to load parse tree", "Error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		}

	private class HelpContentsAction extends AbstractAction
		{
		private JDialog helpWindow;
		private JTextPane aboutText = new JTextPane();
		
		public static final int helpWindowBorder = 10;

		public HelpContentsAction()
			{
			putValue(NAME, "Contents");

			// create the about window
			helpWindow = new JDialog(frame, "Help contents");
			helpWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

			aboutText.setEditable(false);
				
			try
				{
				//aboutText.setPage(new File(helpPath, helpFile).toURL());
				aboutText.setContentType("text/html");
				aboutText.setPage(getClass().getClassLoader().getResource(helpPath + "/" + helpFile));
				}
			catch (java.net.MalformedURLException e)
				{
				throw new RuntimeException(e);
				}
			catch (IOException e)
				{
				aboutText.setText("Unable to display help\nMaybe the help file was deleted?");
				}
				
			JScrollPane scrollPane = new JScrollPane(aboutText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			helpWindow.setContentPane(scrollPane);

			helpWindow.setLocation(new Point((int)frame.getLocation().getX() + helpWindowBorder, (int)frame.getLocation().getY() + helpWindowBorder));
			helpWindow.setSize(new Dimension((int)frame.getSize().getWidth() - 2 * helpWindowBorder, (int)frame.getSize().getHeight() - 2 * helpWindowBorder));
			}

		public void actionPerformed(ActionEvent e)
			{
			// show the help contents
			helpWindow.show();
			}
		}

	private class AboutAction extends AbstractAction
		{
		private JDialog aboutWindow;

		public AboutAction()
			{
			putValue(NAME, "About");

			// create the about window
			aboutWindow = new JDialog(frame, "About");
			aboutWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

			Container pane = aboutWindow.getContentPane();
			pane.setLayout(new BorderLayout());

			JTextPane aboutText = new JTextPane();
			aboutText.setEditable(false);
			aboutText.setText("Parse Tree Application 1.0\nby Keith Trnka (trnka.dev@gmail.com)");
			JScrollPane scrollPane = new JScrollPane(aboutText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			pane.add(scrollPane, BorderLayout.CENTER);

			JPanel buttonPanel = new JPanel();
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new ActionListener()
				{
				public void actionPerformed(ActionEvent e)
					{
					aboutWindow.setVisible(false);
					}
				});
			buttonPanel.add(okButton);
			pane.add(buttonPanel, BorderLayout.SOUTH);

			// pack the window
			aboutWindow.pack();
			aboutWindow.setResizable(false);
			
			}

		public void actionPerformed(ActionEvent e)
			{
			// set the location of the window
			int x = (int)(frame.getLocation().getX() + (frame.getSize().getWidth() - aboutWindow.getSize().getWidth()) / 2);
			int y = (int)(frame.getLocation().getY() + (frame.getSize().getHeight() - aboutWindow.getSize().getHeight()) / 2);
			aboutWindow.setLocation(new Point(x, y));

			// show the about screen
			aboutWindow.show();
			}
		}
		
	private class CompareNonMinimizedAction extends AbstractAction
		{
		public CompareNonMinimizedAction()
			{
			putValue(NAME, "Compare non-minimized windows");
			putValue(SHORT_DESCRIPTION, "Compares all parse trees that are in non-minimized windows.");
			}
			
		public void actionPerformed(ActionEvent e)
			{
			// count the number of non-minimized windows that are parse tree windows
			int numberFound = 0;
			JInternalFrame[] windows = desktopPane.getAllFrames();
			for (int i = 0; i < windows.length; i++)
				if (windows[i] instanceof ParseTreeWindow && !windows[i].isIcon())
					numberFound++;
			
			// build an array of windows for comparison
			ParseTreeWindow[] compareWindows = new ParseTreeWindow[numberFound];
			int currentIndex = 0;
			for (int i = 0; i < windows.length; i++)
				if (windows[i] instanceof ParseTreeWindow && !windows[i].isIcon())
					compareWindows[currentIndex++] = (ParseTreeWindow)windows[i];
					
			// build an array of parse trees
			ComparedParseTree[] comparisonArray = new ComparedParseTree[numberFound];
			for (int i = 0; i < numberFound; i++)
				comparisonArray[i] = compareWindows[i].panel.promoteForComparison();
				
			// compare them
			try
				{
				ComparedParseTree.compare(comparisonArray);
				}
			catch (Exception exc)
				{
				// error message
				exc.printStackTrace();
				debugTrees(comparisonArray);
				JOptionPane.showMessageDialog(frame, "Unable to compare. Maybe some have different strings?", "Error", JOptionPane.ERROR_MESSAGE); 
				
				// restore the parse trees
				for (int i = 0; i < windows.length; i++)
					if (windows[i] instanceof ParseTreeWindow)
						((ParseTreeWindow)windows[i]).panel.restoreToNormalTree();
				
				return;
				}
				
			// repaint them
			for (int i = 0; i < numberFound; i++)
				compareWindows[i].panel.repaint();
			}
		
		/**
		  * When the comparison fails for some reason, this will show
		  * how many of each underlying string there is. It's purpose
		  * is primarily for debugging, but it may be a desirable feature
		  * to show the user.
		  */
		private void debugTrees(ComparedParseTree[] trees)
			{
			// figure out how many underlying strings there are
			HashMap<String,int[]> underlyingMap = new HashMap<String,int[]>();

			for (int i = 0; i < trees.length; i++)
				{
				String s = trees[i].getUnderlyingString();
				if (underlyingMap.containsKey(s))
					underlyingMap.get(s)[0]++;
				else
					underlyingMap.put(s, new int[] { 1 });
				}
			
			// now, show all of the underlying strings
			for (String s : underlyingMap.keySet())
				{
				System.out.println("'" + s + "' (" + underlyingMap.get(s)[0] + ")");
				}
			}
		}
		
	private class UncompareAllAction extends AbstractAction
		{
		public UncompareAllAction()
			{
			putValue(NAME, "Uncompare all");
			}
			
		public void actionPerformed(ActionEvent e)
			{
			JInternalFrame[] windows = desktopPane.getAllFrames();
			for (int i = 0; i < windows.length; i++)
				if (windows[i] instanceof ParseTreeWindow)
					{
					((ParseTreeWindow)windows[i]).panel.restoreToNormalTree();
					windows[i].repaint();
					}
			}
		}

	/**
	  * represents settings which are stored using the Java Preferences API.
	  * Under Solaris, they are stored in ~/.java/.userPrefs/NAME, where
	  * NAME is the namespace of the settings. For this, that namespace is
	  * /edu/udel/cis/trnka/pta, following the recommendation that settings
	  * namespaces be based on some other namespace, such as a URL with the 
	  * things reversed (general-to-specific order).
	  * On Linux, preferences are stored **fill this in**
	  * On Windows, preferences are stored **fill this in**
	  */
	public class Settings
		{
		public Point windowLocation;
		public Dimension windowSize;
		public File workingDirectory;
		public boolean featuresEnabled;
		public boolean terminalLinesEnabled;

		public Point inputLocation;
		public Dimension inputSize;
		public boolean inputKeepText;

		public static final String preferencesPathName = "/edu/udel/cis/trnka/pta";

		public void loadDefaults()
			{
			windowLocation = new Point(0, 0);
			windowSize = new Dimension(600, 600);

			// this should get the current directory on most systems
			workingDirectory = new File(".");
			
			// by default, features are enabled
			featuresEnabled = true;
			
			// by default, disabled
			terminalLinesEnabled = false;
			
			inputLocation = new Point(-1, -1);
			inputSize = new Dimension(-1,-1);
			inputKeepText = true;
			}
		
		/**
		  * loads settings using the Java preferences API.
		  * This way, I don't have to assume that the application
		  * is loaded in any particular directory.
		  */
		public void load()
			{
			try
				{
				Preferences userRoot = Preferences.userRoot();

				// check if the settings exist
				if (!userRoot.nodeExists(preferencesPathName))
					loadDefaults();
				else
					{
					Preferences prefs = userRoot.node(preferencesPathName);
					windowLocation = new Point(prefs.getInt("windowLocationX", 0), prefs.getInt("windowLocationY", 0));
					windowSize = new Dimension(prefs.getInt("windowSizeWidth", 600), prefs.getInt("windowSizeHeight", 600));
					workingDirectory = new File(prefs.get("workingDirectory", "."));
					featuresEnabled = prefs.getBoolean("featuresEnabled", true);
					terminalLinesEnabled = prefs.getBoolean("terminalLinesEnabled", false);

					inputLocation = new Point(prefs.getInt("inputLocationX", -1), prefs.getInt("inputLocationY", -1));
					inputSize = new Dimension(prefs.getInt("inputSizeWidth", -1), prefs.getInt("inputSizeHeight", -1));
					inputKeepText = prefs.getBoolean("inputKeepText", true);
					}
				}
			catch (BackingStoreException e)
				{
				System.out.println("Unable to load preferences, using defaults");
				loadDefaults();
				}
			}
			
		/**
		  * apply these settings to the application
		  */
		public void apply()
			{
			frame.setLocation(windowLocation);
			frame.setSize(windowSize);
			}
		
		/**
		  * saves the settings using the preferences API.
		  * This means that I don't have to assume that 
		  * preferences are stored in any particular directory.
		  */
		public void save()
			{
			// first, update the settings
			windowLocation = frame.getLocation();
			windowSize = frame.getSize();
			
			inputLocation = inputDialog.getLocation();
			inputSize = inputDialog.getSize();
			inputKeepText = keepContentBox.isSelected();

			// store the settings			
			Preferences prefs = Preferences.userRoot().node(preferencesPathName);
			prefs.putInt("windowLocationX", windowLocation.x);
			prefs.putInt("windowLocationY", windowLocation.y);
			prefs.putInt("windowSizeWidth", windowSize.width);
			prefs.putInt("windowSizeHeight", windowSize.height);
			prefs.put("workingDirectory", workingDirectory.getAbsolutePath());
			prefs.putBoolean("featuresEnabled", featuresEnabled);
			prefs.putBoolean("terminalLinesEnabled", terminalLinesEnabled);

			prefs.putInt("inputLocationX", inputLocation.x);
			prefs.putInt("inputLocationY", inputLocation.y);
			prefs.putInt("inputSizeWidth", inputSize.width);
			prefs.putInt("inputSizeHeight", inputSize.height);

			prefs.putBoolean("inputKeepText", inputKeepText);
			
			// Java flushes the preferences automatically, so I don't do it
			}
		}
		
	}
