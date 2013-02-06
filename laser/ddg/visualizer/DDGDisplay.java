package laser.ddg.visualizer;

/**
 * displays a DDG using prefuse
 * 
 * @author Antonia Miruna Oprescu
 * 
 */
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.controls.Control;
import prefuse.data.tuple.TupleSet;
import prefuse.visual.VisualItem;
import prefuse.visual.tuple.TableNodeItem;

public class DDGDisplay extends Display {
	private static final String STEP = "Step";
	private static final String START = "Start";
	private static final String TYPE = "Type";

	// collapse or expand control
	private MouseControl mControl = new MouseControl();

	// proportions for the position of the focus center
	private double proportionX = 0.25;
	private double proportionY = 0.25;

	public void closeWindow() {
		Window frame = (Window) SwingUtilities.getRoot(this);
		frame.dispose();
	}

	@Override
	public void addControlListener(Control cl) {
		m_controls.add(cl);
		if (cl instanceof MouseControl) {
			mControl = (MouseControl) cl;
		}
	}
	
	public void stopRefocusing() {
		// change the position of the focus center
		proportionX = 0;
		proportionY = -0.25;
	}

	public class AutoPanAction extends Action {
		private Point2D mCur = new Point2D.Double();
		private int xBias;
		private int yBias;

		@Override
		public void run(double frac) {
			TupleSet ts = m_vis.getFocusGroup(Visualization.FOCUS_ITEMS);
			if (ts.getTupleCount() == 0) {
				return;
			}
			
			xBias = (int) (getWidth() * proportionX);
			yBias = (int) (getHeight() * proportionY);
			VisualItem vi = (VisualItem) ts.tuples().next();
			mCur.setLocation(vi.getX() + xBias, vi.getY() - yBias);
			panToAbs(mCur);
		}

	}

	public class PopupMenu implements ActionListener, ItemListener {

		private Point p = new Point();

		public void createPopupMenu() {
			JMenuItem menuItem;
			// Create the popup menu.
			JPopupMenu popup = new JPopupMenu();
			menuItem = new JMenuItem("Expand All");
			menuItem.addActionListener(this);
			popup.add(menuItem);

			// Add listener so the popup menu can come up.
			MouseListener popupListener = new PopupListener(popup);
			addMouseListener(popupListener);
		}

		class PopupListener extends MouseAdapter {
			private JPopupMenu popup;

			PopupListener(JPopupMenu popupMenu) {
				popup = popupMenu;
			}

			@Override
			public void mousePressed(MouseEvent e) {
				showPopupMenu(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				showPopupMenu(e);
			}

			private void showPopupMenu(MouseEvent e) {
				if (e.isPopupTrigger()) {
					p = e.getPoint();
					VisualItem item = findItem(p);
					// popup menu comes up only if the node is type "Step" or
					// "Start"
					if (item instanceof TableNodeItem) {
						String itemType = item.getString(TYPE);
						if (itemType.equals(STEP)) {
							showPopup(e, "Expand All");
						}

						else if (itemType.equals(START)) {
							showPopup(e, "Collapse");
						}

					}
				}
			}

			private void showPopup(MouseEvent e, String command) {
				((JMenuItem) (popup.getSubElements())[0])
					.setText(command);
				popup.show(e.getComponent(), e.getX(), e.getY());
			}

		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// expands all the nodes within a step and focuses on the start node
			// collapses a step and focuses on it
			Visualization vis = getVisualization();
			VisualItem item = findItem(p);
			String state = mControl.collapseExpand(item);
			if (state.equals(START)) {
				mControl.updateFocusGroup(mControl.getStepId(), vis);
			}
			else if (state.equals(STEP)) {
				mControl.updateFocusGroup(mControl.getStartId(), vis);
			}
			vis.run("color");
			vis.run("repaint");
		}

	}

}
