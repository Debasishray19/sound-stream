package jass.generators;
import jass.engine.*;
import java.util.*;

/** Mixer UG. Also allows conversion to stereo.
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class Mixer extends InOut {
    // gains of sources
    protected float[] gains;
    // pans of sources
    protected float[] pans;
    protected int nChannels = 1;
    
    protected float[] tmp_buf; // scratchpad
    
    /** Set input gain control vector.
        @param k index of gain
        @param gains input gain
     */
    public void setGain(int k, float g) {
        if(k<0 || k >= gains.length) {
            return;
        } else {
            gains[k] = g;
        }
    }

    /** Set input pan 
        @param k index of pan
        @param pan pan
     */
    public void setPan(int k, float pan) {
        if(k<0 || k >= pans.length) {
            return;
        } else {
            this.pans[k] = pan;
        }
    }

    /** Set nchannels
        @param n nchannels
     */
    public void setNChannels(int n) {
        nChannels = n;
    }

    /** Get nchannels
        @return n nchannels
     */
    public int getNChannels() {
        return this.nChannels;
    }

    /** Get input gain control vector.
        @param gains input gains
     */
    public float[] getGains() {
        return gains;
    }

    /** Get input pan control vector.
        @param gains input gains
     */
    public float[] getPans() {
        return pans;
    }

    /** Clear gains ato zero and pans to middle */
    public void clear() {
        for(int i=0;i<gains.length;i++) {
            gains[i] = 0;
            pans[i] = 0.5f;
        }
    }
    
    /** Create mono mixer
        @param bufferSize Buffer size used for real-time rendering.
        @param n no inputs
    */
    public Mixer(int bufferSize,int n) {
        super(bufferSize);
        gains = new float[n];
        pans = new float[n];
        nChannels = 1;
        clear();
        tmp_buf = new float[bufferSize];
    }

    /** Create stereo mixer
        @param bufferSize Buffer size used for real-time rendering. For stereo must be 2X input buffersize
        @param n no inputs
        @parm nChannels number of channels (1 or 2)
    */
    public Mixer(int bufferSize,int n,int nChannels) {
        super(bufferSize);
        gains = new float[n];
        this.nChannels = nChannels;
        pans = new float[n];
        clear();
        tmp_buf = new float[bufferSize];
    }

    /** Create. For superclasses
        @param bufferSize Buffer size used for real-time rendering.
    */
    public Mixer(int bufferSize) {
        super(bufferSize);
    }

    /** Compute the next buffer and store in member float[] buf.
        Note if stereo then output buf[] is twice as big as input buffers
     */
    protected void computeBuffer() {
        int bufsz = getBufferSize();
        int nsrc = sourceContainer.size();
        if(nsrc > gains.length) {
            nsrc = gains.length;
            System.out.println("Warning: Mixer has more sources than allowed");
        }
        for(int k=0;k<bufsz;k++) {
            // can't overwrite buf[] yet as one of the srcBuffers may be pointing to it!
            tmp_buf[k] = 0; 
        }
        for(int i=0;i<nsrc;i++) {
            float[] tmpsrc = srcBuffers[i];
            //System.out.println("i= "+i+ "src[] = " + srcBuffers[i][5]);
            float g = gains[i];
            if(nChannels == 1) {
                for(int k=0;k<bufsz;k++) {
                    tmp_buf[k] += g*tmpsrc[k];
                }
            } else if(nChannels == 2) {
                int inbufsz = bufsz/2;
                int iout = 0;
                float p = pans[i];
                for(int k=0;k<inbufsz;k++) {
                    tmp_buf[iout++] += g*(1-p)*tmpsrc[k];
                    tmp_buf[iout++] += g*p*tmpsrc[k];
                }
            }
        }
        for(int k=0;k<bufsz;k++) {
            buf[k] = tmp_buf[k];
        }
    }
}
