/* 
 * Copyright 2015 Charles Allen Schultz II.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package edu.claflin.cyfinder.internal.ui;

import edu.claflin.cyfinder.internal.logic.ConfigurationBundle;
import static edu.claflin.cyfinder.internal.ui.GridBagBuilder.getConstraints;

import edu.claflin.finder.algo.Algorithm;
import edu.claflin.finder.algo.Algorithm.GraphSortOrder;
import edu.claflin.finder.algo.ArgumentsBundle;
import edu.claflin.finder.algo.BreadthFirstTraversalSearch;
import edu.claflin.finder.algo.Bundle;
import edu.claflin.finder.algo.DepthFirstTraversalSearch;
import edu.claflin.finder.logic.Condition;

import edu.claflin.finder.logic.Edge;
import edu.claflin.finder.logic.comp.EdgeWeightComparator;
import edu.claflin.finder.logic.cond.BipartiteCondition;
import edu.claflin.finder.logic.cond.CliqueCondition;
import edu.claflin.finder.logic.cond.DirectedCliqueCondition;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.regex.Pattern;
import javax.swing.*;

import edu.claflin.finder.logic.cygrouper.Communicator;
import edu.claflin.finder.logic.cygrouper.CygrouperNode;
import org.cytoscape.model.CyColumn;

/**
 * Represents a configuration dialog for the Subgraph Finder external utility. 
 * 
 * @author Charles Allen Schultz II
 * @version 1.7 June 19, 2015
 */
