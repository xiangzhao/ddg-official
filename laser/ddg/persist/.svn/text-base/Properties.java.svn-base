package laser.ddg.persist;

import laser.ddg.DataInstanceNode;
import laser.ddg.ProcedureInstanceNode;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * Description of the properties of SINs and DINs that are made persistent, and
 * methods that persist the nodes. To persist a DDG, call persistDin for all
 * DINs, persistSin for all SINs, completePersistSin, and completePersistDin in
 * that order (swapping completePersistDin with completePersistSin and
 * persistDin with persistSin is also allowed)
 * 
 * 
 * @author Sophia. Created Jan 4, 2012.
 */
public class Properties {

	
	// Property names
	private static final String PROCESS_NAME = "processName";
	private static final String NUM_EXECUTIONS = "numExecutions";
	private static final String NUM_PROCESSES = "numProcesses";
	private static final String TIMESTAMP = "timestamp";
	private static final String STEP = "step";
	private static final String USERS = "users";
	public static final String PRODUCER = "producer";
	public static final String OUTPUTS = "outputs";
	public static final String INPUTS = "inputs";
	public static final String TYPE = "type";
	public static final String DDG_ID = "DDGId";
	public static final String PREDECESSORS = "predecessors";
	public static final String SUCCESSORS = "successors";
	public static final String NAME = "name";
	private static final String VALUE = "value";

	// URIs
	public static final String ALL_PROCESSES_URI = "http://allprocesses/";
	public static final String PROCESS_NAME_URI = ALL_PROCESSES_URI + PROCESS_NAME;
	public static final String NUM_EXECUTIONS_URI = ALL_PROCESSES_URI + NUM_EXECUTIONS;
	public static final String NUM_PROCESSES_URI = ALL_PROCESSES_URI + NUM_PROCESSES;
	public static final String TIMESTAMP_URI = ALL_PROCESSES_URI + TIMESTAMP;
	private String sinURI;
	private String dinURI;
	private String curProcessURI;
	

	public Properties(String processName, String timestamp) {
		curProcessURI = "http://process/" + processName.replaceAll(" ", "") + "/" + timestamp.replaceAll(" ", "") + "/";
		sinURI = curProcessURI + "sins/";
		dinURI = curProcessURI + "dins/";
	}


	public String getDinResourceId(DataInstanceNode din) {
		return dinURI + din.getId();
	}

	public String getSinResourceId(ProcedureInstanceNode sin){
		return sinURI + sin.getId();
	}

	public Property getSinName(Model m) {
		return m.createProperty(sinURI, NAME);
	}

	public Property getSinPredecessors(Model m) {
		return m.createProperty(sinURI, PREDECESSORS);
	}

	public Property getSinSuccessors(Model m) {
		return m.createProperty(sinURI, SUCCESSORS);
	}

	public Property getSinDDGId(Model m) {
		return m.createProperty(sinURI, DDG_ID);
	}

	public Property getDinDDGId(Model m) {
		return m.createProperty(dinURI, DDG_ID);
	}

	public Property getDinType(Model m) {
		return m.createProperty(dinURI, TYPE);
	}

	public Property getSinType(Model m) {
		return m.createProperty(sinURI, TYPE);
	}

	public Property getSinInputs(Model m) {
		return m.createProperty(sinURI, INPUTS);
	}

	public Property getSinOutputs(Model m) {
		return m.createProperty(sinURI, OUTPUTS);
	}

	public Property getDinName(Model m) {
		return m.createProperty(dinURI, NAME);
	}

	public Property getDinValue(Model m) {
		return m.createProperty(dinURI, VALUE);
	}

	public Property getDinProducer(Model m) {
		return m.createProperty(dinURI, PRODUCER);
	}

	public Property getDinUsers(Model m) {
		return m.createProperty(dinURI, USERS);
	}

	public Property getStepProperty(Model m) {
		return m.createProperty(dinURI, STEP);
	}


	public String getCurProcessURI() {
		return curProcessURI;
	}


	public Property getProcessNameProperty(Model m) {
		return m.createProperty(ALL_PROCESSES_URI, PROCESS_NAME);
	}


	public Property getNumProcessesProperty(Model m) {
		return m.createProperty(ALL_PROCESSES_URI, NUM_PROCESSES);
	}

	public Property getNumExecutions(Model m) {
		return m.createProperty(ALL_PROCESSES_URI, NUM_EXECUTIONS);
	}
	
	public Property getTimestamp(Model m) {
		return m.createProperty(ALL_PROCESSES_URI, TIMESTAMP);
	}


	public String getSinURI() {
		return sinURI;
	}


	public String getDinURI() {
		return dinURI;
	}
}
