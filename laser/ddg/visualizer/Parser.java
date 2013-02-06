package laser.ddg.visualizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Reads a textual description of a DDG and constructs a prefuse graph for it.
 * @author Barbara Lerner
 */
public class Parser {
	private BufferedReader in;
	private PrefuseGraphBuilder builder;
	private int numPins;

	/**
	 * Initializes the parser
	 * @param file the file to read the DDG from
	 * @param builder the prefuse object that will build the graph
	 * @throws FileNotFoundException if the file to parse cannot be found
	 */
	public Parser(File file, PrefuseGraphBuilder builder) 
		throws FileNotFoundException {
		in = new BufferedReader (new FileReader (file));
		this.builder = builder;
	}

	/**
	 * Adds the nodes and edges from the DDG to the graph.
	 * @throws IOException if there is a problem reading the file
	 */
	public void addNodesAndEdges() throws IOException {
		String nextLine = in.readLine();
		if (nextLine == null) {
			throw new IOException("Number of pins is missing from the file.");
		}
		numPins = Integer.parseInt(nextLine);
		nextLine = in.readLine();
		while (nextLine != null) {
			parseDeclaration(nextLine);
			nextLine = in.readLine();
		}
	}

	private void parseDeclaration(String nextLine) {
		if (!nextLine.equals("")) {
			String[] tokens = nextLine.split(" ");
			if (tokens[0].equals("CF") || tokens[0].equals("DF")) {
				parseEdge(tokens);
			}
			else {
				parseNode(tokens);
			}
		}
	}

	private void parseNode(String[] tokens) {
		System.out.println("Found node " + tokens[1] + " named " + tokens[2]);
		builder.addNode(tokens[0], extractUID(tokens[1]), 
				constructName(tokens));
		
	}

	private String constructName(String[] tokens) {
		StringBuilder str = new StringBuilder();
		for (int i = 2; i < tokens.length; i++) {
			str.append(tokens[i] + " ");
		}
		if (isMultipleNodePIN(tokens[0])){
			str.append(tokens[0]);
		}
		return str.toString();
	}

	private boolean isMultipleNodePIN(String type) {
		if (type.equals("Start") || type.equals("Interm") || type.equals("Finish")) {
			return true;
		}

		if (type.equals("VStart") || type.equals("VInterm") || type.equals("VFinish")) {
			return true;
		}
		
		return false;
	}

	private int extractUID(String idToken) {
		int uid = Integer.parseInt(idToken.substring(1));
		if (idToken.charAt(0) == 'd') {
			uid = uid + numPins;
		}
		return uid;
	}

	private void parseEdge(String[] tokens) {
		System.out.println("Found edge " + tokens[1] + " to " + tokens[2]);
		builder.addEdge(tokens[0], extractUID(tokens[1]), 
				extractUID(tokens[2]));
	}

}
