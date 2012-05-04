/*
 * Copyright (C) 2011 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class AverageUpslopeFlowpathLength implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    // Constants
    private static final double LnOf2 = 0.693147180559945;

    @Override
    public String getName() {
        return "AverageUpslopeFlowpathLength";
    }

    @Override
    public String getDescriptiveName() {
        return "Average Upslope Flowpath Length";
    }

    @Override
    public String getToolDescription() {
        return "Measures the average length of all upslope flowpaths draining to each grid cell.";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"FlowpathTAs"};
        return ret;
    }

    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    private void showFeedback(String message) {
        if (myHost != null) {
            myHost.showFeedback(message);
        } else {
            System.out.println(message);
        }
    }

    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }
    private int previousProgress = 0;
    private String previousProgressLabel = "";

    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null && ((progress != previousProgress)
                || (!progressLabel.equals(previousProgressLabel)))) {
            myHost.updateProgress(progressLabel, progress);
        }
        previousProgress = progress;
        previousProgressLabel = progressLabel;
    }

    private void updateProgress(int progress) {
        if (myHost != null && progress != previousProgress) {
            myHost.updateProgress(progress);
        }
        previousProgress = progress;
    }

    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    private boolean cancelOp = false;

    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }

    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    private boolean amIActive = false;

    @Override
    public boolean isActive() {
        return amIActive;
    }

    @Override
    public void run() {
        amIActive = true;

        String inputHeader = null;
        String outputHeader = null;
        int row, col, x, y;
        int progress = 0;
        double z, val, val2;
        int i, c;
        int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        double[] inflowingVals = new double[]{16, 32, 64, 128, 1, 2, 4, 8};
        boolean flag = false;
        double flowDir = 0;
        double flowLength = 0;
        double numUpslopeFlowpaths = 0;
        double flowpathLengthToAdd = 0;
        
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        inputHeader = args[0];
        outputHeader = args[1];
        

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster pntr = new WhiteboxRaster(inputHeader, "r");
            int rows = pntr.getNumberRows();
            int cols = pntr.getNumberColumns();
            double noData = pntr.getNoDataValue();

            double gridResX = pntr.getCellSizeX();
            double gridResY = pntr.getCellSizeY();
            double diagGridRes = Math.sqrt(gridResX * gridResX + gridResY * gridResY);
            double[] gridLengths = new double[]{diagGridRes, gridResX, diagGridRes, gridResY, diagGridRes, gridResX, diagGridRes, gridResY};
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw",
                    inputHeader, WhiteboxRaster.DataType.FLOAT, -999);
            output.setPreferredPalette("blueyellow.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);
            output.setZUnits(pntr.getXYUnits());

            WhiteboxRaster numInflowingNeighbours = new WhiteboxRaster(outputHeader.replace(".dep", 
                    "_temp1.dep"), "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, 0);
            numInflowingNeighbours.isTemporaryFile = true;
            
            WhiteboxRaster numUpslopeDivideCells = new WhiteboxRaster(outputHeader.replace(".dep", 
                    "_temp2.dep"), "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, 0);
            numUpslopeDivideCells.isTemporaryFile = true;
            
            WhiteboxRaster totalFlowpathLength = new WhiteboxRaster(outputHeader.replace(".dep", 
                    "_temp3.dep"), "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, 0);
            totalFlowpathLength.isTemporaryFile = true;
            
            updateProgress("Loop 1 of 3:", 0);
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    if (pntr.getValue(row, col) != noData) {
                        z = 0;
                        for (i = 0; i < 8; i++) {
                            if (pntr.getValue(row + dY[i], col + dX[i]) ==
                                    inflowingVals[i]) { z++; }
                        }
                        if (z > 0) {
                            numInflowingNeighbours.setValue(row, col, z);
                        } else {
                            numInflowingNeighbours.setValue(row, col, -1);
                        }
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress("Loop 1 of 3:", progress);
            }
            
            updateProgress("Loop 2 of 3:", 0);
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    val = numInflowingNeighbours.getValue(row, col);
                    if (val <= 0 && val != noData) {
                        flag = false;
                        x = col;
                        y = row;
                        do {
                            val = numInflowingNeighbours.getValue(y, x);
                            if (val <= 0 && val != noData) {
                                //there are no more inflowing neighbours to visit; carry on downslope
                                if (val == -1) {
                                    //it's the start of a flowpath
                                    numUpslopeDivideCells.setValue(y, x, 0);
                                    numUpslopeFlowpaths = 1;
                                } else {
                                    numUpslopeFlowpaths = numUpslopeDivideCells.getValue(y, x);
                                }
                                
                                numInflowingNeighbours.setValue(y, x, noData);

                                // find it's downslope neighbour
                                flowDir = pntr.getValue(y, x);
                                if (flowDir > 0) {
                                    // what's the flow direction as an int?
                                    c = (int) (Math.log(flowDir) / LnOf2);
                                    flowLength = gridLengths[c];
                                    val2 = totalFlowpathLength.getValue(y, x);
                                    flowpathLengthToAdd = val2 + numUpslopeFlowpaths * flowLength;

                                    //move x and y accordingly
                                    x += dX[c];
                                    y += dY[c];
                                    
                                    numUpslopeDivideCells.setValue(y, x, 
                                            numUpslopeDivideCells.getValue(y, x) 
                                            + numUpslopeFlowpaths);
                                    totalFlowpathLength.setValue(y, x, 
                                            totalFlowpathLength.getValue(y, x)
                                            + flowpathLengthToAdd);
                                    numInflowingNeighbours.setValue(y, x, 
                                            numInflowingNeighbours.getValue(y, x) - 1);
                                } else {  // you've hit the edge or a pit cell.
                                    flag = true;
                                }
                                
                            } else {
                                flag = true;
                            }
                        } while (!flag);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress("Loop 2 of 3:", progress);
            }
            
            numUpslopeDivideCells.flush();
            totalFlowpathLength.flush();
            numInflowingNeighbours.close();
            
            updateProgress("Loop 3 of 3:", 0);
            double[] data1 = null;
            double[] data2 = null;
            double[] data3 = null;
            for (row = 0; row < rows; row++) {
                data1 = numUpslopeDivideCells.getRowValues(row);
                data2 = totalFlowpathLength.getRowValues(row);
                data3 = pntr.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    if (data3[col] != noData) {
                        if (data1[col] > 0) {
                            val = data2[col] / data1[col];
                            output.setValue(row, col, val);
                        } else {
                            output.setValue(row, col, 0);
                        }
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress("Loop 3 of 3:", progress);
            }
            
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            pntr.close();
            numUpslopeDivideCells.close();
            totalFlowpathLength.close();
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