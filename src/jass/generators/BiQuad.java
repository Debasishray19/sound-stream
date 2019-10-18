package jass.generators;

import jass.render.*;

/** Biquad bandpass Filter
 * See bottom of BiQuadFilterBase.java for details. This implementation has normalized a0=1
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class BiQuad extends BiQuadFilterBase {
    
    protected float srate;
    public static final int LP=0,HP=1,BP=2;
    protected int type=0; //LP,HP, or BP
        
    /** Defining coefficients*/
    //protected float a1,a2,b0,b1,b2;
    
    /* control variables: center freq and bandwidth. Q only for bandpass */
    private float f0,bw,q=1;
    
    private static final double LN2_2 = Math.log(2)/2;
    
    public void setF0(float f0) {
        this.f0=f0;
        calcCoeff();
    }
    public void setBW(float bw) {
        this.bw=bw;
        calcCoeff();                       
    }
    public void setQ(float q) {
        this.q=q;
        calcCoeff();                       
    }
    public float getF0() {
        return this.f0;
    }
    public float getBW() {
        return this.bw;
    }
    public float getQ() {
        return this.q;
    }
    
    private void calcCoeff() {
        float w0 = (float)(2*Math.PI*f0/srate);
        float sinw0 = (float) Math.sin(w0);
        float cosw0 = (float) Math.cos(w0);
        float alpha = (float)(sinw0*Math.sinh(LN2_2*bw*w0/sinw0));

        float a0 = 1+alpha;
        switch (type) {
            case LP:
                b0 = ((1 - cosw0) / 2) / a0;
                b1 = 2 * b0;
                b2 = b0;
                a1 = - 2 * cosw0 / a0;
                a2 = (1 - alpha) / a0;
                break;
            case HP:
                b0 = ((1+cosw0)/2)/a0;
                b1 = -2*b0;
                b2 = b0;
                a1 = -2*cosw0/a0;
                a2 = (1 - alpha)/a0;
                break;
            case BP:
                b0 = q * alpha / a0;
                b1 = 0;
                b2 = -b0;
                a1 = -2 * cosw0 / a0;
                a2 = (1 - alpha) / a0;
                break;
        }
       
    }

    /*
     * Type = LP,HP,BP for lowpass,highpassbandpass,
     * **/
    public BiQuad(float srate,int type) {
        super();
        this.srate = srate;
        this.type=type;
    }

    public static void main(String args[]) throws Exception {
        float srate = 44100.f;
        int bufferSize = 256;
        int bufferSizeJavaSound = 1024 * 8;
        final SourcePlayer player;
        final BiQuad hp = new BiQuad(srate,BiQuad.HP);
        final FilterContainer filterHP = new FilterContainer(srate,bufferSize,hp);
        final BiQuad lp = new BiQuad(srate,BiQuad.LP);
        final FilterContainer filterLP = new FilterContainer(srate,bufferSize,lp);
        RandOut ro = new RandOut(bufferSize);
        player = new SourcePlayer(bufferSize, bufferSizeJavaSound, srate);
        
        filterHP.addSource(ro);
        //filterHP.addSource(filterLP);
        player.addSource(filterHP);
        
        String[] names = {"HPF ", "LPF ", "BW "};
        double[] val = {400, 1000,1};
        double[] min = {10, 200,.01};
        double[] max = {10000,10000, 1};
        int nbuttons = 1;
        Controller a_controlPanel = new Controller(new java.awt.Frame("Demo"),
                false, val.length, nbuttons) {

            public void onButton(int k) {
                switch (k) {
                    case 0:
                        player.resetAGC();
                        break;
                }
            }

            public void onSlider(int k) {
                switch (k) {
                    case 0:
                        hp.setF0((float)this.val[k]);
                        break;
                    case 1:
                        lp.setF0((float)this.val[k]);
                        break;
                    case 2:
                        lp.setBW((float)this.val[k]);
                        hp.setBW((float)this.val[k]);
                        break;
                }
            }
        };

        a_controlPanel.addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent e) {
                player.stopPlaying();
                try {
                    Thread.sleep(500);
                } catch (Exception e3) {
                }
                System.exit(0);
            }
        });

        a_controlPanel.setSliders(val, min, max, names);
        a_controlPanel.setButtonNames(new String[]{"Reset"});
        a_controlPanel.setVisible(true);
        player.start();
        
       
    }

}
