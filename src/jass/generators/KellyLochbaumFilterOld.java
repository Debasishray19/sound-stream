package jass.generators;

/** Kelly-Lochbaum filter. Follow notation of MATLAB sample code from
    www.cs.tut.fi/sgn/arg/8003051 and associated writeup therein
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public class KellyLochbaumFilterOld implements Filter {

    /** Sampling rate in Hertz. */
    protected float srate;

    private static final double DEFAULT_glottalReflectionCoeff = 0;

    private static final double DEFAULT_dampingCoeff = 1;
    
    /** State of filter. */
    protected double[] F0,F1,B0,B1;

    /** How much is reflected back at glottis*/
    protected double glottalReflectionCoeff=DEFAULT_glottalReflectionCoeff;

    /** How much damping in system (1 == no damping)*/
    protected double dampingCoeff=DEFAULT_dampingCoeff;

    /** Scratch variables */
    protected double[] F0old,F1old,B0old,B1old;

    /** This many cylinder segments */
    protected int nTubeSections;

    /** This many junctions (above -1) */
    protected int nJunctions;

    /** Radii of the segments */
    protected double[] cylRadius;

    /** Filter coefficients derived form cylinder radii */
    protected double[] kCoeff;
    
    /** Create and initialize.
        @param srate sampling rate in Hertz.
        @param nTubeSection number of sections
     */
    public KellyLochbaumFilterOld(float srate, int nTubeSections) {
        this.srate = srate;
        this.nTubeSections = nTubeSections;
        this.nJunctions = nTubeSections-1;
        allocate();
        resetFilter();
        System.out.println("ns="+nTubeSections);
    }

    public KellyLochbaumFilterOld() {}

    private void allocate() {
        F0 = new double[nJunctions];
        F0old = new double[nJunctions];
        F1 = new double[nJunctions];  // F1 needs 1 less really
        F1old = new double[nJunctions];
        B0 = new double[nJunctions];
        B0old = new double[nJunctions];
        B1 = new double[nJunctions];
        B1old = new double[nJunctions];
        cylRadius = new double[nTubeSections];
        kCoeff = new double[nJunctions];
        for(int i=0;i<nJunctions;i++) {
            cylRadius[i] = 1;
        }
        cylRadius[nJunctions] = 1;
        computeKCoeff();
    }

    /** Compute low level filter values from geometry */
    protected void computeKCoeff() {
        for(int i=0;i<nJunctions;i++) {
            kCoeff[i] = (cylRadius[i]*cylRadius[i]-cylRadius[i+1]*cylRadius[i+1])/(cylRadius[i]*cylRadius[i]+cylRadius[i+1]*cylRadius[i+1]);
        }
    }

    /** Set an individual segment radius
        @param k index of segment (0,...)
        @param r radius to set
     */
    public void setCylinderRadius(int k,double r) {
        cylRadius[k]=r;
        computeKCoeff();
    }
    
    /** Set all radii
        @param array of r radii 
     */
    public void setAllCylinderRadii(double[] r) {
        for(int k=0;k<nTubeSections;k++) {
            cylRadius[k]=r[k];
        }
        computeKCoeff();
    }

    /** Clear filter of past history */
    public void resetFilter() {
        for(int i=0;i<nJunctions;i++) {
            F0[i] = F0old[i] = F1[i] = F1old[i] = 0;
            B0[i] = B0old[i] = B1[i] = B1old[i] = 0;
            //System.out.println("B0[0]="+B0[0]);
        }
    }

    /** Set the glottal reflection coeff. 
        @param val glottal reflection coefficient
    */
    public void setGlottalReflectionCoeff(double val) {
        glottalReflectionCoeff = val;
    }
    
    /** Set damping coeff. (1 == no damping)
        @param val damping coefficient
    */
    public void setDampingCoeff(double val) {
        dampingCoeff = val;
    }
    
    /** Proces input (may be same as output). Implements Filter interface
        @param output user provided buffer for returned result.
        @param input user provided input buffer.
        @param nsamples number of samples written to output buffer.
        @param inputOffset where to start in circular buffer input.
    */
    public void filter(float [] output, float[] input, int nsamples, int inputOffset) {
        int inputLen = input.length;
        int ii = inputOffset;
        for(int i=0;i<nJunctions-1;i++) {
            double kkk = kCoeff[i];
            if(Math.abs(kkk)>=1) {
                System.out.println("kCoef="+kCoeff[i]);
            }
        }
        //System.out.println("B0[0]="+B0[0]);
        for(int k=0;k<nsamples;k++) {

            for(int i=0;i<nJunctions;i++) {
                F0old[i] = F0[i];
                F1old[i] = F1[i];
                B0old[i] = B0[i];
                B1old[i] = B1[i];
            } 
            B0[0] = B1old[0];
            F0[0] = B0[0]*glottalReflectionCoeff + input[ii];


            F1[0] = F0old[0];
            B1[0] = (B1[1]*(1+kCoeff[0]) + F1[0]*kCoeff[0])*dampingCoeff;
            for(int i=1;i<nJunctions-1;i++) {
                B0[i] = B1old[i];
                F0[i] = (F1[i-1]*(1-kCoeff[i-1]) - B0[i]*kCoeff[i-1])*dampingCoeff;
                F1[i] = F0old[i-1];
                B1[i] = (B1[i+1]*(1+kCoeff[i]) + F1[i]*kCoeff[i])*dampingCoeff;
            }
            B0[nJunctions-1] = 0;
            F0[nJunctions-1] = F1[nJunctions-2]*(1-kCoeff[nJunctions-1])*dampingCoeff;
            output[k]=(float)F0[nJunctions-1];


            if(ii == inputLen - 1) {
                ii = 0;
            } else {
                ii++;
            }
        }
    }
}
