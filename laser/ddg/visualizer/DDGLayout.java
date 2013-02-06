package laser.ddg.visualizer;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import prefuse.Constants;
import prefuse.Display;
import prefuse.action.layout.graph.TreeLayout;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Schema;
import prefuse.data.tuple.TupleSet;
import prefuse.util.ArrayLib;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

/**
 * Creates a layout for DDG graphs
 * 
 * @author Antonia Miruna Oprescu
 * 
 */
public class DDGLayout extends TreeLayout {

	private int mOrientation; // the orientation of the tree
	private double mBspace = 5; // the spacing between sibling nodes
	private double mTspace = 25; // the spacing between subtrees
	private double mDspace = 50; // the spacing between depth levels
	private double mOffset = 50; // pixel offset for root node position

	private double[] mDepths = new double[10];
	private int mMaxDepth = 0;

	private double mAx;
	private double mAy; // for holding anchor co-ordinates

	// keep track of the nodes already added to the layout
	private Set<NodeItem> laidOutNodes;
	private Set<NodeItem> secondWalkDone;

	/**
	 * Create a new NodeLinkTreeLayout. A left-to-right orientation is assumed.
	 * 
	 * @param group
	 *            the data group to layout. Must resolve to a Graph instance.
	 */
	public DDGLayout(String group) {
		super(group);
		mOrientation = Constants.ORIENT_TOP_BOTTOM;
	}

	/**
	 * replaces method neighbors() from class Node
	 * 
	 * @param n
	 * @return an iterator over the neighbors of a node, ordered by their Ids
	 */
	private Iterator<NodeItem> orderedNeighbors(NodeItem n) {

		Iterator neighbors = n.neighbors();
		NodeItem neighbor;
		List<NodeItem> orderedNeighborList = new ArrayList<NodeItem>();

		while (neighbors.hasNext()) {
			neighbor = (NodeItem) neighbors.next();
			orderedNeighborList.add(neighbor);
		}

		Collections.sort(orderedNeighborList, new Comparator<NodeItem>() {
			@Override
			public int compare(NodeItem node1, NodeItem node2) {
				return node1.getInt("id") - node2.getInt("id");
			}

		});

		return orderedNeighborList.iterator();
	}

	/**
	 * method getChildCount() from class Node
	 * 
	 * @param n
	 * @return the number of visible neighbors which are connected to the node
	 *         by outgoing edges
	 */
	private int getChildCount(NodeItem n) {

		int childrenNum = 0;
		Iterator<NodeItem> neighbors = orderedNeighbors(n);
		NodeItem neighbor;
		EdgeItem neighborEdge;

		while (neighbors.hasNext()) {
			neighbor = neighbors.next();
			neighborEdge = (EdgeItem) (n.getGraph()).getEdge(neighbor, n);

			if (neighborEdge != null && neighborEdge.isVisible()) {
				childrenNum++;
			}
		}

		return childrenNum;
	}

	/**
	 * replaces method getFirstChild() from class Node
	 * 
	 * @param n
	 * @return the first visible neighbor connected to the node by an outgoing
	 *         edge or null if none
	 */
	private NodeItem getFirstChild(NodeItem n) {

		NodeItem firstChild = null;
		Iterator<NodeItem> neighbors = orderedNeighbors(n);
		NodeItem neighbor;
		EdgeItem neighborEdge;

		while (neighbors.hasNext()) {
			neighbor = neighbors.next();
			neighborEdge = (EdgeItem) (n.getGraph()).getEdge(neighbor, n);

			if (neighborEdge != null && neighborEdge.isVisible()) {
				firstChild = neighbor;
				break;
			}
		}

		return firstChild;
	}

	/**
	 * replaces method getLastChild() from class Node
	 * 
	 * @param n
	 * @return the first neighbor connected to the node by an outgoing edge or
	 *         null if none
	 */
	private NodeItem getLastChild(NodeItem n) {

		NodeItem lastChild = null;
		int childrenNum = getChildCount(n);
		Iterator<NodeItem> neighbors = orderedNeighbors(n);
		NodeItem neighbor;
		EdgeItem neighborEdge;
		while (neighbors.hasNext()) {
			neighbor = neighbors.next();
			neighborEdge = (EdgeItem) (n.getGraph()).getEdge(neighbor, n);

			if (neighborEdge != null && neighborEdge.isVisible()) {
				childrenNum--;
				if (childrenNum == 0) {
					lastChild = neighbor;
					break;
				}
			}
		}

		return lastChild;
	}

