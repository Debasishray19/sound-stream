package jass.generators;
import jass.engine.*;
import java.util.*;

/**
   Output random pulses. Interpulse time T is stochastic. Gaussian with mean meanT, and
   standard deviation stdT. Amplitudes are modeled as in RandPulses.
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public class StatPulses extends Out {
    protected double meanT=1;
    protected float srate;
    protected double stdT=.1;
    protected float gain=1;
    protected double exponent = 1;
    protected int pulseInterval=-1;
    protected int interval_i=0;
    Random random;
    
    public StatPulses(float srate, int bufferSize) {
        super(bufferSize);
        random = new Random();
        this.srate = srate;
    }

    /** Set mean interpulse time
        @param meanT mean interpulse time
    */
    public void setMeanT(float meanT) {
        this.meanT = meanT;
    }

    /** Set pulse gain
        @param gain gain
    */
    public void setGain(float gain) {
        this.gain = gain;
    }

    /** Set std of interpulse time
        @param stdT standard deviation
    */
    public void setStdT(float stdT) {
        this.stdT = stdT;
    }
    
    /** Get mean interpulse time
        @return meanT mean interpulse time
    */
    public float getMeanT() {
        return (float)this.meanT;
    }

    /** Get std of interpulse time
        @return stdT standard deviation
    */
    public float getStdT() {
        return (float)this.stdT;
    }

    /** Set impulse prob. exponent. Volume of impact is r^exponent, with
        r uniform on [0 1]
        @param exponent exponent of prob. distribution
    */
    public void setProbabilityDistributionExponent(float exponent) {
        this.exponent = (double)exponent;
    }

    protected double getPulseInterval() {
        double r;
        while((r=(stdT*random.nextGaussian()) + meanT) < 0);
        return r;
    }

    /** Recompute pulse interval
     */
    public void reset() {
        interval_i = 0;
        pulseInterval = (int)(srate*getPulseInterval());
    }
    
    protected void computeBuffer() {
        int bufsz = getBufferSize();
        for(int i=0;i<bufsz;i++) {
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
                buf[i] = (float) (gain * vol * sign);
                pulseInterval = (int)(srate*getPulseInterval());
            } else {
                buf[i] = 0;
            }
        }
    }
    
}

