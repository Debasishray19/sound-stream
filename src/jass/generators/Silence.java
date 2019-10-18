package jass.generators;
import jass.engine.*;

/**
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public class Silence extends Out {
    
    public Silence(int bufferSize) {
        super(bufferSize);
    }

    protected void computeBuffer() {
        int bufsz = getBufferSize();
        for(int i=0;i<bufsz;i++) {
            buf[i] = 0;
        }
    }
    
}

