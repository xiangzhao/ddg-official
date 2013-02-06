package laser.ddg;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import laser.ddg.visualizer.PrefuseGraphBuilder;

/**
 * This is the class that is the root of all information. This is probably
 * incomplete. It should hold metadata about the process as well as serve as the
 * starting point for walking the data derivation graph beginning either with
 * the inputs or the outputs. The ProvenanceData object holds a collection of
 * all Data Instance Nodes and a collection of all process instance nodes; the
 * user needs to add each DIN and each PIN to the respective collection
 * separately from constructing the node itself. ProvenanceData also stores
 * Information about the agents used in the process, and holds collections of
 * inputs and of outputs of the process
 * 
 * @author B. Lerner & S. Taskova
 * 
 */

public class ProvenanceData {
	// Name of the process the provenance data is for
	private String processName;
	
	// Information about the agents used in the process.
	private Set<AgentConfiguration> agentConfigurations;

	// All input DINs in the process
	private List<DataInstanceNode> processInputs;

	// All output DINs in the process
	private List<DataInstanceNode> processOutputs;

	// All Data Instance Nodes in a DDG
	private List<DataInstanceNode> dins;

	// All Procedure Instance Nodes in a DDG
	private List<ProcedureInstanceNode> pins;

	// The root procedure
	private ProcedureInstanceNode rootProcedure;
	
	// Map to/from resource URIs
	//private Hashtable<Node, Resource> nodesToResources;
	private Hashtable<Node, String> nodesToResources;
	
	//private Hashtable<Resource, Node> resourcesToNodes;
	private Hashtable<String, Node> resourcesToNodes;

	// The ID of the DIN that is incremented when the DIN is added to the dins
	// set
	private int nextDinId = 1;

	// The ID of the PIN that is incremented when the PIN is added to the pins
	// set
	private int nextPinId = 1;

	// The objects that want to be notified of data bindings.
	private List<DataBindingListener> bindingListeners
		= new LinkedList<DataBindingListener>();

	// Listeners to changes to the DDG
	private List<ProvenanceListener> provListeners = new LinkedList<ProvenanceListener>();

	/** Construct a default object
	 *  @param processName the name of the process or activity this is the provenance data for
	 */
	public ProvenanceData(String processName) {
		this.processName = processName;
		agentConfigurations = new TreeSet<AgentConfiguration>();
		pins = new LinkedList<ProcedureInstanceNode>();
		dins = new LinkedList<DataInstanceNode>();
		processInputs = new LinkedList<DataInstanceNode>();
		processOutputs = new LinkedList<DataInstanceNode>();
		
		// nodesToResources = new Hashtable<Node, Resource>();
		// resourcesToNodes  = new Hashtable< Resource, Node>();

		nodesToResources = new Hashtable<Node, String>();
		resourcesToNodes  = new Hashtable< String, Node>();
	}

	/**
	 * Add an agent to the configuration
	 * 
	 * @param newAgent
	 *            the agent to add
	 * 
	 * */
	public synchronized void addAgent(AgentConfiguration newAgent) {
		agentConfigurations.add(newAgent);
	}

	/**
	 * Add a PIN to the PINs set
	 * 
	 * @param p
	 *            PIN to add
	 */
	public synchronized void addPIN(ProcedureInstanceNode p) {
		
		pins.add(p);
		p.setId(nextPinId);
		nextPinId++;
		notifyPinCreated(p);
	}

	/**
	 * Add a PIN to the PINs set.  This should be called when the node is being
	 * read from a database.
	 * 
	 * @param pin
	 *            PIN to add
	 * @param resURI The URI of the resource for this pin
	 * @param id the pin's id 
	 */
	public synchronized void addPIN(ProcedureInstanceNode pin, String resURI, int id) {
		pins.add(pin);
		this.nodesToResources.put(pin, resURI);
		this.resourcesToNodes.put(resURI, pin);
		pin.setId(id);
		notifyPinCreated(pin);
	}

