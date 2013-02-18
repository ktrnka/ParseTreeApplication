package edu.udel.trnka.pta;
import javax.swing.JInternalFrame;

/**
 * Note to self: LASWindow is unable to properly call pack() on itself; it must
 * be added to a JDesktopPane first.
 */
public class LASWindow extends JInternalFrame
	{
	CondensedLexicalAmbiguitySelector las;
	public LASWindow(ComparedParseTreeSet treeSet)
		{
		super("Lexical Ambiguity Selector", true, true, true, true);
		las = new CondensedLexicalAmbiguitySelector(treeSet.trees);
		setContentPane(las);
		}
	}
