package jass.patches;
import jass.engine.*;
import jass.generators.*;

/** Map HSB color to [pitch reson-width lowpass-cutoff]
    Represent a color (h,s,b) by a noise source of maximum freq~b,
    filtered through a reson bank with Shepard frequencies
    (i.e. octaves apart covering the audible range) and some damping d = 1*freq/freq_lowest.
    The hue h [0 1] will be mapped to an octave range in freq. (Note the dampings
    are also scaled when freq. is scaled to preserve scale invariance of octaves.) The saturation
    s [0 1] will be mapped to the "material" (i.e., the width of the resonances will be
    multiplied by a factor depending on the saturation.
    This is a patch using ColorSonificator (with b==1) and then filter through Butterworth filter.
    So brightness is mapped to surface roughness if we think of the sound as being scrpe sounds.
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class LowpassColorSonificator extends InOut {

    protected float srate=44100;
    protected float upperFreq=4000;
    protected float lowerFreq=50;
    protected FilterContainer filterContainer;
    protected Butter2LowFilter butter2LowFilter;
    protected HS1ColorSonificator theHS1ColorSonificator; // ColorSonificator but Brightness==1
    protected float minSlideVelocity = 0.01f;
    protected float maxSlideVelocity = 2.5f; // sound drops out if >2.5 and using unsaturated sound (BUG)

    /** As parent class but lock its brightness to 1 as this will now be mapped to
        lowpasscutoff
    */
    class HS1ColorSonificator extends ColorSonificator {
        public HS1ColorSonificator(float srate,int bufferSize) {
            super(srate,bufferSize);
        }
        
        /** Set hue, saturation (brightness ==1)
            @param h hue in range 0-1
            @param s saturation in range 0-1
        */
        public void setHS(float h,float s) {
            super.setHSB(h,s,1);
        }
        
        /** Set hue, saturation (brightness ==1), and slide velocity
            @param h hue in range 0-1
            @param s saturation in range 0-1
            @param v velocity in range 0-1
        */
        public void setHS_V(float h,float s,float v) {
            super.setHSB_V(h,s,1,v);
        }

    }

    /** Create and initialize.
        @param srate sampling rate in Hertz.
        @param bufferSize Buffer size used for real-time rendering.
    */
    public LowpassColorSonificator(float srate,int bufferSize) {
        super(bufferSize);
        this.srate = srate;
        createPatch();
    }

    /** Create. For derived classes.
        @param bufferSize Buffer size used for real-time rendering.
    */
    public LowpassColorSonificator(int bufferSize) {
        super(bufferSize);
    }

    /** Set freq. limits of lowpass
        @param lower freq for black
        @param upper freq. for white
    */
    public void setLowpassFrequencyRange(float lower,float upper) {
        lowerFreq = lower;
        upperFreq = upper;
    }

    private float[] freqLimits = new float[2];

    /** Get freq. limits of lowpass
        @return [lower upper] freq for black and white
    */
    public float[] getLowpassFrequencyRange() {
        freqLimits[0]=lowerFreq;
        freqLimits[1]=upperFreq;
        return freqLimits;
    }

    /** Create. For derived classes.
        @param bufferSize Buffer size used for real-time rendering.
        @param srate sampling rate in Hz
    */
    public LowpassColorSonificator(int bufferSize,float srate) {
        super(bufferSize);
        this.srate = srate;
        createPatch();
    }

    /**
       Set power in scaling law for gains a = a*d^fudgePower. .5 for constant energy but lower
       in practice because of critical band effect, to have d-independent loudness
       @param p fudgePower
    */
    public void setFudgePower(float p) {
        theHS1ColorSonificator.setFudgePower(p);
    }

    /**
       Get power in scaling law for gains a = a*d^fudgePower. .5 for constant energy but lower
       in practice because of critical band effect, to have d-independent loudness
       @return fudgePower
    */
    public float getFudgePower(float p) {
        return theHS1ColorSonificator.getFudgePower();
    }

    /** Set damping range corresponding to saturation
        @param dmin damping for saturated color
        @param dmax damping for unsaturated color
    */
    public void setSaturationLimits(float dmin,float dmax) {
        theHS1ColorSonificator.setSaturationLimits(dmin,dmax);
    }

    private void setLowpassCutoff(float f) {
        butter2LowFilter.setCutoffFrequency(f);
    }

    private void setLowpassGain(float g) {
        butter2LowFilter.setGain(g);
    }

    protected void createPatch() {
        butter2LowFilter = new Butter2LowFilter(srate);
        filterContainer = new FilterContainer(srate,bufferSize,butter2LowFilter);
        theHS1ColorSonificator = new HS1ColorSonificator(srate,bufferSize);
        try {
            theHS1ColorSonificator.addSource(filterContainer);
        } catch(SinkIsFullException e) {
            System.out.println(this+" "+e);
        }
        // set model parameters to defaults (this is code for documentation really, can leave it out)
        theHS1ColorSonificator.setMaximumLevelDifference(theHS1ColorSonificator.getMaximumLevelDifference());
        float[] satLimits = theHS1ColorSonificator.getSaturationLimits();
        theHS1ColorSonificator.setSaturationLimits(satLimits[0],satLimits[1]);
        float[] freqLimits = this.getLowpassFrequencyRange();
        this.setLowpassFrequencyRange(freqLimits[0],freqLimits[1]);

        // synchronize in case patch is created at runtime
        long t = getTime();
        filterContainer.setTime(t);
        theHS1ColorSonificator.setTime(t);
    }
    
    /** Add source to input of patch, the lowpass filter in this case. Allow 1 input only
        @param s Source to add.
        @return object representing Source in Sink (may be null).
    */
    public Object addSource(Source s) throws SinkIsFullException {
        if(getSources().length > 0) {
            throw new SinkIsFullException();
        } else {
            filterContainer.addSource(s);
            // add to Vector so that other  stuff like getSources will work
            return super.addSource(s); 
        }
    }

    // set brightness and velocity (0-1)
    private void setB_V(float b,float v) {
        if(v<minSlideVelocity) {
            v = minSlideVelocity;
        } else if(v>maxSlideVelocity) {
            v = maxSlideVelocity;
        }
        double fcutoff = v*lowerFreq*Math.pow(upperFreq/lowerFreq,b);
        if(fcutoff<lowerFreq) {
            fcutoff = lowerFreq;
        }
        //System.out.println(fcutoff);
        setLowpassCutoff((float)fcutoff);
    }

    /** Set hue, saturation and brightness
        @param h hue in range 0-1
        @param s saturation in range 0-1
        @param b brightness in range 0-1
    */
    public void setHSB(float h,float s,float b) {
        float velocity = 1;
        theHS1ColorSonificator.setHS_V(h,s,velocity);
        this.setB_V(b,velocity);
    }

    
    /** Set hue, saturation, brightness  and slide velocity (1 = max)
        @param h hue in range 0-1
        @param s saturation in range 0-1
        @param b brightness in range 0-1
        @param v velocity in range 0-1
    */
    public void setHSB_V(float h,float s,float b,float v) {
        if(v<minSlideVelocity) {
            v = minSlideVelocity;
        } else if(v>maxSlideVelocity) {
            v = maxSlideVelocity;
        }
        theHS1ColorSonificator.setHS_V(h,s,v);
        this.setB_V(b,v);
    }


    /** Compute the next buffer and store in member float[] buf.
     */
    protected void computeBuffer() {
        try {
            // get buffer from internal patch output
            buf = theHS1ColorSonificator.getBuffer(getTime());
        } catch(BufferNotAvailableException e) {
            System.out.println(this+" "+e);            
        }
    }

}
