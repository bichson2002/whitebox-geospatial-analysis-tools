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

import java.io.File;
import java.io.DataInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import java.awt.Image;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ImportImage implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "ImportImage";
    }

    @Override
    public String getDescriptiveName() {
        return "Import Image";
    }

    @Override
    public String getToolDescription() {
        return "Imports an image file (.png, .gif, .bmp, .jpg).";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"IOTools"};
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

        String inputFilesString = null;
        String fileName = null;
        String idrisiDataFile = null;
        String whiteboxHeaderFile = null;
        String whiteboxDataFile = null;
        WhiteboxRaster output = null;
        int i = 0;
        String[] imageFiles;
        int numImages = 0;
        double noData = -32768;
        InputStream inStream = null;
        OutputStream outStream = null;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        inputFilesString = args[0];

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputFilesString == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        imageFiles = inputFilesString.split(";");
        numImages = imageFiles.length;

        try {

            for (i = 0; i < numImages; i++) {
                int progress = (int)(100f * i / (numImages - 1));
                updateProgress("Loop " + (i + 1) + " of " + numImages + ":", (int) progress);
                fileName = imageFiles[i];
                // check to see if the file exists.
                if (!((new File(fileName)).exists())) {
                    showFeedback("IDRISI raster file does not exist.");
                    break;
                }
                
                //whiteboxHeaderFile = fileName.replace(".rdc", ".dep");
                //whiteboxDataFile = idrisiHeaderFile.replace(".rdc", ".tas");
//
//                // see if they exist, and if so, delete them.
//                (new File(whiteboxHeaderFile)).delete();
//                (new File(whiteboxDataFile)).delete();
//
//                boolean success = createHeaderFile(idrisiHeaderFile, whiteboxHeaderFile);
//                if (!success) {
//                    showFeedback("IDRISI header file was not read properly. "
//                            + "Tool failed to import");
//                    return;
//                }
//                
                Image image = ImageIO.read(new File(fileName));
                
                
                
                // copy the data file.
                File fromfile = new File(idrisiDataFile);
                File tofile = new File(whiteboxDataFile);
                inStream = new FileInputStream(fromfile);
                outStream = new FileOutputStream(tofile);
                byte[] buffer = new byte[1024];
                int length;
                //copy the file content in bytes 
                while ((length = inStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, length);
                }
                inStream.close();
                outStream.close();

                output = new WhiteboxRaster(whiteboxHeaderFile, "r");
                output.findMinAndMaxVals();
                output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
                output.addMetadataEntry("Created on " + new Date());
                output.writeHeaderFile();
                output.close();

                // returning a header file string displays the image.
                returnData(whiteboxHeaderFile);
            }


        } catch (IOException e) {
            showFeedback(e.getMessage());
        } catch (Exception e) {
            showFeedback(e.getMessage());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }

    private boolean createHeaderFile(String idrisiHeaderFile, String whiteboxHeaderFile) {
        int nrows = 0;
        int ncols = 0;
        double north = 0;
        double east = 0;
        double west = 0;
        double south = 0;
        double noData = 0;
        String dataType = "float";
        String dataScale = "continuous";
        DataInputStream in = null;
        BufferedReader br = null;
        String delimiter = ":";
        String byteOrder = java.nio.ByteOrder.nativeOrder().toString();
        
        String str1 = null;
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        
        try {
            // Open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(idrisiHeaderFile);
            // Get the object of DataInputStream
            in = new DataInputStream(fstream);

            br = new BufferedReader(new InputStreamReader(in));

            if (idrisiHeaderFile != null) {
                String line;
                String[] str;
                //Read File Line By Line
                while ((line = br.readLine()) != null) {
                    str = line.split(delimiter);
                    if (str.length <= 1) {
                        delimiter = " ";
                        str = line.split(delimiter);
                        if (str.length <= 1) {
                            delimiter = "\t";
                            str = line.split(delimiter);
                        }
                    }
                    if (str[0].toLowerCase().contains("data type")) {
                        if (str[str.length - 1].toLowerCase().contains("byte")) {
                            showFeedback("This tool does not support byte file types. "
                                    + "Please convert the file before importing. Only"
                                    + " 16-bit integer and 32-bit real formats are "
                                    + "supported.");
                            return false;
                        } else if (str[str.length - 1].toLowerCase().contains("int")) {
                            dataType = "integer";
                        } else if (str[str.length - 1].toLowerCase().contains("real")) {
                            dataType = "float";
                        } else if (str[str.length - 1].toLowerCase().contains("rbg")) {
                            dataType = "float";
                            dataScale = "rbg";
                        }
                    } else if (str[0].toLowerCase().contains("file type")) {
                        if (!str[str.length - 1].toLowerCase().contains("binary")) {
                            showFeedback("This tool only support binary file types. "
                                    + "Please convert the file before importing.");
                            return false;
                        }
                    } else if (str[0].toLowerCase().contains("columns")) {
                        ncols = Integer.parseInt(str[str.length - 1].trim());
                    } else if (str[0].toLowerCase().contains("rows")) {
                        nrows = Integer.parseInt(str[str.length - 1].trim());
                    } else if (str[0].toLowerCase().contains("min. x")) {
                        west = Double.parseDouble(str[str.length - 1].trim());
                    } else if (str[0].toLowerCase().contains("max. x")) {
                        east = Double.parseDouble(str[str.length - 1].trim());
                    } else if (str[0].toLowerCase().contains("min. y")) {
                        south = Double.parseDouble(str[str.length - 1].trim());
                    } else if (str[0].toLowerCase().contains("max. y")) {
                        north = Double.parseDouble(str[str.length - 1].trim());
                    }
                }
                //Close the input stream
                in.close();
                br.close();

                fw = new FileWriter(whiteboxHeaderFile, false);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw, true);

                str1 = "Min:\t" + Double.toString(Integer.MAX_VALUE);
                out.println(str1);
                str1 = "Max:\t" + Double.toString(Integer.MIN_VALUE);
                out.println(str1);
                str1 = "North:\t" + Double.toString(north);
                out.println(str1);
                str1 = "South:\t" + Double.toString(south);
                out.println(str1);
                str1 = "East:\t" + Double.toString(east);
                out.println(str1);
                str1 = "West:\t" + Double.toString(west);
                out.println(str1);
                str1 = "Cols:\t" + Integer.toString(ncols);
                out.println(str1);
                str1 = "Rows:\t" + Integer.toString(nrows);
                out.println(str1);
                str1 = "Data Type:\t" + dataType;
                out.println(str1);
                str1 = "Z Units:\t" + "not specified";
                out.println(str1);
                str1 = "XY Units:\t" + "not specified";
                out.println(str1);
                str1 = "Projection:\t" + "not specified";
                out.println(str1);
                str1 = "Data Scale:\t" + dataScale;
                out.println(str1);
                str1 = "Preferred Palette:\t" + "spectrum.pal";
                out.println(str1);
                str1 = "NoData:\t-9999";
                out.println(str1);
                if (byteOrder.toLowerCase().contains("lsb") || 
                        byteOrder.toLowerCase().contains("little")) {
                    str1 = "Byte Order:\t" + "LITTLE_ENDIAN";
                } else {
                    str1 = "Byte Order:\t" + "BIG_ENDIAN";
                }
                out.println(str1);

            }
            return true;
        } catch (java.io.IOException e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        } catch (Exception e) { //Catch exception if any
            System.err.println("Error: " + e.getMessage());
            return false;
        } finally {
            try {
                if (in != null || br != null) {
                    in.close();
                    br.close();
                }
            } catch (java.io.IOException ex) {
            }
            if (out != null || bw != null) {
                out.flush();
                out.close();
            }

        }

    }
}
