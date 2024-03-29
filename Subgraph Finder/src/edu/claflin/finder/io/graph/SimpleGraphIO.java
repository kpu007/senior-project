package edu.claflin.finder.io.graph;

import edu.claflin.finder.io.graph.sub.GraphWriter;
import edu.claflin.finder.io.graph.sub.GraphReader;
import static edu.claflin.finder.Global.getLogger;
import static edu.claflin.finder.Global.getOutput;
import edu.claflin.finder.log.LogLevel;
import edu.claflin.finder.logic.Graph;
import edu.claflin.finder.logic.BronKerboschBipartiteUtils;
import edu.claflin.finder.logic.BronKerboschHandler;
import edu.claflin.finder.logic.Edge;
import edu.claflin.finder.logic.Node;
import edu.claflin.finder.logic.cygrouper.GraphAverageWeight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

/**
 * A class for reading and writing graphs.
 * 
 * @author Charles Allen Schultz II
 * @version 3.1.2 February 2, 2016
 */
public final class SimpleGraphIO implements GraphReader, GraphWriter {

    /**
     * Empty Constructor for instantiating the class.  No customization 
     * necessary.
     */
    public SimpleGraphIO() {}
    
    /**
     * {@inheritDoc } Reads simple tab delimited graphs.
     * 
     * A graph file consists of an edge on each line with a tab delimiting the 
     * two nodes joined by the edge and followed by an integer value 
     * representing the edge weight.
     * 
     * @param source the File object representing the graph.
     * @param undirected a boolean indicating if the graph should be 
     * interpreted as undirected.
     * @return the Graph object parsed from the file.
     */
    @Override
    public Graph parseGraph(File source, boolean undirected) {
        
        // Make sure file is actually a file.
        if (!testFile(source)) {
            return null;
        }
        
        Graph graph = new Graph(source.getName().split("\\.")[0]);
        
        if (getLogger() != null) {
            getLogger().logInfo(LogLevel.VERBOSE, 
                    "GraphIO: Attempting to read graph: "
                    + source.getAbsolutePath());
        }
        
        // Read graph here.  Assume File is a valid Graph file.
        try (BufferedReader bR = new BufferedReader(new FileReader(source))) {
            String line;
            
            do {
                line = bR.readLine();
                if (line != null) {
                    String[] nodeString = line.split("\t");
                    
                    // Add nodes to Graph.
                    Node[] nodes = new Node[2];
                    for (int nodeIndex = 0; nodeIndex < 2; nodeIndex++) {
                        nodes[nodeIndex] = new Node(nodeString[nodeIndex]);
                    }
                    for (Node node : nodes) {
                        if (!graph.getNodeList().contains(node))
                            graph.addNode(node);
                    }
                    
                    graph.addEdge(new Edge(nodes[0], nodes[1], Integer.parseInt(nodeString[2]), undirected));
                }
            } while (line != null);
        } catch (IOException ioe) {
            if (getLogger() != null) {
                getLogger().logError(LogLevel.NORMAL, 
                        "GraphIO: Error processing Graph from file: "
                        + source.getAbsolutePath());
            }
            graph = null;
        } finally {
            if (graph != null && getLogger() != null) {
                getLogger().logInfo(LogLevel.NORMAL, 
                        "GraphIO: Successfully constructed graph from file.");
            }
            return graph;
        }
    }
    