	/**
	 * Add DIN to the process inputs set
	 * 
	 * @param idin
	 *            the DIN to add
	 */
	public synchronized void addInputDIN(DataInstanceNode idin) {		
		processInputs.add(idin);
	}

	/**
	 * Add DIN to the process outputs set
	 * 
	 * @param odin
	 *            the DIN to add
	 */
	public synchronized void addOutputDIN(DataInstanceNode odin) {
		processOutputs.add(odin);
	}

	/**
	 * Add a DIN to the DINs set
	 * 
	 * @param d
	 *            DIN to add
	 */
	public synchronized void addDIN(DataInstanceNode d) {		
		dins.add(d);
		d.setId(nextDinId);
		nextDinId += 1;
		notifyDinCreated(d);
	}
	
	/**
	 * Add a DIN to the DINs set method specific to RDF
	 * 
	 * @param d DIN to add
	 * @param resURI the URI of the JENA resource
	 *            
	 */
	public synchronized void addDIN(DataInstanceNode d, String resURI) {	
		resourcesToNodes.put(resURI, d);
		dins.add(d);
		notifyDinCreated(d);
	}

	/**
	 * @return an iterator through all output DINs in the process
	 */
	public Iterator<DataInstanceNode> outputDinIter() {
		return processOutputs.iterator();
	}
	
	/**
	 * @param din
	 * @return resource corresponding to this DIN
	 */
	//public Resource getResource(DataInstanceNode din){
	public String getResource(DataInstanceNode din){
		return nodesToResources.get(din);
	}
	
	/**
	 * Records that the ddg node is represented by the rdf resource
	 * @param node the ddg node
	 * @param resURI the URI for the rdf resource
	 */
	public void bindNodeToResource (Node node, String resURI) {
		nodesToResources.put(node, resURI);
	}
	
	/**
	 * Returns true if the rdf resource exists in the provenance data
	 * @param resURI the URI for the rdf resource
	 * @return true if the rdf resource exists in the provenance data
	 */
	public boolean containsResource (String resURI) {
		return nodesToResources.containsValue(resURI);
	}
	
	/**
	 * Gets the DDG node associated with a particular RDF resource
	 * @param resURI the URI of the resource to look up
	 * @return the associated DDG node
	 */
	public Node getNodeForResource (String resURI) {
		return resourcesToNodes.get(resURI);
	}
	
	/**
	 * @param pin
	 * @return resource corresponding to this PIN
	 */
	//public Resource getResource(ProcedureInstanceNode pin){
	public String getResource(ProcedureInstanceNode pin){
		return nodesToResources.get(pin);
	}

	/**
	 * @return an iterator through all input DINs in the process
	 */
	public Iterator<DataInstanceNode> inputDinIter() {
		return processInputs.iterator();
	}

	/**
	 * Create an iterator through the DINs
	 * 
	 * @return an iterator through the DINs
	 * 
	 * */
	public Iterator<DataInstanceNode> dinIter() {
		return dins.iterator();
	}

	/**
	 * Create an iterator through the PINs
	 * 
	 * @return an iterator through the PINs
	 * 
	 * */
	public Iterator<ProcedureInstanceNode> pinIter() {
		return pins.iterator();
	}

	/**
	 * Create an iterator through the agents
	 * 
	 * @return an iterator through the agents
	 * 
	 * */
	public Iterator<AgentConfiguration> agentIter() {
		return agentConfigurations.iterator();
	}

	/**
	 * Set the root procedure.
	 * 
	 * @param sin
	 *            the procedure which will be the root
	 * 
	 * @throws RootAlreadySetException
	 *             if the root is already set
	 */
	public synchronized void setRoot(ProcedureInstanceNode sin)
		throws RootAlreadySetException {
		if (rootProcedure == null) {
			rootProcedure = sin;
			
			// put root on top of list b/c prefuse visualizer expects root to 
			// be added to graph first
			pins.remove(sin);
			pins.add(0,sin);	
			
		} else if (sin.canBeRoot()){
			throw new RootAlreadySetException("Root already set");
		}
	}

	/**
	 * @return the root procedure
	 */
	public synchronized ProcedureInstanceNode getRoot() {
		return rootProcedure;
	}

