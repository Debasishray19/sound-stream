package jass.generators;
import jass.engine.*;
import jass.utils.*;
import jass.render.*;
import java.awt.*;

/**
   Ishizaka-Flanagan two-mass model.
   @author Kees van den Doel (kvdoel@cs.ubc.ca)


*/

public class TwoMassModel extends GlottalModel {
    // State variables (ug is in superclass)
    // Mass positions and velocities
    protected double x1=0,x2=0,xold1=0,xold2=0,ugold=0;
    protected double flowNoiseLevel=0;
    protected double flowNoiseBandwidth=100;
    protected double flowNoiseFrequency=300;
    double myf1,myf2;
    protected Vars vars;
    protected PressureServer pressureServer;// get p1 (right pressure) and leftmost area from this contained model
    ResonFilter resonFilter; // for flow noise
    protected int nOverSamplings=4;
    
    public TwoMassModel(int bufferSize, double srate) {
        super(bufferSize,srate);
        vars = new Vars();
	resonFilter = new ResonFilter((float)srate);
	updateFlowFilter();
    }

    public void setPressureServer(PressureServer pressureServer) {
        this.pressureServer = pressureServer;
    }
    
    public PressureServer getPressureServer() {
        return pressureServer;
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

    public double getUg() {
        return ug;
    }
    
    protected void updateP1() {
        if(pressureServer != null) {
            vars.p1 = pressureServer.getPressure();
        }
    }


    public TwoMassModel.Vars getVars() {
        return vars;
    }

    public void setVars(TwoMassModel.Vars vars) {
        this.vars = vars;
    }
    
    // Use eq. 18 from Ishizaka-Flanagan
    protected double computeForceOnMass1() {
        double f1=0;

        if(x1 > -vars.interpolatedAg01/(2*vars.lg) && x2 > -vars.interpolatedAg02/(2*vars.lg)) {
            double Ag1 = vars.interpolatedAg01+2*vars.lg*x1;
            double Rv1 = 12*vars.mu*vars.lg*vars.lg*(vars.d1/(Ag1*Ag1*Ag1));
            double Lg1 = vars.rho*(vars.d1/Ag1);
            f1 = vars.interpolatedPs - 1.37*(vars.rho/2)*(ug/Ag1)*(ug/Ag1)-.5*(Rv1*ug+srate*Lg1*(ug-ugold));
            f1 *= vars.d1*vars.lg;
        } else {
            f1 = vars.interpolatedPs * vars.d1*vars.lg;
        }
        myf1=f1;
        return f1;
    }

    protected double computeForceOnMass2(double f1) {
        double f2=0;
        if(x1 > -vars.interpolatedAg01/(2*vars.lg)) {
            if(x2 > -vars.interpolatedAg02/(2*vars.lg)) {
                double Ag1 = vars.interpolatedAg01+2*vars.lg*x1;
                double Ag2 = vars.interpolatedAg02+2*vars.lg*x2;
                double Rv1 = 12*vars.mu*vars.lg*vars.lg*(vars.d1/(Ag1*Ag1*Ag1));
                double Lg1 = vars.rho*(vars.d1/Ag1);
                double Rv2 = 12*vars.mu*vars.lg*vars.lg*(vars.d2/(Ag2*Ag2*Ag2));
                double Lg2 = vars.rho*(vars.d2/Ag2);
                f2 = f1/(vars.d1*vars.lg) -.5*(Rv1+Rv2)*ug-(Lg1+Lg2)*srate*(ug-ugold)-.5*vars.rho*ug*ug*(1/(Ag2*Ag2)-1/(Ag1*Ag1));
                f2 *= vars.d2*vars.lg;
            } else {
                f2 = vars.interpolatedPs * vars.d2*vars.lg;
            }
        } else {
            f2 = 0;
        }
        myf2=f2;
        return f2;
    }
    
    protected void advanceMasses() {
        // compute pressures (following notation of  Sondhi-Schroeter)
        double f1 = computeForceOnMass1();
        double f2 = computeForceOnMass2(f1);

        double srate = this.srate*nOverSamplings;
        // calc. matrix elements
        double h1=0,h2=0,r1=0,r2=0;
        if(x1<-vars.interpolatedAg01/(2*vars.lg)) {
            h1 = vars.h1;
            r1 = vars.r1closed;
        } else {
            h1 = 0;
            r1 = vars.r1open;
        }
        if(x2<-vars.interpolatedAg02/(2*vars.lg)) {
            h2 = vars.h2;
            r2 = vars.r2closed;
        } else {
            h2 = 0;
            r2 = vars.r2open;
        }
        double a11 = (vars.k1+h1+vars.kc)/(srate*srate) + r1/srate + vars.m1;
        double a12 = -vars.kc/(srate*srate);
        double a21 = a12;
        double a22 = (vars.k2+h2+vars.kc)/(srate*srate) + r2/srate + vars.m2;
        double s1prime = vars.k1*vars.etak1*x1*x1*x1+h1*(vars.interpolatedAg01/(2*vars.lg) +
                                                         vars.etah1*(vars.interpolatedAg01/(2*vars.lg)+x1)*(vars.interpolatedAg01/(2*vars.lg)+x1)*(vars.interpolatedAg01/(2*vars.lg)+x1));
        double s2prime = vars.k2*vars.etak2*x2*x2*x2+h2*(vars.interpolatedAg02/(2*vars.lg) +
                                                         vars.etah2*(vars.interpolatedAg02/(2*vars.lg)+x2)*(vars.interpolatedAg02/(2*vars.lg)+x2)*(vars.interpolatedAg02/(2*vars.lg)+x2));
        double b1 = (2*vars.m1+r1/srate)*x1 - vars.m1*xold1-s1prime/(srate*srate) + f1/(srate*srate);
        double b2 = (2*vars.m2+r2/srate)*x2 - vars.m2*xold2-s2prime/(srate*srate) + f2/(srate*srate);
        

        xold1 = x1;
        xold2 = x2;
        double det = a11*a22-a21*a12;
        if(det==0) {
            det = 1;
        }
        x1 = (a22*b1-a12*b2)/det;
        x2 = (a11*b2-a21*b1)/det;
    }

    protected void advanceUg(boolean addNoise) {
        double srate = this.srate*nOverSamplings;
        double Ag1 = vars.interpolatedAg01+2*vars.lg*x1;
        double Ag2 = vars.interpolatedAg02+2*vars.lg*x2;
        if(Ag1<0 || Ag2<0) {
            ug = 0;
        } else {
            // get noise source
            if(pressureServer != null) {
                vars.A1 = pressureServer.getA1(); // area at bottom of VT
            }
            double uc = (ug + ugold)/2;
            double Re2 = (4*vars.rho*vars.rho/(Math.PI*vars.mu*vars.mu))*uc*uc/Ag2;
            if(Re2>vars.Rec2 && addNoise) {
                float rrr = resonFilter.filter1Sample((float)(Math.random()-.5));
                //System.out.println(rrr);
                vars.png = vars.gng*(Re2-vars.Rec2)*rrr;
            } else {
                vars.png = 0;
            }
            double Rtot = (vars.rho/2)*( 0.37/(Ag1*Ag1) +  (1-2*(Ag2/vars.A1)*(1-Ag2/vars.A1))/(Ag2*Ag2) )
                *Math.abs(ug)
                + 12*vars.mu*vars.lg*vars.lg*(vars.d1/(Ag1*Ag1*Ag1) + vars.d2/(Ag2*Ag2*Ag2) );
            double Ltot = vars.rho*(vars.d1/Ag1 + vars.d2/Ag2);
            ugold = ug;
            ug = ((vars.interpolatedPs-vars.p1-vars.png)/srate + Ltot*ug)/(Rtot/srate + Ltot);
            
        }
        if(addNoise) {
            //System.out.println(ug+" "+x1+" "+x2);
        }
    }

    /** Advance state by one sample
     */
    public void advance(double lambda) {
        vars.interpolateVars(lambda);
        //System.out.println(lambda+" "+vars.interpolatedQ);
        updateP1(); // update pressure at right end (from VT model)
        for(int i=0;i<nOverSamplings-1;i++) {
            advanceMasses();
            advanceUg(false);
        }
        advanceMasses();
        advanceUg(true);
        if(wentUnstable()) {
            reset();
        }
    }

    protected void computeBuffer() {
        int bufsz = getBufferSize();
        double lambda; // pars interpolated as x = x_0ld + lambda*(x_new-x_old). Lambda = i/bufferSize
        for(int i=0;i<bufsz;i++) {
            lambda = i/((double)bufsz);
            advance(lambda);
            buf[i] = (float) (ug);
                    }
        vars.setVars();
        //System.out.println(ug);
    }

    private boolean wentUnstable() {
        double big = 1000;
        if(ug>big || ug<-big ||x1>big || x1<-big ||x2>big || x2<-big
           ||ug==Double.NaN || x1==Double.NaN || x2==Double.NaN) {
            System.out.println("Twomassmodel went unstable");
            return true;
        } else {
            return false;
        }
    }

    public void reset() {
        ug = 0;
        ugold = 0;
        x1 = 0;
        x2 = 0;
        xold1 = 0;
        xold2 = 0;
        vars.setVars();
    }

    /**
       Parameters of the model (which are themselves parametrized).  See
       Sondhi   and   Schroeter,   "A   hybrid   Time-Frequency   Domain
       Articulatory Pseech  Synthesizer", IEEE Trans.   Acoust., Speech,
       and Signal Processing, Vol ASSP-35,  no 7, July 1987, Table I., p
       958. These are converted to SI units here.
       When settting new values the old values are remembered and inside the
       audio loop the interpolated values are computed.
    */
    public class Vars {
        private static final double DYN = 1.e-5;
        private static final double GRAM = 1.e-3;
        private static final double CM = 1.e-2;
        private static final double SECOND = 1;
        // Table I variables:
        public double m1,m2;
        public double d1,d2;
        public double etak1,etak2;
        public double etah1,etah2;
        public double h1,h2;
        public double k1,k2,kc;
        public double mu;
        public double rho;
        public double r1open,r1closed,r2open,r2closed;
        // other variables
        public double Ag0; // glottal rest area (of both chords)
        public double Ag01,Ag02; // glottal rest areas of two parts of chords (usually same)
        public double Ag01_old,Ag02_old; // old values
        
        public double interpolatedAg01;
        public double interpolatedAg02;
        
    
        public double A1; // input area to vocal tract
        public double lg;  // glottal width
        public double gng; // RNG gain (see p961, Sondhi-Schroeter)
        public double Rec2;     // square of critical Reynolds number
        // control variables:
        public double q,gs; // pitch factor, glottal damping parameter
        public double q_old;
        public double interpolatedQ;;
        public double ps; // subglottal lung pressure
        public double ps_old;
        public double interpolatedPs;
        
        public double p1; // pressure downstream from glottis (determined by VT model)
        public double png; // noise pressure source

        public Vars() {
            // constants
            mu = 1.86e-4 * (DYN *SECOND/(CM*CM));
            rho = 1.14e-3 *(GRAM/(CM*CM*CM));
            ps = 64*8;
            ps_old = ps;
            p1 = 0;
            png = 0;
            q=1; // dimensionless
            q_old = 1;
            A1 = 1*CM*CM;
            gs = 1; // dimensionless
            Ag0 = 0.05*CM*CM; // see Ishizaka-Flanagan, p 1250.
            Ag01 = Ag0; // see Ishizaka-Flanagan, p 1250.
            Ag02 = Ag0;
            Ag01_old = Ag01;
            Ag02_old = Ag02;
            lg = 1.4*CM; // see Ishizaka-Flanagan, p 1250.
            gng=2.e-6;
            Rec2 = 2700*2700;
            etak1 = 100/(CM*CM);
            etak2 = 100/(CM*CM);
            etah1 = 500/(CM*CM);
            etah2 = 500/(CM*CM);
            setVars();    
        }

        /**
           Compute the interpolated vlaues using interpolation parameter lambda in [0 1]
        */
        public void interpolateVars(double lambda) {
            interpolatedAg01= vars.Ag01_old + lambda*(vars.Ag01-vars.Ag01_old);
            interpolatedAg02= vars.Ag02_old + lambda*(vars.Ag02-vars.Ag02_old);
            interpolatedPs = vars.ps_old + lambda*(vars.ps-vars.ps_old);
            interpolatedQ = vars.q_old + lambda*(vars.q-vars.q_old);
            m1 = .125*GRAM/interpolatedQ;
            m2 = .025*GRAM/interpolatedQ;
            d1 = .25*CM/interpolatedQ;
            d2 = .05*CM/interpolatedQ;;
            k1 = 80000*(DYN/CM)*interpolatedQ;
            k2 = 8000*(DYN/CM)*interpolatedQ;
            h1 = 3*k1;
            h2 = 3*k2;
            kc = 25000*(DYN/CM)*interpolatedQ*interpolatedQ;
            r1open = 2*0.2*Math.sqrt(k1*m1)/(gs*gs);
            r1closed = 2*1.1*Math.sqrt(k1*m1)/(gs*gs);
            r2open = 2*0.6*Math.sqrt(k2*m2)/(gs*gs);
            r2closed = 2*1.9*Math.sqrt(k2*m2)/(gs*gs);
        }
        
        /**
           Calculate non-constant parameters from control parameters.
        */
        public void setVars() {

            // end interpolated variables
            q_old = q;
            Ag01_old = Ag01;
            Ag02_old = Ag02;
            Ag01 = Ag0;
            Ag02 = Ag0;
            ps_old = ps;
        }

        /**
           Set the dimensionless control parameters.
           @param ps subglottal lung pressure
           @param q pitch factor
           @param A1 input area of vocal tract
           @param gs damping factor from Sondhi-Schroeter
        */
        public void setControlPars(double ps,double q,double A1,double gs) {
            this.ps = ps;
            this.q = q;
            this.A1 = A1;
            this.gs=gs;
        }
    }

    public interface PressureServer {
        double getPressure();
        double getA1();
    }
    
    public static class Test {
        public static void main(String[] args) {
            new Test(args);
        }

        public Test(String[] args) {
            Controller a_controlPanel;
            int bufferSize = 4*128;
            int bufferSizeJavaSound=1024*8;
            float srate = 44100;
            final TwoMassModel m = new TwoMassModel(bufferSize,44100);
            RandOut r = new RandOut(bufferSize);
            final SourcePlayer player = new SourcePlayer(bufferSize,bufferSizeJavaSound,srate);
            try {
                player.addSource(m);
            } catch(Exception e) {}
            
            int nbuttons = 4;

            final int nSliders = 6;
            String[] names = new String[nSliders];
            double[] val = new double[nSliders];
            double[] min = new double[nSliders];
            double[] max = new double[nSliders];
            names[0] = "q factor";
            val[0] = 1; min[0] = 0.001; max[0] = 5;
            names[1] = "lung press.";
            val[1] = 500; min[1] = 1; max[1] = 5000;
            names[2] = "Ag0(cm^2)"; // -.005
            val[2] = .03; min[2] = -.5; max[2] = .5;
            names[3] = "noiseLevel";
            val[3] = 1; min[3] = 0; max[3] = 10;
            names[4] = "noiseFreq.";
            val[4] = 500; min[4] = 200; max[4] = 10000;
            names[5] = "noiseBW";
            val[5] = 1000; min[5] = 250; max[5] = 10000;


            a_controlPanel = new Controller(new java.awt.Frame ("TestTwoMassModel"),
                                            false,val.length,nbuttons) {
                    private static final long serialVersionUID = 1L;

                    boolean muted=false;
                
                    public void onButton(int k) {
                        switch(k) {
                        case 0: 
                            player.resetAGC();
                            break;
                        case 1: {
                            FileDialog fd = new FileDialog(new Frame(),"Save");
                            fd.setMode(FileDialog.SAVE);
                            fd.setVisible(true);
                            saveToFile(fd.getFile());
                        }
                            break;
                        case 2: {
                            FileDialog fd = new FileDialog(new Frame(),"Load");
                            fd.setMode(FileDialog.LOAD);
                            fd.setVisible(true);
                            loadFromFile(fd.getFile());
                        }
                            break;
                        case 3: {
                            muted = !muted;
                            player.setMute(muted);
                            player.resetAGC();
                        }
                            break;
                        }
                    }
                
                    public void onSlider(int k) {
                        TwoMassModel.Vars vars = m.getVars();
                        switch(k) {
                        case 0:
                            vars.q = this.val[k];
                            //vars.setVars();
                            break;
                        case 1:
                            vars.ps = this.val[k];
                            //vars.setVars();
                            break;
                        case 2:
                            // glottal rest area (displayed in cm^2)
                            vars.Ag0 = 1.e-4 * this.val[k];
                            //vars.setVars();
                            break;
                        case 3:
                            m.setFlowNoiseLevel(this.val[k]);
                            break;
                        case 4:
                            m.setFlowNoiseFrequency(this.val[k]);
                            break;
                        case 5:
                            m.setFlowNoiseBandwidth(this.val[k]);
                            break;
                        default:
                            break;
                        }
                    }
                };

            a_controlPanel.setSliders(val,min,max,names);
            a_controlPanel.setButtonNames (new String[] {"Reset","Save","Load","(De)Mute"});
            a_controlPanel.setVisible(true);
            player.start();
            player.resetAGC();
        }
    }

}

