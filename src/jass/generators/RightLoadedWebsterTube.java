package jass.generators;
import jass.engine.*;
import jass.utils.*;

/**
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
   
   Implement  Webster equation  with  open left  end  where velocity  is
   prescribed (glottal  source) and right  side terminated in  baffle in
   infinite plane.

   Physical quantities  (capitals) are  P (pressure) RHO  (density) PHO0
   (equilibrium density) U velocity, F  force/volume on fluid, S area of
   tube. c is sound speed. L length of tube [0 L].

   Dimensionless quantities u,p, and f of dimension 1/m^2 are introduced:

   RHO=RHO0*(1+p)
   U = c*u
   f = F/(c*RHO0)

   To translate between these and Peter Birkholz's varaibles (Interspeech 04 and thesis):
   (S = area, C = circumferencem, h = segment length).
   
   Peter               Kees

   u/(cS)              u
   p/(rho c^2)         p
   2*S*R_i/(h*rho)     d(S)
   u_w                 h*C*z
   u_{N+1}             cS(L)v
   u_{N+2}             cS(L)w
   
   Continuum eq.  are:
   
   du/dt  + c*dp/dx + (dWall*c/sqrt(S))*u - (dSecond*c/sqrt(S))* d^2u/dx^2 = f(x,t) [1a]
   d(Sp)/dt + c*d(Su)/dx = - C(x)*z                                                      [1b]
   M dz/dt + Bz + Ky = p                                                            [1c]
   dy/dt = z                                                                        [1d]
   u(0) = ug (given source: glottal velocity)                                       [1e]
   u(L) = v.                                                                        [1f]

   where C(x) is the circumference. In the paper AscherDoel07 we call
   (dWall*c/sqrt(S))=d(S).
   
   v satisfies eq. for radiation impedance:
   
   v = dw/dt*L_R/R_R + w,                  [2a]
   dw/dt = rho*c/(L_R*S_end) * p(L)        [2b]

   ( Birkholz has this in the form where on LHS of [2b] there are extra terms
   h*rho/(2*L_R*S) dv/dt + h*rho*d(S)/(2*L_R*S)  v )
   
   where (c.f. Birkholz Interspeech 04)
   L_R = 8 rho/(3pi sqrt(pi S_end)
   R_R = 128rho c/(9 pi^2 S_end).

   M = Mw/(rhoc^2)   B = Bw/(rhoc^2)   K = Kw/(rhoc^2) with
   Mw=21, Bw=8000, Kw=845000 (see Birkholz 04).
   
   In the code w is called u_N2 and v is pu[N-1] (i.e., last element in array).
   dWall is an attenuation factor, as is dSecond.
   
   Discretization with staggered grid, u on  even points, p on odd, S on
   both. Grid of N points x =  i*h, N odd, grid space h. The wall vibration
   parameters z,y live on the same nodes as pressure.
   
   CFL condition  is: eta = c*dt/2h <  1. Or h =  c*dt/(2*eta). We would
   expect the  cutoff freq. to be  (eta/2)*Fs with Fs  sampling rate. In
   practice the cutoff is lower, about (eta/2.5) * Fs. (Why?)

   We determine N from the minimum  L we want to simulate, then increase
   h when  appropriate for  longer tubes. To  get freq. up  to (roughly)
   srate/2 resolved we have to oversample with a factor 2.
   
   Grid state vector x(i) i = 0,...,N-1: (NB N is odd)
   
   x(i) = u(i*h) i=0,2,...,N-1
   x(i) = p(i*h) i=1,3,...,N-2

   0  1  2  3  4  5  6 (N=7,NNasal=5)
   u  p  u  p  u  p  u
   uN[0]
   pN[1]
   uN[2]
   pN[3]
   uN[4]
   
   Discretize Eq.  1a-e using symplectic  Euler, i.e., update  u first,
   then update p in terms of the new u's. Note that the last u is really the
   u outside the lips so the actual length of the tract is up to the last pressure.

   Nasal tract id coupled at a  pressure pivot point, so the nasal tract
   has an  odd number NNasal  of grid points  starting with a  velocity point
   coupled to the pivot pressure. So if the area SNasal[0] (leftmost) is
   zero the nasal tract decouples.

   Theoretically we need a damping dW(om) * c/sqrt(S) with freq. dependent dW
   given by (see Birkholz 04) dW(om)=sqrt[pi * mu*om/(rho c^2)]. We can approximate
   by dWall*c/sqrt(S) * u - dSecond*c/sqrt(S) * u_xx and chose dWall and dSecond
   to match as closely as possible. So make them equal at om1 and om2.
   This results in

   dSecond = [dW(om2)-dW(om1)]/[(om2/c)^2-(om1/c)^2]
   dWall = dW(om1) - dSecond*(om1/c)^2
   

*/

public class RightLoadedWebsterTube implements Filter, TwoMassModel.PressureServer {
    /** Sampling rate in Hertz. */
    protected float srate;
    protected double minLen; // minimum length of vocal tract
    protected double minLenNasal; // minimum length of nasal tract
    protected double c = 350; // speed of sound
    double rho = 1.14; // mass density of air
    // Wall model parameters
    protected double wallPressureCoupling = 0.1;
    protected double MWall = 21/(rho*c*c);
    protected double BWall = 8000/(rho*c*c);
    protected double KWall = 845000/(rho*c*c);

