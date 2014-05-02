/*
 * Copyright (C) 2014 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
 
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import java.util.Date
import java.util.ArrayList
import java.util.Queue
import java.util.LinkedList
import whitebox.interfaces.WhiteboxPluginHost
import whitebox.geospatialfiles.WhiteboxRaster
import whitebox.geospatialfiles.WhiteboxRasterInfo
import whitebox.geospatialfiles.WhiteboxRasterBase.DataType
import whitebox.geospatialfiles.WhiteboxRasterBase.DataScale
import whitebox.ui.plugin_dialog.ScriptDialog
import whitebox.utilities.StringUtilities
import whitebox.structures.NibbleArray2D
import groovy.transform.CompileStatic

// The following four variables are required for this 
// script to be integrated into the tool tree panel. 
// Comment them out if you want to remove the script.
def name = "TotalLengthOfUpstreamChannels"
def descriptiveName = "Total Length of Upstream Channels"
def description = "Calculates the total length of channels upstream."
def toolboxes = ["StreamAnalysis"]

public class TotalLengthOfUpstreamChannels implements ActionListener {
	private WhiteboxPluginHost pluginHost
	private ScriptDialog sd;
	private String descriptiveName
	
	public TotalLengthOfUpstreamChannels(WhiteboxPluginHost pluginHost, 
		String[] args, def name, def descriptiveName) {
		this.pluginHost = pluginHost
		this.descriptiveName = descriptiveName
			
		if (args.length > 0) {
			execute(args)
		} else {
			// Create a dialog for this tool to collect user-specified
			// tool parameters.
		 	sd = new ScriptDialog(pluginHost, descriptiveName, this)	
		
			// Specifying the help file will display the html help
			// file in the help pane. This file should be be located 
			// in the help directory and have the same name as the 
			// class, with an html extension.
			sd.setHelpFile(name)
		
			// Specifying the source file allows the 'view code' 
			// button on the tool dialog to be displayed.
			def pathSep = File.separator
			def scriptFile = pluginHost.getResourcesDirectory() + "plugins" + pathSep + "Scripts" + pathSep + name + ".groovy"
			sd.setSourceFile(scriptFile)
			
			// add some components to the dialog
			sd.addDialogFile("Input streams raster", "Input Streams Raster:", "open", "Raster Files (*.dep), DEP", true, false)
			sd.addDialogFile("Input D8 flow pointer raster", "Input D8 Pointer Raster:", "open", "Raster Files (*.dep), DEP", true, false)
			sd.addDialogFile("Output file", "Output Raster File:", "save", "Raster Files (*.dep), DEP", true, false)
			
			// resize the dialog to the standard size and display it
			sd.setSize(800, 400)
			sd.visible = true
		}
	}

	// The CompileStatic annotation can be used to significantly
	// improve the performance of a Groovy script to nearly 
	// that of native Java code.
	@CompileStatic
	private void execute(String[] args) {
		try {
	  		int progress, oldProgress, x, y, x2, y2, c
	  		int[] dX = [ 1, 1, 1, 0, -1, -1, -1, 0 ]
			int[] dY = [ -1, 0, 1, 1, 1, 0, -1, -1 ]
			double[] inflowingVals = [16, 32, 64, 128, 1, 2, 4, 8]
			double flowDir, nFlowDir, streamVal, length
        	double outNodata = -32768
        	boolean flag
        	final double LnOf2 = 0.693147180559945
        	
			if (args.length != 3) {
				pluginHost.showFeedback("Incorrect number of arguments given to tool.")
				return
			}
			// read the input parameters
			String streamsFile = args[0]
			String pointerFile = args[1]
			String outputFile = args[2]
			
			WhiteboxRaster streams = new WhiteboxRaster(streamsFile, "r")
			double nodata = streams.getNoDataValue()
			int rows = streams.getNumberRows()
			int cols = streams.getNumberColumns()
			double gridResX = streams.getCellSizeX()
			double gridResY = streams.getCellSizeY()
			double diagRes = Math.sqrt(gridResX * gridResX + gridResY * gridResY)
			double[] lengths = [diagRes, gridResX, diagRes, gridResY, diagRes, gridResX, diagRes, gridResY]
			
			WhiteboxRaster pntr = new WhiteboxRaster(pointerFile, "r")
			double pointerNodata = pntr.getNoDataValue()
			if (rows != pntr.getNumberRows() || cols != pntr.getNumberColumns()) {
				pluginHost.returnData("Error: All input files must have the same dimensions (rows and columns).")
				return
			}

			WhiteboxRaster output = new WhiteboxRaster(outputFile, "rw", 
  		  	     streamsFile, DataType.FLOAT, outNodata, outNodata)
  		  	output.setPreferredPalette("spectrum.pal")

  		  	NibbleArray2D numInflowing = new NibbleArray2D(rows, cols)

  		  	Queue<ChannelHeadCell> heads = new LinkedList<>()

  		  	oldProgress = -1
			for (int row in 0..(rows - 1)) {
				for (int col in 0..(cols - 1)) {
					streamVal = streams.getValue(row, col)
  					if (streamVal != nodata && streamVal > 0) {
  						int i = 0
  						for (c = 0; c < 8; c++) {
                            x2 = col + dX[c]
                            y2 = row + dY[c]
                            nFlowDir = pntr.getValue(y2, x2)
                            if (streams.getValue(y2, x2) > 0 
                                    && nFlowDir == inflowingVals[c]) {
                                i++
                            }
                        }
                        if (i == 0) {
                        	heads.add(new ChannelHeadCell(row, col))
                        }
                        numInflowing.setValue(row, col, i)
                        output.setValue(row, col, 0.0d)
  					}
  				}
  				progress = (int)(100f * row / rows)
				if (progress > oldProgress) {
					pluginHost.updateProgress("Loop 1 of 2:", progress)
					oldProgress = progress

					// check to see if the user has requested a cancellation
					if (pluginHost.isRequestForOperationCancelSet()) {
						pluginHost.showFeedback("Operation cancelled")
						return
					}
				}
			}

			pluginHost.updateProgress("Loop 2 of 2:", 0)
			while (heads.size() > 0) {
				ChannelHeadCell head = heads.poll()
				x = head.column
                y = head.row
                flag = true
                while (flag) {
                    flowDir = pntr.getValue(y, x)
                    if (flowDir != pointerNodata && flowDir > 0) {
                    	c = (int) (Math.log(flowDir) / LnOf2)
                    	length = output.getValue(y, x)
                       	x += dX[c]
                        y += dY[c]
                        streamVal = streams.getValue(y, x)
  						if (streamVal != nodata && streamVal > 0) {
	                        int i = numInflowing.getValue(y, x) - 1
	                        numInflowing.setValue(y, x, i)
	                        output.incrementValue(y, x, lengths[c] + length)
	                        if (i > 0) {
	                        	flag = false
	                        }
  						} else {
  							flag = false
  						}
                    } else {
                    	flag = false
                    }
                }
			}
			
			streams.close()
			pntr.close()
			
			output.addMetadataEntry("Created by the "
	                    + descriptiveName + " tool.")
	        output.addMetadataEntry("Created on " + new Date())
			output.close()
	
			// display the output image
			pluginHost.returnData(outputFile)
	
		} catch (OutOfMemoryError oe) {
            pluginHost.showFeedback("An out-of-memory error has occurred during operation.")
	    } catch (Exception e) {
	        pluginHost.showFeedback("An error has occurred during operation. See log file for details.")
	        pluginHost.logException("Error in " + descriptiveName, e)
        } finally {
        	// reset the progress bar
        	pluginHost.updateProgress("Progress:", 0)
        }
	}
	
	@Override
    public void actionPerformed(ActionEvent event) {
    	if (event.getActionCommand().equals("ok")) {
    		final def args = sd.collectParameters()
			sd.dispose()
			final Runnable r = new Runnable() {
            	@Override
            	public void run() {
                	execute(args)
            	}
        	}
        	final Thread t = new Thread(r)
        	t.start()
    	}
    }

    class ChannelHeadCell {
		int row
		int column
		
		public ChannelHeadCell(int row, int column) {
			this.row = row
			this.column = column
		}
    	
    }
}

if (args == null) {
	pluginHost.showFeedback("Plugin arguments not set.")
} else {
	def tdf = new TotalLengthOfUpstreamChannels(pluginHost, args, name, descriptiveName)
}