	/**
	 * replaces method getParent() from class Node
	 * 
	 * @param n
	 * @return the first neighbor connected to the node by an incoming edge
	 */
	private NodeItem getParent(NodeItem n) {

		NodeItem parent = null;
		Iterator<NodeItem> neighbors = orderedNeighbors(n);
		NodeItem neighbor;
		EdgeItem nodeNeighborEdge;

		while (neighbors.hasNext()) {
			neighbor = neighbors.next();
			nodeNeighborEdge = (EdgeItem) (n.getGraph()).getEdge(n, neighbor);

			if (nodeNeighborEdge != null && nodeNeighborEdge.isVisible()) {
				parent = neighbor;
				break;
			}
		}

		return parent;
	}

	/**
	 * @param n
	 * @return an interator over the children of a node
	 */
	private Iterator<NodeItem> children(NodeItem n) {

		Iterator<NodeItem> neighbors = orderedNeighbors(n);
		NodeItem neighbor;
		EdgeItem nodeNeighborEdge;
		List<NodeItem> childrenList = new ArrayList<NodeItem>();

		while (neighbors.hasNext()) {
			neighbor = neighbors.next();
			nodeNeighborEdge = (EdgeItem) (n.getGraph()).getEdge(neighbor, n);

			if (nodeNeighborEdge != null && nodeNeighborEdge.isVisible()) {
				childrenList.add(neighbor);
			}
		}

		return childrenList.iterator();
	}

	/**
	 * 
	 * @param n
	 * @param c
	 * @return the next child of a node, current child being c
	 */
	private NodeItem getNextChild(NodeItem n, NodeItem c) {
		NodeItem nextChild = null;
		Iterator<NodeItem> children = children(n);
		NodeItem child;
		boolean foundNode = false;
		
		while (children.hasNext()) {
			child = children.next();
			if (foundNode) {
				nextChild = child;
				break;
			}
			if (child == c) {
				foundNode = true;
			}
		}

		return nextChild;
	}

	/**
	 * 
	 * @param n
	 * @param c
	 * @return the previous child of a node, current child being c
	 */
	private NodeItem getPreviousChild(NodeItem n, NodeItem c) {
		if (n == null) {
			return null;
		}
		NodeItem lastChild = null;
		Iterator<NodeItem> children = children(n);
		NodeItem child;
		
		while (children.hasNext()) {
			child = children.next();
			if (child == c) {
				break;
			}
			lastChild = child;
		}

		return lastChild;
	}

	// ------------------------------------------------------------------------

	/**
	 * Set the orientation of the tree layout.
	 * 
	 * @param orientation
	 *            the orientation value. One of
	 *            {@link prefuse.Constants#ORIENT_LEFT_RIGHT},
	 *            {@link prefuse.Constants#ORIENT_RIGHT_LEFT},
	 *            {@link prefuse.Constants#ORIENT_TOP_BOTTOM}, or
	 *            {@link prefuse.Constants#ORIENT_BOTTOM_TOP}.
	 */
	public void setOrientation(int orientation) {
		if (orientation < 0 || orientation >= Constants.ORIENTATION_COUNT
				|| orientation == Constants.ORIENT_CENTER) {
			throw new IllegalArgumentException(
					"Unsupported orientation value: " + orientation);
		}
		mOrientation = orientation;
	}

	/**
	 * Get the orientation of the tree layout.
	 * 
	 * @return the orientation value. One of
	 *         {@link prefuse.Constants#ORIENT_LEFT_RIGHT},
	 *         {@link prefuse.Constants#ORIENT_RIGHT_LEFT},
	 *         {@link prefuse.Constants#ORIENT_TOP_BOTTOM}, or
	 *         {@link prefuse.Constants#ORIENT_BOTTOM_TOP}.
	 */
	public int getOrientation() {
		return mOrientation;
	}

	/**
	 * Set the spacing between depth levels.
	 * 
	 * @param d
	 *            the depth spacing to use
	 */
	public void setDepthSpacing(double d) {
		mDspace = d;
	}

	/**
	 * Get the spacing between depth levels.
	 * 
	 * @return the depth spacing
	 */
	public double getDepthSpacing() {
		return mDspace;
	}