    /**
     * {@inheritDoc } Writes simple tab delimited graphs.
     * 
     * Outputs to the disk a graph.  Each line
     * of the outputted graph represents an edge.  The two nodes joined by the
     * edge are delimited by a tab.
     * 
     * @param toWrite the {@link Graph} object to write to a file.
     * 
     */
    @Override
    public void writeGraph(Graph toWrite){
        File output = new File(getOutput(), toWrite.getName());
        boolean error = false;
        
        if (getLogger() != null) {
            getLogger().logInfo(LogLevel.NORMAL, 
                    "GraphIO: Attempt to log graph to file: "
                    + toWrite.getName());
        }
        
        try (BufferedWriter bW = new BufferedWriter(new FileWriter(output))) {
        	if (!toWrite.getEdgeList().isEmpty())
        	{
        		
      			// to see if the graph given has no nodesS
	            for (Edge edge : toWrite.getEdgeList()) {
	            	Integer weight = verifyRelationship(edge.getData());
	                String line = String.format("%s\t%s\t%s", 
	                        edge.getSource().getIdentifier(), 
	                        edge.getDestination().getIdentifier(), weight); // last variable is for 
	                bW.write(line);
	                bW.newLine();
	               
	                if (getLogger() != null) {
	                    getLogger().logInfo(LogLevel.VERBOSE, 
	                            "GraphIO: Wrote line to graph file: " + line);
	                } 
	            }
	            bW.write("Average edge weight: " + GraphAverageWeight.getAverageWeight(toWrite));
	            bW.newLine();
        	}
        	
        } catch (IOException ioe) {
            error = true;
            if (getLogger() != null) {
                getLogger().logInfo(LogLevel.NORMAL, 
                        "GraphIO: Error writing graph to file: "
                        + toWrite.getName());
            }
        }
        finally {
            String success = error ? "Failed to write" : "Succesfully wrote";
            if (getLogger() != null) {
                getLogger().logInfo(LogLevel.NORMAL, "GraphIO: " + success +
                        " graph to file: " + output.getAbsolutePath());
            }
        }
        
        //This makes the program take a long time. Upon thinking about it further it might simply not be possible
        //to use this algorithm with any reasonably sized graph
        //writeExtraGraph(BronKerboschHandler.maximumCompleteClique(toWrite));
       
    }
    
    
    /**
     * TESTING METHOD FOR BRON KERBOSCH BIPARTITE STUFF
     */
    /*
    public void writeExtraGraph(Graph toWrite){
        File output = new File(getOutput(), toWrite.getName() + " LCCG" + toWrite.getNodeCount() + toWrite.getEdgeList().size());
        boolean error = false;
        
        if (getLogger() != null) {
            getLogger().logInfo(LogLevel.NORMAL, 
                    "GraphIO: Attempt to log graph to file: "
                    + toWrite.getName());
        }
        
        try (BufferedWriter bW = new BufferedWriter(new FileWriter(output))) {
        	if (!toWrite.getEdgeList().isEmpty())
        	{
        		
      			// to see if the graph given has no nodesS
	            for (Edge edge : toWrite.getEdgeList()) {
	              
	            	
	            	Integer weight = verifyRelationship(edge.getData());
	                String line = String.format("%s\t%s\t%s", 
	                        edge.getSource().getIdentifier(), 
	                        edge.getDestination().getIdentifier(), weight); // last variable is for 
	                bW.write(line);
	                bW.newLine();
	               
	                
	                if (getLogger() != null) {
	                    getLogger().logInfo(LogLevel.VERBOSE, 
	                            "GraphIO: Wrote line to graph file: " + line);
	                            
	               
	                }
	                
	            }

	            bW.write("Average edge weight: " + GraphAverageWeight.getAverageWeight(toWrite));
	            bW.newLine();
        	}
        	
        } catch (IOException ioe) {
            error = true;
            if (getLogger() != null) {
                getLogger().logInfo(LogLevel.NORMAL, 
                        "GraphIO: Error writing graph to file: "
                        + toWrite.getName());
            }
        }
        finally {
            String success = error ? "Failed to write" : "Succesfully wrote";
            if (getLogger() != null) {
                getLogger().logInfo(LogLevel.NORMAL, "GraphIO: " + success +
                        " graph to file: " + output.getAbsolutePath());
            }
        }
    } */
    
    /**
     * Tests to see if a File is a valid graph file.  Currently only tests by
     * checking to ensure the provided file is not a directory.
     * 
     * @param file the File to test.
     * @return the boolean value representing if the file is valid.
     */
    private static boolean testFile(File file) {
        if (file.isDirectory()) {
            if (getLogger() != null) {
                getLogger().logError(LogLevel.NORMAL,
                        "GraphIO: File is a directory: " + file.getAbsolutePath());
            }
            return false;
        }
        return true;
    }
    
    /**
     * rapin001 @ 3/2020
     * Function to verify the edge data is an Integer
     * 
     * @param edgeData object that you want to verify the class of to be Integer
     * @return	Integer		edge weight or 0 if edge data is not an Integer
     */
    private Integer verifyRelationship(Object edgeData) {
        if (edgeData instanceof Integer)
            return (Integer) edgeData;
        else
            return 0;
    }
}