    protected double muVisc = .0000186; // coeff of viscosity
    public double om1=250*2*Math.PI,om2 = 2000*2*Math.PI; // for matching theoretical sqrt(omega) damping
    protected double len; // len of vocal tract must be >= minLen
    protected double lenNasal; // len of vnasal tract must be >= minLenNasal
    public double velumNasal = 0.; // 0 for closed nasal tract, .5 both open, 1 only nasal tract
    protected double h,hMin; // grid separations for vocal tract
    protected double hNasal,hMinNasal; // grid separations for nasal tract
    protected int N; // grid points vocal tract
    protected int NNasal; // grid points nasal tract
    public double M=0.01,d=1; // mass, damping at lip
    public double lipAreaMultiplier=1; // mult. lip area with a fudge factor
    public double multM=1,multD=1; // multiplies Mass and damping at lip
    // following refer to vocal tract unless prepended by Nasal
    public double dWall = 1.; // wall damping
    public double multDSecond=1; // mult. dSecond parameter
    public double multDWall=1; // mult. dWall parameter
    public double dSecond = 0.000005; // u_xx proportional wall damping
    protected double[] S; // Area of the segments
    protected double[] Sold; // sqrt Area of the segments
    
    protected double[] Snow; // Area of the segments for use inside loop
    protected double[] Sprev; // Area of the segments for use inside loop
    
    protected double[] sqrtS; // sqrt Area of the segments
    protected double[] sqrtSold; // Area of the segments
    protected double[] pu; // velocity u (even) and pressure p (odd) on staggered grid (called x[] in comments)
    protected double[] yWall,zWall; // wall locations and velocity
    protected double u_N2; // auxiliary variable for radiation at mouth (cf. Birkholz)
    protected double u_N2_nose; // auxiliary variable for radiation at nose (cf. Birkholz)
    protected double[] pu_old; // old state variables
    protected double[] pu_noise; // last moise term fo1r pressure noise source for low-passing
    protected double[] aa,bb,cc,dd; // for Thomas algorithm
    protected int nn; // for Thomas algorithm (size of above arrays)
    protected int nnNasal; // for Thomas algorithm (size of above arrays)
    protected TubeShape tubeShape; // tubeshape of vocal tract
    public double dWallNasal = 1.; // wall damping
    protected double[] SNasal; // Area of the segments
    protected double[] SoldNasal; // sqrt Area of the segments
    protected double[] SnowNasal; // Area of the segments inside loop 
    protected double[] SprevNasal; // Area of the segments
    protected double[] sqrtSNasal; // sqrt Area of the segments
    protected double[] sqrtSoldNasal; // Area of the segments
    protected double[] puNasal; // velocity u (even) and pressure p (odd) on staggered grid (called x[] in comments)
    protected double[] yWallNasal,zWallNasal; // nasal wall locations and velocity
    protected TubeShape tubeShapeNasal; // tubeshape of nasal tract

    protected double[] outBuf; // oversampled buffer
    protected double relativeLocationOfNasalTract = .5; // location nasal tract [0 1] is vocal tract from larynx to lip
    protected int iNasal; // odd index of vocal tract grid which is the pressure pivot point coupled to nasal tract
    private boolean isAllocated = false;
    protected int overSamplingFactor = 1;
    public boolean useLipModel=true;
    protected double dt=0;
    protected double eta = 0;
    protected double etaNasal = 0;
    public double mouthNoseBalance=0; //0 for mouth only 1 for nose only
    protected TwoMassModel twoMassModel=null;
    private boolean outputVelocity = false; // for FFT want to dsplay velocity spectrum
    protected boolean twoMassCouplingOn=true;
    protected double CFLNumber = 1/2;
    protected double flowNoiseLevel=1;
    protected double flowNoiseBandwidth=8000;
    protected double flowNoiseFrequency=900;
    ResonFilter resonFilter; // for flow noise

    public void setCFLNumber(double val) {
        CFLNumber = val;
    }

    public double getCFLNumber() {
        return CFLNumber;
    }

    public void setOutputVelocity(boolean val) {
        outputVelocity = val;
    }

    public void setOm1(double val) {
        this.om1 = val;
        computeDampingPars();
    }

    public void setOm2(double val) {
        this.om2 = val;
        computeDampingPars();
    }

    public void setWallPressureCoupling(double val) {
        wallPressureCoupling = val;
    }

    public double getWallPressureCoupling() {
        return wallPressureCoupling;
    }
    
