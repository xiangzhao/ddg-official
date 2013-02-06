package laser.ddg.visualizer;

import java.awt.HeadlessException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import laser.ddg.DataBindingEvent;
import laser.ddg.DataBindingEvent.BindingEvent;
import laser.ddg.DataInstanceNode;
import laser.ddg.ProcedureInstanceNode;
import laser.ddg.ProvenanceData;
import laser.ddg.ProvenanceListener;
import laser.ddg.visualizer.DDGDisplay.AutoPanAction;
import laser.ddg.visualizer.DDGDisplay.PopupMenu;
import prefuse.Constants;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.controls.DragControl;
import prefuse.controls.PanControl;
import prefuse.controls.ZoomControl;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.data.tuple.TupleSet;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

/**
 * Builds a visual DDG graph using prefuse.
 * 
 * @author Barbara Lerner, Antonia Miruna Oprescu
 * 
 */
public class PrefuseGraphBuilder implements ProvenanceListener {
	private static final int MIN_DIN_ID_DATA = ((Integer.MAX_VALUE) / 3) * 2;
	private static final int MIN_DIN_ID_STEP = Integer.MAX_VALUE / 3;
	private static final String TYPE = "Type";
	private static final String TARGET = "target";
	private static final String SOURCE = "source";
	private static final String NAME = "name";
	private static final String ID = "id";
	private static final String GRAPH = "graph";
	private static final String GRAPH_NODES = GRAPH + ".nodes";
	private static final String GRAPH_EDGES = GRAPH + ".edges";
	private static final int DATA_FLOW_COLOR = ColorLib.rgb(255, 0, 0);
	private static final int CONTROL_FLOW_COLOR = ColorLib.rgb(0, 255, 0);
	private static final int SIMPLE_HANDLER_COLOR = ColorLib.rgb(140, 209, 207);
	private static final int VIRTUAL_COLOR = ColorLib.rgb(217, 132, 181);
	private static final int EXCEPTION_COLOR = ColorLib.rgb(209, 114, 110);
	private static final int LEAF_COLOR = ColorLib.rgb(255, 255, 98);
	private static final int DATA_COLOR = ColorLib.rgb(175, 184, 233);
	private static final int NONLEAF_COLOR = ColorLib.rgb(175, 217, 123);

	// step color
	private static final int STEP_COLOR = ColorLib.rgb(176, 226, 255);
	private static final int STEP_EDGE_COLOR = ColorLib.rgb(0, 0, 0);

	private static final int INTERPRETER_COLOR = ColorLib.rgb(255, 191, 248);
	private Table nodes = new Table();
	private Table edges = new Table();
	private Graph graph;
	private boolean rootDrawn = false;

	// incremental collapse of the steps
	private boolean foundFinishNode = false;
	private Node finishNode = null;
	private Node startNode = null;
	private int successorId = 0;

	// nodes and edges types
	private static final String DATA_FLOW = "DF";
	private static final String CONTROL_FLOW = "CF";
	private static final String STEP = "Step";
	private static final String DATA_NODE = "Data";

	// visualization and display tools
	private Visualization vis = new Visualization();
	private BufferedReader in = new BufferedReader(new InputStreamReader(
			System.in));
	private MouseControl mControl = new MouseControl();

	// display
	private DDGDisplay d = new DDGDisplay();
	private AutoPanAction autoPan = d.new AutoPanAction();
	private String group = Visualization.FOCUS_ITEMS;
	
	

	/**
	 * replaces method getParent() from class node
	 * 
	 * @param n
	 * @return the first neighbor connected to the node by an incoming, control
	 *         flow ("CF") type edge
	 */
	private Node getParent(Node n) {

		Iterator neighbors = n.neighbors();
		Node neighbor;
		Node parent = null;
		Edge edge;

		while (neighbors.hasNext()) {
			neighbor = (Node) neighbors.next();
			edge = graph.getEdge(n, neighbor);
			if (edge != null) {
				if (edge.getString(TYPE).equals(CONTROL_FLOW)) {
					parent = neighbor;
					break;
				}
			}
		}

		return parent;
	}


