package edu.udel.trnka.pta.examples;

import javax.swing.*;

import edu.udel.trnka.pta.ComparedParseTree;
import edu.udel.trnka.pta.ParseTree;
import edu.udel.trnka.pta.ParseTreeApplication;
import edu.udel.trnka.pta.ParseTreePanel;

import java.awt.*;

/**
 * Example application that's a window with two parse trees that have been compared.
 */
public class ExampleCompareTreeUsage
	{
	public static void main(String[] args) throws Exception
		{
		// This is ugly code - there needs to be an instance of PTA around for the rendering settings.
		// Unfortunately, I didn't anticipate anyone interested in using PTA as an API when I started.
		ParseTreeApplication appInstance = new ParseTreeApplication();

		JFrame mainFrame = new JFrame("Example comparison");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// make a couple trees of the same content
		ParseTree tree1 = ParseTree.build("(NP (ADJP nice ass) (NOMP (NN car)))");
		ParseTree tree2 = ParseTree.build("(NP (ADJP (ADJ nice)) (NOMP (NN ass) (NN car)))");

		// create the renderings of them
		ParseTreePanel panel1 = new ParseTreePanel(tree1);
		ParseTreePanel panel2 = new ParseTreePanel(tree2);

		// compare the trees - note that a lot happens in these methods.  
		// Also note that it applies for arbitrarily many trees, but they have to have to be parses of the same text.
		ComparedParseTree[] trees = new ComparedParseTree[2];
		trees[0] = panel1.promoteForComparison();
		trees[1] = panel2.promoteForComparison();
		ComparedParseTree.compare(trees);

		// Just making one on the left, one on the right.
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(panel1, BorderLayout.WEST);
		panel.add(panel2, BorderLayout.EAST);

		mainFrame.setContentPane(panel);

		mainFrame.pack();
		mainFrame.setVisible(true);
		}
	}
