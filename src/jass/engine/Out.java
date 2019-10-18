package jass.engine;

/**
   Output-only unit. Will produce audio-rate buffers. Needs
   only implementation of computeBuffer(). 
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public abstract class Out implements Source {
    
    /**
       The current time of this object, measured in number of frames of
       length bufferSize. So this is the number of frames processed.
    */
    private long currentTime;

    /** Buffer length of processed audio buffers. */
    protected int bufferSize;

    /** The current buffer. */
    protected float[] buf;

        /** The old buffer. */
    protected float[] bufOld;

    /** To provide access to the old buffer without locking whole class */
    protected Object lock;

    protected void copyToOld() {
        synchronized(lock) {
            System.arraycopy(bufOld,0,buf,0,buf.length);
        }
    }

   /**
      Create at time 0 (which you may want to change by calling setTime()
      if objects are created in the middle of some jass.sis process). 
    */
    public Out(int bufferSize) {
        this.bufferSize = bufferSize;
        setTime(0);
        buf = new float[bufferSize];
        bufOld = new float[bufferSize];
        lock = new Object();
        clearBuffer();
    }

    /**
      Create.
      Does not allocate bufferSize or set time which is still unknown yet
      if objects are created in the middle of some jass.sis process). 
    */
    public Out() {
    } 
    
    /** Peek at buffer. Noone will notice.
     */
    public float[] peekAtBuffer() {
        return buf;
    }
    
    /** Get current time.
        @return current time.
    */
    public synchronized long getTime() {
        return currentTime;
    }
    
    /** Set current time. Usually called only at init time.
        @param t current time.
    */
    public synchronized void setTime(long t) {
        currentTime = t;
        notify();
    }

    /** Set current time and notify waiting threads (used by ThreadMixer).
        @param t current time.
    */
    public synchronized void setTimeAndNotify(long t) {
        currentTime = t;
        notifyAll();
    }
    
    /**
       Reset time of self and all inputs
       @param t time to reset to. Patch must be in a state s.t. none of the current times == t
    */
    public synchronized void resetTime(long t) {
        setTime(t);
    }
    /** Get buffer size.
        @return buffer size in samples.
    */
    public int getBufferSize() {
        return bufferSize;
    }
    
    /** Set buffer size.  Will also reallocate buffer and clear.
        @param bufferSize buffer size.
    */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        buf = new float[bufferSize];
        clearBuffer();
    }
    
    /** Compute the next buffer and store in member float[] buf.
        This is the core processing method which will be implemented
        for each generator.
     */
    protected abstract void computeBuffer();

    /** Clears buffer to zero.
     */
    public void clearBuffer() {
        for(int i=0;i<bufferSize;i++) {
            buf[i] = 0;
        }
    }

    /**
       Get buffer with frame index t. Return old buffer if have it in cache.
       Compute next buffer and advance time if requested, throw exception if
       requested buffer lies in the past or future.  This method will be
       called "behind the scenes" when processing filtergraphs.
       @param t timestamp of buffer = frame index. 
    */
    public synchronized float[] getBuffer(long t) throws BufferNotAvailableException {
        copyToOld();
        if(t == currentTime+1) { // requested next buffer
            setTime(t);
            computeBuffer();
        } else if(t != currentTime) { // neither current or next buffer requested: deny request
            System.out.println("Error! "+this+" Out.java: t="+t+" currentTime="+currentTime);
            throw new BufferNotAvailableException();
        }
        // return new or old buffer:
        return buf;
    }

    /**
       Get old buffer in cache. Deliberately not synchronized.
    */
    public float[] getBuffer() throws BufferNotAvailableException {
        synchronized(lock) {
            return bufOld;
        }
    }


}

