package jass.generators;
import jass.engine.*;
import java.util.*;
import java.io.File;

/** Spatial Mixer UG. 
    @author Reynald Hoskinson (reynald@cs.ubc.ca)
*/
public class SpatialMixer extends InOut {

    static final boolean DEBUG = true;
    static final int windowLength = 256;
    protected float[] tmp_bufL; // scratchpad
    protected float[] tmp_bufR; // scratchpad

    protected float[] tmp_out; // intermediate output
   
    protected HRTF hrtf;
    
    protected PositionData []  positions;

    /** set filename to obtain hrir info for 1 subject
        @param filename filename of subject's hrir data
       */

    public void startHRTF() {
	hrtf.startSpatial();
    }

    public void stopHRTF() {
	hrtf.stopSpatial();
    }

    public void changeHRTF(File filename) {
	hrtf.fillHRTF(filename);
    }

    /** set position of source
        @param inputNo buffer number to set position of
        @param p Position data gives the 3D coordinate of the sound source
    */
    public void setPosition(int inputNo, PositionData p) {
	if (inputNo > positions.length) {
	  System.out.println("SpatialMixer warning: inputNo exceeds number of sources");
	}
	else {
	    positions[inputNo] = p;
	}
    }	

    /** Create stereo Spatialized mixer
        @param bufferSize Buffer size used for real-time rendering. 
	For stereo must be 2X input buffersize
        @param n no inputs
        @param hrtf_data hrtf file. 
    */
    public SpatialMixer(int bufferSize,int n, String hrtf_data) {
        super(bufferSize);
	hrtf = new HRTF(windowLength, hrtf_data);

	// one channel each
	int bufsz = bufferSize/2;
        tmp_bufL = new float[bufsz];
        tmp_bufR = new float[bufsz];
        tmp_out  = new float[bufferSize];
	positions = new PositionData[n];
    }

    /** Create. For superclasses
        @param bufferSize Buffer size used for real-time rendering.
    */
    public SpatialMixer(int bufferSize) {
        super(bufferSize);
    }

    /** 
	Compute the next buffer and store in member float[] buf.
        Note if stereo then output buf[] is twice as big as input buffers
	--> this means that each source is a mono stream. 
	will sourceContainer.size() always == n in Mixer constructor? 
     */
    protected void computeBuffer() {
        int bufsz = getBufferSize();
	// will this return the right thing?  ie. 1 channel worth
	int nsrc = sourceContainer.size();
	int inbufsz = bufsz/2;
	/*
	if (DEBUG) {
	    System.out.println("bufsz: " + bufsz);
	    System.out.println("nsrc: " + nsrc);
	    System.out.println("inbufsz: " + inbufsz);
	}
	*/
        for(int k=0;k<bufsz;k++) {
            //can't overwrite buf[] yet as one of the srcBuffers may be pointing to it!
            tmp_out[k] = 0; 
	}

        for(int i=0;i<nsrc;i++) {
            float[] tmpsrc = srcBuffers[i];
	    // System.out.println("tmpsrc.length: " + tmpsrc.length);
	    // tmpsrc.length 256
	    // inbufsz: 256
	    // nsrc 1
	    // bufsz 512
	    // System.out.println("tmpsrc.length: " + tmpsrc.length);

	    // for each source, do spatailize. 	    
	    /*
	      q1. I guess this is where they are mixed too?
	      q2. are buffer overruns handled elsewhere? 
	      q3. should I keep srcBuffers unchanged? 
	    */
	    hrtf.setLocation(positions[i]);
	    hrtf.process(tmpsrc, tmp_bufL, tmp_bufR);
	    
	    // add this source to the output buffers. 
	    // interleaved
	    int iout = 0;
	    for(int k=0;k<inbufsz;k++) {
		tmp_out[iout++] += tmp_bufL[k];
		tmp_out[iout++] += tmp_bufR[k];
	    }
	}
	// now move to real output buf. 
	for(int k=0;k<bufsz;k++) {
            buf[k] = tmp_out[k];
        }
    }
}
