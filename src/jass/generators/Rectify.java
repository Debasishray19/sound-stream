package jass.generators;
import jass.engine.*;
import java.util.*;

/** Abs value of signal
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class Rectify extends InOut {

    public Rectify(int bufferSize) {
        super(bufferSize);
    }
    
    
    /** Compute the next buffer and store in member float[] buf.
        Note if stereo then output buf[] is twice as big as input buffers
     */
    protected void computeBuffer() {
        int bufsz = getBufferSize();
        int nsrc = sourceContainer.size();
        if(nsrc > 1) {
            nsrc = 1;
            System.out.println("Warning: Rectify has more sources than allowed");
        }
        float[] tmpsrc = srcBuffers[0];
        for(int k=0;k<bufsz;k++) {
            buf[k] = Math.abs(tmpsrc[k]);
        }
    }
}