    protected void computeDampingPars() {
        /*
          double dW1 = Math.sqrt(2*Math.PI*muVisc*om1/(rho*c*c));
          double dW2 = Math.sqrt(2*Math.PI*muVisc*om2/(rho*c*c));
          dSecond = (dW2-dW1)/((om2/c)*(om2/c) - (om1/c)*(om1/c));
          dWall = dW1 - dSecond*(om1/c)*(om2/c);
        */
        double den = (om2*om2-om1*om1)/(c*c);
        double d = (Math.sqrt(om1/c)*(om2*om2)/(c*c)-Math.sqrt(om2/c)*(om1*om1)/(c*c))/den;
        double d2 = (Math.sqrt(om2/c)-Math.sqrt(om1/c))/den;
        dWall = d*Math.sqrt(2*Math.PI*muVisc/(rho*c));
        dSecond = d2*Math.sqrt(2*Math.PI*muVisc/(rho*c));
        //System.out.println("dW="+dWall*c+" dSecond = "+dSecond*c);
        // values below are tuned on the Fant bandwidths
        dWall = 4*(1/CFLNumber)*dWall;
        dSecond = 4*(1/CFLNumber)*dSecond;
    }

    public boolean getOutputVelocity() {
        return outputVelocity;
    }

    public void allocate() {
        eta = CFLNumber;
        h = c/(2*srate*overSamplingFactor*eta);
        N = 1 + (int)(minLen/h) + 1;
        if(N%2 == 0) {
            N--;
        }
        System.out.println(N+" "+h);
        // since the tube really ends at the last velocity
        // effectively there are N-1 sections  
        // u -- p -- u -- p -- u(N-1) [-- "p_outside__ ]
        h = minLen/(N-1);
        hMin = h;
        len = minLen;

        etaNasal = CFLNumber;
        hNasal = c/(2*srate*overSamplingFactor*etaNasal);
        NNasal = 1 + (int)(minLenNasal/hNasal);
        if(NNasal%2 == 0) {
            NNasal--;
        }
        //System.out.println(NNasal);
        hNasal = minLenNasal/(NNasal-1);
        hMinNasal = hNasal;
        lenNasal = minLenNasal;

        // Li = h*i = relativeLocationOfNasalTract*minLen, i odd (is pressure)
        iNasal = (int)Math.round((relativeLocationOfNasalTract*minLen/h -1)/2);
        iNasal = 2*iNasal+1;

        //        System.out.println("iNasal="+iNasal);
        
        S = new double[N];
        Sold = new double[N];
        Snow = new double[N]; // no need to init these
        Sprev = new double[N]; // no need to init these
        sqrtS = new double[N];
        sqrtSold = new double[N];
        pu = new double[N]; // p on even points, u on odd
        yWall = new double[N]; // need only the even points
        zWall = new double[N];
        pu_old = new double[N]; // p on even points, u on odd
        pu_noise = new double[N]; // last generated noise value, for low-passing
        for(int i=0;i<N;i++) {
            S[i] = 1;
            Sold[i] = 1;
            sqrtS[i] = 1;
            sqrtSold[i] = 1;
            pu[i]=0;
            yWall[i] = zWall[i] = 0;
            pu_old[i]=0;
            pu_noise[i] = 0;
        }

        SNasal = new double[NNasal];
        SoldNasal = new double[NNasal];
        SnowNasal = new double[NNasal];
        SprevNasal = new double[NNasal];
        sqrtSNasal = new double[NNasal];
        sqrtSoldNasal = new double[NNasal];
        puNasal = new double[NNasal]; // p on even points, u on odd
        yWallNasal = new double[NNasal];
        zWallNasal = new double[NNasal];
        for(int i=0;i<NNasal;i++) {
            SNasal[i] = 1;
            SoldNasal[i] = 1;
            sqrtSNasal[i] = 1;
            sqrtSoldNasal[i] = 1;
            puNasal[i]=0;
            yWallNasal[i] = zWallNasal[i] = 0;
        }
        u_N2 = u_N2_nose = 0;
        // scratch variables for Thomas algorithm on velocity
        this.nnNasal = (NNasal-3)/2;
        this.nn = (N-3)/2;
        int sz = nn>nnNasal ? nn:NNasal;
        aa = new double[sz];
        bb = new double[sz];
        cc = new double[sz];
        dd = new double[sz];
        outBuf = new double[1024*overSamplingFactor];
        isAllocated = true;
        reset();
    }

    public TwoMassModel getTwoMassModel() {
        return twoMassModel;
    }

    public void setTwoMassModel(TwoMassModel twoMassModel) {
        this.twoMassModel = twoMassModel;
        twoMassModel.setPressureServer(this);
    }

