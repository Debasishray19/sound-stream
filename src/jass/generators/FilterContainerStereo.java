package jass.generators;
import jass.engine.*;
import java.io.*;

/** Filter UG. One input only. Processes input through filter. 
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class FilterContainerStereo extends InOut {
    // to store left and right buffers
    private float[] ch1in,ch2in,ch1out,ch2out;
    
    private void init() {
        ch1in = new float[bufferSize/2];
        ch2in = new float[bufferSize/2];
        ch1out = new float[bufferSize/2];
        ch2out = new float[bufferSize/2];
    }
    
    // split interleaved input buffer
    private void split(float[] buf) {
        for(int i=0;i<bufferSize/2;i++) {
            ch1in[i] = buf[2*i];
            ch2in[i] = buf[2*i+1];
        }
    }
    
    // merge into interleaved outputbuffer
    private void merge(float[] buf) {
        for(int i=0;i<bufferSize/2;i++) {
            buf[2*i]=ch1out[i];
            buf[2*i+1]=ch2out[i];
        }
    }
    
    /** Add source to Sink. Override to allow only one input.
        @param s Source to add.
        @return object representing Source in Sink (may be null).
    */
    public Object addSource(Source s) throws SinkIsFullException {
        if(sourceContainer.size() > 0) {
            throw new SinkIsFullException();
        } else {
            sourceContainer.addElement(s);
        }
        return null;
    }
    
    /** Filter */
    Filter filter1 = null, filter2=null;

    /** SetFilter contained.
        @param f Filter contained.
    */
    public void setFilters(Filter f1,Filter f2) {
        filter1 = f1;
        filter2 = f2;
    }
    
    /** Create container around Filter.
        @param srate sampling rate in Hertz.
        @param bufferSize Buffer size used for real-time rendering.
        @param f Filter contained.
    */
    public FilterContainerStereo(float srate, int bufferSize, Filter f1, Filter f2) {
        super(bufferSize);
        setFilters(f1,f2);
        init();
    }

    /** Compute the next buffer and store in member float[] buf.
     */
    protected void computeBuffer() {
        int offSet = 0;
        split(srcBuffers[0]);
        filter1.filter(ch1out,ch1in,bufferSize/2,offSet);
        filter2.filter(ch2out,ch2in,bufferSize/2,offSet);
        merge(this.buf);
    }

}
