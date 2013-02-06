package laser.ddg.persist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import laser.ddg.DataInstanceNode;
import laser.ddg.ProcedureInstanceNode;
import laser.ddg.ProvenanceData;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * This class reads provenance data from a Jena database.
 * 
 * @author Sophia. Created Jan 10, 2012.
 */

public class JenaLoader {
	/** URL that defines rdf syntax */
	public static final String RDF_PREFIX 
		= "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>";
	
	private static final String DIN_PREFIX = "j.0:";
	private static final String SIN_PREFIX = "j.1:";
	private static final String SIN_DDG_ID = SIN_PREFIX + Properties.DDG_ID;
	private static final String DIN_DDG_ID = DIN_PREFIX + Properties.DDG_ID;

	// The provenance graph being constructed
	private ProvenanceData pd;
	
	// The database object
	private Dataset dataset;
	
	// Helper object that encapsulates the database properties.
	private Properties prop;
	
	private String processName;
	private String timestamp;

	/**
	 * Create an object that can read data from a Jena database
	 * @param dir the directory containing the database
	 */
	public JenaLoader(String dir) {
		dataset = RdfModelFactory.getDataset(dir);
	}
	
	/**
	 * @return a list of the names of processes that have DDGs stored in this database
	 */
	public List<String> getAllProcessNames() {
		String queryVar = "name";
		
		String selectProcessNamesQueryString = JenaLoader.RDF_PREFIX
				+ "SELECT ?" + queryVar + " WHERE  { ?res <" + Properties.PROCESS_NAME_URI + "> ?" + queryVar 
				+ " . ?res <" + Properties.NUM_EXECUTIONS_URI + "> ?numExec }" ;

		return getStringListResult(queryVar,
				selectProcessNamesQueryString);
		
	}

	/**
	 * @return the number of different processes that have DDGs stored in the database.
	 *   This is done by the process name.  Thus, if process A is executed once and 
	 *   process B is executed 3 times, this method would return 2 (counting A and B 
	 *   once each).
	 */
	public int getNumProcesses() {
		String queryVar = "num";
		String selectNumProcessesQueryString = JenaLoader.RDF_PREFIX
				+ "SELECT ?" + queryVar + " WHERE  { ?res <" + Properties.NUM_PROCESSES_URI + "> ?" + queryVar + "}";

		return getNumFromResult(queryVar, selectNumProcessesQueryString);
	}
	
	/**
	 * Returns the URI to use for this process or null if there are no executions
	 * of this process in the database yet.
	 * @param processName the name of the process to look for
	 * @return the URI to use for this process
	 */
	public String getProcessURI(String processName) {
		String queryVar = "res";
		String selectProcessQueryString = JenaLoader.RDF_PREFIX
					+ "\nSELECT ?" + queryVar + " "
					+ "\nWHERE  { ?" + queryVar + " <" + Properties.PROCESS_NAME_URI + "> \"" + processName + "\"}";

		ResultSet processResultSet = performQuery(selectProcessQueryString);
		if (processResultSet.hasNext()) {
			QuerySolution nextProcessResult = processResultSet.next();
			Resource currentRes = nextProcessResult.getResource(queryVar);
			return currentRes.getURI();
		}
		return null;
	}

	/**
	 * 
	 * @param processURI the URI for the process to look up
	 * @return the number of executions of a specific process in the DB
	 */
	public int getNumExecutions(String processURI) {
		String queryVar = "num";
		String selectNumExecutionsQueryString = JenaLoader.RDF_PREFIX
				+ "\nSELECT ?" + queryVar 
				+ "\n WHERE  { <" + processURI + "> <" + Properties.NUM_EXECUTIONS_URI + "> ?" + queryVar + "}";

		return getNumFromResult(queryVar, selectNumExecutionsQueryString);
	}

	/**
	 * 
	 * @param selectedProcessName the name of the process to get timestamps for
	 * @return a list of the timestamps for all executions of processes in the DB
	 */
	public List<String> getTimestamps(String selectedProcessName) {
		String queryVar = "timestamp";
		
		String selectTimestampsQueryString = JenaLoader.RDF_PREFIX
				+ "\nSELECT ?" + queryVar
				+ "\n WHERE  { ?res <" + Properties.PROCESS_NAME_URI + "> \"" + selectedProcessName + "\""
				+ "\n . ?res <" + Properties.TIMESTAMP_URI + "> ?" + queryVar + " }" ;

		return getStringListResult(queryVar, selectTimestampsQueryString);
		
	}

