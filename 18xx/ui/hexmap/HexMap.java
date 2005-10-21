/*
 * Created on Aug 4, 2005
 */
package ui.hexmap;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import game.*;
import ui.*;

/**
 * Base class that stores common info for HexMap independant of Hex orientations. 
 */
public abstract class HexMap extends JComponent implements MouseListener,
		WindowListener
{
	// Abstract Methods
	protected abstract void setupHexesGUI();
	
	// GUI hexes need to be recreated for each object, since scale varies.
	protected GUIHex[][] h;
	protected ArrayList hexes;
	
	protected int scale = 2 * Scale.get();
	protected int cx = 6 * scale;
	protected int cy = 2 * scale;

	protected ImageLoader imageLoader = new ImageLoader();
	private boolean hexSelected = false;

	void setupHexes()
	{
		setupHexesGUI();
	}

	void unselectAllHexes()
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (hex.isSelected())
			{
				hex.unselect();
				this.repaint();
			}
		}
	}

	/**
	 * Return the GUIBattleHex that contains the given point, or null if none
	 * does.
	 */
	GUIHex getHexContainingPoint(Point2D.Double point)
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (hex.contains(point))
			{
				return hex;
			}
		}

		return null;
	}

	GUIHex getHexContainingPoint(Point point)
	{
		Iterator it = hexes.iterator();
		while (it.hasNext())
		{
			GUIHex hex = (GUIHex) it.next();
			if (hex.contains(point))
			{
				return hex;
			}
		}

		return null;
	}

	public void paintComponent(Graphics g)
	{
		try
		{
			super.paintComponent(g);
			
			// Abort if called too early.
			Rectangle rectClip = g.getClipBounds();
			if (rectClip == null)
			{
				return;
			}

			Iterator it = hexes.iterator();
			while(it.hasNext())
			{
				GUIHex hex = (GUIHex) it.next();
				Rectangle hexrect = hex.getBounds();

				if (g.hitClip(hexrect.x,
						hexrect.y,
						hexrect.width,
						hexrect.height))
				{
						hex.paint(g);
				}
			}
		}
		catch (NullPointerException ex)
		{
			// If we try to paint before something is loaded, just retry later.
		}
	}

	public Dimension getMinimumSize()
	{
		Dimension dim = getPreferredSize();
		dim.height /= 2;
		dim.width /= 2;
		return dim;
	}

	public Dimension getPreferredSize()
	{
		return new Dimension(60 * Scale.get(), 45 * Scale.get());
	}

	public void mouseClicked(MouseEvent arg0)
	{
		Point point = arg0.getPoint();

		try
		{
			GUIHex hex = getHexContainingPoint(point);

			// Temporary, to check for correct neighbour setting
			StringBuffer b = new StringBuffer();
			b.append("Clicked hex ")
					.append(hex.getName())
					.append(" Neighbors:");
			MapHex[] nb = hex.getHexModel().getNeighbors();
			for (int i = 0; i < 6; i++)
			{
				if (nb[i] != null)
					b.append(" ").append(i).append(":").append(nb[i].getName());
			}
			System.out.println(b.toString());

			if (hex.isSelected() && hexSelected == true)
			{
				hex.x_adjust = hex.x_adjust_arr[hex.arr_index];
				hex.y_adjust = hex.y_adjust_arr[hex.arr_index];
				hex.rotation = hex.rotation_arr[hex.arr_index];

				hex.rotateHexCW();
			}
			else if (!hex.isSelected() && hexSelected == false)
			{
				hex.setSelected(true);
				hexSelected = true;
			}
			else
			{
				unselectAllHexes();
				hex.setSelected(true);
				hexSelected = true;
			}

			//FIXME: Performance of this repaint could be improved.
			this.repaint(hex.getBounds());
			
			// setToolTipText (hex.getToolTip());
		}
		catch (NullPointerException e)
		{
			// No hex clicked
			if (hexSelected)
			{
				unselectAllHexes();
				hexSelected = false;
			}
		}
	}

	public void mouseEntered(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	public void windowActivated(WindowEvent e)
	{
		// TODO Auto-generated method stub
	}

	public void windowClosed(WindowEvent e)
	{
		// TODO Auto-generated method stub

	}

	public void windowClosing(WindowEvent e)
	{
		// TODO Auto-generated method stub

	}

	public void windowDeactivated(WindowEvent e)
	{
		// TODO Auto-generated method stub

	}

	public void windowDeiconified(WindowEvent e)
	{
		// TODO Auto-generated method stub

	}

	public void windowIconified(WindowEvent e)
	{
		// TODO Auto-generated method stub

	}

	public void windowOpened(WindowEvent e)
	{
		// TODO Auto-generated method stub

	}

}
