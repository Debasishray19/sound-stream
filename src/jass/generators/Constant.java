package jass.generators;
import jass.engine.*;

/**
   Onst. output
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public class Constant extends Out {
    protected float c=1;

    public void setConstant(float c) {
        this.c=c;
    }

    public float getConstant() {
        return c;
    }
    
    public Constant(int bufferSize) {
        super(bufferSize);
    }

    protected void computeBuffer() {
        int bufsz = getBufferSize();
        for(int i=0;i<bufsz;i++) {
            buf[i] = c;
        }
    }
    
}