	/**
	 * Gets the integer value returned by a query
	 * @param queryVar the SPARQL variable containing the result
	 * @param queryString the query to execute
	 * @return the integer value returned by the query.  Returns 0 if there is no match.
	 */
	private int getNumFromResult(String queryVar,
			String queryString) {
		ResultSet resultSet = performQuery(queryString);
		if (resultSet.hasNext()) {
			QuerySolution nextResult = resultSet.next();
			int value = nextResult.getLiteral(queryVar)
					.getInt();
			return value;
		}
		return 0;
	}

	/**
	 * Gets a result that is a list of strings from a query
	 * @param queryVar the SPARQL variable that will hold the results
	 * @param queryString the query to ask
	 * @return the list of strings returned by the query.  Returns an empty list if there is no match.
	 */
	private List<String> getStringListResult(String queryVar, String queryString) {
		ArrayList<String> resultList = new ArrayList<String>();
		ResultSet resultSet = performQuery(queryString);
		while (resultSet.hasNext()) {
			QuerySolution nextResult = resultSet.next();
			String value = nextResult.getLiteral(queryVar).getString();
			resultList.add(value);
		}
		
		return resultList;
	}

	/**
	 * Load a DDG for one execution of a process
	 * @param processName the name of the process
	 * @param timestamp the timestamp for the execution
	 * @param provData 
	 * @return the ddg
	 */
	public ProvenanceData loadDDG(String processName,
			String timestamp, ProvenanceData provData) {
		this.processName = processName;
		this.timestamp = timestamp;
		pd = provData;
		pd.notifyProcessStarted(processName);
		
		String queryPrefix = getQueryPrefix(processName, timestamp);
	
		// Process the pins in the order in which they were originally added to the 
		// DDG so that the events can be sent to the visualization tool to do the 
		// incremental visualizations.
		SortedSet<Resource> sortedResources = getAllStepInstanceNodes(queryPrefix);
		for (Resource res : sortedResources) {
			// Add the next pin
			ProcedureInstanceNode pin = addProcResourceToProvenance(res, pd);
			
			// Connect the pin to its predecessors, inputs and outputs.
			setPredecessors(queryPrefix, pin);
			getAllInputs(queryPrefix, pin);
			getAllOutputs(queryPrefix, pin);
		}
		
		pd.notifyProcessFinished();
		return pd;
	}

	public ProcedureInstanceNode addProcResourceToProvenance(Resource res, ProvenanceData provData) {
		int id = retrieveSinId(res);
		String type = retrieveSinType(res);
		String name = retrieveSinName(res);
		ProcedureInstanceNode pin = addSinToProvData(name,
				type, res, id, provData);
		System.out.println("Adding sin" + id + ": "
				+ pin.toString());
		return pin;
	}

	/**
	 * Read all the pins for this DDG from the database
	 * @param queryPrefix the prefix string to include in the query
	 * @return the set of pins sorted by their ids, which represents the order
	 * 	in which they were created
	 */
	private SortedSet<Resource> getAllStepInstanceNodes(String queryPrefix) {
		String queryVar = "s";

		String selectStepsQueryString = queryPrefix
				+ "\nSELECT ?" + queryVar 
				+ "\n WHERE  { ?" + queryVar + " " + SIN_PREFIX + Properties.NAME + " ?sinname}";

		ResultSet stepResultSet = performQuery(selectStepsQueryString);
		
		SortedSet<Resource> sortedResources = new TreeSet<Resource>(new Comparator<Resource>() {

			@Override
			// Allows sorting of pin resources by id.
			public int compare(Resource r0, Resource r1) {
				return retrieveSinId(r0) - retrieveSinId(r1);
			}
			
		});
		
		// Go through the result set putting them into a sorted set.
		while (stepResultSet.hasNext()) {
			QuerySolution nextStepResult = stepResultSet.next();
			Resource currentRes = nextStepResult.getResource(queryVar);
			sortedResources.add(currentRes);
		}
		return sortedResources;
	}

