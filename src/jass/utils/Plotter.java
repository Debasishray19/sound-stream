/*
*   Class Plotter
*
*   A free standing plotting application that takes data from
*   an input file and plots a graph in a window
*
*   WRITTEN BY: Dr Michael Thomas Flanagan
*
*   DATE:    July 2002
*   UPDATE:  22 June 2003 and 14 August 2004
*
*   DOCUMENTATION:
*   See Michael Thomas Flanagan's Java library on-line web page:
*   Plotter.html
*
*   Copyright (c) June 2003, August 2004
*
*   PERMISSION TO COPY:
*   Permission to use, copy and modify this software and its documentation for
*   NON-COMMERCIAL purposes is granted, without fee, provided that an acknowledgement
*   to the author, Michael Thomas Flanagan at www.ee.ucl.ac.uk/~mflanaga, appears in all copies.
*
*   Dr Michael Thomas Flanagan makes no representations about the suitability
*   or fitness of the software for any or for a particular purpose.
*   Michael Thomas Flanagan shall not be liable for any damages suffered
*   as a result of using, modifying or distributing this software or its derivatives.
*
***************************************************************************************/

package jass.utils;

import java.awt.*;
import javax.swing.*;
import javax.swing.JOptionPane;

public class Plotter{

    	// main method
    	public static void main(String[] argv){

        	int nCurves = 0;        // number of curves
        	int[] nPoints = null;   // number of points per curve
        	int nMax = 0;           // maximum no of points on a curves
        	int ii = 0;             // counter

        	double[][] data = null;	// data points

        	String title =" ";      // plot title
        	String xLeg=" ";        // x axis legend
        	String xUnits=" ";      // x axis units
        	String yLeg=" ";        // y axis legend
        	String yUnits=" ";      // y axis units
        	String fileName=" "; 	// name of file containing input data

        	fileName = JOptionPane.showInputDialog(null, "Enter input file name, including any extension, e.g. .txt,\nand the full address if the input data file & Plotter are\nin different folders, e.g. C:/folder1/folder2/inp.txt", "Input File for Program Plotter",  JOptionPane.QUESTION_MESSAGE);

        	// Read data
        	FileInput fin = new FileInput(fileName);

        	title = fin.readLine();
        	xLeg = fin.readLine();
        	xUnits = fin.readLine();
        	yLeg = fin.readLine();
        	yUnits = fin.readLine();

        	nCurves = fin.readInt();
        	nMax = fin.readInt();

        	data = PlotGraph.data(nCurves,nMax);
        	nPoints = new int[nCurves];
        	ii=0;
        	for(int i=0; i<nCurves; i++){
            		nPoints[i]=fin.readInt();
            		for(int j=0; j<nPoints[i]; j++){
                		data[ii][j]=fin.readDouble();
                		data[ii+1][j]=fin.readDouble();
            		}
            		ii+=2;
        	}

        	// Create a graph object
        	PlotGraph pg = new PlotGraph(data);


        	pg.setGraphTitle(title);
        	pg.setXaxisLegend(xLeg);
        	pg.setXaxisUnitsName(xUnits);
        	pg.setYaxisLegend(yLeg);
        	pg.setYaxisUnitsName(yUnits);

        	// Call plotting method
        	pg.plot();
    	}
}




