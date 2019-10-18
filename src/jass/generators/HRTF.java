
// class file for HRTF data


// organized in terms of azimuth (naz) elevation (nel) time (nt)
// right now as a 25x50x200 data structure, as organized in CIPIC

/*
  % two arrays: hrtf_l and hrtf
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
   class for using CIPIC-type HRTF files
*/

public class HRTF {
    
    boolean doSpatial = true;

    static final boolean DEBUG = false;
    // real
    double [][][] hrtf_l;
    double [][][] hrtf_r;

    // imaginary
    double [][][] hrtfI_l;
    double [][][] hrtfI_r;

    // working buffers
    double [] hrtfL;
    double [] hrtfR;
    double [] hrtfL_img;
    double [] hrtfR_img;

    // buffers for the separated right/left channels of original sample
    // imaginary ones needed for FFT
    double [] channelL;
    double [] channelL_img;
    double [] channelR;
    double [] channelR_img;

    int windowLength;   // FFT window length used

    // formula used to interplolate/choose
    // between HRTF points. 
    CIPIC_HRTF ucdpulse;
    FFT fft;
    
    float headRadius = 0.3f; // meters

    float gain = 1000;  // for some reason, these HRTF's make everything _very_ quiet.
    
    // conversion between rectangular/ polar
    RectPolar rp = new RectPolar(headRadius, windowLength);
    
    /* 
       takes the window size in samples
       and the filename of the HRTF data
    */
    public HRTF (int windowlength, String filename) {
	windowLength = windowlength;

	if (DEBUG) {
	    System.out.println("windowlength: " + windowLength);
	}
	
	// this is dependent on window size. 
	// figure out the formula later!
	fft = new FFT(8);

	// put HRTF data here
	hrtf_l =       new double[25][50][windowLength];
	hrtf_r =       new double[25][50][windowLength];

	hrtfI_l =      new double[25][50][windowLength];
	hrtfI_r =      new double[25][50][windowLength];

	// intermediate buffers for separated signal
	channelL =     new double[windowLength];
	channelR =     new double[windowLength];

	channelL_img = new double[windowLength];
	channelR_img = new double[windowLength];

	// intermediate buffers for HRTF
	hrtfR = new double[windowLength];
	hrtfR_img = new double[windowLength];

	hrtfL = new double[windowLength];
	hrtfL_img = new double[windowLength];

	ucdpulse = new CIPIC_HRTF();
       	
	// zero imaginary portion
	for(int i = 0; i < channelL_img.length; i++) {
	    channelL_img[0] = 0.0;
	    channelR_img[0] = 0.0;		    
	}	
	loadHRTFFromCIPIC(filename);
    }
    
    // sets a value of the HRTF. 
    // r true: right.  r false: left
    public void set(int naz, int nel, int nt, float val, boolean r) {
	if (r) {
	    hrtf_r[naz][nel][nt] = val;
	}
	else {
	    hrtf_l[naz][nel][nt] = val;
	}
    }

    // gets an array of the HRTF for specific azimuth index, elevation index
    public double[] getNL(int naz, int nel, boolean r) {
	if (r) {
	    return hrtf_r[naz][nel];
	} 
	else {
	    return hrtf_r[naz][nel];
	}
    }
    	    
    /* 
     * loads HRTF data from file 
     */
    public void loadHRTFFromCIPIC(String filename) {
	try {
	    File file = new File(filename);
	    fillHRTF(file);
	} catch(Exception e) { 
	    e.printStackTrace();
	}
    }

