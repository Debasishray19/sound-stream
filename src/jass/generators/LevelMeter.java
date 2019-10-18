package jass.generators;
import jass.engine.*;
import java.util.*;

/** Level meter. Has null audio buffer so can only be attached to mixer as source
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class LevelMeter extends InOut {

    protected float level=0; // average |a|^2
    protected float dBOffset=0; // dB level reported is 10LOG(level) + dBOffset

    /** Add source to Sink. Override to allow only one input.
    @param s Source to add.
    @return object representing Source in Sink (may be null).
     */
    public Object addSource(Source s) throws SinkIsFullException {
        if (sourceContainer.size() > 0) {
            throw new SinkIsFullException();
        } else {
            sourceContainer.addElement(s);
        }
        return null;
    }
    
    public float getDBLevel() {
        return (float)(10*Math.log((double)level)+dBOffset);
    }
    
    /** Create level meter
        @param bufferSize Buffer size used for real-time rendering.
    */
    public LevelMeter(int bufferSize) {
        super(bufferSize);
        level = 0;
        dBOffset = 0;
        buf = null;    
    }

    /** Create level meter
        @param decibel offset
        @param bufferSize Buffer size used for real-time rendering.
    */
    public LevelMeter(int bufferSize,float dBOffset) {
        super(bufferSize);
        level = 0;
        this.dBOffset = dBOffset;
        buf = null;
    }
  
    /** Compute the next buffer (none here)
     */
    protected void computeBuffer() {
        if (srcBuffers == null ||srcBuffers[0] == null ) {
            return;
        }
        float[] tmpbuf = srcBuffers[0];
        float tmplevel = 0;
        int bufsz = getBufferSize();
        for (int k = 0; k < bufsz; k++) {
            tmplevel += tmpbuf[k]*tmpbuf[k];
        }
        tmplevel /= bufsz;
        level = tmplevel;
    }
   
}
