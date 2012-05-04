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
 * @author johnlindsay
 */
public class MaxDownslopeElevationChange implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "MaxDownslopeElevationChange";
    }

    @Override
    public String getDescriptiveName() {
    	return "Maximum Downslope Elevation Change";
    }

    @Override
    public String getToolDescription() {
    	return "Calculates the maximum downslope change in elevation between a "
                + "grid cell and its eight downslope neighbors.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "TerrainAnalysis" };
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
        if (myHost != null && ((progress != previousProgress) || 
                (!progressLabel.equals(previousProgressLabel)))) {
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
    
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    private boolean cancelOp = false;
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }
    
    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    
    private boolean amIActive = false;
    public boolean isActive() {
        return amIActive;
    }

    public void run() {
        amIActive = true;
        
        String inputHeader = null;
        String outputHeader = null;
        double slope;
        double z, z2;
        int c, i;
        int progress;
        int[] dY = {-1, 0, 1, 1, 1, 0, -1, -1};
        int[] dX = {1, 1, 1, 0, -1, -1, -1, 0};
        int row, col, x, y;
        double dist;
        double maxSlope = 0;
        double maxZChange = 0;
                
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                outputHeader = args[i];
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster DEM = new WhiteboxRaster(inputHeader, "r");
            DEM.isReflectedAtEdges = true;
            
            int rows = DEM.getNumberRows();
            int cols = DEM.getNumberColumns();
            double noData = DEM.getNoDataValue();
            double gridResX = DEM.getCellSizeX();
            double gridResY = DEM.getCellSizeY();
            double diagGridRes = Math.sqrt(gridResX * gridResX + gridResY * gridResY);
            double[] gridLengths = new double[]{diagGridRes, gridResX, diagGridRes, gridResY, diagGridRes, gridResX, diagGridRes, gridResY};

            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette("spectrum.pal");

            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = DEM.getValue(row, col);
                    if (z != noData) {
                        maxSlope = -99999999;
                        for (c = 0; c < 8; c++) {
                            x = col + dX[c];
                            y = row + dY[c];
                            dist = gridLengths[c];
                            z2 = DEM.getValue(y, x);
                            if (z2 != noData) {
                                slope = (z - z2) / dist;

                                if (slope > maxSlope) {
                                    maxSlope = slope;
                                    maxZChange = z - z2;
                                }
                            }
                            
                        }
                        if (maxSlope > 0) {
                            output.setValue(row, col, maxZChange);

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
                progress = (int)(100f * row / (rows - 1));
                updateProgress(progress);
            }

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            DEM.close();
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