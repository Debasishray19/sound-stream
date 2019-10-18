package jass.engine;
import java.util.*;

/**
   Mixer that runs a thread on each input. Add sources, then call init() method, then you can use it
   as a normal mixer. Unlike other UG's you can't add or remove sources while running.
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public final class ThreadMixer extends Out implements Sink {

    private float[] gains;     // gains of sources
    private float[] tmp_buf; // scratchpad
    private Vector<Source> sourceContainer; 
    public float[][] srcBuffers; // Array of buffers of the sources
    private boolean isInitted = false;
    private Source [] src; // array of sources
    private int nSourcesDone;
    private SourceThread[] sourceThreads;
    private Object lock;
    
    public synchronized void signalDone() {
        D.prt("Enter signalDone");
        nSourcesDone++;
        notifyAll();
        D.prt("Exit signalDone");
    }

    private int getNSourcesDone() {
        return nSourcesDone;
    }

    private void clearNSourcesDone() {
        nSourcesDone = 0;
    }
    
    /** Set input gain control vector.
        @param k index of gain
        @param gains input gain
    */
    public void setGain(int k, float g) {
        if(isInitted) {
            if(k<0 || k >= gains.length) {
                return;
            } else {
                gains[k] = g;
            }
        } else {
            System.out.println("ThreadMixer.setGain called before init(), ignored ");
        }
    }

    /** Get input gain control vector.
        @param gains input gains
     */
    public float[] getGains() {
        return gains;
    }

    /** add source to Sink.
        @param s Source to add.
        @return object representing Source in Sink (may be null).
    */
    public synchronized Object addSource(Source s) throws SinkIsFullException {
        if(isInitted) {
            throw new SinkIsFullException();
        }
        sourceContainer.addElement(s);
        s.setTime(getTime());
        return null;
    }
    
    public synchronized void removeSource(Source s) {
        if(!isInitted) {
            sourceContainer.removeElement(s);
        }
    }

    /** Get array of sources.
        @return array of the Sources, null if there are none.
    */
    public Source [] getSources() {
        return src;
    }
    
    public ThreadMixer(int bufferSize) {
        super(bufferSize);
        sourceContainer = new Vector<Source>();
        srcBuffers = new float[1][];
    }

    public void init() {
        isInitted = true;
        src = sourceContainer.toArray(new Source[0]);
        int n = src.length; // number of sources
        srcBuffers = new float[n][];
        tmp_buf = new float[bufferSize];
        gains = new float[n];
        sourceThreads = new SourceThread[n];
        lock = new Object();
        startThreads();
    }


    private void startThreads() {
        int n = src.length;
        clearNSourcesDone();
        try {
            for(int i=0;i<n;i++) {
                sourceThreads[i] = new SourceThread(this,src[i],i,lock);
                sourceThreads[i].start();
            }
        } catch(Exception e) {
            System.out.println("ThreadMixer.startThreads: "+this+" "+e);
        }
    }
    
    /** Call all the sources and cache their returned buffers.
     */
    private final void callSources() {
        int n = srcBuffers.length; // number of source buffers allocated
        clearNSourcesDone();
        for(int i=0;i<n;i++) {
            D.prt("callSources going to wake up thread "+i);
            sourceThreads[i].wakeUp(getTime());
            D.prt("callSources has woken up thread "+i);
        }
        while(nSourcesDone < n) {
            try {
                D.prt("main waiting (" +nSourcesDone+" sources done)");
                wait();
            } catch(InterruptedException e) {}       
        }
        D.prt("main done waiting ("+nSourcesDone+" sources done)");
    }

    /**
       Get buffer with frame index t. Return old buffer if have it in cache.
       Compute next buffer and advance time if requested, throw exception if
       requested buffer lies in the past or future.  This method will be
       called "behind the scenes" when processing filtergraphs.
       @param t timestamp of buffer = frame index. 
    */
    public final synchronized float[] getBuffer(long t) throws BufferNotAvailableException {
        if(!isInitted) {
            init();
        }
        D.prt("getBufferCalled ");
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

    /** Compute the next buffer and store in member float[] buf.
     */
    protected final void computeBuffer() {
        int bufsz = getBufferSize();
        int nsrc = sourceContainer.size();
        if(nsrc > gains.length) {
            nsrc = gains.length;
            System.out.println("Warning: ThreadMixer has more sources than allowed");
        }
        for(int k=0;k<bufsz;k++) {
            // can't overwrite buf[] yet as one of the srcBuffers may be pointing to it!
            tmp_buf[k] = 0; 
        }
        for(int i=0;i<nsrc;i++) {
            float[] tmpsrc = srcBuffers[i];
            //System.out.println("i= "+i+ "src[] = " + srcBuffers[i][5]);
            float g = gains[i];
            for(int k=0;k<bufsz;k++) {
                tmp_buf[k] += g*tmpsrc[k];
            }
        }
        for(int k=0;k<bufsz;k++) {
            buf[k] = tmp_buf[k];
        }
        D.prt("Compute buffer");
    }

}

    class SourceThread extends Thread {
        private ThreadMixer tm;
        private Source s;
        private int index;
        private long localtime;
        private Object lock;
        
        public SourceThread(ThreadMixer tm,Source s,int index,Object lock) {
            this.tm = tm;
            this.s = s;
            this.index = index;
            this.localtime = 0;
            this.lock = lock;
        }

        public synchronized void wakeUp(long t) {
            D.prt("Enter wakeUp");
            localtime = t;
            //D.prt("thread "+index+ " wakeUp" +" t="+s.getTime()+" tsys="+tm.getTime());
            notifyAll();
            D.prt("Exit wakeUp");
        }
        
        private synchronized void process() {
            while(true) {
                D.prt("another loop in process starts");
                D.prt("source time = "+s.getTime()+" localtime= "+localtime);
                while(s.getTime() == localtime) {
                    D.prt("process got here");
                    try {
                        D.prt("thread "+index+ " waiting"+ " t= "+s.getTime()+" tsys="+localtime);
                        wait();
                    } catch(InterruptedException e) {}
                }
                D.prt("thread "+index+ " done waiting");
                try {
                    tm.srcBuffers[index] = s.getBuffer(localtime);
                    tm.signalDone();
                    D.prt("thread "+index+" done getting buffer");
                } catch(Exception e) {
                    System.out.println("SourceThread: "+this+" "+e);
                }
                
            }
            
        }
        
        public void run() {
            process();
        }
    }

class D {
    public static void prt(String s) {
        System.out.println(s);
    }
    
}