	/**
	 * @param din
	 *            DIN to check for in the process outputs
	 * @return true if the DataInstanceNode on which the method is called is
	 *         among the outputs of the process
	 */
	public boolean isProcessOutput(DataInstanceNode din) {
		return processOutputs.contains(din);
	}

	/**
	 * @param din
	 *            DIN to check for in the process inputs
	 * @return true if the DataInstanceNode on which the method is called is
	 *         among the outputs of the process
	 */
	public boolean isProcessInput(DataInstanceNode din) {
		return processInputs.contains(din);
	}

	/**
	 * Returns a String representation of all the nodes, edges and agents held
	 * in this ProvenanceData object.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(pins.size() + "\n");

		// Agents
		for (AgentConfiguration agent : agentConfigurations) {
			sb.append(agent + "\n");
		}

		// All Data Instance Nodes in a DDG
		for (DataInstanceNode dataNode : dins) {
			sb.append(dataNode + "\n");
		}

		// All Procedure Instance Nodes in a DDG
		for (ProcedureInstanceNode procNode : pins) {
			sb.append(procNode + "\n");
		}

		return sb.toString();

	}

	/**
	 * Draw visual representation of DDG
	 */
	public void drawGraph() {
		new PrefuseGraphBuilder().drawGraph(this);
	}

	/**
	 * Add an object as a listener to data bindings
	 * 
	 * @param l
	 *            the new listener
	 */
	public void addDataBindingListener(DataBindingListener l) {
		bindingListeners.add(l);
	}

	/**
	 * Remove a data binding listener
	 * 
	 * @param l
	 *            the listener to remove
	 */
	public void removeDataBindingListner(DataBindingListener l) {
		bindingListeners.remove(l);
	}

	/**
	 * Notify data binding listeners of a new data binding
	 * 
	 * @param e
	 *            the event that provides details about the new data binding.
	 */
	void notifyDataBindingListeners(DataBindingEvent e) {
		for (DataBindingListener l : bindingListeners) {
			l.bindingCreated(e);
		}

		for (ProvenanceListener l : provListeners ) {
			l.bindingCreated(e);
		}
	}

	public void addProvenanceListener(ProvenanceListener l) {
		provListeners.add(l);
	}
	
	public void removeProvenanceListener (ProvenanceListener l) {
		provListeners.remove(l);
	}
	
	/**
	 * Notifies provenance listeners when a process is started
	 * @param processName the name of the process
	 */
	public void notifyProcessStarted (String processName) {
		for (ProvenanceListener l : provListeners) {
			l.processStarted(processName, this);
		}
	}
	
	/**
	 * Notifies provenance listeners when a process is finished
	 */
	public void notifyProcessFinished() {
		for (ProvenanceListener l : provListeners) {
			l.processFinished();
		}
	}
	
	/**
	 * Notifies provenance listeners when a procedure node is added to the DDG
	 * @param pin the node added
	 */
	private void notifyPinCreated(ProcedureInstanceNode pin) {
		for (ProvenanceListener l : provListeners) {
			l.procedureNodeCreated(pin);
		}
	}

	/**
	 * Notfies provenance listeners when a data node is added the DDG
	 * @param din the node added
	 */
	private void notifyDinCreated(DataInstanceNode din) {
		for (ProvenanceListener l : provListeners) {
			l.dataNodeCreated(din);
		}
	}
	
	/**
	 * Notifies provenance listeners when a predecessor/successor edge is added to a DDG
	 * @param predecessor the predecessor procedure node
	 * @param successor the successor procedure node
	 */
	synchronized void notifySuccessorEdgeCreated (ProcedureInstanceNode predecessor, ProcedureInstanceNode successor) {
		for (ProvenanceListener l : provListeners) {
			l.successorEdgeCreated(predecessor, successor);
		}
	}


	/**
	 * 
	 * Return the name of the process that was executed to create this ddg
	 * @return the name of the process that was executed to create this ddg
	 */
	public String getProcessName() {
		return processName;
	}
}
