package jass.generators;

/** Kelly-Lochbaum filter. Follow sample code from
    http://people.ee.ethz.ch/~jniederh/VocSynth/
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public class KellyLochbaumFilter implements Filter {

    private static final double DEFAULT_dampingCoeff = 1;
    
    /** How much damping in system (1 == no damping)*/
    protected double dampingCoeff=DEFAULT_dampingCoeff;

    /** Sampling rate in Hertz. */
    protected float srate;

    /** State of filter. */
    protected double[] li,lo,gi,go;

    /** This many cylinder segments */
    protected int nTubeSections;

    /** Radii of the segments */
    protected double[] cylRadius;

    /** Filter coefficients derived form cylinder radii */
    protected double[] kCoeff;
    
    /** Create and initialize.
        @param srate sampling rate in Hertz.
        @param nTubeSection number of sections
     */
    public KellyLochbaumFilter(float srate, int nTubeSections) {
        this.srate = srate;
        this.nTubeSections = nTubeSections;
        allocate();
        resetFilter();
        System.out.println("ns="+nTubeSections);
    }

    public KellyLochbaumFilter() {}

    private void allocate() {
		li=new double[nTubeSections+1]; 	//to lips input to reflection --(z-1)--li----lo--
		lo=new double[nTubeSections+1]; 	//to lips output of reflection          |refl|
		gi=new double[nTubeSections+1]; 	//to glottis input to reflection ------go----gi--
		go=new double[nTubeSections+1]; 	//to glottis output of reflection
        cylRadius = new double[nTubeSections+1];
        kCoeff = new double[nTubeSections+1]; //reflections coefficients
        for(int i=0;i<=nTubeSections;i++) {
            cylRadius[i] = 1;
            li[i]=lo[i]=gi[i]=go[i]=0;
            kCoeff[i]=0;
        }

        computeKCoeff();
    }

    /** Compute low level filter values from geometry */
    protected void computeKCoeff() {
        kCoeff[0]=1.0; //Zgl=0
        for(int i=1;i<nTubeSections;i++) {
            kCoeff[i] = (cylRadius[i]*cylRadius[i]-cylRadius[i-1]*
                         cylRadius[i-1])/(cylRadius[i]*cylRadius[i]+cylRadius[i-1]*cylRadius[i-1]);
        }
        kCoeff[nTubeSections]=1.0; //Zl=inf
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

    /** Set damping coeff. (1 == no damping)
        @param val damping coefficient
    */
    public void setDampingCoeff(double val) {
        dampingCoeff = val;
    }

    /** Clear filter of past history */
    public void resetFilter() {
        for(int i=0;i<=nTubeSections;i++) {
            li[i]=lo[i]=gi[i]=go[i]=0;
        }
    }

    /** Set the glottal reflection coeff. 
        @param val glottal reflection coefficient
    */
    public void setGlottalReflectionCoeff(double val) {
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
        for (int k=0;k<nsamples;k++) {
			//Input into system
			li[0]=input[k]/2.0;
			//Calculate all reflections
			for (int i=nTubeSections;i>=0;i--)
			{
		  		//to lips
		  		lo[i]=dampingCoeff*((1+kCoeff[i])*li[i]+kCoeff[i]*gi[i]);
		  		//to glottis
		  		go[i]=dampingCoeff*((1-kCoeff[i])*gi[i]-kCoeff[i]*li[i]);
		  		//To glottis without delay!
		  		if(i>1)
		  		{
		  			gi[i-1]=dampingCoeff*go[i];
		  		}
			}
			//calculate delays towards lips
			for (int i=0;i<nTubeSections;i++) {
				li[i+1]=dampingCoeff*lo[i];
			}
			//Lip output
			output[k]=(float)lo[nTubeSections];
		}
    }
}