    public void fillHRTF(File file) {
	try {
	    Reader in = new FileReader(file);
	    BufferedReader bf = new BufferedReader(in);
	    String delimiter = " ";
	    String thisLine;
	    
	    for (int i = 0; i < 25; i ++) {
		for (int j = 0; j < 50; j++) {
		    thisLine = bf.readLine(); 
		    java.util.StringTokenizer st = 
			new java.util.StringTokenizer(thisLine, delimiter);
		    for (int k = 0; k < 256; k++ ) {
			hrtf_l[i][j][k] = 
			    Double.valueOf(st.nextToken()).doubleValue();
		    }
		    thisLine = bf.readLine(); 
		    st = new java.util.StringTokenizer(thisLine, delimiter);
		    for (int k = 0; k < 256; k++ ) {
			hrtfI_l[i][j][k] = 
			    Double.valueOf(st.nextToken()).doubleValue();
		    }

		    // right side 
		    thisLine = bf.readLine(); 
		    st = new java.util.StringTokenizer(thisLine, delimiter);
		    for (int k = 0; k < 256; k++ ) {
			hrtf_r[i][j][k] =
			    Double.valueOf(st.nextToken()).doubleValue();
		    }
		    // imaginary right
		    thisLine = bf.readLine(); 
		    st = new java.util.StringTokenizer(thisLine, delimiter);
		    for (int k = 0; k < 256; k++ ) {
			hrtfI_r[i][j][k] = 
			    Double.valueOf(st.nextToken()).doubleValue();
		    }
		}
	    }
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void startSpatial() {
	doSpatial = true;
    }

    public void stopSpatial() {
	doSpatial = false;
    }

    /**
     * spatialize a mono audio buffer
     * at this point, the HRTF has already been prepared     
     @param buf mono audio buffer to process. 
     @param outL left channel output 
     @param outR right channel output
    */    
    public void process(float [] buf, float [] outL, float [] outR) 
    {
	int numWindows = buf.length / windowLength;
	
	// convert to input buf to double and
	// separate them into left/right channels	    
	int index = 0;  //keeps track of window-index.
	for (int i = 0; i < numWindows; i++) {

	    // copy source into left and right channels.
	    separate(buf, index, index+windowLength, channelL, channelR);

	    if (doSpatial) {
		
		// zero imaginary part
		for (int k = 0; k< channelL_img.length; k++) {
		    channelL_img[k] = 0;
		}

		// fft input signal
		fft.doFFT(channelL, channelL_img, false);
		fft.doFFT(channelR, channelR_img, false);
		if (DEBUG) {
		    if (i == 0) {
			System.out.println("orig");
			for (int k = 0; k < windowLength; k++) {
			    System.out.println(channelL[k] + " " + channelL_img[k]);
			}
		    }
		    if (i == 0) {
			System.out.println("hrtf_______________");
			for (int k = 0; k < windowLength; k++) {
			    System.out.println(hrtfL[k] + " " + hrtfL_img[k]);
			}
		    }
		}
		
		// multiply signals together
		rp.multiplyRect(hrtfL, hrtfL_img, channelL, channelL_img);
		rp.multiplyRect(hrtfR, hrtfR_img, channelR, channelR_img);

		if (DEBUG) {
		    if (i == 0) {
			System.out.println("after");
			for (int k = 0; k < windowLength; k++) {
			    System.out.println(channelL[k] + " "+ channelL_img[k]);
			}
		    }
		}
		
		// inverse fft
		fft.doFFT(channelL, channelL_img, true);
		fft.doFFT(channelR, channelR_img, true);
		// copy window to output array 
		for (int j = 0; j < windowLength; j++ ) {
		    outL[index + j] = gain*(float)channelL[j];
		    outR[index + j] = gain*(float)channelR[j];
		}
	    }
	    else {
		// copy window to output array 
		for (int j = 0; j < windowLength; j++ ) {
		    outL[index + j] = (float)channelL[j];
		    outR[index + j] = (float)channelR[j];
		}
	    }
	    index +=  windowLength;
	}
    }
    
    /**
       right now, this just copies 
    */
    public void separate(float [] inputBuf, int start, int end,
			 double [] outLeft, 
			 double [] outRight) {
	int j = 0;
	for (int i = start; i < end; i++) {
	    outLeft[j] = inputBuf[i];
	    outRight[j] = inputBuf[i];
	    j++;
	}
    }

    public void setLocation(PositionData p) { 	
	float azi = p.getAzimuth();
	float elev = p.getElevation();
	// System.out.println("setlocation: orig " + azi + " " + elev); 

	int [] init = ucdpulse.getNearest(azi, elev);
	int az = init[0];
	int el = init[1];
	/*
	  System.out.println("HRTF.setlocation init[0]: " + init[0] + " init[1] "+ init[1]); 	
	  System.out.println("HRTF.setlocation fftdHrtf.length: " + fftdHrtfL.length); 
	  System.out.println("HRTF.setlocation init.length: " + init.length); 
	*/
	
	hrtfL = hrtf_l[az][el];
	hrtfR = hrtf_r[az][el];
	hrtfL_img = hrtfI_l[az][el];
	hrtfR_img = hrtfI_r[az][el];
    }   
}
