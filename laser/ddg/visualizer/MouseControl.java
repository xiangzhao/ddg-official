package laser.ddg.visualizer;

/**
 * Collapses/Expands sections of the DDG when a node is clicked
 * 
 * @author Antonia Miruna Oprescu
 * 
 */

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import prefuse.Visualization;
import prefuse.controls.ControlAdapter;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.tuple.TupleSet;
import prefuse.visual.VisualItem;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

public class MouseControl extends ControlAdapter {

	private static final String TYPE = "Type";
	private static final String STEP = "Step";
	private static final String START = "Start";
	private static final String NAME = "name";
	private static final String ID = "id";
	private static final String CONTROL_FLOW = "CF";
	private int stepId = 0;
	private int startId = 0;
	private boolean revalidated = false;

	public int getStepId() {
		return stepId;
	}

	public int getStartId() {
		return startId;
	}


	/**
	 * replaces method children() from class Node
	 * 
	 * @param n
	 * @return an iterator over neighbors connected to n by outgoing edges
	 */
	private Iterator<TableNodeItem> children(TableNodeItem n) {

		Iterator neighbors = n.neighbors();
		TableNodeItem neighbor;
		TableEdgeItem nodeNeighborEdge;
		List<TableNodeItem> childrenList = new ArrayList<TableNodeItem>();

		while (neighbors.hasNext()) {
			neighbor = (TableNodeItem) neighbors.next();
			nodeNeighborEdge = (TableEdgeItem) (n.getGraph()).getEdge(neighbor,
					n);

			if (nodeNeighborEdge != null) {
				childrenList.add(neighbor);
			}
		}

		return childrenList.iterator();
	}

	/**
	 * replaces method getParent() from class Node
	 * 
	 * @param node
	 * @return the first neighbor connected to node by an incoming, control flow
	 *         ("CF") type edge
	 */
	private TableNodeItem getParent(TableNodeItem node) {

		TableNodeItem parent = null;
		Iterator neighbors = node.neighbors();
		TableNodeItem neighbor;
		Edge nodeNeighborEdge;

		while (neighbors.hasNext()) {
			neighbor = (TableNodeItem) neighbors.next();
			nodeNeighborEdge = (node.getGraph()).getEdge(node, neighbor);

			if (nodeNeighborEdge != null) {
				if (nodeNeighborEdge.getString(TYPE).equals(CONTROL_FLOW)) {
					parent = neighbor;
					break;
				}
			}
		}

		return parent;
	}

	/**
	 * replaces method getFirstChild() from class Node
	 * 
	 * @param node
	 * @return the first neighbor connected to node by an outgoing, control flow
	 *         ("CF") type edge
	 */
	private TableNodeItem getChild(TableNodeItem node) {

		TableNodeItem child = null;
		Iterator neighbors = node.neighbors();
		TableNodeItem neighbor;
		Edge nodeNeighborEdge;

		while (neighbors.hasNext()) {
			neighbor = (TableNodeItem) neighbors.next();
			nodeNeighborEdge = (node.getGraph()).getEdge(neighbor, node);

			if (nodeNeighborEdge != null) {
				if (nodeNeighborEdge.getString(TYPE).equals(CONTROL_FLOW)) {
					child = neighbor;
					break;
				}
			}
		}

		return child;
	}

	/**
	 * gets the parent of a "Step" type node
	 * 
	 * @param node
	 * @return the first neighbor connected to the node by an outgoing, "Step"
	 *         type edge
	 */
	public TableNodeItem getStepParent(TableNodeItem node) {

		TableNodeItem stepParent = null;
		Iterator neighbors = node.neighbors();
		TableNodeItem neighbor;
		Edge nodeNeighborEdge;

		while (neighbors.hasNext()) {
			neighbor = (TableNodeItem) neighbors.next();
			nodeNeighborEdge = (node.getGraph()).getEdge(node, neighbor);

			if (nodeNeighborEdge != null) {
				if (nodeNeighborEdge.getString(TYPE).equals(STEP)) {
					stepParent = neighbor;
					break;
				}
			}
		}

		return stepParent;
	}