	/**
	 * Set the spacing between neighbor nodes.
	 * 
	 * @param b
	 *            the breadth spacing to use
	 */
	public void setBreadthSpacing(double b) {
		mBspace = b;
	}

	/**
	 * Get the spacing between neighbor nodes.
	 * 
	 * @return the breadth spacing
	 */
	public double getBreadthSpacing() {
		return mBspace;
	}

	/**
	 * Set the spacing between neighboring subtrees.
	 * 
	 * @param s
	 *            the subtree spacing to use
	 */
	public void setSubtreeSpacing(double s) {
		mTspace = s;
	}

	/**
	 * Get the spacing between neighboring subtrees.
	 * 
	 * @return the subtree spacing
	 */
	public double getSubtreeSpacing() {
		return mTspace;
	}

	/**
	 * Set the offset value for placing the root node of the tree. The dimension
	 * in which this offset is applied is dependent upon the orientation of the
	 * tree. For example, in a left-to-right orientation, the offset will a
	 * horizontal offset from the left edge of the layout bounds.
	 * 
	 * @param o
	 *            the value by which to offset the root node of the tree
	 */
	public void setRootNodeOffset(double o) {
		mOffset = o;
	}

	/**
	 * Get the offset value for placing the root node of the tree.
	 * 
	 * @return the value by which the root node of the tree is offset
	 */
	public double getRootNodeOffset() {
		return mOffset;
	}

	// ------------------------------------------------------------------------

	/**
	 * @see prefuse.action.layout.Layout#getLayoutAnchor()
	 */
	@Override
	public Point2D getLayoutAnchor() {
		if (m_anchor != null) {
			return m_anchor;
		}

		m_tmpa.setLocation(0, 0);
		if (m_vis != null) {
			Display d = m_vis.getDisplay(0);
			Rectangle2D b = this.getLayoutBounds();
			switch (mOrientation) {
			case Constants.ORIENT_LEFT_RIGHT:
				m_tmpa.setLocation(mOffset, d.getHeight() / 2.0);
				break;
			case Constants.ORIENT_RIGHT_LEFT:
				m_tmpa.setLocation(b.getMaxX() - mOffset, d.getHeight() / 2.0);
				break;
			case Constants.ORIENT_TOP_BOTTOM:
				m_tmpa.setLocation(d.getWidth() / 2.0, mOffset);
				break;
			case Constants.ORIENT_BOTTOM_TOP:
				m_tmpa.setLocation(d.getWidth() / 2.0, b.getMaxY() - mOffset);
				break;
			}
			d.getInverseTransform().transform(m_tmpa, m_tmpa);
		}
		return m_tmpa;
	}

	private double spacing(NodeItem l, NodeItem r, boolean siblings) {
		double totalDistance;
		if (mOrientation == Constants.ORIENT_TOP_BOTTOM || mOrientation == Constants.ORIENT_BOTTOM_TOP) {
			totalDistance = l.getBounds().getWidth() + r.getBounds().getWidth();
		}
		else {
			totalDistance = l.getBounds().getHeight() + r.getBounds().getHeight();
		}
		double space;
		if (siblings) {
			space = mBspace;
		}
		else {
			space = mTspace;
		}
		return space + 0.5 * totalDistance;
	}

	private void updateDepths(int depth, NodeItem item) {
		double d;
		if (mOrientation == Constants.ORIENT_TOP_BOTTOM || mOrientation == Constants.ORIENT_BOTTOM_TOP) {
			d = item.getBounds().getHeight();
		}
		else {
			d = item.getBounds().getWidth();
		}
		if (mDepths.length <= depth) {
			mDepths = ArrayLib.resize(mDepths, 3 * depth / 2);
		}
		mDepths[depth] = Math.max(mDepths[depth], d);
		mMaxDepth = Math.max(mMaxDepth, depth);
	}