	/**
	 * collapses start-finish node sequences as they are added to the ddg
	 * 
	 * @param nodeStart
	 */
	private void setStepsInvisibleIncrementally(Node nodeStart) {

		Iterator visualItems = vis.items();
		VisualItem vItem;

		while (visualItems.hasNext()) {
			vItem = (VisualItem) visualItems.next();
			if (vItem instanceof TableNodeItem 
					&& vItem.getInt(ID) == nodeStart.getInt(ID)) {
				mControl.collapseExpand(vItem);
			}
		}

	}

	/**
	 * @param nodeFinish
	 * @return the start node corresponding to the finish node passed as
	 *         parameter
	 */
	private Node completeGraphIncrementally(Node nodeFinish) {

		Node nodeStart = null;
		String nodeFinishName = nodeFinish.getString(NAME);
		String nodeStartName = nodeFinishName.substring(0,
				nodeFinishName.lastIndexOf("Finish"))
				+ "Start";
		int startNodes = 0;
		int finishNodes = 1;
		int nodesNum = graph.getNodeCount() - 1;

		while (nodesNum >= 0) {

			Node next = graph.getNode(nodesNum);
			if (next.getInt(ID) < nodeFinish.getInt(ID))  {
				if (next.getString(NAME).equals(nodeFinishName)) {
					finishNodes++;
				} 
				else if (next.getString(NAME).equals(nodeStartName)) {
					startNodes++;
				}
			}

			if (startNodes == finishNodes) {
				completeGraph(next, nodeFinish);
				nodeStart = next;
				break;
			}
			nodesNum--;
		}

		return nodeStart;
	}

	/**
	 * adds a step node to the graph
	 * 
	 * @param name
	 * @return the id of the node just added
	 */
	private int addStepNode(String name) {

		int rowNum = nodes.addRow();
		int id = rowNum + MIN_DIN_ID_STEP;

		nodes.setString(rowNum, TYPE, STEP);
		nodes.setInt(rowNum, ID, id);
		nodes.setString(rowNum, NAME, name);

		return id;
	}

	/**
	 * adds a step node and its edges to replace a collapsed start node - finish
	 * node sequence
	 * 
	 * @param startNode
	 * @param target
	 *            the first child of the finish node connected by a control flow
	 *            ("CF") edge
	 */
	private void completeGraph(Node nodeStart, Node target) {

		String stepName = target.getString(NAME).substring(0,
				target.getString(NAME).lastIndexOf(" Finish"));
		Node startNodeParent = getParent(nodeStart);
		int targetId = successorId;

		int id = addStepNode(stepName);
		addEdge(STEP, targetId, id);
		addEdge(STEP, id, startNodeParent.getInt(ID));
	}

	/**
	 * sets visible any invisible node with visible edges connected to it
	 */
	private void setSingleNodesVisible() {

		Iterator visualItems = vis.items();
		VisualItem vItem;
		TableNodeItem visualNode;
		TableEdgeItem visualEdge;

		while (visualItems.hasNext()) {
			vItem = (VisualItem) visualItems.next();

			if (vItem instanceof TableNodeItem && !vItem.isVisible()) {
				visualNode = (TableNodeItem) vItem;
				Iterator edges = visualNode.edges();

				while (edges.hasNext()) {
					visualEdge = (TableEdgeItem) edges.next();

					if (visualEdge.isVisible()) {
						visualNode.setVisible(true);
						buildInvisibleEdge(visualNode);
						break;
					}
				}
			}

		}

	}

