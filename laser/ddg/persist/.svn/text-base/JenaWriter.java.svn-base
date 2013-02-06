package laser.ddg.persist;

import laser.ddg.DataBindingEvent;
import laser.ddg.DataBindingEvent.BindingEvent;
import laser.ddg.DataInstanceNode;
import laser.ddg.ProcedureInstanceNode;
import laser.ddg.ProvenanceData;
import laser.ddg.ProvenanceListener;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.Calendar;

/**
 * Writes an RDF model to a Jena database.  It does this incrementally as
 * the process executes.
 * 
 * @author Barbara Lerner
 * @version Jun 21, 2012
 *
 */
public class JenaWriter implements ProvenanceListener {
	private Dataset dataset;
	private Properties props;
	private ProvenanceData provData;
	
	/**
	 * Creates a new dataset in the database for this DDG.
	 */
	@Override
	public void processStarted(String processName, ProvenanceData provData) {
		// This creates a model that persists in a tdb database.
		System.out.println("Creating tdb database");
		// System.getProperties().list(System.out);
		dataset = RdfModelFactory.getNewDataset();
		this.provData = provData;
		
		addDDG(provData.getProcessName());
	}

	private void addDDG(String processName) {
		String timestamp = DateFormat.getDateTimeInstance(DateFormat.FULL,
				DateFormat.FULL).format(Calendar.getInstance());
		JenaLoader jenaLoader = new JenaLoader(
				RdfModelFactory.JENA_DIRECTORY);
		String processURI = jenaLoader.getProcessURI(processName);

		props = new Properties(processName, timestamp);
			
		if (processURI == null) {
			processURI = addProcessNameToDB(processName, timestamp,
						jenaLoader);
		} else {
			updateProcessExecutionCounter(processName, processURI,
						timestamp, jenaLoader);
		}

	}

	// Inside a write transaction
	private void updateProcessExecutionCounter(String processName, String processURI, 
			String timestamp, JenaLoader jenaLoader) {
		int numExecutions = jenaLoader.getNumExecutions(processURI);
		numExecutions++;
		System.out.println("Updating numExecutions of " + processURI + " in DB to " + numExecutions);
		dataset.begin(ReadWrite.WRITE);

		try {
			Model model = dataset.getDefaultModel();
			Resource processResource = model.getResource(processURI);
			Statement numExecutionssProperty = processResource.getProperty(props.getNumExecutions(model));
			numExecutionssProperty.changeLiteralObject(numExecutions);
			addTimestamp(processName, timestamp, processURI, numExecutions-1, model);
			dataset.commit();
		} finally {
			dataset.end();
		}
	}

	// Inside a write transaction
	private String addProcessNameToDB(String processName, String timestamp, JenaLoader jenaLoader) {
		int numProcesses = jenaLoader.getNumProcesses();
		System.out.println("numProcesses = " + numProcesses + " (different process definitions executed");
		String processURI = Properties.ALL_PROCESSES_URI + numProcesses;

		dataset.begin(ReadWrite.WRITE);

		try {
			Model model = dataset.getDefaultModel();
			Resource newProcess = model.createResource(processURI);
			System.out.println("Adding process " + processName + " to model " + System.identityHashCode(model));
			Property processNameProperty = props.getProcessNameProperty(model);

			newProcess.addProperty(processNameProperty, processName);
			numProcesses++;
			
			if (numProcesses == 1) {
				System.out.println("num processes in db is 0");
				Resource allProcesses = model.createResource(Properties.ALL_PROCESSES_URI);
				Property numProcessesProperty = props.getNumProcessesProperty(model);
				allProcesses.addLiteral(numProcessesProperty, numProcesses);
			}
			else {
				System.out.println("Updating numProcesses in DB to " + numProcesses);
				Resource allProcesses = model.getResource(Properties.ALL_PROCESSES_URI);
				Statement numProcessesProperty = allProcesses.getProperty(props.getNumProcessesProperty(model));
				numProcessesProperty.changeLiteralObject(numProcesses);
			}
			
			System.out.println("This is the first execution of " + processName + " in the db");
			Property processExecutionsProperty = props.getNumExecutions(model);
			newProcess.addLiteral(processExecutionsProperty, 1);
			
			addTimestamp(processName, timestamp, processURI, 0, model);
			dataset.commit();
		} finally {
			dataset.end();
		}

		return processURI;
	}