    public synchronized void changeTubeModel() {
        double minS=1.e10,maxS=-1;
        if(isAllocated) {
            dt = 1/(overSamplingFactor*srate);
            double len = tubeShape.getLength();
            //System.out.println("len="+len);
            if(len>=minLen) {
                h = hMin * len/minLen;
                for(int k=0;k<N;k++) {
                    double x = k*h;
                    double r = tubeShape.getRadius(x);
                    S[k] = Math.PI*r*r;
                    sqrtS[k] = Math.sqrt(Math.PI*r*r);
                    //System.out.println("s("+x+")="+S[k]);
                    if(S[k]<minS) {
                        minS = S[k];
                    }
                    if(S[k]>maxS) {
                        maxS = S[k];
                    }
                }
                eta = dt*c/(2*h);
                //System.out.println(eta);
            } else {
                System.out.println("VT tube too short");
            }

            len = tubeShapeNasal.getLength();
            if(len>=minLenNasal) {
                hNasal = hMinNasal * len/minLenNasal;
                for(int k=0;k<NNasal;k++) {
                    double x = k*hNasal;
                    double r = tubeShapeNasal.getRadius(x);
                    SNasal[k] = Math.PI*r*r;
                    sqrtSNasal[k] = Math.sqrt(Math.PI*r*r);
                    //System.out.println("s("+x+")="+SNasal[k]);
                }
                etaNasal = dt*c/(2*hNasal);
                //System.out.println(etaNasal);
            } else {
                System.out.println("nasal tube too short");
            }
            //System.out.println(minS+" "+maxS);
        }
    }

    
    public void reset() {
        u_N2 = u_N2_nose = 0;
        for(int i=0;i<N;i++) {
            pu[i]=0;
            yWall[i] = zWall[i] = 0;
        }
        for(int i=0;i<N;i++) {
            Sold[i] = S[i];
            sqrtSold[i] = sqrtS[i];
        }

        for(int i=0;i<NNasal;i++) {
            puNasal[i]=0;
            yWallNasal[i] = zWallNasal[i] = 0;
        }
        for(int i=0;i<NNasal;i++) {
            SoldNasal[i] = SNasal[i];
            sqrtSoldNasal[i] = sqrtSNasal[i];
        }
        computeDampingPars();
    }

    public RightLoadedWebsterTube(float srate, TubeShape tm, double minLen) {
        this.minLen = minLen;
        this.srate = srate;
        this.tubeShape = tm;
        this.minLenNasal = minLen;
        this.tubeShapeNasal = tm;
        allocate();
        resonFilter = new ResonFilter((float)srate);
        updateFlowFilter();
    }

    public RightLoadedWebsterTube(float srate, TubeShape tm, double minLen, TubeShape tmNasal, double minLenNasal) {
        this.minLen = minLen;
        this.srate = srate;
        this.tubeShape = tm;
        this.minLenNasal = minLenNasal;
        this.srate = srate;
        this.tubeShapeNasal = tmNasal;
        allocate();
        resonFilter = new ResonFilter((float)srate);
        updateFlowFilter();
    }

    public RightLoadedWebsterTube(float srate, TubeShape tm, double minLen, TubeShape tmNasal, double minLenNasal,double cflNumber) {
        this.CFLNumber = cflNumber;
        this.minLen = minLen;
        this.srate = srate;
        this.tubeShape = tm;
        this.minLenNasal = minLenNasal;
        this.srate = srate;
        this.tubeShapeNasal = tmNasal;
        allocate();
        resonFilter = new ResonFilter((float)srate);
        updateFlowFilter();
    }

    public void setFlowNoiseLevel(double v) {
        flowNoiseLevel = v;
        updateFlowFilter();
    }

    public double getFlowNoiseLevel() {
        return flowNoiseLevel;
    }

    public void setFlowNoiseFrequency(double v) {
        flowNoiseFrequency = v;
        updateFlowFilter();
    }

    public double getFlowNoiseFrequency() {
        return flowNoiseFrequency;
    }

    public void setFlowNoiseBandwidth(double v) {
        flowNoiseBandwidth = v;
        updateFlowFilter();
    }

    public double getFlowNoiseBandwidth() {
        return flowNoiseBandwidth;
    }

    protected void updateFlowFilter() {
        resonFilter.setResonCoeff((float)flowNoiseFrequency,(float)flowNoiseBandwidth,(float)flowNoiseLevel);
    }

    /**
       Implement TwoMassModel.PressureServer
       @ return pressure at left end
    */
    public double getPressure() {
        if(twoMassCouplingOn) {
            return pu[1];
        } else {
            return 0;
        }
    }
    
    /**
       Implement TwoMassModel.PressureServer
       @ return area  at left end
    */
    public double getA1() {
        //System.out.println(S[0]);
        return S[0];
    }

    /** Proces input (may be same as output). Implements Filter interface
        @param output user provided buffer for returned result.
        @param input user provided input buffer.
        @param nsamples number of samples written to output buffer.
        @param inputOffset where to start in circular buffer input (unused)
    */
    synchronized public void  filter(float [] output, float[] input, int nsamples, int inputOffset) {
        filterIMEX(output,input, nsamples, inputOffset);

    }
    
    private float last_input=0;
    private double newU=0,lastU=0;
    private boolean useLocalPressure=false; // use pressure near lip or just diff. velocity if false