	/**
	 * builds the edge between a data node and a step when the node connected to
	 * the data node has collapsed into a step
	 * 
	 * @param node
	 */
	private void buildInvisibleEdge(TableNodeItem node) {

		Iterator edges = node.edges();
		TableEdgeItem edge;
		int id = 0;

		while (edges.hasNext()) {
			edge = (TableEdgeItem) edges.next();

			if (!edge.isVisible()) {
				id = edge.getInt(TARGET);
				break;
			}
		}

		String type;
		if (node.getString(TYPE).equals(DATA_NODE)) {
			type = DATA_FLOW;
		}
		else if (node.getString(TYPE).equals(STEP)) {
			type = STEP;
		}
		else {
			type = CONTROL_FLOW;

		}

		Iterator visibleItems = vis.visibleItems();
		VisualItem vItem = null;
		TableNodeItem lastVisibleStep = null;

		assert visibleItems.hasNext();
		while (visibleItems.hasNext()) {
			vItem = (VisualItem) visibleItems.next();
			if (vItem instanceof TableNodeItem
					&& vItem.getString(TYPE).equals(STEP)
					&& mControl.getStepParent((TableNodeItem) vItem).getInt(ID) < id
					&& mControl.getStepChild((TableNodeItem) vItem).getInt(ID) > id) {
				lastVisibleStep = (TableNodeItem) vItem;
			}
		}

		addEdge(type, node.getInt(ID), lastVisibleStep.getInt(ID));
		buildAllInvisibleEdges(node, lastVisibleStep, id, type);

	}

	/**
	 * builds edges between a data node and all the steps collapsed within the
	 * last visible step and sets them invisible
	 * 
	 * @param node
	 * @param step
	 * @param id
	 * @param type
	 */
	private void buildAllInvisibleEdges(TableNodeItem node, TableNodeItem step,
			int id, String type) {

		Iterator visibleItems = vis.visibleItems();
		VisualItem vItem = null;
		VisualItem lastVisibleStep = null;
		int stepId = step.getInt(ID);

		while (visibleItems.hasNext()) {
			vItem = (VisualItem) visibleItems.next();
			if (vItem instanceof TableNodeItem
					&& vItem.getString(TYPE).equals(STEP)
					&& vItem.getInt(ID) < stepId) {
				lastVisibleStep = vItem;
			}
		}

		int lastVisibleId = 0;
		if (lastVisibleStep != null) {
			lastVisibleId = lastVisibleStep.getInt(ID);
		}

		Iterator visualItems = vis.items();
		vItem = null;

		while (visualItems.hasNext()) {
			vItem = (VisualItem) visualItems.next();

			if (vItem instanceof TableNodeItem
					&& vItem.getString(TYPE).equals(STEP)
					&& vItem.getInt(ID) < stepId
					&& vItem.getInt(ID) > lastVisibleId && !vItem.isVisible()) {
				int source = 0;
				int target = 0;
				TableNodeItem stepItem = (TableNodeItem) vItem;
				Iterator stepEdges = stepItem.edges();
				TableEdgeItem stepEdge;

				while (stepEdges.hasNext()) {
					stepEdge = (TableEdgeItem) stepEdges.next();
					if (stepEdge.getString(TYPE).equals(STEP)
							&& stepEdge.getInt(SOURCE) == stepItem.getInt(ID)) {
						target = stepEdge.getInt(TARGET);
					}
					if (stepEdge.getString(TYPE).equals(STEP)
							&& stepEdge.getInt(TARGET) == stepItem.getInt(ID)){
						source = stepEdge.getInt(SOURCE);
					}
				}
				if (id > target && id < source) {
					addEdge(type, node.getInt(ID), stepItem.getInt(ID));
					TableEdgeItem vEdge = (TableEdgeItem) (node.getGraph())
							.getEdge(node, stepItem);
					vEdge.setVisible(false);
				}
			}
		}

	}

