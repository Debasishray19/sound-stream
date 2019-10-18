package jass.generators;
import jass.engine.*;
import jass.utils.*;

/**
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

/**
   Output a volume velocity according to a dynamical glottal model.
   
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public abstract class GlottalModel extends Out {
    protected double srate;
    protected double ug=0,ug2=0; // glottal volume velocity and old value
    
    public GlottalModel(int bufferSize,double srate) {
        super(bufferSize);
        this.srate = srate;
    }

    /** Get glottal volume velocity
        @param k index of pan
        @return glottal volume velocity
     */
    public double getUg() {
        return ug;
    }
    
    /** Advance state by one sample. Lambda interpolation parameter
     */
    public abstract void advance(double lambda);
    
    protected abstract void computeBuffer();

    
}