	/**
	 * returns the first child of a "Step" type node
	 * 
	 * @param node
	 * @return the first neighbor connected to the node by an incoming, "Step"
	 *         type edge
	 */
	public TableNodeItem getStepChild(TableNodeItem node) {

		TableNodeItem stepChild = null;
		Iterator neighbors = node.neighbors();
		TableNodeItem neighbor;
		Edge nodeNeighborEdge;

		while (neighbors.hasNext()) {
			neighbor = (TableNodeItem) neighbors.next();
			nodeNeighborEdge = (node.getGraph()).getEdge(neighbor, node);

			if (nodeNeighborEdge != null) {
				if (nodeNeighborEdge.getString(TYPE).equals(STEP)) {
					stepChild = neighbor;
					break;
				}
			}
		}

		return stepChild;
	}

	/**
	 * alternative for method getOutDegree() from class node. Finds the number
	 * of visible incident edges.
	 * 
	 * @param node
	 * @return
	 */
	private int getOutVDegree(TableNodeItem node) {
		Iterator neighbors = node.neighbors();
		TableNodeItem neighbor;
		TableEdgeItem edge;
		int outVDegree = 0;
		while (neighbors.hasNext()) {
			neighbor = (TableNodeItem) neighbors.next();
			edge = (TableEdgeItem) (node.getGraph()).getEdge(node, neighbor);
			if (edge != null && edge.isVisible()) {
				outVDegree++;
			}
		}
		return outVDegree;
	}

	// collapse methods //

	/**
	 * sets all nodes and incident edges between startNode and finishNode
	 * invisible nodes must be connected through control flow ("CF") edge or
	 * "Step" edge to their parent to be set invisible
	 * 
	 * @param startNode
	 * @param finishNode
	 */
	private void setNodesAndEdgesInvisible(TableNodeItem startNode,
			TableNodeItem finishNode) {

		startNode.setVisible(false);
		Iterator startNodeEdges = startNode.edges();
		while (startNodeEdges.hasNext()) {
			TableEdgeItem startNodeEdge = (TableEdgeItem) startNodeEdges.next();
			startNodeEdge.setVisible(false);
		}

		Iterator<TableNodeItem> startNodeChildren;
		TableNodeItem startNodeChild;
		if (startNode != finishNode) {
			startNodeChildren = children(startNode);
			while (startNodeChildren.hasNext()) {
				startNodeChild = startNodeChildren.next();
				if (checkFlow(startNodeChild, startNode) 
						&& startNodeChild.isVisible()) {
					setNodesAndEdgesInvisible(startNodeChild, finishNode);
				}
			}
		}

	}