	/**
	 * build the invisible edges connected to a visible node when the visible
	 * node is before the start-finish sequence that just collapsed into a step
	 * 
	 * @param nodeStart
	 */
	private void buildOtherInvisibleEdges(Node nodeStart) {
		Node source = getParent(nodeStart);
		Iterator sourceNeighbors = source.neighbors();
		Node neighbor;
		Node step = null;

		assert sourceNeighbors.hasNext();
		while (sourceNeighbors.hasNext()) {
			neighbor = (Node) sourceNeighbors.next();
			if (neighbor.getString(TYPE).equals(STEP)
					&& (nodeStart.getGraph()).getEdge(neighbor, source) != null) {
				step = neighbor;
				break;
			}
		}

		Iterator stepEdges = step.edges();
		Edge stepEdge;
		int parentId = source.getInt(ID);
		int childId = 0;

		while (stepEdges.hasNext()) {
			stepEdge = (Edge) stepEdges.next();
			if (stepEdge.getString(TYPE).equals(STEP)
					&& stepEdge.getInt(TARGET) == step.getInt(ID)) {
				childId = stepEdge.getInt(SOURCE);
				break;
			}
		}

		Iterator visibleItems = vis.items();
		VisualItem vItem;
		TableEdgeItem edgeItem;

		while (visibleItems.hasNext()) {
			vItem = (VisualItem) visibleItems.next();
			if (vItem instanceof TableEdgeItem) {
				edgeItem = (TableEdgeItem) vItem;
				if (edgeItem.getInt(SOURCE) > parentId
						&& edgeItem.getInt(SOURCE) < childId
						&& edgeItem.getInt(TARGET) != parentId
						&& (edgeItem.getTargetItem()).isVisible()
						&& !edgeItem.isVisible()) {
					addEdge(edgeItem.getString(TYPE), step.getInt(ID),
							(edgeItem.getTargetItem()).getInt(ID));
				}
				if (edgeItem.getInt(TARGET) > parentId
						&& edgeItem.getInt(TARGET) < childId
						&& edgeItem.getInt(SOURCE) != childId
						&& (edgeItem.getSourceItem()).isVisible()
						&& !edgeItem.isVisible()) {
					addEdge(edgeItem.getString(TYPE),
							(edgeItem.getSourceItem()).getInt(ID),
							step.getInt(ID));
				}
			}
		}

	}

