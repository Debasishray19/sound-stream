package jass.generators;
import jass.engine.*;

/**
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
   
   Implement Webster equation with open ends (pressure=0) and external fluid force applied on left end.
   
   u,f on o grid, S (area) on both p on x grid, p=0 on boundaries
   O x o x o x o x O
   N grid points, N is odd. Let i= 0,...,N-1
   S[i] p[i=0,2...,N-1], u[i=1,3,..,N-2], same for f[]
   Physical parameters:
   - pressure deviation PP = rho * p (rho = dmass dens. air)
   - velocity UU = u/c
   - force on fluid FF = (rho/c)*f
   
   CFL conditions give delta_x = h > c/2*srate.
*/

public class OpenWebsterTube extends InOut{
    /** Sampling rate in Hertz. */
    protected float srate;
    protected double minLen=.15;
    protected double c = 340;
    protected double len; // > minLen
    protected double h;
    protected int overSamplingFactor = 20;
    protected int N;
        
    protected double[] S; // Area of the segments
    protected double[] Sold; // Area of the segments
    protected double[] pu; // velocity u and pressure p on staggered grid
    protected double[] d_pu;
    protected double[] outBuf; // oversampled
    TubeModel tubeModel;

    private void allocate() {
        h = 2*c/(2*srate);
        N = 1 + (int)(minLen/h);
        if(N%2 == 0) {
            N--;
        }
        System.out.println(N);
        h = minLen/(N-1);
        len = minLen;
        S = new double[N];
        Sold = new double[N];
        pu = new double[N]; // p on even points, u on odd
        d_pu = new double[N];
        outBuf = new double[getBufferSize()*overSamplingFactor];
        for(int i=0;i<N;i++) {
            S[i] = 1;
            Sold[i] = 1;
            pu[i]=0;
            d_pu[i]=0;
        }
    }

    
    public void changeTubeModel() {
        double len = tubeModel.getLength();
        for(int k=0;k<N;k++) {
            double x = k*h;
            double r = tubeModel.getRadius(x);
            S[k] = r*r;
        }
        for(int k=0;k<N;k++) {
            System.out.println("s: "+S[k]);
        }
        
    }
    /*
    int sdfsdfs=0;
    public void changeTubeModel() {
        System.out.println("changed: "+sdfsdfs);
        if(sdfsdfs++>2) {
            for(int k=N/2;k<N;k++) {
                S[k] = .1;
            }
        }
        for(int k=0;k<N;k++) {
            System.out.println(S[k]);
        }
    }
    */
    public void reset() {
        for(int i=0;i<N;i++) {
            pu[i]=0;
        }
    }

    /** Create and initialize.
        @param srate sampling rate in Hertz.
        @param nTubeSection maximum number of sections (must be even)
    */
    public OpenWebsterTube(float srate, int bufferSize,TubeModel tm) {
        super(bufferSize);
        this.srate = srate;
        this.tubeModel = tm;
        allocate();
    }

    /** Set an individual segment area
        @param k index of segment (0,....N-1)
        @param a area to set
    */
    public void setArea(int k,double a) {
        S[k]=a;

    }
    
    /** Set all areas
        @param array of r areas
    */
    public void setAllAreas(double[] a) {
        for(int k=0;k<N;k++) {
            S[k] = a[k];
        }
    }
    
    /** Compute the next buffer and store in member float[] buf.
     */
    protected void computeBuffer() {
        float[] tmpbuf = srcBuffers[0];
        int bufsz = getBufferSize()*overSamplingFactor;
        double eta = c/(srate*2*h*overSamplingFactor);
        //System.out.println(eta);
        for(int k=0;k<bufsz;k++) {
            /*
              Area at time indexed by k is given by
              (1-spar)S_old + spar * S where
              spar = (k+1)/bufsz;
            */
            double s_now = (k+1)/bufsz; // runs from 0 -1 over sample buffer to interp.
            double s_prev = k/bufsz;
            for(int i=2;i<=N-3;i+=2) {
                double S_now = (1-s_now)*Sold[i] + s_now*S[i];
                double S_prev = (1-s_prev)*Sold[i] + s_prev*S[i];
                double S_prev_right = (1-s_prev)*Sold[i+1] + s_prev*S[i+1];
                double S_prev_left = (1-s_prev)*Sold[i-1] + s_prev*S[i-1];
                double tmp = S_prev/S_now-1;
                double tmp2 = (S_prev_right*pu[i+1] - S_prev_left*pu[i-1])/S_now;
                d_pu[i] = tmp*pu[i]-eta*tmp2 + c*c*tmp;
                //System.out.println(i+" "+d_pu[i]);
            }
            for(int i=1;i<N;i+=2) {
                d_pu[i] = -eta*(pu[i+1]-pu[i-1]);
            }
            double damping = .003;
            for(int i=1;i<N;i+=2) {
                pu[i] *= (1-damping);
            }
            d_pu[1] += tmpbuf[k/overSamplingFactor]/srate;
            //System.out.println(System.out.println(pu[N-2]+" "+d_pu[N-2]);
            
            for(int i=0;i<N;i++) {
                pu[i] += d_pu[i];
                Sold[i] = S[i];
            }
            outBuf[k] = (float)pu[N-2];
        }
        bufsz = getBufferSize();
        for(int k=0;k<bufsz;k++) {
            buf[k] = (float)outBuf[k*overSamplingFactor];
        }
        // System.out.println(buf[0]);
    }
}