	/**
	 * @param source
	 * @param target
	 * @return true if the edge between the source and the target is a control
	 *         flow ("CF") or "Step" type edge, false otherwise.
	 */
	private boolean checkFlow(TableNodeItem source, TableNodeItem target) {
		Edge edge = (source.getGraph()).getEdge(source, target);
		if (edge.getString(TYPE).equals(CONTROL_FLOW)
				|| edge.getString(TYPE).equals(STEP)) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * @param startNode
	 * @return the finish node corresponding to the start node
	 */
	private TableNodeItem finishNode(TableNodeItem startNode) {

		TableNodeItem nodeFinish = null;
		String startNodeName = startNode.getString(NAME);
		String finishNodeName = startNodeName.substring(0,
				startNodeName.lastIndexOf(START))
				+ "Finish";
		int startNodes = 1;
		int finishNodes = 0;
		Iterator nodes = (startNode.getGraph()).nodes();

		while (nodes.hasNext()) {
			TableNodeItem node = (TableNodeItem) nodes.next();
			if (node.getInt(ID) > startNode.getInt(ID)
					&& (node.getString(NAME)).equals(startNodeName)) {
				startNodes++;
			}
			if (node.getInt(ID) > startNode.getInt(ID)
					&& (node.getString(NAME)).equals(finishNodeName)) {
				finishNodes++;
			}

			if (startNodes == finishNodes) {
				nodeFinish = node;
				break;
			}
		}

		return nodeFinish;
	}

	/**
	 * sets nodes with no visible edges connected to them (isolated nodes)
	 * invisible these nodes exist because the setNodesAndEdgesInvisible(...)
	 * method does not follow data flow ("DF") edges
	 * 
	 * @param graph
	 */
	private void setIsolatedNodesInvisible(Graph graph) {

		Iterator graphNodes = graph.nodes();
		TableNodeItem node;
		boolean rootDrawn = false;
		while (graphNodes.hasNext()) {
			node = (TableNodeItem) graphNodes.next();
			if (node.isVisible() && getOutVDegree(node) == 0 && rootDrawn) {
				node.setVisible(false);
				Iterator incidentEdges = node.edges();
				while (incidentEdges.hasNext()) {
					((TableEdgeItem) incidentEdges.next()).setVisible(false);

				}
			}
			rootDrawn = true;
		}

	}

	/**
	 * sets all "Step" type nodes (and their incident edges) connected to the
	 * startNode passed as parameter visible
	 * 
	 * @param startNode
	 */
	private void setStepVisible(TableNodeItem startNode) {

		TableNodeItem startNodeParent = getParent(startNode);
		Iterator<TableNodeItem> children = children(startNodeParent);
		TableNodeItem child = null;
		Iterator stepEdges = null;
		TableEdgeItem stepEdge;

		assert children.hasNext();
		while (children.hasNext()) {
			child = children.next();
			if (child.getString(TYPE).equals(STEP)) {
				child.setVisible(true);
				stepEdges = child.edges();
				break;
			}
		}
		while (stepEdges.hasNext()) {
			stepEdge = (TableEdgeItem) stepEdges.next();
			stepEdge.setVisible(true);
		}

		stepId = child.getInt(ID);
	}

	// Expand methods //

	/**
	 * sets a node of type "Step" and its incident edges invisible
	 * 
	 * @param stepNode
	 */
	private void setStepInvisible(TableNodeItem stepNode) {

		stepNode.setVisible(false);
		Iterator stepEdges = stepNode.edges();
		TableEdgeItem stepEdge;

		while (stepEdges.hasNext()) {
			stepEdge = (TableEdgeItem) stepEdges.next();
			stepEdge.setVisible(false);
		}
	}

	/**
	 * checks whether the edge between two invisible nodes is a control flow
	 * ("CF") type edge
	 * 
	 * @param source
	 * @param target
	 * @return true if the edge is a control flow edge, false otherwise
	 */
	private boolean checkInvisibleFlow(TableNodeItem source, TableNodeItem target) {
		Edge edge = (source.getGraph()).getEdge(source, target);
		if (edge.getString(TYPE).equals(CONTROL_FLOW)) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * sets all nodes and incident edges between startNode and finishNode
	 * visible nodes must be connected through control flow ("CF") edge to their
	 * parent to be set visible
	 * 
	 * @param startNode
	 * @param finishNode
	 * @param isFirstNode
	 */
	private void setNodesAndEdgesVisible(TableNodeItem startNode,
			TableNodeItem finishNode, boolean isFirstNode) {

		startNode.setVisible(true);
		Iterator startNodeEdges;
		if (isFirstNode) {
			startNodeEdges = startNode.inEdges();
		}
		else {
			startNodeEdges = startNode.edges();
		}
		while (startNodeEdges.hasNext()) {
			TableEdgeItem startNodeEdge = (TableEdgeItem) startNodeEdges.next();
			if (!startNodeEdge.getString(TYPE).equals(STEP) 
					&& !(startNodeEdge.getTargetItem()).getString(TYPE).equals(
							STEP)
					&& !(startNodeEdge.getSourceItem()).getString(TYPE).equals(
							STEP)) {
				startNodeEdge.setVisible(true);
			}
		}

		Iterator<TableNodeItem> startNodeChildren;
		TableNodeItem child;
		if (startNode != finishNode) {
			startNodeChildren = children(startNode);
			while (startNodeChildren.hasNext()) {
				child = startNodeChildren.next();
				if (checkInvisibleFlow(child, startNode)
						&& !(child.isVisible())) {
					setNodesAndEdgesVisible(child, finishNode, false);
				}
			}
		}
	}

	/**
	 * sets invisible nodes with visible edges connected to them visible these
	 * nodes exist because the setNodesAndEdgesVisible(...) method does not
	 * follow data flow ("DF") edges
	 * 
	 * @param graph
	 */
	private void setIsolatedNodesVisible(Graph graph) {

		Iterator graphNodes = graph.nodes();
		TableNodeItem node;

		while (graphNodes.hasNext()) {
			node = (TableNodeItem) graphNodes.next();

			if (!node.isVisible()) {
				Iterator incidentEdges = node.edges();
				int visibleEdges = 0;

				while (incidentEdges.hasNext()) {
					TableEdgeItem edge = (TableEdgeItem) incidentEdges.next();
					if (edge.isVisible()) {
						visibleEdges++;
					}
				}
				if (visibleEdges != 0) {
					node.setVisible(true);
					Iterator edges = node.edges();
					while (edges.hasNext()) {
						TableEdgeItem edgeItem = (TableEdgeItem) (edges.next());
						if (!edgeItem.getSourceItem().getString(TYPE)
								.equals(STEP)
								&& !edgeItem.getTargetItem().getString(TYPE)
										.equals(STEP)) {
							edgeItem.setVisible(true);
						}
					}
				}

			}
		}

	}

	/**
	 * collapses nodes between a start node and a finish node when the start
	 * node is clicked expand nodes between a start and finish node when the
	 * step connecting them is clicked
	 * 
	 * @param item
	 */
	public String collapseExpand(VisualItem item) {

		if (item instanceof TableNodeItem && item.getString(TYPE).equals(START)) {

			TableNodeItem nodeItem = (TableNodeItem) item;

			if (finishNode(nodeItem) != null
					&& getChild(finishNode(nodeItem)) != null) {

				setNodesAndEdgesInvisible(nodeItem, finishNode(nodeItem));
				setStepVisible(nodeItem);
				setIsolatedNodesInvisible(nodeItem.getGraph());

				Visualization vis = item.getVisualization();
				vis.run("layout");

			}

			return START;
		}

		if (item instanceof TableNodeItem && item.getString(TYPE).equals(STEP)) {

			TableNodeItem nodeItem = (TableNodeItem) item;
			TableNodeItem startNode = getStepParent(nodeItem);
			startId = (getChild(startNode)).getInt(ID);
			TableNodeItem finishNode = getStepChild(nodeItem);

			setStepInvisible(nodeItem);
			setNodesAndEdgesVisible(startNode, finishNode, true);
			setIsolatedNodesVisible(nodeItem.getGraph());

			Visualization vis = item.getVisualization();
			vis.run("layout");

			return STEP;
		}

		return "Other";
	}

	/**
	 * updates the focus group to the step if a sequence is being collapsed or
	 * to the start node of the sequence if the sequence is being expanded
	 * 
	 * @param id
	 */
	public void updateFocusGroup(int nodeId, Visualization vis) {
		Iterator visualItems = vis.items();
		VisualItem item = null;
		final String group = Visualization.FOCUS_ITEMS;

		while (visualItems.hasNext()) {
			item = (VisualItem) visualItems.next();
			if (item instanceof TableNodeItem && item.getInt(ID) == nodeId) {
				TupleSet ts = vis.getFocusGroup(group);
				ts.setTuple(item);
				break;
			}
		}

		vis.run("animate");
	}

	/**
	 * expands one level at a time when step node is clicked
	 * 
	 * @param item
	 */
	public void expandOneLevel(VisualItem item) {

		Visualization vis = item.getVisualization();
		TableNodeItem stepItem = (TableNodeItem) item;
		TableNodeItem stepParent = getStepParent(stepItem);
		TableNodeItem stepChild = getStepChild(stepItem);

		int finishNodeId = 0;
		Iterator visualItems = vis.visibleItems();
		VisualItem node;

		while (visualItems.hasNext()) {
			node = (VisualItem) visualItems.next();
			if (node instanceof TableNodeItem
					&& node.getInt(ID) > (stepParent.getInt(ID) + 1)
					&& node.getInt(ID) < stepChild.getInt(ID)
					&& node.getString(TYPE).equals(START)
					&& node.getInt(ID) > finishNodeId) {
				collapseExpand(node);
				finishNodeId = finishNode((TableNodeItem) node).getInt(ID);
			}
		}

	}

	@Override
	public void itemClicked(VisualItem item, MouseEvent e) {

		Visualization vis = item.getVisualization();
		DDGDisplay d = (DDGDisplay) vis.getDisplay(0);

		// revalidates the display by finding the item with the same position as
		// the mouse pointer
		if (revalidated) {
			String state = collapseExpand(item);
			if (state.equals(START)) {
				updateFocusGroup(stepId, item.getVisualization());
				vis.run("color");
				vis.run("repaint");
			}
			if (state.equals(STEP)) {
				expandOneLevel(item);
				updateFocusGroup(startId, item.getVisualization());
				vis.run("color");
				vis.run("repaint");
			}
			revalidated = false;
		} else {
			revalidated = true;
			VisualItem myItem = d.findItem(e.getPoint());
			itemClicked(myItem, e);
		}

	}

}// end of class MouseControl