	/**
	 * updates the focus group to the node which is being added to the DDG
	 * 
	 * @param nodeId
	 */
	private void updateFocusGroup(int nodeId) {
		Iterator visualItems = vis.items();
		VisualItem item = null;

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
	 * Build the visual graph
	 * 
	 * @param ddg
	 *            the data derivation graph data
	 */

	private void buildGraph(ProvenanceData ddg) {
		System.out.println("Using new code");
		addNodesAndEdges(ddg);

		graph = new Graph(nodes, edges, true, ID, SOURCE, TARGET);

	}

	/**
	 * Builds a visual ddg from a textual ddg in a file
	 * 
	 * @param file
	 *            the file containing the ddg
	 * @throws IOException
	 *             if the file cannot be read
	 */
	private void buildGraph(File file) throws IOException {
		Parser parser = new Parser(file, this);
		parser.addNodesAndEdges();
		graph = new Graph(nodes, edges, true, ID, SOURCE, TARGET);

	}

	private void buildNodeAndEdgeTables() {
		nodes.addColumn(TYPE, String.class);
		nodes.addColumn(ID, int.class);
		nodes.addColumn(NAME, String.class);

		edges.addColumn(TYPE, String.class);
		edges.addColumn(SOURCE, int.class);
		edges.addColumn(TARGET, int.class);
	}

	private void addNodesAndEdges(ProvenanceData ddg) {
		Iterator<ProcedureInstanceNode> pins = ddg.pinIter();

		int numPins = 0;
		while (pins.hasNext()) {
			ProcedureInstanceNode nextPin = pins.next();
			addNode(nextPin.getType(), nextPin.getId(),
					nextPin.getNameAndType());
			addControlFlowEdges(nextPin);
			numPins++;
		}

		Iterator<DataInstanceNode> dins = ddg.dinIter();
		while (dins.hasNext()) {
			DataInstanceNode nextDin = dins.next();
			addNode(nextDin.getType(), nextDin.getId() + numPins,
					nextDin.getName() + " = " + nextDin.getValue().toString());
		}

		pins = ddg.pinIter();
		while (pins.hasNext()) {
			ProcedureInstanceNode nextPin = pins.next();
			addInputEdges(nextPin, numPins);
			addOutputEdges(nextPin, numPins);
		}

	}

	private void addInputEdges(ProcedureInstanceNode pin, int numPins) {
		Iterator<DataInstanceNode> inputIter = pin.inputParamValues();
		while (inputIter.hasNext()) {
			DataInstanceNode input = inputIter.next();
			addEdge(DATA_FLOW, pin.getId(), input.getId() + numPins);
		}
	}

	private void addOutputEdges(ProcedureInstanceNode pin, int numPins) {

		Iterator<DataInstanceNode> outputIter = pin.outputParamValues();
		while (outputIter.hasNext()) {
			DataInstanceNode output = outputIter.next();
			addEdge(DATA_FLOW, output.getId() + numPins, pin.getId());
		}
	}

	private void addControlFlowEdges(ProcedureInstanceNode pin) {
		Iterator<ProcedureInstanceNode> predecessors = pin.predecessorIter();
		while (predecessors.hasNext()) {
			ProcedureInstanceNode pred = predecessors.next();
			addEdge(CONTROL_FLOW, pin.getId(), pred.getId());
		}
	}

	/**
	 * Adds a node to the prefuse graph
	 * 
	 * @param type
	 *            the type of node
	 * @param id
	 *            the node's id
	 * @param name
	 *            the node's name
	 */
	public synchronized void addNode(String type, int id, String name) {
		int rowNum = nodes.addRow();
		nodes.setString(rowNum, TYPE, type);
		nodes.setInt(rowNum, ID, id);
		nodes.setString(rowNum, NAME, name);
		
		//write to a file
		/*outFile.println(id+" \""+name+"\" "+type);*/
	}

	/**
	 * Adds an edge to a prefuse ddg
	 * 
	 * @param type
	 *            the type of the edge
	 * @param source
	 *            the source of the edge
	 * @param target
	 *            the target of the edge
	 */
	public synchronized void addEdge(String type, int source, int target) {
		int rowNum = edges.addRow();
		edges.setString(rowNum, TYPE, type);
		edges.setInt(rowNum, SOURCE, source);
		edges.setInt(rowNum, TARGET, target);
		
		//write to a file
		/*if(type.equals("CF")) outFile.println(source+" "+target+" "+"Control Flow");
		if(type.equals("DF")) outFile.println(source+" "+target+" "+"Data Flow");*/
		
	}

	/**
	 * Display a DDG visually
	 * 
	 * @param ddg
	 *            the ddg to display
	 */
	public void drawGraph(ProvenanceData ddg) {
		buildNodeAndEdgeTables();

		// -- 1. load the data ------------------------------------------------

		buildGraph(ddg);
		drawGraph();

		// assign the colors
		vis.run("color");
		// start up the animated layout
		vis.run("layout");
		// do the repaint
		vis.run("repaint");

	}

	private void drawGraph() {

		// -- 2. the visualization --------------------------------------------

		vis.add(GRAPH, graph);
		vis.setInteractive(GRAPH_EDGES, null, false);

		// -- 3. the renderers and renderer factory ---------------------------

		// draw the "name" label for NodeItems
		LabelRenderer r = new LabelRenderer(NAME);
		r.setRoundedCorner(8, 8); // round the corners

		// create a new default renderer factory
		// return our name label renderer as the default for all non-EdgeItems
		DefaultRendererFactory rendererFactory = new DefaultRendererFactory(r);

		// Add arrowheads to the edges
		EdgeRenderer edgeRenderer = new EdgeRenderer(
				prefuse.Constants.EDGE_TYPE_LINE,
				prefuse.Constants.EDGE_ARROW_FORWARD);
		rendererFactory.setDefaultEdgeRenderer(edgeRenderer);

		vis.setRendererFactory(rendererFactory);

		// -- 4. the processing actions ---------------------------------------

		// map data values to colors using our provided palette
		ColorAction fill = new ColorAction(GRAPH_NODES, VisualItem.FILLCOLOR);
		fill.add(ExpressionParser.predicate("Type = 'Binding'"),
				INTERPRETER_COLOR);
		fill.add(ExpressionParser.predicate("Type = 'Start'"), NONLEAF_COLOR);
		fill.add(ExpressionParser.predicate("Type = 'Finish'"), NONLEAF_COLOR);
		fill.add(ExpressionParser.predicate("Type = 'Interm'"), NONLEAF_COLOR);
		fill.add(ExpressionParser.predicate("Type = 'Leaf'"), LEAF_COLOR);
		fill.add(ExpressionParser.predicate("Type = 'Data'"), DATA_COLOR);
		fill.add(ExpressionParser.predicate("Type = 'Exception'"),
				EXCEPTION_COLOR);
		fill.add(ExpressionParser.predicate("Type = 'SimpleHandler'"),
				SIMPLE_HANDLER_COLOR);
		fill.add(ExpressionParser.predicate("Type = 'VStart'"), VIRTUAL_COLOR);
		fill.add(ExpressionParser.predicate("Type = 'VFinish'"), VIRTUAL_COLOR);
		fill.add(ExpressionParser.predicate("Type = 'VInterm'"), VIRTUAL_COLOR);

		// color for Steps
		fill.add(ExpressionParser.predicate("Type = 'Step'"), STEP_COLOR);

		// use black for node text
		ColorAction text = new ColorAction(GRAPH_NODES, VisualItem.TEXTCOLOR,
				ColorLib.gray(0));

		ColorAction edgeColors = new ColorAction(GRAPH_EDGES,
				VisualItem.STROKECOLOR, ColorLib.gray(0));
		edgeColors.add(ExpressionParser.predicate("Type = 'CF'"),
				CONTROL_FLOW_COLOR);
		edgeColors.add(ExpressionParser.predicate("Type = 'DF'"),
				DATA_FLOW_COLOR);
		edgeColors.add(ExpressionParser.predicate("Type = 'Step'"),
				STEP_EDGE_COLOR);

		ColorAction arrow = new ColorAction(GRAPH_EDGES, VisualItem.FILLCOLOR,
				ColorLib.gray(200));

		// create an action list containing all color assignments
		ActionList color = new ActionList();
		color.add(fill);
		color.add(text);
		color.add(edgeColors);
		color.add(arrow);

		// create an action list with an animated layout
		ActionList layout = new ActionList();
		DDGLayout treeLayout = new DDGLayout(GRAPH);
		layout.add(treeLayout);

		ActionList repaint = new ActionList();
		repaint.add(new RepaintAction());

		// add the actions to the visualization
		vis.putAction("color", color);
		vis.putAction("layout", layout);
		vis.putAction("repaint", repaint);

		// -- 5. the display and interactive controls -------------------------

		// DDGDisplay
		d.setVisualization(vis);
		// display size
		d.setSize(720, 500);
		// drag individual items around
		d.addControlListener(new DragControl());
		// pan with left-click drag on background
		d.addControlListener(new PanControl());
		// zoom with right-click drag
		d.addControlListener(new ZoomControl());
		// make node and incident edges invisible
		d.addControlListener(mControl);

		// focus action
		ActionList animate = new ActionList();
		animate.add(autoPan);
		vis.putAction("animate", animate);

		// -- 6. launch the visualization -------------------------------------

		// create a new window to hold the visualization
		JFrame frame = new JFrame("DDG Viewer");
		// ensure application exits when window is closed
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(d);
		frame.pack(); // layout components in window
		frame.setVisible(true); // show the window

		// new code
		PopupMenu options = d.new PopupMenu();
		options.createPopupMenu();
	}

	/**
	 * Initializes the prefuse tables.
	 */
	@Override
	public void processStarted(String processName, ProvenanceData provData) {
		//initialize file
		/*initFile();*/
		
		buildNodeAndEdgeTables();
		graph = new Graph(nodes, edges, true, ID, SOURCE, TARGET);
		drawGraph();
	}

	/**
	 * Repaints the finished ddg
	 */
	@Override
	public void processFinished() {
		System.out.println("Drawing DDG");
		d.stopRefocusing();

		repaint();
		
		//close file
		/*outFile.close();*/
		
		System.out.println("Done drawing DDG");
		try {
			System.out.println("Hit return to exit");
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		d.closeWindow();
	}

	/**
	 * Adds a node to the visualization
	 */
	@Override
	public synchronized void procedureNodeCreated(ProcedureInstanceNode pin) {
		if (pin.getId() >= MIN_DIN_ID_DATA) {
			throw new RuntimeException("PIN id is too big for prefuse!");
		}

		addNode(pin.getType(), pin.getId(), pin.getNameAndType());

		// collapses start-finish sequence when a finish node is found and
		// replaces it with a step
		if (foundFinishNode) {
			successorId = pin.getId();
			startNode = completeGraphIncrementally(finishNode);
			foundFinishNode = false;
		}

		if ((pin.getType()).equals("Finish")) {
			foundFinishNode = true;
			Iterator graphNodes = graph.nodes();
			Node graphNode;

			while (graphNodes.hasNext()) {
				graphNode = (Node) graphNodes.next();
				if (graphNode.getInt(ID) == pin.getId()) {
					finishNode = graphNode;
				}
			}

		}

		// Draw the root node immediately, but delay drawing the other nodes
		// until
		// there is an edge connecting them. Otherwise, they just go in the top
		// left
		// corner of the window.
		if (!rootDrawn) {
			vis.run("layout");
			updateFocusGroup(pin.getId());
			repaint();
			rootDrawn = true;
		}
	}

	private void repaint() {

		vis.run("color");
		vis.run("repaint");

		try {
			System.out.println("Hit return to continue.");
			in.readLine();
		} catch (IOException exception) {
			// TODO Auto-generated catch-block stub.
			exception.printStackTrace();
		}

	}

	/**
	 * Add a data node to the visualization. Does not redraw immediately since
	 * the node won't appear in the right place unless there is an edge
	 * connecting it to the graph.
	 */
	@Override
	public synchronized void dataNodeCreated(DataInstanceNode din) {

		addNode(din.getType(), din.getId() + MIN_DIN_ID_DATA, din.getName()
				+ " = " + din.getValue().toString());
	}

	/**
	 * Add a control flow edge to the visualization and redraw the graph
	 */
	@Override
	public synchronized void successorEdgeCreated(
			ProcedureInstanceNode predecessor, ProcedureInstanceNode successor) {

		addEdge(CONTROL_FLOW, successor.getId(), predecessor.getId());

		if (successorId == successor.getId()) {
			setStepsInvisibleIncrementally(startNode);
			// build invisible edges connected to visible nodes 
			buildOtherInvisibleEdges(startNode);
		}

		// handle invisible data nodes with visible edges connected to them
		setSingleNodesVisible();
		// change the focus to recently added node
		vis.run("layout");
		updateFocusGroup(successor.getId());
		repaint();

	}

	/**
	 * Add a dataflow edge to the graph and redraw it.
	 */
	@Override
	public synchronized void bindingCreated(DataBindingEvent e) {

		if (e.getEvent() == BindingEvent.INPUT) {
			addEdge(DATA_FLOW, e.getProcNode().getId(), e.getDataNode().getId()
					+ MIN_DIN_ID_DATA);
		} else {
			addEdge(DATA_FLOW, e.getDataNode().getId() + MIN_DIN_ID_DATA, e
					.getProcNode().getId());
		}

		// handle invisible data nodes with visible edges connected to them
		setSingleNodesVisible();
		// change the focus to recently added node
		vis.run("layout");
		vis.run("animate");
		repaint();

	}

	/**
	 * Displays a file chooser for textual DDGs and displays the result
	 * visually.
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {
		PrefuseGraphBuilder builder = new PrefuseGraphBuilder();
		builder.buildNodeAndEdgeTables();

		// -- 1. load the data ------------------------------------------------

		JFileChooser fileChooser = new JFileChooser(
				System.getProperty("user.dir"));
		try {
			if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				builder.buildGraph(fileChooser.getSelectedFile());
			}

			builder.drawGraph();
		} catch (HeadlessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Cannot read the file");
		}
	}

}