	// Inside a transaction
	private void addTimestamp(String processName, String timestamp, String processURI, int executionId, Model model) {
		String executionURI = processURI + "/" + executionId;
		Resource newExecution = model.createResource(executionURI);
		Property processNameProperty = props.getProcessNameProperty(model);
		newExecution.addProperty(processNameProperty, processName);
		Property timeStampProperty = props.getTimestamp(model);
		newExecution.addProperty(timeStampProperty, timestamp);
	}

	/**
	 * Makes the din persistent
	 */
	@Override
	public void dataNodeCreated(DataInstanceNode din) {
		persistDin(din);
	}

	/**
	 * @param din
	 */
	private void persistDin(DataInstanceNode din) {
		dataset.begin(ReadWrite.WRITE);

		try {
			Model model = dataset.getDefaultModel();
			String resourceId = props.getDinResourceId(din);
			Resource newDin = model.createResource(resourceId);
			provData.bindNodeToResource(din, newDin.getURI());
			System.out.println("Adding name " + din.getName() + " to resource "
					+ resourceId + " in model " + System.identityHashCode(model) + " with value " + din.getValue().toString());
			newDin.addProperty(props.getDinName(model), din.getName());
			newDin.addLiteral(props.getDinDDGId(model), din.getId());
			newDin.addProperty(props.getDinType(model), din.getType());
			newDin.addProperty(props.getDinValue(model), din.getValue().toString());
			dataset.commit();
		} finally {
			dataset.end();
		}
	}

	/**
	 * Makes the pin persistent
	 */
	@Override
	public void procedureNodeCreated(ProcedureInstanceNode pin) {
		persistSin(pin);
	}

	/**
	 * @param sin
	 */
	private void persistSin(ProcedureInstanceNode sin) {
		dataset.begin(ReadWrite.WRITE);
		try {
			Model model = dataset.getDefaultModel();
			Resource newSin = model.createResource(props.getSinResourceId(sin));
			
			provData.bindNodeToResource(sin, newSin.getURI());
			System.out.println("Adding name " + sin.getName()
					+ " to resource" + newSin);

			newSin.addProperty(props.getSinName(model), sin.getName());
			newSin.addProperty(props.getSinType(model), sin.getType());
			newSin.addLiteral (props.getSinDDGId(model), sin.getId());
			newSin.addProperty(props.getStepProperty(model), "Dummy Step Def");
			dataset.commit();
		} finally {
			dataset.end();
		}
	}

	/**
	 * Adds the successor/predecessor properties to the nodes that are connected.
	 */
	@Override
	public void successorEdgeCreated(ProcedureInstanceNode predecessor,
			ProcedureInstanceNode successor) {
		dataset.begin(ReadWrite.WRITE);
		try {
			Model model = dataset.getDefaultModel();
			Resource succResource = model.getResource(provData.getResource(successor));
			Resource predResource = model.getResource(provData.getResource(predecessor));
			succResource.addProperty(props.getSinPredecessors(model), predResource);
			predResource.addProperty(props.getSinSuccessors(model), succResource);
			dataset.commit();
		} finally {
			dataset.end();
		}
	}

	/**
	 * Adds the input and output properties to the pin and user/producer properties to the din
	 */
	@Override
	public void bindingCreated(DataBindingEvent e) {
		dataset.begin(ReadWrite.WRITE);
		try {
			Model model = dataset.getDefaultModel();
			Resource proc = model.getResource(provData.getResource(e.getProcNode()));
			Resource data = model.getResource(provData.getResource(e.getDataNode()));
	
			if (e.getEvent() == BindingEvent.INPUT) {
				proc.addProperty(props.getSinInputs(model), data);
				data.addProperty(props.getDinUsers(model), proc);
			}
			
			else {
				proc.addProperty(props.getSinOutputs(model), data);
				data.addProperty(props.getDinProducer(model), proc);
			}
			dataset.commit();
		} finally {
			dataset.end();
		}
	}

	/**
	 * Closes the model, causing it to get flushed to disk.
	 */
	@Override
	public void processFinished() {
		dataset.begin(ReadWrite.READ);
		try {
			Model model = dataset.getDefaultModel();
			model.write(System.out);
		} finally {
			dataset.end();
		}
	}

}
