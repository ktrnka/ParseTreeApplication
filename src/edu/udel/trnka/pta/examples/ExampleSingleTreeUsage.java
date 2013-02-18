package edu.udel.trnka.pta.examples;

import javax.swing.*;

import edu.udel.trnka.pta.ParseTree;
import edu.udel.trnka.pta.ParseTreeApplication;
import edu.udel.trnka.pta.ParseTreePanel;

/**
 * Example application that's just a window with a parse tree in it.
 */
public class ExampleSingleTreeUsage
	{
	public static void main(String[] args) throws Exception
		{
		// This is ugly code - there needs to be an instance of PTA around for the rendering settings.
		// Unfortunately, I didn't anticipate anyone interested in using PTA as an API when I started.
		ParseTreeApplication appInstance = new ParseTreeApplication();

		JFrame mainFrame = new JFrame("Example parse tree");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// The static function "build" parses the string and makes a ParseTree.
		// The ParseTreePanel handles the rendering.
		ParseTreePanel panel = new ParseTreePanel(ParseTree.build("(S (NP (PRP This)) (VP (VBZ is) (NP (DT an) (NN example) (NN parse))) .)"));

		mainFrame.setContentPane(panel);
		mainFrame.pack();
		mainFrame.setVisible(true);
		}
	}