	private void determineDepths() {
		for (int i = 1; i < mMaxDepth; ++i) {
			mDepths[i] += mDepths[i - 1] + mDspace;
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * @see prefuse.action.Action#run(double)
	 */
	@Override
	public void run(double frac) {

		// initialize the two sets every time the layout is done
		laidOutNodes = new HashSet<NodeItem>();
		secondWalkDone = new HashSet<NodeItem>();

		Graph g = (Graph) m_vis.getGroup(m_group);
		initSchema(g.getNodes());

		Arrays.fill(mDepths, 0);
		mMaxDepth = 0;

		Point2D a = getLayoutAnchor();
		mAx = a.getX();
		mAy = a.getY();

		NodeItem root = getLayoutRoot();
		Params rp = getParams(root);

		// do first pass - compute breadth information, collect depth info
		firstWalk(root, 0, 1);

		// sum up the depth info
		determineDepths();

		// do second pass - assign layout positions
		secondWalk(root, null, -rp.prelim, 0);
	}

	private void firstWalk(NodeItem n, int num, int depth) {

		// returns if the node has already been laid out. Avoids infinite while
		// loops corresponding to closed loops in the graph.

		if (laidOutNodes.contains(n)) {
			return;
		}
		laidOutNodes.add(n);

		Params np = getParams(n);
		np.number = num;

		updateDepths(depth, n);

		boolean expanded = n.isExpanded();

		if ((getChildCount(n) == 0 || !expanded) && n.isVisible()) // is leaf,
																	// added
																	// visibility
																	// condition
		{

			NodeItem l = getPreviousChild(getParent(n), n);

			if (l == null) {
				np.prelim = 0;
			} else {
				np.prelim = getParams(l).prelim + spacing(l, n, true);
			}

		} else if (expanded && n.isVisible()) {
			NodeItem leftMost = getFirstChild(n);
			NodeItem rightMost = getLastChild(n);
			NodeItem defaultAncestor = leftMost;
			NodeItem c = leftMost;

			for (int i = 0; c != null; ++i, c = getNextChild(n, c)) {
				firstWalk(c, i, depth + 1);
				defaultAncestor = apportion(c, defaultAncestor);
			}

			executeShifts(n);

			double midpoint = 0.5 * (getParams(leftMost).prelim + getParams(rightMost).prelim);
			
			NodeItem left = getPreviousChild(getParent(n), n);

			if (left != null) {

				np.prelim = getParams(left).prelim + spacing(left, n, true);
				np.mod = np.prelim - midpoint;
			} else {

				np.prelim = midpoint;
			}
		}

	}

	private NodeItem apportion(NodeItem v, NodeItem a) {

		NodeItem w = getPreviousChild(getParent(v), v);

		if (w != null) {

			NodeItem vip = v;
			NodeItem vim = w;
			NodeItem vop = v;
			NodeItem vom = getFirstChild(getParent(vip));

			double sip = getParams(vip).mod;
			double sop = getParams(vop).mod;
			double sim = getParams(vim).mod;
			double som = getParams(vom).mod;

			NodeItem nr = nextRight(vim);
			NodeItem nl = nextLeft(vip);

			// keep track of the laid out nodes
			Set<NodeItem> iteratedOverNr = new HashSet<NodeItem>();
			Set<NodeItem> iteratedOverNl = new HashSet<NodeItem>();

			while (nr != null && nl != null && !iteratedOverNr.contains(nr)
					&& !iteratedOverNl.contains(nl)) {

				iteratedOverNr.add(nr);
				iteratedOverNr.add(nl);

				vim = nr;
				vip = nl;
				vom = nextLeft(vom);
				vop = nextRight(vop);

				getParams(vop).ancestor = v;
				double shift = (getParams(vim).prelim + sim)
						- (getParams(vip).prelim + sip)
						+ spacing(vim, vip, false);
				if (shift > 0) {
					moveSubtree(ancestor(vim, v, a), v, shift);
					sip += shift;
					sop += shift;
				}
				sim += getParams(vim).mod;
				sip += getParams(vip).mod;
				som += getParams(vom).mod;
				sop += getParams(vop).mod;

				nr = nextRight(vim);
				nl = nextLeft(vip);

			}

			if (nr != null && nextRight(vop) == null) {
				Params vopp = getParams(vop);
				vopp.thread = nr;
				vopp.mod += sim - sop;
			}
			if (nl != null && nextLeft(vom) == null) {
				Params vomp = getParams(vom);
				vomp.thread = nl;
				vomp.mod += sip - som;
				a = v;
			}
		}
		return a;
	}

	private NodeItem nextLeft(NodeItem n) {
		NodeItem c = null;

		if (n.isExpanded()) {
			c = getFirstChild(n);
		}
		if (c == null) {
			return getParams(n).thread;
		}
		else {
			return c;
		}
	}

	private NodeItem nextRight(NodeItem n) {
		NodeItem c = null;
		if (n.isExpanded()) {
			c = getLastChild(n);
		}
		if (c == null) {
			return getParams(n).thread;
		}
		else {
			return c;
		}
	}

	private void moveSubtree(NodeItem wm, NodeItem wp, double shift) {
		Params wmp = getParams(wm);
		Params wpp = getParams(wp);

		double subtrees = wpp.number - wmp.number;

		if (subtrees == 0) {
			return;
		}

		wpp.change -= shift / subtrees;
		wpp.shift += shift;
		wmp.change += shift / subtrees;
		wpp.prelim += shift;
		wpp.mod += shift;
	}

	private void executeShifts(NodeItem n) {
		double shift = 0;
		double change = 0;
		for (NodeItem c = getLastChild(n); c != null; c = getPreviousChild(n, c)) {

			Params cp = getParams(c);
			cp.prelim += shift;
			cp.mod += shift;
			change += cp.change;
			shift += cp.shift + change;
		}
	}

	private NodeItem ancestor(NodeItem vim, NodeItem v, NodeItem a) {

		NodeItem p = getParent(v);
		Params vimp = getParams(vim);

		if (getParent(vimp.ancestor) == p) {
			return vimp.ancestor;
		} else {
			return a;
		}
	}

	private void secondWalk(NodeItem n, NodeItem p, double m, int depth) {

		if (secondWalkDone.contains(n)) {
			return;
		}
		secondWalkDone.add(n);

		Params np = getParams(n);
		setBreadth(n, p, np.prelim + m);
		setDepth(n, p, mDepths[depth]);

		if (n.isExpanded() && n.isVisible()) { // added visibility condition
			depth += 1;
			for (NodeItem c = getFirstChild(n); c != null; c = getNextChild(n, c)) {
				secondWalk(c, n, m + np.mod, depth);
			}
		}

		np.clear();
	}

	private void setBreadth(NodeItem n, NodeItem p, double b) {
		switch (mOrientation) {
		case Constants.ORIENT_LEFT_RIGHT:
		case Constants.ORIENT_RIGHT_LEFT:
			setY(n, p, mAy + b);
			break;
		case Constants.ORIENT_TOP_BOTTOM:
		case Constants.ORIENT_BOTTOM_TOP:
			setX(n, p, mAx + b);
			break;
		default:
			throw new IllegalStateException();
		}
	}

	private void setDepth(NodeItem n, NodeItem p, double d) {
		switch (mOrientation) {
		case Constants.ORIENT_LEFT_RIGHT:
			setX(n, p, mAx + d);
			break;
		case Constants.ORIENT_RIGHT_LEFT:
			setX(n, p, mAx - d);
			break;
		case Constants.ORIENT_TOP_BOTTOM:
			setY(n, p, mAy + d);
			break;
		case Constants.ORIENT_BOTTOM_TOP:
			setY(n, p, mAy - d);
			break;
		default:
			throw new IllegalStateException();
		}
	}

	// ------------------------------------------------------------------------
	// Params Schema

	/**
	 * The data field in which the parameters used by this layout are stored.
	 */
	public static final String PARAMS = "_reingoldTilfordParams";
	/**
	 * The schema for the parameters used by this layout.
	 */
	public static final Schema PARAMS_SCHEMA = new Schema();
	static {
		PARAMS_SCHEMA.addColumn(PARAMS, Params.class);
	}

	protected void initSchema(TupleSet ts) {
		ts.addColumns(PARAMS_SCHEMA);
	}

	private Params getParams(NodeItem item) {
		Params rp = null;
		if (item != null) {
			rp = (Params) item.get(PARAMS);
		}

		if (rp == null) {
			rp = new Params();
			if (item != null) {
				item.set(PARAMS, rp);
			}
		}
		if (rp.number == -2 && item != null) { 
			rp.init(item);
		}
		return rp;
	}

	/**
	 * Wrapper class holding parameters used for each node in this layout.
	 */
	public static class Params implements Cloneable {
		private double prelim;
		private double mod;
		private double shift;
		private double change;
		private int number = -2;
		private NodeItem ancestor = null;
		private NodeItem thread = null;

		public void init(NodeItem item) {
			ancestor = item;
			number = -1;
		}

		public void clear() {
			number = -2;
			prelim = 0;
			mod = 0;
			shift = 0;
			change = 0;
			ancestor = null; 
			thread = null;
		}
	}

} // end of class DDGLayout

