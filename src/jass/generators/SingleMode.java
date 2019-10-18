package jass.generators;
import jass.engine.*;
import java.io.*;

/** Vibration model of object with 1 mode
    Changes in freq damping and gains are linearly changed over one buffer rather than abruptly
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class SingleMode extends InOut {
    /** Sampling rate in Hertz. */
    public float srate;

    /** xi factor for rising bubbles as explained in paper */
    protected float xi = 0.1f;

    /*
      f = f_base(1 + s *t)
      xi = 0.1
      s = xi * d
      s = (srate/bufferSize)/riseFactor
      riseFactor = (srate/bufferSize)/(xi*d);
     */
    private boolean isRising = true;
    private int  k_rise = 0;
    private float rise_factor;// (float)(10000/bufferSize);

    /** Cutoff excitation pulse strength above which a bubble is considered rising */
    protected float cutoffRiseExcitation = 1.f;
    
    /** The transfer function of a reson filter is H(z) = 1/(1-twoRCosTheta/z + R2/z*z). */
    
    protected float twoRCosTheta;
    protected float R2;
    
    /** Reson filter gain. */
    protected float ampR; 
    
    /** Cached values. */
    protected float c_i;
    
    /** modal parameters */
    protected float f=100,f_rise = 100, f_base = 100,d=1,a=1;

    /** Temp storage */
    protected float[] tmpBuf = null;
    
    /** State of filter */
    protected float  yt_1, yt_2;
    
    // 0.001 works to prevent strange underflow (??) bug
    static final float eps = 0.001f;

    /** Constructor for derived classes to call super
        @param bufferSize Buffer size used for real-time rendering.
     */
    public SingleMode(int bufferSize) {
        super(bufferSize);
    }
    
    /** Create and initialize, but don't set any modal parameters.
        @param srate sampling rate in Hertz.
        @param bufferSize Buffer size used for real-time rendering.
    */
    public SingleMode(float srate,int bufferSize) {
        super(bufferSize);
        this.srate = srate;
        tmpBuf = new float[bufferSize];
    }

    /** Create and initialize with provided modal data.
        @param f freq.
        @param d damping
        @param a gain
        @param srate sampling rate in Hertz.
        @param bufferSize Buffer size used for real-time rendering.
    */
    public SingleMode(float f,float d,float a,float srate,int bufferSize) {
        super(bufferSize);
        this.srate = srate;
        this.f = f;
        this.f_base = f;
        this.d=d;
        this.a=a;
        computeResonCoeff();
        computeRiseFactor();
        tmpBuf = new float[bufferSize];
    }

    private void computeRiseFactor() {
        rise_factor = (float)((srate/bufferSize)/(xi*d));
    }
    
    /** Set xi value for rising bubbles
        @param x xi value (0.1 is normal)
     */
    public void setXi(float x) {
        this.xi = x;
        computeRiseFactor();
    }
    
    /** Set xi value for rising bubbles
        @param x excitation value above which bubble is considered rising
     */
    public void setRiseCutoffExcitation(float x) {
        this.cutoffRiseExcitation = x;
    }

    /** Add a Source. Overrides Sink interface implementation from
        InOut. Allow only one Source. 
        @param s Source to add.
    */    
    public Object addSource(Source s) throws SinkIsFullException {
        if(sourceContainer.size() > 0) {
            throw new SinkIsFullException();
        } else {
            sourceContainer.addElement(s);
        }
        return null;
    }
    
    /** Compute the reson coefficients from the modal model parameters.
        Cache values used in {@link #setLocation}.
    */
    public void computeResonCoeff() {
        float tmp_r = (float)(Math.exp(-this.d/srate));
        R2 = tmp_r*tmp_r;
        twoRCosTheta = (float)(2*Math.cos(2*Math.PI*this.f/srate)*tmp_r);
        c_i = (float)(Math.sin(2*Math.PI*this.f/srate)*tmp_r);
        ampR = c_i * this.a;
    }

    /** Compute the next buffer and store in member float[] buf.
     */
    protected void computeBuffer() {
        computeModalFilterBank(this.buf, srcBuffers[0], getBufferSize());
    }

    /** Set damping
        @param d damping scale. 
    */
    public void setDamping(float d) {
        this.d = d;
        computeRiseFactor();
        computeResonCoeff();
    }
    /** Set freq
        @param f freq.
    */
    public void setFreq(float f) {
        this.f = f;
        this.f_base = f;
        computeResonCoeff();
    }
    
    /** Set gain
        @param a gain
    */
    public void setGain(float a) {
        this.a = a;
        computeResonCoeff();
    }

    
    /** Apply external force[] and compute response through bank of modal filters.
        Interpolate filter parameters over the buffer. c[k]=k*(c_new-c_old)/nsamples + c_old
        @param output user provided output buffer.
        @param force input force.
        @param nsamples number of samples to compute.
    */
    protected void computeModalFilterBank(float[] output, float[] force, int nsamples) {
        // filter parameters are:
        // twoRCosTheta[nf]; R2[nf]; ampR[nf]
        boolean isnul = true;
        float impulse = 0;
        for(int k=0;k<nsamples;k++) {
            output[k] = 0;
            if((impulse=Math.abs(force[k]))>=eps) {
                isnul = false;
                k_rise = 0;
                this.f = this.f_base;
                computeResonCoeff();
                if(impulse>cutoffRiseExcitation) {
                    isRising = true;
                } else {
                    isRising = false;

                }
            }
        }
        if(isRising) {
            this.f = (float)(f_base*(1.+(++k_rise)/rise_factor));
            computeResonCoeff();
        }
        if(isnul) {
            if(Math.abs(yt_1) >= eps || Math.abs(yt_2) >= eps) {
                isnul = false;
            }
        }
        if(isnul) {
            return;
        }
        float tmp_yt_1 = yt_1;
        float tmp_yt_2 = yt_2;
        float tmp_twoRCosTheta = twoRCosTheta;
        float tmp_R2 = R2;
        float tmp_ampR = ampR;
        for(int k=0;k<nsamples;k++) {
            // optimize by taking */ out ofthe loop
            float ynew = tmp_twoRCosTheta*tmp_yt_1 - tmp_R2 * tmp_yt_2 + tmp_ampR * force[k];
            tmp_yt_2 = tmp_yt_1;
            tmp_yt_1 = ynew;
            output[k] += ynew;
        }
        yt_1 = tmp_yt_1;
        yt_2 = tmp_yt_2;
    }

}
