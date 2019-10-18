package jass.engine;
import java.util.*;

/**
   Input/output unit. Needs only implementation of computeBuffer().
   Each attached source can be labeled passive, which means that we
   will call getBuffer() without a time stamp on it, so no computation is triggered.
   This is needed only when using the ThreadMixer which would result in deadlocks on
   closed loops. So you have to explicitly mark source connections passive to eliminate
   loops. When using only 1 thread this is not needed.
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public abstract class InOut extends Out implements Sink {
    
    protected Vector<Source> sourceContainer;
    protected Vector<Boolean> sourcePassivity;

    /** add source to Sink.
        @param s Source to add.
        @return object representing Source in Sink (may be null).
    */
    public synchronized Object addSource(Source s) throws SinkIsFullException {
        sourceContainer.addElement(s);
        sourcePassivity.addElement(new Boolean(false));
        s.setTime(getTime());
        return null;
    }
    
    /** add source to Sink, can flag as passive (so will trigger no computation)
        @param s Source to add.
        @param passive flag; true if passive, otherwise will be active (normal)
        @return object representing Source in Sink (may be null).
    */
    public synchronized Object addSource(Source s,boolean p) throws SinkIsFullException {
        sourceContainer.addElement(s);
        sourcePassivity.addElement(new Boolean(p));
        s.setTime(getTime());
        return null;
    }
    
    public synchronized void removeSource(Source s) {
        int i = sourceContainer.indexOf(s);
        sourceContainer.removeElement(s);
        sourcePassivity.removeElementAt(i);
    }

    /** Get array of sources.
        @return array of the Sources, null if there are none.
    */
    public Source [] getSources() {
        return sourceContainer.toArray(new Source[0]);
    }
    
    /**
       Array of buffers of the sources
     */
    protected float[][] srcBuffers;

    public InOut(int bufferSize) {
        super(bufferSize);
        sourceContainer = new Vector<Source>();
        sourcePassivity = new Vector<Boolean>();
        srcBuffers = new float[1][];
    }

    /** Call all the sources and cache their returned buffers.
     */
    private final void callSources() {
        Source [] src = sourceContainer.toArray(new Source[0]);
        int n = src.length; // number of sources
        int n_buf = srcBuffers.length; // number of source buffers allocated

        if(n_buf < n) {
            srcBuffers = new float[n][];
        }
        try {
            for(int i=0;i<n;i++) {
                //if(sourcePassivity.elementAt(i).booleanValue()) {
                //    srcBuffers[i] = (src[i]).getBuffer();
                //} else {
                    srcBuffers[i] = (src[i]).getBuffer(getTime());
                    //}
            }
        } catch(BufferNotAvailableException e) {
            System.out.println("InOut.callSources: "+this+" "+e);
        }
    }

    /**
       Get buffer with frame index t. Return old buffer if have it in cache.
       Compute next buffer and advance time if requested, throw exception if
       requested buffer lies in the past or future.  This method will be
       calle "behind the scenes" when processing filtergraphs.
       @param t timestamp of buffer = frame index. 
    */
    public synchronized float[] getBuffer(long t) throws BufferNotAvailableException {
        copyToOld();
        if(t == (getTime()+1)) { // requested next buffer
            setTime(t);
            callSources();
            computeBuffer(); // use cached source buffers to compute buf.
        } else if(t != getTime()) { // neither current or next buffer requested: deny request
            System.out.println("Error! "+this+" Out.java: t="+t+" currentTime="+getTime());
            throw new BufferNotAvailableException();
        }
        // return new or old buffer:
        return buf;
    }

    /**
       Reset time of self and all inputs
       @param t time to reset to. Patch must be in a state s.t. none of the current times == t
    */
    public synchronized void resetTime(long t) {
        setTime(t);
        resetTimeSources(t);
    }

    /** Call all the sources and reset time
     */
    private final void resetTimeSources(long t) {
        Object [] src = getSources();
        int n = src.length; // number of sources
        for(int i=0;i<n;i++) {
            if(src[i] instanceof Out) {
                if(((Out)src[i]).getTime() != t) {
                    ((Out)src[i]).resetTime(t);
                }
            }
        }
    }
    

}

