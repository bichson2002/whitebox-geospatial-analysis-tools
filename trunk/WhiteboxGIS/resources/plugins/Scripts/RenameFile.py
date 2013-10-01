import os
from shutil import copyfile
from threading import Thread
from whitebox.ui.plugin_dialog import ScriptDialog
from java.awt.event import ActionListener

# The following four variables are required for this 
# script to be integrated into the tool tree panel. 
# Comment them out if you want to remove the script.
name = "RenameFile"
descriptiveName = "Rename File"
description = "Renames a raster or vector file"
toolboxes = ["FileUtilities"]
	
class RenameFile(ActionListener):
	def __init__(self, args):
		if len(args) != 0:
			t = Thread(target=lambda: self.execute(args))
			t.start()
		else:
			# Create a dialog for this tool to collect user-specified
			# tool parameters.
			self.sd = ScriptDialog(pluginHost, descriptiveName, self)	
		
			# Specifying the help file will display the html help
			# file in the help pane. This file should be be located 
			# in the help directory and have the same name as the 
			# class, with an html extension.
			helpFile = self.__class__.__name__
			self.sd.setHelpFile(helpFile)
		
			# Specifying the source file allows the 'view code' 
			# button on the tool dialog to be displayed.
			self.sd.setSourceFile(os.path.abspath(__file__))

			# add some components to the dialog
			self.sd.addDialogFile("Input file", "Input File:", "open", "Whitebox Files (*.shp; *.dep), DEP, SHP", True, False)
			self.sd.addDialogFile("Output file", "Output File:", "close", "Whitebox Files (*.shp; *.dep), DEP, SHP", True, False)

			# resize the dialog to the standard size and display it
			self.sd.setSize(800, 400)
			self.sd.visible = True
		
	def execute(self, args):
		try:
			inputfile = args[0]
			outputfile = args[1]

			# is the input file a WhiteboxRaster or a shapefile?
			if inputfile.endswith(".dep"):
				israster = True
			elif inputfile.endswith(".shp"):
				israster = False
			else:
				pluginHost.showFeedback("The input file must be a Whitebox raster or shapefile.")
				return
			
			if israster:
				# make sure that the output file is of the same 
				# file type
				if not outputfile.endswith(".dep"):
					fileName, fileExtension = os.path.splitext(outputfile)
					outputfile = fileName + ".dep"

				# rename the header file
				os.rename(inputfile, outputfile)

				# rename the data file
				oldfile = inputfile.replace(".dep", ".tas")
				newfile = outputfile.replace(".dep", ".tas")
				os.rename(oldfile, newfile)

				oldfile = inputfile.replace(".dep", ".wstat")
				newfile = outputfile.replace(".dep", ".wstat")
				if os.path.exists(oldfile):
					os.rename(oldfile, newfile)
				
			else:
				# make sure that the output file is of the same 
				# file type
				if not outputfile.endswith(".shp"):
					fileName, fileExtension = os.path.splitext(outputfile)
					outputfile = fileName + ".shp"
				
				# .shp file 
				os.rename(inputfile, outputfile)

				# .dbf file
				oldfile = inputfile.replace(".shp", ".dbf")
				newfile = outputfile.replace(".shp", ".dbf")
				if os.path.exists(oldfile):
					os.rename(oldfile, newfile)
					
				# .shx file
				oldfile = inputfile.replace(".shp", ".shx")
				newfile = outputfile.replace(".shp", ".shx")
				if os.path.exists(oldfile):
					os.rename(oldfile, newfile)

				# .prj file
				oldfile = inputfile.replace(".shp", ".prj")
				newfile = outputfile.replace(".shp", ".prj")
				if os.path.exists(oldfile):
					os.rename(oldfile, newfile)

				# .sbn file
				oldfile = inputfile.replace(".shp", ".sbn")
				newfile = outputfile.replace(".shp", ".sbn")
				if os.path.exists(oldfile):
					os.rename(oldfile, newfile)

				# .sbx file
				oldfile = inputfile.replace(".shp", ".sbx")
				newfile = outputfile.replace(".shp", ".sbx")
				if os.path.exists(oldfile):
					os.rename(oldfile, newfile)
					
			pluginHost.showFeedback("Operation complete")
			
		except:
			pluginHost.showFeedback("File not written properly.")
			return

	def actionPerformed(self, event):
		if event.getActionCommand() == "ok":
			args = self.sd.collectParameters()
			self.sd.dispose()
			t = Thread(target=lambda: self.execute(args))
			t.start()

if args is None:
	pluginHost.showFeedback("The arguments array has not been set.")
else:
	RenameFile(args)
