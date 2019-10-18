package jass.generators;
import jass.engine.*;

/**
   Output white noise with amplitude [-1 +1]
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public class RandOut extends Out {
    protected float gain=1;
    
    public RandOut(int bufferSize) {
        super(bufferSize);
    }

    public void setGain(float g) {
        this.gain = g;
    }

    protected void computeBuffer() {
        int bufsz = getBufferSize();
        for(int i=0;i<bufsz;i++) {
            double x = Math.random();
            buf[i] = (float) (gain*(2*x -1));
        }
    }
    
}