	public SortedSet<String> getAllDinNames(String processName, String timestamp) {
		String queryPrefix = getQueryPrefix(processName, timestamp);
		String queryVar = "dinname";

		String selectDinNamesQueryString = queryPrefix
				+ "\nSELECT ?" + queryVar + "\n WHERE  { ?d " + DIN_PREFIX + Properties.NAME + " ?" + queryVar + "}";

		ResultSet nameResultSet = performQuery(selectDinNamesQueryString);
		
		SortedSet<String> sortedNames= new TreeSet<String>();
		
		// Go through the result set putting them into a sorted set.
		while (nameResultSet.hasNext()) {
			QuerySolution nextNameResult = nameResultSet.next();
			String currentName = nextNameResult.getLiteral(queryVar).getString();
			sortedNames.add(currentName);
		}
		return sortedNames;
	}
	
	/**
	 * Load all the data nodes that are inputs to a pin and connect them to the pin
	 * @param queryPrefix the prefix to include with the query
	 * @param pin the node to get inputs of
	 */
	private void getAllInputs(String queryPrefix, ProcedureInstanceNode pin) {
		String queryVarName = "in";
		ResultSet inputsResultsSet = getAllInputs(processName, timestamp, pin.getId(), queryVarName);

		while (inputsResultsSet.hasNext()) {
			QuerySolution inputSolution = inputsResultsSet.next();
			Resource inputResource = inputSolution.getResource(queryVarName);
			DataInstanceNode inputDin;
			
			// Check if the data node has already been created.  It probably has
			// since it must be an output for a step that already has been loaded.
			if (pd.containsResource(inputResource.getURI())) {
				inputDin = (DataInstanceNode) pd
						.getNodeForResource(inputResource.getURI());
			}
			
			// Create the DIN
			else {
				inputDin = addDataResourceToProvenance(pin, inputResource, pd);
			}
			
			// Connect the data node to the pin it is an input for
			pin.addInput(inputDin.getName(), inputDin);
			inputDin.addUserPIN(pin);
		}
	}

	public ResultSet getAllInputs(String processName, String timestamp, int pinId, String queryVarName) {
		String queryPrefix = getQueryPrefix(processName, timestamp);		

		String sinVarName = "sin";
		String selectInputQuery = queryPrefix
				+ "\nSELECT ?" + queryVarName 
				+ "\n WHERE  { ?" + sinVarName + " " + SIN_PREFIX + Properties.INPUTS + " ?" + queryVarName 
				+ "\n . ?" + sinVarName + " " + SIN_DDG_ID + " " + pinId + "}";
		return performQuery(selectInputQuery);
	}

	/**
	 * Read all the output data nodes of a pin and add it to the provenance graph
	 * @param queryPrefix the prefix to add to the query
	 * @param pin the pin to get the outputs of
	 */
	private void getAllOutputs(String queryPrefix, ProcedureInstanceNode pin) {
		String queryVarName = "out";
		ResultSet outputsResultsSet = getAllOutputs(processName, timestamp, pin.getId(), queryVarName);

		while (outputsResultsSet.hasNext()) {
			QuerySolution outputSolution = outputsResultsSet.next();
			Resource outputResource = outputSolution.getResource(queryVarName);
			DataInstanceNode outputDin;
			
			// If the node has already been loaded (probably not since each node is 
			// only output by one node), just look it up
			if (pd.containsResource(outputResource.getURI())) {
				assert false;
				outputDin = (DataInstanceNode) pd
						.getNodeForResource(outputResource.getURI());
				// No way to set the producer with the current API!
			}
			
			// Load the node from the database
			else {
				outputDin = addDataResourceToProvenance(pin, outputResource, pd);
			}
			System.out.println("Adding output " + outputDin.getName()
						+ " to " + pin.getName());
			pin.addOutput(outputDin.getName(), outputDin);
			// Producere is set when the data node is created.
		}
			
	}
	
	public ResultSet getAllOutputs(String processName, String timestamp, int pinId, String queryVarName) {
		String queryPrefix = getQueryPrefix(processName, timestamp);		

		String sinVarName = "sin";
		String selectOutputsQuery = queryPrefix
				+ "\nSELECT ?" + queryVarName 
				+ "\n WHERE  { ?" + sinVarName + " " + SIN_PREFIX + Properties.OUTPUTS + " ?" + queryVarName 
				+ "\n . ?" + sinVarName + " " + SIN_DDG_ID + " " + pinId + "}";

		return performQuery(selectOutputsQuery);
	}
	
