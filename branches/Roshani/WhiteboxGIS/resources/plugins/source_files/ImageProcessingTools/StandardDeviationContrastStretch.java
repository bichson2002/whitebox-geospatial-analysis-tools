/*
 * Copyright (C) 2011-2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package plugins;

import java.util.Date;
import java.io.*;
import java.nio.*;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class StandardDeviationContrastStretch implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name containing no spaces.
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "StandardDeviationContrastStretch";
    }
    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer name (containing spaces) and is used in the interface to list the tool.
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
    	return "Standard Deviation Contrast Stretch";
    }
    /**
     * Used to retrieve a short description of what the plugin tool does.
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
    	return "Performs a Standard Deviation Contrast Stretch on an input image.";
    }
    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
    	String[] ret = { "ImageEnhancement" };
    	return ret;
    }
    /**
     * Used to communicate feedback pop-up messages between a plugin tool and the main Whitebox user-interface.
     * @param feedback String containing the text to display.
     */
    private void showFeedback(String feedback) {
        if (myHost != null) {
            myHost.showFeedback(feedback);
        } else {
            System.out.println(feedback);
        }
    }
    /**
     * Used to communicate a return object from a plugin tool to the main Whitebox user-interface.
     * @return Object, such as an output WhiteboxRaster.
     */
    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }
    /**
     * Used to communicate a progress update between a plugin tool and the main Whitebox user interface.
     * @param progressLabel A String to use for the progress label.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null) {
            myHost.updateProgress(progressLabel, progress);
        } else {
            System.out.println(progressLabel + " " + progress + "%");
        }
    }
    /**
     * Used to communicate a progress update between a plugin tool and the main Whitebox user interface.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(int progress) {
        if (myHost != null) {
            myHost.updateProgress(progress);
        } else {
            System.out.println("Progress: " + progress + "%");
        }
    }
    /**
     * Sets the arguments (parameters) used by the plugin.
     * @param args 
     */
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    /**
     * Sets the WhiteboxPluginHost to which the plugin tool is tied. This is the class
     * that the plugin will send all feedback messages, progress updates, and return objects.
     * @param host The WhiteboxPluginHost that called the plugin tool.
     */  
    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }
    
    private boolean cancelOp = false;
    /**
     * Used to communicate a cancel operation from the Whitebox GUI.
     * @param cancel Set to true if the plugin should be canceled.
     */
    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }
    
    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    
    private boolean amIActive = false;
    /**
     * Used by the Whitebox GUI to tell if this plugin is still running.
     * @return a boolean describing whether or not the plugin is actively being used.
     */
    @Override
    public boolean isActive() {
        return amIActive;
    }
    
    @Override
    public void run() {
        amIActive = true;
        
        String inputHeader = null;
        String outputHeader = null;
        int row, col;
        int numCols;
        int numRows;
        double z;
        double noData;
        float progress;
        int i;
        int numReclassRanges;
        int numReclassRangesMinusOne;
        String[] reclassRangeStr = null;
        double[][] reclassRange;
        boolean blnAssignMode = false;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                outputHeader = args[i];
            } else if (i == 2) {
                reclassRangeStr = args[i].split("\t");
                if (reclassRangeStr[2].toLowerCase().equals("not specified")) { blnAssignMode = true; }
            }
        }

        // check to see that the inputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }
        
        try {
            
            WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");
            int rows = image.getNumberRows();
            int cols = image.getNumberColumns();
            double[] data;
            noData = image.getNoDataValue();
            
            //How many rows should there be in the ReclassRange array?
            //There are three numbers in each range: New Value, From Value, To Just Less Than Value
            numReclassRanges = reclassRangeStr.length / 3;
            numReclassRangesMinusOne = numReclassRanges - 1;
            reclassRange = new double[3][numReclassRanges];
            i = 0;
            for (int b = 0; b < reclassRangeStr.length; b++) {
                if (!reclassRangeStr[b].toLowerCase().equals("not specified")) {
                    if (!reclassRangeStr[b].toLowerCase().equals("nodata")) {
                        reclassRange[i][b / 3] = Double.parseDouble(reclassRangeStr[b]);
                    } else {
                        reclassRange[i][b / 3] = noData;
                    }
                } else {
                    reclassRange[i][b / 3] = 0;
                }
                i++;
                if (i == 3) {
                    i = 0;
                }
            }

            if (numReclassRanges == 0) {
                showFeedback("There is an error with the reclass ranges.");
                return;
            }

            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette(image.getPreferredPalette());


            if (blnAssignMode) {
                for (row = 0; row < rows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        for (i = 0; i < numReclassRanges; i++) {
                            if (data[col] == reclassRange[1][i]) {
                                output.setValue(row, col, reclassRange[0][i]);
                                break;
                            }
                            if (i == numReclassRangesMinusOne) { //z was not in the reclass ranges; output value equals input value
                                output.setValue(row, col, data[col]);
                            }
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress((int) progress);
                }

            } else {
                for (row = 0; row < rows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        for (i = 0; i < numReclassRanges; i++) {
                            if (data[col] >= reclassRange[1][i] && data[col] < reclassRange[2][i]) {
                                output.setValue(row, col, reclassRange[0][i]);
                                break;
                            }
                            if (i == numReclassRangesMinusOne) { //z was not in the reclass ranges; output value equals input value
                                output.setValue(row, col, data[col]);
                            }
                        }

                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress((int) progress);
                }

            }

            image.close();

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            output.close();

            // returning a header file string displays the image.
            returnData(outputHeader);


        } catch (Exception e) {
            showFeedback(e.getMessage());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }
    
}