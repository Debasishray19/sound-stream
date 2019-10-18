package jass.render;
import java.io.*;
import jass.engine.*;

/** Write line data to output file (time sample\n). Place this UG in a line.
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public class Probe extends FilterUG {
    protected FileOutputStream outStream=null;
    protected PrintStream printStream=null;
    protected boolean isOn = true;

    /** Create and initialize.
        @param bufferSize Buffer size used for real-time rendering.
        @param srate sampling rate in Hertz.
        @param fn log file name
     */
    public Probe(int bufferSize,String fn) {
        super(bufferSize);
        try {
            outStream = new FileOutputStream(new File(fn));
            printStream = new PrintStream(outStream);
        } catch(FileNotFoundException e) {
            System.out.println( e);
        }
    }

    public void on() {
        isOn = true;
    }

    public void off() {
        isOn = false;
    }

    public boolean isOn() {
        return isOn;
    }
    
    /** Create. For derived classes.
        @param bufferSize Buffer size used for real-time rendering.
     */
    public Probe(int bufferSize) {
        super(bufferSize);
    }

    /** Compute the next buffer and store in member float[] buf.
     */
    protected void computeBuffer() {
        int bufsz = getBufferSize();
        float[] tmpsrc = srcBuffers[0];
        long iframe = getTime();
        long isample = (iframe-1)*bufsz;
        for(int k=0;k<bufsz;k++,isample++) {
            buf[k] = tmpsrc[k];
            if(isOn) {
                printStream.print(isample);
                printStream.print(' ');
                printStream.println(tmpsrc[k]);
            }
        }
    }
}


