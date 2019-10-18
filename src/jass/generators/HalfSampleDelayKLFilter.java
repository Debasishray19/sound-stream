package jass.generators;

/** Half sample delay Kelly-Lochbaum filter. See
    Master thesis of Siddarth Mathur, Univ. of Arizona 2003
    for details of the algorithm.
    This implementation defines a tube of a certain maximum number of segments.
    the actual end segment depends on the actual length and can be modified at run-time.
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public class HalfSampleDelayKLFilter implements Filter {
    /** Sampling rate in Hertz. */
    protected float srate;
    protected State state;

    public class State {
        public double glottalRefl = 0.99;
        public double lipRefl = -0.99;
        public double dampingCoeff = 1.0; // overall damping in system
        public int nTubeSections; // maximum segments (must be even)
        public int nSectionsUsed; // actual used (may change dynamically)
        
        public double[] area; // Area of the segments
        public double[] f,b; // filter state
    }

    private double[] r; // reflection coefficients (not in State for efficiency)
    
    protected void allocate() {
		state.area = new double[state.nTubeSections];
        r = new double[state.nTubeSections-1];
        state.f = new double[state.nTubeSections];
        state.b = new double[state.nTubeSections];
        for(int i=0;i<state.nTubeSections;i++) {
            state.area[i] = 1;
        }
        computeReflectionCoeff();
    }
    
    /** Clear filter of past history */
    public void resetFilter() {
        for(int i=0;i<state.nTubeSections;i++) {
            state.f[i] = 0;
            state.b[i] = 0;
        }
    }

    /** Set actual number of segments used
        @param end number of segments (so last segment used has index end-1)
    */
    public void setEnd(int end) {
        if(end > state.nTubeSections) {
            state.nSectionsUsed = state.nTubeSections;
        } else if(end > 2) { // do nothing if called with ridiculous value
            if(end != state.nSectionsUsed) {
                state.nSectionsUsed = end;
            }
        }
        computeReflectionCoeff();
    }
    
    /** Compute reflection coefficients from areas */
    protected void computeReflectionCoeff() {
        for(int i=0;i<state.nSectionsUsed-1;i++) {
            r[i] = (state.area[i] - state.area[i+1])/(state.area[i] + state.area[i+1]);
        }
        for(int i=state.nSectionsUsed;i<state.nTubeSections-1;i++) {
            r[i] = 0;
        }
    }

    /** Set the glottal reflection coeff. 
        @param val glottal reflection coefficient
    */
    public void setGlottalReflectionCoeff(double val) {
        state.glottalRefl = val;
    }
    
    /** Set the end (lip) reflection coeff. 
        @param val glottal reflection coefficient (positive, actual coeff is negative)
    */
    public void setLipReflectionCoeff(double val) {
        state.lipRefl = -val;
    }
    
    /** Set damping coeff. (1 == no damping)
        @param val damping coefficient
    */
    public void setDampingCoeff(double val) {
        state.dampingCoeff = val;
    }

    /** Create and initialize.
        @param srate sampling rate in Hertz.
        @param nTubeSection maximum number of sections (must be even)
     */
    public HalfSampleDelayKLFilter(float srate, int nTubeSections) {
        state = new State();
        this.srate = srate;
        state.nTubeSections = 2*nTubeSections; // can double in size: always even
        state.nSectionsUsed = nTubeSections;
        allocate();
        setEnd(state.nSectionsUsed);
        resetFilter();
        System.out.println("nsections="+state.nTubeSections);
        System.out.println("nsectionsUsed="+state.nSectionsUsed);
    }

    public HalfSampleDelayKLFilter() {}

    /** Set an individual segment radius
        @param k index of segment (0,...)
        @param r radius to set
     */
    public void setCylinderRadius(int k,double r) {
        state.area[k]=r*r;
        computeReflectionCoeff();
    }
    
    /** Set all radii
        @param array of r radii 
     */
    public void setAllCylinderRadii(double[] r) {
        for(int k=0;k<state.nSectionsUsed;k++) {
            state.area[k]=r[k]*r[k];
        }
        computeReflectionCoeff();
    }

    /** Proces input (may be same as output). Implements Filter interface
        @param output user provided buffer for returned result.
        @param input user provided input buffer.
        @param nsamples number of samples written to output buffer.
        @param inputOffset where to start in circular buffer input (unused)
    */
    public void filter(float [] output, float[] input, int nsamples, int inputOffset) {
        int nJunctions = state.nTubeSections-1;
        int nJunctionsUsed = state.nSectionsUsed-1;
        if(state.nSectionsUsed%2 == 1) { // odd # sections
            nJunctionsUsed++; // last segment is "virtual": nJunctionsUsed always odd
        }
        double damp = state.dampingCoeff;
        double delta;
        double [] f = state.f;
        double [] b = state.b;
        for (int k=0;k<nsamples;k++) {
			//Input into system
			f[0]=input[k]/state.area[0] + state.glottalRefl*b[0];
			for (int i=1;i<nJunctionsUsed-1;i+=2) {
                delta = r[i]*(f[i] - b[i+1]);
                f[i+1] = damp*(f[i] + delta);
                b[i] = damp*(b[i+1] + delta);
            }
            for (int i=0;i<nJunctionsUsed;i+=2) {
                delta = r[i]*(f[i] - b[i+1]);
                f[i+1] = damp*(f[i] + delta);
                b[i] = damp*(b[i+1] + delta);
            }
            b[state.nSectionsUsed-1] = state.lipRefl*f[state.nSectionsUsed-1];
			output[k]=(float)(f[state.nSectionsUsed-1] + b[state.nSectionsUsed-1]);
            //System.out.println(output[k]);
		}
    }
}