	public DataInstanceNode addDataResourceToProvenance(
			ProcedureInstanceNode producer, Resource dataResource, ProvenanceData provData) {
		DataInstanceNode din;
		String name = retrieveDinName(dataResource);
		int dinId= retrieveDinId(dataResource);
		String type = retrieveDinType(dataResource);
		String currentVal = retrieveDinValue(dataResource);
		din = addDinToProvData(
			name, type, dataResource,
			currentVal, dinId, producer, provData);
		return din;
	}

	/**
	 * Sets the predecessor nodes of the pin.  Assumes the predecessors have already been
	 * loaded from the DB
	 * @param queryPrefix the query to start the prefix
	 * @param pin the node to set the predecessors for
	 */
	private void setPredecessors(String queryPrefix, ProcedureInstanceNode pin) {
		String queryVarName = "pred";
		String sinVarName = "sin";
		String selectSuccessorsQuery = queryPrefix
				+ "\nSELECT ?" + queryVarName 
				+ "\n WHERE  { ?" + sinVarName + " " + SIN_PREFIX + Properties.PREDECESSORS + " ?" + queryVarName 
				+ "\n . ?" + sinVarName + " " + SIN_DDG_ID + " " + pin.getId() + "}";

		// Find out which nodes are the predecessors in the DB
		ResultSet predecessorsResultsSet = performQuery(selectSuccessorsQuery);
		while (predecessorsResultsSet.hasNext()) {
			QuerySolution predecessorSolution = predecessorsResultsSet.next();
			Resource predecessorResource = predecessorSolution.getResource(queryVarName);

			// Find the nodes in the provenance graph that correspond to the predecessors
			ProcedureInstanceNode pred = (ProcedureInstanceNode) pd.getNodeForResource(predecessorResource.getURI());
			
			// Connect the predecessor and successor
			pred.addSuccessor(pin);
			pin.addPredecessor(pred);
		}
	}
	
	/**
	 * Send the query to the DB 
	 * @param selectQueryString the query to ask
	 * @return the result set
	 */
	private ResultSetRewindable performQuery(String selectQueryString) {
		System.out.println(selectQueryString);
		Query selectQuery = QueryFactory.create(selectQueryString);

		// Execute the query and obtain results
		QueryExecution selectQueryExecution = QueryExecutionFactory.create(
				selectQuery, dataset);

		ResultSet resultSet;
		
		// Do the query in a read transaction
		dataset.begin(ReadWrite.READ);
		try {
			resultSet = selectQueryExecution.execSelect();
		} finally {
			dataset.end();
		}

		// Allows the result set to be reused
		ResultSetRewindable rewindableResultSet = ResultSetFactory
				.makeRewindable(resultSet);

		// Output the result set as text
		ResultSetFormatter.out(System.out, rewindableResultSet, selectQuery);
		rewindableResultSet.reset();
		return rewindableResultSet;
	}

	/**
	 * Create the appropriate type of data instance node for this resource
	 * and add it to the provenance data
	 * 
	 * @param currentName The parameter name 
	 * @param currentType The type of node
	 * @param currentRes The RDF resource describing the node
	 * @param currentVal The data value
	 * @param id The node id
	 * @param pin the producer of the data
	 * @return the node in the provenance data
	 */
	private DataInstanceNode addDinToProvData(String currentName,
			String currentType, Resource currentRes, String currentVal, int id, ProcedureInstanceNode pin,
			ProvenanceData provData) {
		DataInstanceNode din = createDataInstanceNode(currentName, currentType, currentVal, provData, pin);
		
		din.setId(id);

		if (!nodesToResContains(currentRes, provData)) {
			provData.addDIN(din, currentRes.getURI());
		}
		return din;
	}

	/**
	 * Create a data instance node for this type stored in the database.  Subclasses
	 * must define this method to support their specific type system
	 * @param name the name of the node
	 * @param type the type of the node
	 * @param currentVal the node's value
	 * @param provData the provenance data object to add the node to
	 * @param pin 
	 * @return the node created
	 */
	@SuppressWarnings("unused")
	protected DataInstanceNode createDataInstanceNode(String name, String type,
			String currentVal, ProvenanceData provData, ProcedureInstanceNode pin) {
		throw new RuntimeException("Subclass must define this!");
	}
	
	/**
	 * Create the appropriate procedure instance node for this type of data
	 * and add it to the provenance data.
	 * 
	 * @param name The node's name
	 * @param currentType The type of node
	 * @param currentRes The rdf resource describing the node
	 * @param id The node's id
	 * @return the procedure instance node that has been added to the provenance data
	 */
	private ProcedureInstanceNode addSinToProvData(String name, String type,
			Resource res, int id, ProvenanceData provData) {
		ProcedureInstanceNode pin = createProcedureInstanceNode (name, type, provData);

		if (!nodesToResContains(res, provData)) {
			provData.addPIN(pin, res.getURI(), id);
		}

		return pin;
	}