    private int filterIMEXCallCounter =0;
    /**
       Uses IMEX Euler as in paper with Uri Ascher
    */
    public void filterIMEX(float [] output, float[] input, int nsamples, int inputOffset) {
        if(twoMassModel!=null) {
            twoMassModel.vars.setVars(); // set begin and end values of interpolated parameteres here
        }
        if(filterIMEXCallCounter<10) {
            filterIMEXCallCounter++;
        }
        
        float[] f = input; // force
        int bufsz = nsamples*overSamplingFactor;
        if(outBuf.length<bufsz) {
            outBuf = new double[bufsz];                           
        }
        // Renolds numbers for noise generation.
        double r_const=4.78e9; // Re^2 = r_const * S * u^2;
        double Rec2 = 3500*3500; // from Schroeder&Sondhi
        double gng = 1.e-4; // gain for noise generation from Schroeder&Sondhi
        if(twoMassModel!=null) {
            r_const = 4*twoMassModel.getVars().rho*twoMassModel.getVars().rho/
                (Math.PI*twoMassModel.getVars().mu*twoMassModel.getVars().mu);
            rho = twoMassModel.getVars().rho;
        } 
        
        /*  Area at time indexed  by k is given by (1-spar)S_old
            + spar  * S where spar  = (k+1)/bufsz; (spar  = s_now or
            s_prev). S_old  is value  at begin of  buffer, S  is new
            value at end of buffer  */
        
        for(int k=0;k<bufsz;k++) {
            double s_now = (k+1.)/bufsz; // runs from 0 -1 over sample buffer to interpolate
            double s_prev = k/((double)bufsz);

            for(int i=0;i<N;i++) {
                Snow[i] = (1-s_now)*Sold[i] + s_now*S[i];
                Sprev[i] = (1-s_prev)*Sold[i] + s_prev*S[i];
            }


            for(int i=0;i<NNasal;i++) {
                SnowNasal[i] = (1-s_now)*SoldNasal[i] + s_now*SNasal[i];
                SprevNasal[i] = (1-s_prev)*SoldNasal[i] + s_prev*SNasal[i];
            }
                        
            double S_now_right=0;
            double S_now_left=0;
            double S_now=0;
            double S_prev=0;
            double S_prev_left=0;
            double S_prev_right=0;
            double S_now_4pt;
            double S_prev_4pt;
            double sqrtS_now=0;
            double zWall4pt;

            double pnoise = 0,toadd=0;

            //pressures in VT
            for(int i=1;i<=N-2;i+=2) {
                S_now = Snow[i];
                S_prev = Sprev[i];
                S_prev_right = /*(i==N-2?lipAreaMultiplier:1)**/Sprev[i+1];
                S_prev_left = Sprev[i-1];
                S_now_right = /*(i==N-2?lipAreaMultiplier:1)**/Snow[i+1];
                S_now_left = Snow[i-1];
                S_now_4pt = (2*S_now+S_now_left+S_now_right)/4;
                S_prev_4pt = (2*S_prev+S_prev_left+S_prev_right)/4;
                if(i==iNasal) {
                    pu[i] =  (S_prev_4pt/S_now_4pt)*pu[i]-eta*(S_now_right*pu[i+1] - S_now_left*pu[i-1])/S_now_4pt
                        -etaNasal*velumNasal*SNasal[0]*puNasal[0]/S_now_4pt;
                } else {
                    pu[i] =  (S_prev_4pt/S_now_4pt)*pu[i]-eta*(S_now_right*pu[i+1] - S_now_left*pu[i-1])/S_now_4pt;
                }
                // interpolate neighbors as z defined only on pressure nodes
                zWall4pt = (2*Math.sqrt(S_now)*zWall[i] + Math.sqrt(S_now_right)*(zWall[i]+ (i<N-2 ? zWall[i+2]:zWall[i]))/2
                            + Math.sqrt(S_now_left)*(zWall[i]+ (i>1 ? zWall[i-2]:zWall[i]))/2)/4;
                pu[i] =  pu[i] - (S_now-S_prev)/S_now_4pt - dt* zWall4pt * 2*Math.sqrt(Math.PI)/S_now_4pt;
            }
            //pressures in NT
            for(int i=1;i<=NNasal-2;i+=2) {
                S_now = SnowNasal[i];
                S_prev = SprevNasal[i];
                S_prev_right = SprevNasal[i+1];
                S_prev_left = SprevNasal[i-1];
                S_now_right = SnowNasal[i+1];
                S_now_left = SnowNasal[i-1];
                S_now_4pt = (2*S_now+S_now_left+S_now_right)/4;
                S_prev_4pt = (2*S_prev+S_prev_left+S_prev_right)/4;
                puNasal[i] =  (S_prev_4pt/S_now_4pt)*puNasal[i]-etaNasal*
                    (S_now_right*puNasal[i+1] - S_now_left*puNasal[i-1])/S_now_4pt;
                zWall4pt = (2*Math.sqrt(S_now)*zWallNasal[i] + Math.sqrt(S_now_right)*(zWallNasal[i]+(i<(NNasal-2) ? zWallNasal[i+2]:zWallNasal[i]))/2
                            + Math.sqrt(S_now_left)*(zWallNasal[i]+ (i>1 ? zWallNasal[i-2]:zWallNasal[i]))/2)/4;
                puNasal[i] =  puNasal[i] - (S_now-S_prev)/S_now_4pt - dt*zWall4pt* 2*Math.sqrt(Math.PI)/S_now_4pt;
            }

            // wall parameters VT
            if(wallPressureCoupling>1.e-6) { // to prevent underflow don't update if turned off
                for(int i=1;i<=N-2;i+=2) {
                    yWall[i] += dt * zWall[i];
                    zWall[i] = (MWall*zWall[i] + wallPressureCoupling*dt*pu[i] - dt*KWall*yWall[i])/(MWall+dt*BWall);
                    //zWall[i] = zWall[i]*(1- dt*BWall/MWall)+ (wallPressureCoupling*dt*pu[i] - dt*KWall*yWall[i])/MWall;
                }
                // wall parameters Nasal tract
                for(int i=1;i<=NNasal-2;i+=2) {
                    yWallNasal[i] += dt * zWallNasal[i];
                    zWallNasal[i] = (MWall*zWallNasal[i] + wallPressureCoupling*dt*puNasal[i]-dt*KWall*yWallNasal[i])/(MWall+dt*BWall);
                }
            }

            // velocities VT. Interpolate input (boundary condition on pu[0]) if oversampling
            // save old velocities
            for(int i=0;i<=N-2;i+=2) {
                pu_old[i] = pu[i];
            }
            int k_int = k/overSamplingFactor;
            double k_fract = ((double)k)/overSamplingFactor - k_int;
            pu[0] = 0;
            // if using TwoMassModel we get the glottal velocity from there
            // Note oversamplingfactor has to be 1 then!
            if(twoMassModel!=null) {
                double lambda = k/((double)bufsz); // to interpolate 2mass model parameters
                twoMassModel.advance(lambda);
                pu[0] = twoMassModel.ug/Snow[0];
            }
            // add the input to the filter as ug
            if(k_int ==0) { // oversampling
                pu[0] += (1-k_fract)*last_input + k_fract*f[k_int];
            } else { // not oversampling (a MUST for TwoMassModel!)
                pu[0] += (1-k_fract)*f[k_int-1] + k_fract*f[k_int];
            }
            
            //use splitting below to deal with (possibly large) damping
            for(int i=2;i<=N-3;i+=2) {
                S_now = Snow[i];
                S_prev = Sprev[i];
                S_prev_right = Sprev[i+1];
                S_prev_left = Sprev[i-1];
                S_now_right = Snow[i+1];
                S_now_left = Snow[i-1];
                double u_old_factor = (S_prev*(1/S_prev_left+1/S_prev_right)/2 + 1/2);
                double u_new_factor = (S_now*(1/S_now_left+1/S_now_right)/2 + 1/2);

                pu[i] = (u_old_factor/u_new_factor)*pu[i] - eta*(pu[i+1] - pu[i-1])/u_new_factor;
            }

            //pu[N-1] = pu[N-1] - eta*(0 - pu[N-2]);
            
            double smallest_S = 1.e40;
            int narrowest_i = -1;
            for(int i=2;i<=N-3;i+=2) {
                S_now = Snow[i];
                if(S_now < smallest_S) {
                    smallest_S = S_now;
                    narrowest_i = i;
                }
            }

            /*
              Implicit step for pu[i] is now for i= 2,4,...,N-3
              q1_i * pu[i] + q2_iL*pu[i-2]+q2_iR*pu[i-2] = u_new_f[i]*pu_old[i]   [eq 3]
              where pu[0] and pu[N-1] can be considered given. u_new and u_old are functions of the area function
              as given above.

              Define T_i = .5/sqrt(S_i)+.25*S_i*[1/(S_i-left)^1.5+1/(S_i+right)^1.5]
              q1_i = [u_new_f[i] +dt*c*dWall*T_i + (dSecond*eta/h)  * T_i]
              Define R_i = .5/(S_j)^1.5+.25/(S_j+2)^1.5+.25/(S_j-2)^1.5
              q2_iL = -[dSecond*eta/(2*h)] * R_i * S_j-2
              q2_iR = -[dSecond*eta/(2*h)] * R_i * S_j+2

              Let us now stuff pu-array elements 2,4,...,N-3 in a vector xx[i] i=0,...,nn-1 with nn = (N-3)/2,
              so we have xx[k/2-1] = pu[k] for k = 2,4,...,N-3,
              and pu[2*(i+1)] = x[i] for i = 0,...,nn-1.

              Now write eq. [3] in the form

              aa(i)xx(i-1)  + bb(i)xx(i) + cc(i)xx(i+1) = dd(i),    i = 0,..,nn-1
              aa(0) = 0 and cc(nn-1) = 0. (Note that xx(-1) and xx(nn) are unused.)

              We have

              dd(i) = pu_old[2*(i+1)]*u_new_f[2*(i+1)] for i=1,...,nn-2
              dd(0) = pu_old[2]*u_new_f[2] - q2_2L * pu[0]
              dd(nn-1) = pu_old[N-3]*u_old_f[N-3] // (- q2_{N-3} * pu[N-1] * u_new_f[N-1] --> leave out)
              bb(i) = q1_i        i=0,...,nn-1
              aa(i) = q2_iL
              cc(i) = q2_iR except
              aa(0) = cc(nn-1) = 0.
       
            */
            double hagenPossseuilleFactor = 1; // for alternative damping model
            for(int kk=0;kk<nn;kk++) {
                int i = 2*(kk+1); // index in usual pu[] array
                // hagenPossseuilleFactor = 1/(10*Snow[i]); uncommnet for Hagen-Poseuille model for damping
                double u_old_factor = (Sprev[i]*(1/Sprev[i-1]+1/Sprev[i+1])/2 + 1/2);
                double u_new_factor = (Snow[i]*(1/Snow[i-1]+1/Snow[i+1])/2 + 1/2);
                double T_i = .5/Math.sqrt(Snow[i]) + .25*Snow[i]*
                    (1/(Snow[i-2]*Math.sqrt(Snow[i-2]))+1/(Snow[i+2]*Math.sqrt(Snow[i+2])) );
                double R_i = .5/(Snow[i]*Math.sqrt(Snow[i])) + .25*
                    (1/(Snow[i-2]*Math.sqrt(Snow[i-2]))+1/(Snow[i+2]*Math.sqrt(Snow[i+2]))) ;

                //sqrtS_now = (1-s_now)*sqrtSold[i] + s_now*sqrtS[i];
                dd[kk] = pu[2*(kk+1)] * u_new_factor;
                aa[kk] = -(hagenPossseuilleFactor*dSecond*multDSecond*eta/(2*h)) * R_i*Snow[i-2];
                cc[kk] = -(hagenPossseuilleFactor*dSecond*multDSecond*eta/(2*h)) * R_i*Snow[i+2];
                bb[kk] = u_new_factor + (dt*c*hagenPossseuilleFactor*dWall*multDWall+
                                         dSecond*hagenPossseuilleFactor*multDSecond*eta/h)*T_i;
            }
            //sqrtS_now = (1-s_now)*sqrtSold[2] + s_now*sqrtS[2];
            //dd[0] -= -(dSecond*multDSecond*eta/(2*h*sqrtS_now ))*pu[2];
            double R_2 = .5/(Snow[2]*Math.sqrt(Snow[2])) + .25*
                (1/(Snow[2-2]*Math.sqrt(Snow[2-2]))+1/(Snow[2+2]*Math.sqrt(Snow[2+2]))) ;
            dd[0] -= -(hagenPossseuilleFactor*dSecond*multDSecond*eta/(2*h)) * R_2*Snow[2-2] * pu[0];
            //sqrtS_now = (1-s_now)*sqrtSold[N-3] + s_now*sqrtS[N-3];
            //dd[nn-1] -=  -(dSecond*multDSecond*eta/(h*sqrtS_now ))*pu[N-1];
            aa[0]=0; // actually unused...
            cc[nn-1]=0; // actually unused...
            ThomasAlg.thomas(aa,bb,cc,dd,nn);// now dd holds xx
            for(int kk=0;kk<nn;kk++) {
                pu[2*(kk+1)] = dd[kk];
            }
            
            // add noise as in Sondhi-Schroeder
            double noise_toadd = 0;
            double uc = (pu[narrowest_i]+pu_old[narrowest_i])/2;
            double Re2 = r_const*smallest_S * uc*uc;
            if(Re2>Rec2) {
                double Rn = .5* rho*Math.abs(uc)/smallest_S;
                float rrr = resonFilter.filter1Sample((float)(Math.random()-.5));
                pnoise = gng*rrr*(Re2-Rec2)/Rn;
            } else {
                pnoise = 0;
            }
            noise_toadd = pnoise;
            pu[narrowest_i] += noise_toadd;
                
            //kees wrong? sqrtS_now = (1-s_now)*sqrtSold[N-1] + s_now*sqrtS[N-1];
            sqrtS_now = Math.sqrt(Snow[N-1]);
            if(useLipModel) {
                u_N2 = u_N2 + dt*c*3*Math.PI*Math.sqrt(Math.PI)*pu[N-2]/(8*Math.sqrt(lipAreaMultiplier)*sqrtS_now);
                pu[N-1] = u_N2 + (9*Math.PI*Math.PI/128)*pu[N-2];
                //pu[N-1] = u_N2 + (9*Math.PI*Math.PI/128)*pu[N-2] - eta*( - pu[N-2]);
                //pu[N-1] = pu[N-1]/(1+dt*dWall*multDWall*c/sqrtS_now);
            } else {
                pu[N-1] = pu[N-1] - eta*( - pu[N-2]);
                pu[N-1] = pu[N-1]/(1+dt*dWall*multDWall*c/sqrtS_now);
            }

            if(useLocalPressure) {
                outBuf[k] = (1-mouthNoseBalance)*pu[N-2]*Snow[N-1]; // N-2 for pressure
            } else {
                outBuf[k] = (1-mouthNoseBalance)*pu[N-1]*Snow[N-1]; // N-1 for velocity
            }
            // velocities NT. 

            //use splitting to deal with (possibly large) damping

            puNasal[0] = puNasal[0] - etaNasal*(puNasal[0+1] - velumNasal*pu[iNasal]);
            sqrtS_now = (1-s_now)*sqrtSoldNasal[0] + s_now*sqrtSNasal[0];
            puNasal[0] = puNasal[0]/(1+dt*dWall*multDWall*c/sqrtS_now);
            
            for(int i=2;i<=NNasal-3;i+=2) {
                S_now = SnowNasal[i];
                S_prev = SprevNasal[i];
                S_prev_right = SprevNasal[i+1];
                S_prev_left = SprevNasal[i-1];
                S_now_right = SnowNasal[i+1];
                S_now_left = SnowNasal[i-1];
                double u_old_factor = 1;
                double u_new_factor = 1;
                //for bizarre bug....
                /*
                  if(filterIMEXCallCounter>2) {
                  u_old_factor = (S_prev*(1/S_prev_left+1/S_prev_right)/2 + 1/2);
                  u_new_factor = (S_now*(1/S_now_left+1/S_now_right)/2 + 1/2);
                  }
                */
                //System.out.println(u_new_factor);

                puNasal[i] = (u_old_factor/u_new_factor)*puNasal[i] -
                    etaNasal*(puNasal[i+1] - puNasal[i-1])/u_new_factor;
            }

            //////////////////////////////////////////////////////////////////////////
            // Nasal tract losses
            for(int kk=0;kk<nnNasal;kk++) {
                sqrtS_now = (1-s_now)*sqrtSoldNasal[2*(kk+1)] + s_now*sqrtSNasal[2*(kk+1)];
                dd[kk] = puNasal[2*(kk+1)];
                aa[kk] = cc[kk] = -dSecond*multDSecond*eta/(2*h*sqrtS_now);
                bb[kk] = 1+ (dt*c*dWall*multDWall+dSecond*multDSecond*eta/h)/sqrtS_now;
            }
            sqrtS_now = (1-s_now)*sqrtSoldNasal[2] + s_now*sqrtSNasal[2];
            dd[0] -= -(dSecond*multDSecond*eta/(2*h*sqrtS_now ))*puNasal[2];
            sqrtS_now = (1-s_now)*sqrtSoldNasal[NNasal-3] + s_now*sqrtSNasal[NNasal-3];
            //dd[nn-1] -=  -(dSecond*multDSecond*eta/(h*sqrtS_now ))*puNasal[NNasal-1];
            aa[0]=0; // actually unused...
            cc[nnNasal-1]=0; // actually unused...
            ThomasAlg.thomas(aa,bb,cc,dd,nnNasal);// now dd holds xx
            for(int kk=0;kk<nnNasal;kk++) {
                puNasal[2*(kk+1)] = dd[kk];
            }
            ///////////////////////////////////////////////////////////////////////////
            
            sqrtS_now = (1-s_now)*sqrtSoldNasal[NNasal-1] + s_now*sqrtSNasal[NNasal-1];
            if(useLipModel) {
                u_N2_nose = u_N2_nose + dt*c*3*Math.PI*Math.sqrt(Math.PI)*puNasal[NNasal-2]/(8*sqrtS_now);
                puNasal[NNasal-1] = u_N2_nose + (9*Math.PI*Math.PI/128)*puNasal[NNasal-2];
                
            } else {
                puNasal[NNasal-1] = puNasal[NNasal-1] - etaNasal*( - puNasal[NNasal-2]) - dt*dWall*multDWall*c*puNasal[NNasal-1]/sqrtS_now;
                puNasal[NNasal-1] = puNasal[NNasal-1]/(1+dt*dWall*multDWall*c/sqrtS_now);
            }

            if(useLocalPressure) {
                outBuf[k] += (mouthNoseBalance)*puNasal[NNasal-2]*SNasal[NNasal-1]; // N-2 for pressure
            } else {
                outBuf[k] += (mouthNoseBalance)*puNasal[NNasal-1]*SNasal[NNasal-1]; // N-1 for velocity
                // differentiate wrt time to get pressure
                if(!outputVelocity) {
                    newU = outBuf[k];
                    outBuf[k] = (newU-lastU)*srate;
                    lastU = newU;
                }
            }
            //System.out.println(outBuf[k]);
        }

        for(int i=0;i<N;i++) {
            Sold[i] = S[i];
            sqrtSold[i] = sqrtS[i];
        }
        
        for(int i=0;i<NNasal;i++) {
            SoldNasal[i] = SNasal[i];
            sqrtSoldNasal[i] = sqrtSNasal[i];
        }
        // downsample
        bufsz = nsamples;
        double oldAcc=0;
        for(int k=0;k<bufsz;k++) {
            double acc=0;
            for(int i=0;i<overSamplingFactor;i++) {
                acc += outBuf[k*overSamplingFactor+i];
            }
            acc /= overSamplingFactor;
            // hack to get rid of clicks
            //if(Math.abs(acc-oldAcc)>1) {
            //System.out.println(acc-oldAcc);
            //    acc=oldAcc;
            //}
            //oldAcc = acc;
            output[k] = (float)acc;
        }

        last_input = f[bufsz-1];
        if(wentUnstable()) {
            System.out.println("Tube solver went unstable");
            reset();
        }
    }

    
    private boolean wentUnstable() {
        
        if(isBad(u_N2)||isBad(u_N2_nose)) {
            return true;
        }
        for(int i=0;i<N;i++) {
            if(isBad(pu[i])||isBad(yWall[i])||isBad(zWall[i])) {
                return true;
            }
        }
        for(int i=0;i<NNasal;i++) {
            if(isBad(puNasal[i])||isBad(yWallNasal[i])||isBad(zWallNasal[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean isBad(double x) {
        if(x>10000 || x<-10000||x==Double.NaN) {
            return true;
        } else {
            return false;
        }
    }
}
