package laser.ddg;

import java.io.Serializable;
import java.util.Iterator;

/**
 * See comments for AbstractDataInstanceNode
 * 
 * It is required for the data instance node to store information about the
 * procedure instance node that produced the data in the DIN and the procedure
 * instance node(s) that use the data in the DIN
 * 
 * @author Sophia
 * 
 */
public interface DataInstanceNode extends Node {

	/**
	 * Record that a procedure is using this value
	 * 
	 * @param user
	 *            the using procedure
	 */
	public void addUserPIN(ProcedureInstanceNode user);

	/**
	 * @return the Java data produced by the running process
	 */
	public Serializable getValue();

	/**
	 * @return the ProcedureInstanceNode that created the data of this DIN
	 */
	public ProcedureInstanceNode getProducer();

	/**
	 * @return procedures (ProcedureInstanceNodes) that use this data
	 */
	public Iterator<ProcedureInstanceNode> users();

	/**
	 * Getter for the DIN ID
	 * 
	 * @return the ID assigned to a DIN
	 */
	@Override
	public int getId();

	/**
	 * Setter for the DIN ID
	 * 
	 * @param newId
	 * @throws IdAlreadySetException
	 */
	public void setId(int newId) throws IdAlreadySetException;

	/**
	 * Returns a simplified string representation of the type of node
	 * 
	 * @return a simplified string representation of the type of node
	 */
	public String getType();

	/**
	 * @return the name of the Data Instance Node
	 */
	public String getName();

}
