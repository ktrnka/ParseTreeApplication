package edu.udel.trnka.pta;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;

/**
 * Window wrapper around ParseTreePanel
 * 
 * @author keith.trnka
 * 
 */
public class ParseTreeWindow extends JInternalFrame
	{
	public JMenuItem windowToFrontMenuItem;
	ParseTreePanel panel;

	private Component parent;

	public ParseTreeWindow(Component parent, ParseTree t)
		{
		this(parent, t, "Parse Tree");
		}

	public ParseTreeWindow(Component parent, ParseTree t, String title)
		{
		super(title, true, true, true, true);

		this.parent = parent;

		if (t != null)
			{
			panel = new ParseTreePanel(t);
			getContentPane().add(panel);

			pack();

			// use pack to determine the desired size of the panel with the window included
			// Then add scrollbars if necessary.
			if (getWidth() > parent.getWidth() || getHeight() > parent.getHeight())
				{
				// resize the window
				Dimension size = getSize();
				if (getWidth() > parent.getWidth())
					size.width = parent.getWidth();
				if (getHeight() > parent.getHeight())
					size.height = parent.getHeight();
				setSize(size);

				// add it with the scroll pane
				setContentPane(new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
				}
			}
		
		windowToFrontMenuItem = new JMenuItem(title);
		windowToFrontMenuItem.addActionListener(new ActionListener()
			{
			public void actionPerformed(ActionEvent e)
				{
				try
					{
					// un-minimize it if it's minimized
					if (isIcon())
						setIcon(false);

					// bring it to the front
					toFront();

					// make it the selected window
					setSelected(true);
					}
				catch (java.beans.PropertyVetoException exc)
					{
					// nothing we can do
					}
				}
			});
		}
	
	/**
	  * re-pack this window in case the contents
	  * have changed. It is safe to use with ParseTreeWindows
	  * that have scroll bars. If it weren't, you could just
	  * use pack()
	  */
	public void repack()
		{
		// get the preferred size of the window
		Dimension preferred = getPreferredSize();

		// the content pane will be this
		Container content = panel;

		// possibly shrink the window to fit
		if (preferred.width > parent.getWidth() || preferred.height > parent.getHeight())
			{
			// determine the new preferred size
			if (getWidth() > parent.getWidth())
				preferred.width = parent.getWidth();
			if (getHeight() > parent.getHeight())
				preferred.height = parent.getHeight();

			// reset the content to be the panel inside a scroll pane
			content = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			}

		// resize it
		setSize(preferred);
		setContentPane(content);
		}
		
	public void saveToWorkspace(DataOutputStream out) throws IOException
		{
		// save the location
		Point p = getLocation();
		out.writeInt((int)p.getX());
		out.writeInt((int)p.getY());
		
		// save the size
		Dimension d = getSize();
		out.writeInt((int)d.getWidth());
		out.writeInt((int)d.getHeight());
		
		// save the title
		out.writeUTF(getTitle());
		
		// save the parse tree panel
		panel.saveToWorkspace(out);
		}
		
	public static ParseTreeWindow loadFromWorkspace(DataInputStream in) throws IOException
		{
		// read in the location
		Point p = new Point(in.readInt(), in.readInt());
		
		// read in the size
		Dimension d = new Dimension(in.readInt(), in.readInt());
		
		// read the title
		String title = in.readUTF();
		
		// read the parse tree panel
		ParseTreePanel panel = ParseTreePanel.loadFromWorkspace(in);
		
		// create a new window
		ParseTreeWindow window = new ParseTreeWindow(null, null, title);
		window.panel = panel;
		window.setLocation(p);
		window.setSize(d);
		
		return window;
		}
	}
