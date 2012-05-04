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
import java.io.*;
import java.nio.*;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class MinMaxContrastStretch implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "MinMaxContrastStretch";
    }

    @Override
    public String getDescriptiveName() {
    	return "Min-Max Contrast Stretch";
    }

    @Override
    public String getToolDescription() {
    	return "Performs a min-max contrast stretch on an input image.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "ImageEnhancement" };
    	return ret;
    }
    
    private void showFeedback(String feedback) {
        if (myHost != null) {
            myHost.showFeedback(feedback);
        } else {
            System.out.println(feedback);
        }
    }
    
    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }

    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null) {
            myHost.updateProgress(progressLabel, progress);
        } else {
            System.out.println(progressLabel + " " + progress + "%");
        }
    }

    private void updateProgress(int progress) {
        if (myHost != null) {
            myHost.updateProgress(progress);
        } else {
            System.out.print("Progress: " + progress + "%");
        }
    }
    
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
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
        int row, col;
        double z;
        double noData;
        int progress;
        int i;
        double minVal, maxVal;
        int numBins = 1024;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        inputHeader = args[0];
        outputHeader = args[1];
        minVal = Double.parseDouble(args[2]);
        maxVal = Double.parseDouble(args[3]);
        numBins = Integer.parseInt(args[4]);
        
        // check to see that the inputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }
        
        try {
            
            WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");
            int nRows = image.getNumberRows();
            int nCols = image.getNumberColumns();
            noData = image.getNoDataValue();
            double scaleFactor = numBins / (maxVal - minVal);
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.INTEGER, noData);
            output.setPreferredPalette(image.getPreferredPalette());
            
            double[] data = null;
            for (row = 0; row < nRows; row++) {
                data = image.getRowValues(row);
                for (col = 0; col < nCols; col++) {
                    if (data[col] != noData) {
                        z = (int)(data[col] - minVal) * scaleFactor;
                        if (z < 0) { z = 0; }
                        if (z > (numBins - 1)) { z = (numBins - 1); }
                        output.setValue(row, col, z);
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (nRows - 1));
                updateProgress(progress);
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
