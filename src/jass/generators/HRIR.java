
// class file for HRIR data


// organized in terms of azimuth (naz) elevation (nel) time (nt)
// right now as a 25x50x200 data structure, as organized in CIPIC

/*
  % two arrays: hrir_l and hrir
  % indices naz, nel, nt  (azimuth, elevation, time)
  % (25x50x200) 3 dimensional array 
  % in matlab,  azimuth angle corresponding to nax  is the naz'th element
  % of the vector 
  % azimuths = [-80 -65 -55 -45:5:45 55 65 80]
  % elevations range from -45 to +230.625 in steps of 5.625
  % this angular  increment divides the full circle  into 64 equal parts,
  % but only 50 values are used
  %  elevation angle corresponding  to nel  is the  nel-th element  of the
  % vector elevations = -45 + 5.625*(0:49)
  % temporal
      @author Reynald Hoskinson (reynald@cs.ubc.ca)
*/

package jass.generators;

import java.io.*;

/**
   class for using CIPIC-type HRIR files
*/

public class HRIR {

    static final boolean DEBUG = true;
    // stores original hrir data
    float [][][] hrir_l;
    float [][][] hrir_r;
    
    // buffer to read samples from file/stream to process
    byte [] samples;

    // buffers for the separated right/left channels of original sample
    // imaginary ones needed for FFT
    double [] channelL;
    double [] channelL_img;
    double [] channelR;
    double [] channelR_img;
    
    // separate buffer for the FFT'd version of the hrir at a particular
    // location
    double [] fftdHrirL;
    double [] fftdHrirR;

    int windowLength;   // FFT window length used

    // formula used to interplolate/choose
    // between HRIR points. 
    CIPIC_HRTF ucdpulse;
    RectPolar rp;
    FFT fft;
    
    float headRadius = 0.3f; // meters

    /* 
       takes the window size in samples
       and the filename of the HRTF data
    */
    public HRIR (int windowlength, String filename) {
	windowLength = windowlength;

	if (DEBUG) {
	    System.out.println("windowlength: " + windowLength);
	}
	
	// this is dependent on window size. 
	// figure out the formula later!
	fft = new FFT(8);

	hrir_l =       new float[25][50][200];
	hrir_r =       new float[25][50][200];
	channelL =     new double[windowLength];
	channelR =     new double[windowLength];
	channelL_img = new double[windowLength];
	channelR_img = new double[windowLength];

	rp            = new RectPolar(headRadius, windowLength);

	fftdHrirL    = new double[windowLength];
	fftdHrirR    = new double[windowLength];

	ucdpulse = new CIPIC_HRTF();
       	
	// zero imaginary portion
	for(int i = 0; i < channelL_img.length; i++) {
	    channelL_img[0] = 0.0;
	    channelR_img[0] = 0.0;		    
	}	
	loadHRIRFromCIPIC(filename);
    }
    
    // sets a value of the HRIR. 
    // r true: right.  r false: left
    public void set(int naz, int nel, int nt, float val, boolean r) {
	if (r) {
	    hrir_r[naz][nel][nt] = val;
	}
	else {
	    hrir_l[naz][nel][nt] = val;
	}
    }

    // gets an array of the HRIR for specific azimuth index, elevation index
    public float[] getNL(int naz, int nel, boolean r) {
	if (r) {
	    return hrir_r[naz][nel];
	} 
	else {
	    return hrir_r[naz][nel];
	}
    }
    
    /* 
     * save HRIR data to file 
     */
    public void saveToHRTF(String filename) {
	int init = 200;  // original CIPIC
	fftdHrirL    = new double[windowLength];
	fftdHrirR    = new double[windowLength];
	String thisLine;
	try {
	    File file = new File(filename);
	    file.createNewFile();
	    Writer out = new FileWriter(file);
	    PrintWriter pw = new PrintWriter(out);
 	    double theta;
	    for (int az = 0; az < 25; az ++) {
		for (int el = 0; el < 50; el++) {
		   
		    for (int i =0; i < fftdHrirL.length; i++) {
			if (i < init) {
			    fftdHrirL[i] = hrir_l[az][el][i];
			    fftdHrirR[i] = hrir_r[az][el][i];
			    channelL_img[i] = 0.0;
			    channelR_img[i] = 0.0;
			}
			else {
			    fftdHrirR[i] = 0.0;		
			    fftdHrirL[i] = 0.0;
			    channelL_img[i] = 0.0;
			    channelR_img[i] = 0.0;
			}
		    }

		    // fft hrir signal
		    fft.doFFT(fftdHrirL, channelL_img, false);
		    fft.doFFT(fftdHrirR, channelR_img, false);
		    
		    rp.constructHRTF(fftdHrirL, channelL_img);
		    rp.constructHRTF(fftdHrirR, channelR_img);

		    // write real part to file
		    for (int i =0; i < fftdHrirL.length; i++) {
			pw.print(fftdHrirL[i] + " ");			
		    }
		    pw.println();
		    // imaginary
		    for (int i =0; i < fftdHrirL.length; i++) {
			pw.print(channelL_img[i] + " ");	
		    }
		    pw.println();

		    // right side 
		    for (int i =0; i < fftdHrirR.length; i++) {
			pw.print(fftdHrirR[i] + " ");			
		    }
		    pw.println();
		    for (int i =0; i < channelR_img.length; i++) {
			pw.print(channelR_img[i] + " ");	
		    }
		    pw.println();
		}
	    }
	    pw.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
	    
    /* 
     * loads HRIR data from file 
     */
    public void loadHRIRFromCIPIC(String filename) {
	try {
	    String thisLine;
	    File file = new File(filename);
	    Reader in = new FileReader(filename);
	    BufferedReader bf = new BufferedReader(in);
	    String delimiter = " ";
	    
	    for (int i = 0; i < 25; i ++) {
		for (int j = 0; j < 50; j++) {
		    thisLine = bf.readLine(); 
		    java.util.StringTokenizer st = 
			new java.util.StringTokenizer(thisLine, delimiter);
		    for (int k = 0; k < 200; k++ ) {
			hrir_l[i][j][k] = Float.valueOf(st.nextToken()).floatValue();
		    }
		}
	    }
	    if (DEBUG) {
		System.out.println("finished hrir_l");
	    }
	    // ignore blank line in middle 
	    bf.readLine();
	    for (int i = 0; i < 25; i ++) {
		for (int j = 0; j < 50; j++) {
		    thisLine = bf.readLine(); 
		    java.util.StringTokenizer st = 
			new java.util.StringTokenizer(thisLine, delimiter);
		    for (int k = 0; k < 200; k++ ) {
			hrir_r[i][j][k] = Float.valueOf(st.nextToken()).floatValue();
		    }
		}
	    }
	    if (DEBUG) {
		System.out.println("finished hrir_r ");
	    }
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
 
    public static void main(String args[]) {

	if (args.length < 2) {
	   System.out.println ("usage: java HRIR <original HRIR file> <outputfile>");
	}
	else {HRIR hrir = new HRIR(256, args[0]);
	    hrir.saveToHRTF(args[1]);
	}
    }
}
