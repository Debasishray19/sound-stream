package jass.generators;
import jass.engine.*;

/**
   Output random pulses  uniform in range [-1 +1]. Probability per sample
   is pps. Amplitude is rnd^exponent.
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public class RandPulses extends Out {
    /** Probability per sample of pulse */
    protected float pps = 0;
    protected double exponent = 1;
    protected boolean steadyRate = false;
    protected int pulseInterval;
    protected int interval_i=0;
    
    public RandPulses(int bufferSize) {
        super(bufferSize);
    }

    /** Set impulse probability per sample. This will create pulse intervals
        which are exponentially distribted with mean interpulse time
        T = -1/srate ln(1-pps) ~ 1/srate*pps for small pps. So rate is
        srate * pps (-srate * ln(1-pps) precisely).
        Every 1/pps sample will have a pulse on average.
        @param pps probability per sample, or F_pulse/srate
    */
    public void setProbabilityPerSample(float pps) {
        pulseInterval = (int)(1/pps);
        this.pps = pps;
    }

    /** Set impulse prob. exponent. Volume of impact is r^exponent, with
        r uniform on [0 1]
        @param exponent exponent of prob. distribution
    */
    public void setProbabilityDistributionExponent(float exponent) {
        this.exponent = (double)exponent;
    }
    
    protected void computeBuffer() {
        int bufsz = getBufferSize();
        for(int i=0;i<bufsz;i++) {
            if(steadyRate) {
                interval_i++;
                if(interval_i>pulseInterval) {
                    interval_i = 0;
                    double vol = 2*Math.random()-1;
                    int sign;
                    if(vol<0) {
                        sign = -1;
                        vol = -vol;
                    } else {
                        sign = 1;
                    }
                    vol = Math.pow(vol,exponent);
                    buf[i] = (float) (vol * sign);
                } else {
                    buf[i] = 0;
                }
            } else {
                double x = Math.random();
                if(x < (double)pps) {
                    double vol = 2*Math.random()-1;
                    int sign;
                    if(vol<0) {
                        sign = -1;
                        vol = -vol;
                    } else {
                        sign = 1;
                    }
                    vol = Math.pow(vol,exponent);
                    buf[i] = (float) (vol * sign);
                } else {
                    buf[i] = 0;
                }
            }
        }
    }
    
}