public class ConfigDialog extends JDialog implements ActionListener,
        ItemListener {
    
    /**
     * Used to populate the Conditions List.
     */
    private static final Class[] conditions = 
            new Class[] {
                BipartiteCondition.class, 
                CliqueCondition.class,
                DirectedCliqueCondition.class
            };
    /**
     * Used to populate the algorithms list.
     */
    private static final Class[] algorithms = new Class[] {
        BreadthFirstTraversalSearch.class,
        DepthFirstTraversalSearch.class
    };
    /**
     * Used to populate the orderings list.
     */
    private static final Class[] orderings = 
            new Class[] {Void.class, EdgeWeightComparator.class};
    
    private final List<CyColumn> columns;
    private CyColumn selectedColumn = null;
    
    /**
     * The action to execute upon completing the configuration.
     */
    private final Action successAction;
    
    // UI components
    
    /**
     * GUI: Conditions Label.
     * And toolTip text
     */
    private JLabel cLabel = new JLabel("Search Condition ?*");
    private String cLabelInfo = "<html>Select One of the Given Search conditions:<p><br>"
    		+ "1) Bi-Partite Condition 			-> When a node that belongs to one group has no direct\n<p>"
    		+ "													connection to any other  node of the same group\n<p><br>"
    		+ "2) Clique Condition	  			-> When every node is connected to every other node directly\n<p><br>"
    		+ "3) Directed Clique Condtion	-> Similar to Clique, but this condition doesn't require there to be<p>"
    		+ " 												 an edge from every node to every other node<p></html>";
    
    /**
     * GUI: Algorithms Label.
     * And toolTip text
     */
    private JLabel aLabel = new JLabel("Search Algorithm ?*");
    private String aLabelInfo = "<html>Select One of the Given Search Algorithms: <p><br>"
    		+ "1) Breath First Traversal Search ->  It starts at the tree root and explores<p>"
    		+ " all of the neighbor nodes at the present depth prior to moving on to the nodes at the next depth level.<p><br>"
    		+ "2) Depth First Traversal Search -> The algorithm starts at the root node and <p>"
    		+ "explores as far as possible along each branch before backtracking</html>";
    /**
     * GUI: Orderings Label.
     * 
     * And toolTip text
     */
    private JLabel oLabel = new JLabel("Ordering ?*:");
    private String oLabelInfo ="<html>Select One of the Given Search Algorithms: <p><br>"
    		+ "1) Void (Default Setting) -> No specific ordering<p><br>"
    		+ "2) Edge Weight Comparitor -> Compares the subgraphs based on:<p>"
    		+ "2.1. Weight -> based on the average weight of edges in the specific subgraph<p>"
    		+ "2.2. SUID -> based on order of nodes ";
    
    /**
     * GUI: Scroll pane for the conditions list.
     */
    private JScrollPane cPane;
    /**
     * GUI: Conditions list object.
     */
    private JList conditionsList;
    /**
     * GUI: Scroll pane for the algorithms list.
     */
    private JScrollPane aPane;
    /**
     * GUI: Algorithms list object.
     */
    private JList algorithmsList;
    
    /**
     * GUI: Combo box for selecting an ordering.
     */
    private JComboBox orderingSelection;
    
    /**
     * GUI: Checkbox for setting a comparison algorithm to sort in ascending
     * order.
     */
    private JCheckBox aCheckBox = new JCheckBox("Ascending Order");
    
    /**
     * GUI: Checkbox for enabling "preservative" searching.
     * 
     * rapin001 on 2/10/20
     * Set the default value as true, so it's checked
     * 
     *  tried parameter - if the user has made a choice already in case the 
     */
    private JLabel pCheckBoxLabel = new JLabel("Edge Preservative ?*");
    private JCheckBox pCheckBox = new JCheckBox("", true);
    private String pCheckBoxInfo = "<html>Select if the edge preservation is enabed :<p><br>"
    		+ "Enabled -> Will abandon a node if any of its edges violates the condition<p><br>"
    		+ "Disabled -> Will only abandon the edge that violates the condtion, not the whole node<p><br>"
    		+ "Warning!<p>"
    		+ "1) If DISABLED, the results might be false or might display nothing<p>"
    		+ "2) Most use comes when using with the Bi-Partite condition</html>";
    private boolean tried = false;
    
    
    /**
     * GUI: Lable for the save subgraph section of Gui
     * 
     * rapin001
     */
    private JLabel saveGraphOption = new JLabel("Display/storing options ?*");
    private String saveGraphOptionInfo = "<html>Select display/save method for generated sub-graphs"
    		+ "<p>Can select more than one of the following options<p><br>"
    		+ "1) In-Place annotation of source graph -> generates a truth table for each subgraph showing whioch nodes are inclooded<p><br>"
    		+ "2) New Child Graph beneath source graph -> generate and display seperate subgraphs based on the source graph in Cytoscape<p><br>"
    		+ "3) Save found subgraph to file -> save generated subgraphs to file <br> -generates .txt file for each subgraph specifying<p></html>";
    /**
     * GUI: JComboBox for selecting a sort method for the graph outputs by size
     */
   private JLabel sortGraphSelectionLabel = new JLabel("Save Order ?*");
   private String sortGraphSelectionInfo = "<html>Select the order by which to display the subgraphs based on node count/average weight<p><br>"
   		+ "1) None ->  no sorting order (Default option)<p>"
   		+ "2) Ascending -> low to high node count<p>"
   		+ "3) Descending -> high to low node count<p>"
   		+ "4) Average Weight -> based on average weight of subgraph</html>";
    private JComboBox sortGraphSelection;
    
    /**
     * GUI: Checkbox for enabling in-place annotation.
     */
    private JCheckBox iCheckBox = new JCheckBox("In-Place annotation of source graph.");
    /**
     * GUI: Checkbox for enabling new child creation.
     */
    private JCheckBox nCheckBox = new JCheckBox("New Child Graph beneath source graph.");
    /**
     * GUI: Checkbox for enabling saving to file.
     */
    private JCheckBox sCheckBox = new JCheckBox("Save found subgraph to file.");
    
    /**
     * GUI: Button to complete configuration.
     */
    private JButton doneButton = new JButton("Done");
    
    /**
     * JTextField for selecting a k-partite number
     */
    private JLabel partiteFieldLabel = new JLabel("K-partite number (optional, minimum 3) ?*");
    private JTextField partiteField = new JTextField();
    private String partiteFieldInfo = "<html>Specify the k-partie number: <p><br>"
    		+ "1) Leave Blank -> No ordering is applied<p><br>"
    		+ "2) Type the number which specifies the range of the partition number (0 to k)<p>"
    		+ "-- Min number is 3</html>";
    /**
     * The File object indicating the directory to save subgraphs in.
     */
    private File saveDirectory = null;
    
    /**
     * Add lable to instruction on how to get more info
     * rapin001
     */
    private JLabel helpLabel = new JLabel ("Need Help?*");
    private String  helpInfo = "<html>For more information about the available options and features,<p> position your mouse cursor over the  ->\"?*\"<-  symbol</html>";
    
    /**
     *  rapin001 @ 2/19/20
     *  For testing which condition and which algorith is used
     */
    private final int startCharCond = -36;
    
    /**
     * String for Bipartite Condition
     */
    private final String biPartiteCon= "BipartiteCondition";
   
    /**
     * String for Clique Condition
     */
    private final String cliqueCon = "CliqueCondition";
   
    /**
     * String for directed clique condtion
     */
    private final String dirCliqueCon = "DirectedCliqueCondition";
    
    /**
     *  String variable for comparing strings to know which algorithm is used
     */
    
    @SuppressWarnings("unused")
	private final String BFTSSting = "BreadthFirstTraversalSearch]";
   
    /**
     * Variables to hold which algorithm is chosen
     */
    private final int startCharAlgo = 31;
   //----------------------------------------------------------------------------------------------------
    
    /**
     * Constructor for initializing the Panel.
     */
    public ConfigDialog(Frame parent, Action successAction,
            List<CyColumn> columns) {
        super(parent, "Configure Subgraph Finder", true);
        this.successAction = successAction;
        this.columns = columns;
        
        ClassModel cModel = new ClassModel(conditions);
        conditionsList = new JList(cModel);
        conditionsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        conditionsList.setCellRenderer(cModel);
        cPane = new JScrollPane(conditionsList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        ClassModel aModel = new ClassModel(algorithms);
        algorithmsList = new JList(aModel);
        algorithmsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        algorithmsList.setCellRenderer(aModel);
        aPane = new JScrollPane(algorithmsList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        ClassModel orderingsModel = new ClassModel(orderings);
        orderingSelection = new JComboBox(orderingsModel);
        orderingSelection.setSelectedIndex(0);
        orderingSelection.setRenderer(orderingsModel);
        if (columns.isEmpty())
            orderingSelection.setEnabled(false);
        
        orderingSelection.addItemListener(this);
        sCheckBox.addActionListener(this);
        aCheckBox.setSelected(true);
        aCheckBox.setEnabled(false);
        
        doneButton.addActionListener(this);
        
        sortGraphSelection = new JComboBox();
        sortGraphSelection.addItem(new ComboItem("None", 0));
        sortGraphSelection.addItem(new ComboItem("Ascending", 1));
        sortGraphSelection.addItem(new ComboItem("Descending", 2));
        sortGraphSelection.addItem(new ComboItem("Average Weight", 3));
        
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(true);
        init();
    }
    /**
     * Initializes and constructs the GUI.
     */
    private void init() {
    	
    	//FIXME - add tool tip text for improved UI experience
    	cLabel.setToolTipText(cLabelInfo);
    	aLabel.setToolTipText(aLabelInfo);
    	oLabel.setToolTipText(oLabelInfo);
    	pCheckBoxLabel.setToolTipText(pCheckBoxInfo);
    	partiteFieldLabel.setToolTipText(partiteFieldInfo);
    	saveGraphOption.setToolTipText(saveGraphOptionInfo);
    	sortGraphSelectionLabel.setToolTipText(sortGraphSelectionInfo);
    	helpLabel.setToolTipText(helpInfo);
    	
        setLayout(new GridBagLayout());
        Insets insets = new Insets(2, 2, 2, 2);
        
        add(cLabel, getConstraints(0, 0, 2, 1, 1, 1, 
                GridBagConstraints.NONE, GridBagConstraints.PAGE_END, 
                0, 0, insets));
        add(aLabel, getConstraints(2, 0, 2, 1, 1, 1, 
                GridBagConstraints.NONE, GridBagConstraints.PAGE_END, 
                0, 0, insets));
        add(cPane, getConstraints(0, 1, 2, 4, 1, 1, 
                GridBagConstraints.BOTH, GridBagConstraints.CENTER, 
                0, 0, insets));
        add(aPane, getConstraints(2, 1, 2, 4, 1, 1, 
                GridBagConstraints.BOTH, GridBagConstraints.CENTER, 
                0, 0, insets));
        add(new JSeparator(JSeparator.HORIZONTAL), 
                getConstraints(0, 5, 4, 1, 1, 0, 
                        GridBagConstraints.BOTH, GridBagConstraints.CENTER, 
                        0, 0, insets));
        add(oLabel, getConstraints(0, 6, 1, 1, 1, 1, 
                GridBagConstraints.NONE, GridBagConstraints.LINE_START, 
                0, 0, insets));
        add(orderingSelection, getConstraints(0, 7, 2, 1, 1, 1, 
                GridBagConstraints.NONE, GridBagConstraints.CENTER, 
                0, 0, insets));
        add(aCheckBox, getConstraints(0, 8, 2, 1, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.CENTER,
                0, 0, insets));
        add(new JSeparator(JSeparator.VERTICAL),
                getConstraints(2, 6, 1, 3, 1, 0,
                        GridBagConstraints.BOTH, GridBagConstraints.LINE_START,
                        0, 0, insets));
        add(pCheckBoxLabel, getConstraints(3, 6, 1, 1, 1, 1, 
                GridBagConstraints.BOTH, GridBagConstraints.LINE_START, 
                0, 0, insets));
        add(pCheckBox, getConstraints(3, 7, 1, 1, 1, 1, 
                GridBagConstraints.BOTH, GridBagConstraints.CENTER, 
                0, 0, insets));
        add(new JSeparator(JSeparator.HORIZONTAL), 
                getConstraints(0, 9, 4, 1, 1, 0, 
                        GridBagConstraints.BOTH, GridBagConstraints.CENTER, 
                        0, 0, insets));
        add (saveGraphOption,  getConstraints(0, 10, 1, 1, 1, 1, 
                GridBagConstraints.NONE, GridBagConstraints.LINE_START, 
                0, 0, insets));
        add(iCheckBox, getConstraints(0, 11, 4, 1, 1, 1,
                GridBagConstraints.BOTH, GridBagConstraints.CENTER,
                0, 0, insets));
        add(nCheckBox, getConstraints(0, 12, 4, 1, 1, 1,
                GridBagConstraints.BOTH, GridBagConstraints.CENTER,
                0, 0, insets));
        add(sortGraphSelectionLabel, getConstraints(0, 13, 1, 1, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.CENTER,
                0, 0, insets));
        add(sortGraphSelection, getConstraints(1, 13, 1, 1, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.LINE_END,
                0, 0, insets));
        add(sCheckBox, getConstraints(0, 14, 4, 1, 1, 1,
                GridBagConstraints.BOTH, GridBagConstraints.CENTER,
                0, 0, insets));
        add(new JSeparator(JSeparator.HORIZONTAL),
                getConstraints(0, 15, 4, 1, 1, 0,
                        GridBagConstraints.BOTH, GridBagConstraints.CENTER,
                        0, 0, insets));
        add(partiteFieldLabel, getConstraints(0, 16, 2, 1, 1, 1, GridBagConstraints.BOTH, GridBagConstraints.CENTER,0, 0, insets));
        add(partiteField, getConstraints(1, 17, 2, 1, 1, 1,
                GridBagConstraints.BOTH, GridBagConstraints.LINE_START,
                0, 0, insets));
        add(new JSeparator(JSeparator.HORIZONTAL),
                getConstraints(0, 18, 4, 1, 1, 0,
                        GridBagConstraints.BOTH, GridBagConstraints.CENTER,
                        0, 0, insets));
        add(doneButton, getConstraints(2, 19, 2, 1, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.LINE_END,
                0, 0, insets));
        add(helpLabel, getConstraints(0, 19, 1, 1, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.LINE_START,
                0, 0, insets));
        //testing();
        pack();
    }


    /* Evyatar & Ariel, not used. Kept for testing purposes */
    public void testing(){
        Communicator communicator = Communicator.getSingleton();
        JTextArea txt = new JTextArea();
        for(int i = 0; i < communicator.groups.size();i++){
            txt.append("SG"+(i+1)+" :{\n");
            for(Map.Entry<String, CygrouperNode> x : communicator.groups.get(i).entrySet()){
                txt.append("\t"+x.getKey()+" "+x.getValue().group+"\n");
            }
            txt.append("}\n");
        }
        JFrame newFrame = new JFrame();
        JPanel panel = new JPanel();
        JScrollPane scroll = new JScrollPane(panel);
        panel.add(txt);
        newFrame.add(scroll);
        newFrame.setSize(500,500);
        newFrame.setVisible(true);
    }

    /**
     * Creates a ConfigurationBundle object holding the configuration as 
     * defined by the user.
     * 
     * @return the ConfigurationBundle.
     * @throws Exception should a problem with reflection occur.
     */
    public ConfigurationBundle getConfigurationBundle() throws Exception {
        ConfigurationBundle configBundle = new ConfigurationBundle();
        ArgumentsBundle argsBundle = new ArgumentsBundle();
        
        // Add Conditions
        List selectedConditions = conditionsList.getSelectedValuesList();
        for (Object obj : selectedConditions) {
            Class condClass = (Class) obj;
            argsBundle.addCondition((Condition) condClass.newInstance());
        }
        
        // Add Ordering
        Class selectedOrdering = (Class) orderingSelection.getSelectedItem();
        if (selectedOrdering != Void.class) {
            Constructor declaredConstructor = selectedOrdering.getDeclaredConstructor(boolean.class);
            Comparator<Edge> edgeWeightComparator = (Comparator<Edge>) declaredConstructor.newInstance(aCheckBox.isSelected());
            argsBundle.putObject(ArgumentsBundle.COMMON_ARGS.EDGE_WEIGHT_COMPARATOR.toString(), edgeWeightComparator);
            configBundle.setOrderingColumn(selectedColumn);
        }
        
        // Set Preservative
        argsBundle.putBoolean(ArgumentsBundle.COMMON_ARGS.EDGE_PRESERVATION.toString(), pCheckBox.isSelected());
        
        // Select Algorithm
        List selectedAlgorithms = algorithmsList.getSelectedValuesList();
        
        Algorithm algo;
        if (selectedAlgorithms.size() == 1) {
            Class algoClass = (Class) selectedAlgorithms.get(0);
            Constructor constructor = algoClass.getConstructor(ArgumentsBundle.class);
            algo = (Algorithm) constructor.newInstance(argsBundle);    
            
            //FIXME - add check for algorithm selected
        } else {
            ArrayList<Algorithm> bundledAlgos = new ArrayList<>();
            for (Object obj : selectedAlgorithms) {
                Class algoClass = (Class) obj;
                Constructor constructor = algoClass.getConstructor(ArgumentsBundle.class);
                bundledAlgos.add((Algorithm) constructor.newInstance(argsBundle));
            }
            algo = new Bundle(bundledAlgos.toArray(new Algorithm[0]));
        }
        
        int partiteNumber = 0;
        try {
        	partiteNumber = Integer.parseInt(partiteField.getText());
        }
        catch (Exception e) {
        	//do nothing
        }       
        algo.setPartiteNumber(partiteNumber);
        
        int orderIndex = sortGraphSelection.getSelectedIndex();
        if(orderIndex == 0) {
        	//NONE
        	algo.setGraphSortOrder(GraphSortOrder.NONE);
        } else if(orderIndex == 1) {
        	//ASCENDING
        	algo.setGraphSortOrder(GraphSortOrder.ASCENDING);
        } else if(orderIndex == 2) {
        	//DESCENDING
        	algo.setGraphSortOrder(GraphSortOrder.DESCENDING);
        } else if(orderIndex == 3) {
        	algo.setGraphSortOrder(GraphSortOrder.AVERAGE_WEIGHT);
        } else {
        	throw new IllegalArgumentException("Order index wasn't valid for some reason.");
        }             
          
        configBundle.setAlgo(algo);
        
        configBundle.setInPlace(iCheckBox.isSelected());
        configBundle.setNewChild(nCheckBox.isSelected());
        configBundle.setSaveToFile(sCheckBox.isSelected());
        
        if (configBundle.isSaveToFile())
            configBundle.setSaveDirectory(saveDirectory);
        
        return configBundle;
    }

    
    /**
     * {@inheritDoc }
     */
    @Override
    public void actionPerformed(ActionEvent e) {
    	
        if (e.getSource() == doneButton) {
            String errorTitle = "Configuration Error";
            if (conditionsList.getSelectedValuesList().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "You must select a condition to search for!", errorTitle,
                        JOptionPane.ERROR_MESSAGE);
            } else if (algorithmsList.getSelectedValuesList().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "You must select an algorithm to use in the search!",
                        errorTitle, JOptionPane.ERROR_MESSAGE);
            } else if (!iCheckBox.isSelected() && !nCheckBox.isSelected()
                    && !sCheckBox.isSelected()) {
                JOptionPane.showMessageDialog(this,
                        "You must select a means of saving results!", errorTitle,
                        JOptionPane.ERROR_MESSAGE);
            } 
            
            /*
             * rapin001 @ 2/20
             * Added a warning in case clique and no edge preservation is used with an undirected graph created using the addative method while the issue is addressed 
             */

   /*         else if (conditionsList.getSelectedValue().toString().substring(startCharCond).equals(cliqueCon)  && !pCheckBox.isSelected() && !tried)
            {
            	tried = true;
            	JOptionPane.showMessageDialog(this, "Currently the option:\n\n\t Clique + Breath First or Depth First Traversal Searches + No Edge preservation\n"
            			+ "When the graphed worked on is made undirecte using the: Additive method\n\n is not supported!\n\n"
            			+ "Please change your search criteria or press \"Done\" again to run", "Warrning", JOptionPane.ERROR_MESSAGE);
     

            }
            
      */      

// -------------------------------------------------------------------------            
            
            /*
             * rapin001 @ 2/20
             * Added a error message when user tries to select: Currently the option: Bipartite + Breath First Traversal Search + No Edge Presertvation while the issue is resolved
             */

     /*    
            else if (conditionsList.getSelectedValue().toString().substring(startCharCond).equals(biPartiteCon) && algorithmsList.getSelectedValuesList().toString().substring(startCharAlgo).equals(BFTSSting) && !pCheckBox.isSelected())

        	{
        		 JOptionPane.showMessageDialog(this, "Currently the option:\n\n\t Bipartite + Breath First Traversal Search + No Edge Presertvation \n\n is not supported!", errorTitle, JOptionPane.ERROR_MESSAGE);
        	}
     */        

            /*
             * rapin001@ 2/20
             * Added a error message when user selects save to file while issue with feature is being worked on 
             */
    /*    	
     		else if (sCheckBox.isSelected())
        	{
        		 JOptionPane.showMessageDialog(this, "Currently the option:\n\n\t Save found subgraph to file  \n\n is not supported!", errorTitle, JOptionPane.ERROR_MESSAGE);
        	}

      */ 	

            else {
                ActionEvent newEvent = null;
                try {
                    newEvent = new ActionEvent(getConfigurationBundle(), 0,
                            "CONFIG_BUNDLE");
                } catch (Exception ex) {
                    String description = "An error occurred trying to create "
                            + "the ConfigurationBundle.";
                    ErrorPanel errorPanel = new ErrorPanel(description, ex);
                    errorPanel.display(this, errorTitle);
                } finally {
                    setVisible(false);
                    if (newEvent != null) {
                        successAction.actionPerformed(newEvent);
                    }
                    dispose();
                }
            }
        } 
        // FIXME - selecting the output location for when save to file is selected ; THIS CURRENTLY WORKS
        else if (e.getSource() == sCheckBox && sCheckBox.isSelected()) {
            JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home"));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnval = fileChooser.showOpenDialog(this);
            
            if (returnval != JFileChooser.APPROVE_OPTION) {
                sCheckBox.setSelected(false);
            } else {
                saveDirectory = fileChooser.getSelectedFile();
            }
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getItemSelectable() == orderingSelection && 
                e.getStateChange() == ItemEvent.SELECTED && 
                orderingSelection.getSelectedIndex() != 0) {
            CyColumn response = (CyColumn) JOptionPane.showInputDialog(this, 
                    "Please select a column to use for ordering.",
                    "Ordering Configuration",
                    JOptionPane.QUESTION_MESSAGE,
                    null, 
                    columns.toArray(new CyColumn[0]),
                    columns.get(0));
            
            if (response == null) {
                orderingSelection.setSelectedIndex(0);
                aCheckBox.setEnabled(false);
            } else {
                selectedColumn = response;
                aCheckBox.setEnabled(true);
            }
        }
    }
    
    /**
     * Represents a ListModel, a ComboBoxModel, and a ListCellRenderer all in 
     * one.
     */
    private final class ClassModel extends DefaultComboBoxModel<Class> 
            implements ListCellRenderer {

        /**
         * The Class data to use.
         */
        private final Class[] data;
        /**
         * A default renderer for making the majority of the component.
         */
        private DefaultListCellRenderer dLCR = new DefaultListCellRenderer();
        
        /**
         * Initialize the object.
         * @param data the Class array containing the options.
         */
        public ClassModel(Class[] data) {
            this.data = data;
        }
        
        /**
         * {@inheritDoc }
         * @return an integer indicating the size of the data.
         */
        @Override
        public int getSize() {
            return data.length;
        }

        /**
         * {@inheritDoc }
         * @param index the integer index of the object queried.
         * @return the Class object at that index.
         */
        @Override
        public Class getElementAt(int index) {
            return data[index];
        }

        /**
         * {@inheritDoc } Overwrites the label information to be the simple 
         * class name.
         * 
         * @param list the JList being rendered.
         * @param value the value of the object.
         * @param index the index of the object.
         * @param isSelected a boolean indicating if the object is selected.
         * @param cellHasFocus a boolean indicating if the cell has focus.
         * @return a JComponent rendering of the cell (JLabel).
         */
        @Override
        public Component getListCellRendererComponent(JList list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel listLabel = (JLabel) 
                    dLCR.getListCellRendererComponent(list, value, index, 
                            isSelected, cellHasFocus);
            
            Class selectedClass = (Class) value;
            String original = selectedClass.getSimpleName();
            String spaced = Pattern.compile("([a-z])([A-Z])").matcher(original)
                    .replaceAll("$1 $2");
            listLabel.setText(spaced);
            
            return listLabel;
        }
    }
    
    class ComboItem
    {
        private String key;
        private int value;

        public ComboItem(String key, int value)
        {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString()
        {
            return key;
        }

        public String getKey()
        {
            return key;
        }

        public int getValue()
        {
            return value;
        }
    }
}
