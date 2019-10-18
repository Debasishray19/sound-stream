package jass.generators;
import jass.engine.*;

/**
   Output glottal wave. See Rubin et al JASA vol 70 no 2 1981 p323
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public class GlottalWave extends Out {
    /** Sampling rate in Hertz of Out. */
    public float srate;

    /** Amplitude or volume*/
    protected float volume = 1;

    /** Current phase */
    protected float phase = 0;
    protected boolean odd = true; // flip to alternate sign

    /** Freq. in Hertz */
    protected float freq = 440;

    protected float openQuotient = .5f;

    protected float speedQuotient = 4f;

    private float T, Tp,Tn;
    
    public GlottalWave(float srate,int bufferSize) {
        super(bufferSize);
        this.srate = srate;
        computePars();
    }

    /** Set amplitude
        @param val Volume.
    */
    public void setVolume(float val) {
        volume = val;
    }

    public float getVolume() {
        return volume;
    }

    /** Set frequency
        @param f frequency.
    */
    public void setFrequency(float f) {
        freq = f;
        computePars();
    }

    public float getFrequency() {
        return freq;
    }

    /** Set Speed Quotient
        @param sq Speed Quotient
    */
    public void setSpeedQuotient(float sq) {
        speedQuotient = sq;
        computePars();
    }

    public float getSpeedQuotient() {
        return speedQuotient;
    }
    
    /** Set Open Quotient
        @param oq Open Quotient
    */
    public void setOpenQuotient(float oq) {
        openQuotient = oq;
        computePars();
    }
    
    public float getOpenQuotient() {
        return openQuotient;
    }

    private void computePars() {
        T = 1/freq;
        Tn = T*openQuotient/(1+speedQuotient);
        Tp = speedQuotient * Tn;
    }
    
    protected void computeBuffer() {
        int bufsz = getBufferSize();
        float y;
        for(int i=0;i<bufsz;i++) {
            if(phase > (Tn+Tp)) {
                y=0;
            } else if(phase<Tp) {
                float tmp = phase/Tp;
                y = (3-2*tmp)*tmp*tmp;
            } else {
                float tmp = (phase-Tp)/T;
                y = 1-tmp*tmp;
            }
            phase += 1/srate;
            if(phase > T) {
                phase -= T;
                odd = ! odd;
            }
            if(odd) {
                buf[i] = (float) (volume * y);
            } else {
                buf[i] = (float) (volume * y);
            }
        }
    }
    
}