	public SortedSet<Resource> getDinsNamed (String processName,
			String timestamp, String name) {
		String queryPrefix = getQueryPrefix(processName, timestamp);		
		String queryVar = "din";

		String selectDinQueryString = queryPrefix
				+ "\nSELECT ?" + queryVar 
				+ "\n WHERE  { ?" + queryVar + " " + DIN_PREFIX + Properties.NAME + " \"" + name + "\" }";

		ResultSet dinResultSet = performQuery(selectDinQueryString);
		
		SortedSet<Resource> sortedResources = new TreeSet<Resource>(new Comparator<Resource>() {

			@Override
			// Allows sorting of din resources by id.
			public int compare(Resource r0, Resource r1) {
				return retrieveDinId(r0) - retrieveDinId(r1);
			}
			
		});
		
		// Go through the result set putting them into a sorted set.
		while (dinResultSet.hasNext()) {
			QuerySolution nextStepResult = dinResultSet.next();
			Resource currentRes = nextStepResult.getResource(queryVar);
			sortedResources.add(currentRes);
		}
		return sortedResources;
		
	}

	public Resource getProducer(String processName, String timestamp, Resource din) {
		String queryPrefix = getQueryPrefix(processName, timestamp);		
		String queryVar = "s";
		

		String selectStepsQueryString = queryPrefix
				+ "\nSELECT ?" + queryVar 
				+ "\n WHERE  { <" + din.getURI() + "> " + DIN_PREFIX + Properties.PRODUCER + " ?" + queryVar + " }";

		ResultSet stepResultSet = performQuery(selectStepsQueryString);
		
		/* There should be exactly one producer ! */
		while (stepResultSet.hasNext()) {
			QuerySolution nextStepResult = stepResultSet.next();
			Resource currentRes = nextStepResult.getResource(queryVar);
			return currentRes;
		}
		
		assert false;
		return null;
	}

	/**
	 * Create a procedure instance node for the data read from the database.  Subclasses must
	 * override this
	 * @param name the name of the node
	 * @param type the type of node
	 * @param provData the provenance object to add the node to
	 * @return the node created
	 */
	@SuppressWarnings("unused")
	protected ProcedureInstanceNode createProcedureInstanceNode (String name, String type,
			ProvenanceData provData) {
		throw new RuntimeException("Subclass must define this!");
	}
	
	private boolean nodesToResContains(Resource r, ProvenanceData provData) {
		return provData.containsResource(r.getURI());
	}

	public String retrieveSinType(Resource res) {
		return retrieveStringProperty(res, prop.getSinType(res.getModel()));
	}

	public int retrieveSinId(Resource res) {
		return retrieveIntProperty(res, prop.getSinDDGId(res.getModel()));
	}

	public String retrieveSinName(Resource res) {
		return retrieveStringProperty(res, prop.getSinName(res.getModel()));
	}

	public String retrieveDinName(Resource res) {
		return retrieveStringProperty (res, prop.getDinName(res.getModel()));
	}

	public String retrieveDinType(Resource res) {
		return retrieveStringProperty(res, prop.getDinType(res.getModel()));
	}

	public String retrieveDinValue(Resource res) {
		return retrieveStringProperty(res, prop.getDinValue(res.getModel()));
	}

	public int retrieveDinId(Resource res) {
		return retrieveIntProperty (res, prop.getDinDDGId(res.getModel()));
	}

	private int retrieveIntProperty(Resource res, Property propertyName) {
		Statement propertyValue = res.getProperty(propertyName);
		return propertyValue.getInt();
	}

	private String retrieveStringProperty(Resource res, Property propertyName) {
		Statement propertyValue = res.getProperty(propertyName);
		return propertyValue.getString();
	}

	private String getQueryPrefix(String processName, String timestamp) {
		prop = new Properties(processName, timestamp);
	
		String queryPrefix = RDF_PREFIX
				+ "\nPREFIX  " + SIN_PREFIX + "  <" + prop.getSinURI() + ">"
				+ "\nPREFIX  " + DIN_PREFIX + "  <" + prop.getDinURI() + ">";
		return queryPrefix;
	}




}
