package laser.ddg.persist;

import java.util.Collection;

/**
 * Required methods for persisting DDGs and performing queries on the DDG 
 * database (independent of technology used for persisting)
 * @author Sophia
 *
 */
public interface PersistDDG {
	
	/**
	 * saves an object into the database
	 * @param o object to store
	 */
	public void save(Object o);
	
	
	
	/**
	 * Return Collection of query results of the query specified with the 
	 * string queryString
	 * @param queryString
	 * @return result collection of results returned by the query queryString
	 */
	public Collection makeQuery(String queryString); 
	
	/**
	 * print out the contents of the Collection of query results
	 * @param queryResult
	 */
	public void printQueryResult(Collection queryResult);

	/**
	 * print out all the contents of the database
	 */
	public void printAllInDB();


}
