package jass.generators;

import jass.engine.*;
import jass.render.*;

/** Two Biquad bandpass Filter
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class BandPass implements Filter {
    
    float srate;  
    /* Range f1 - f2 , bandwidths bw*/
    protected float f1=100,f2=20000,bw=1;
    private BiQuad hp;
    private BiQuad lp;
    
    private static final double LN2_2 = Math.log(2)/2;
    
    public void setF1(float f1) {
        this.f1=f1;
        hp.setF0(f1);
    }
    
    public void setF2(float f2) {
        this.f2=f2;
        lp.setF0(f2);
    }
    
    public void setBW(float bw) {
        this.bw=bw;
        lp.setBW(bw);
        hp.setBW(bw);
    }
    
    private void init() {
       hp = new BiQuad(srate, BiQuad.HP);
       lp = new BiQuad(srate, BiQuad.LP);
    }
    
    public BandPass(float srate) {
        super();
        this.srate = srate;
        init();
    }
    
     /** Proces input (may be same as output).
    @param output user provided buffer for returned result.
    @param input user provided input buffer.
    @param nsamples number of samples written to output buffer.
    @param inputOffset where to start in circular buffer input.
     */
    public void filter(float[] output, float[] input, int nsamples, int inputOffset) {
        hp.filter(output,input,nsamples,inputOffset);
        lp.filter(output,output,nsamples,inputOffset);
    }
          
    public static void main(String[] args) {
        try {
            new Test();
        } catch(Exception e){}
    }
}

class Test {
    public Test() throws Exception {
        float srate = 44100.f;
        int bufferSize = 256;
        int bufferSizeJavaSound = 1024 * 8;
        final SourcePlayer player;
        final BandPass bp1 = new BandPass(srate);
        final BandPass bp2 = new BandPass(srate);
        final FilterContainerStereo fc = new FilterContainerStereo(srate,bufferSize,bp1,bp2);
        //final FilterContainer fc = new FilterContainer(srate,bufferSize,bp1);
        RandOut ro = new RandOut(bufferSize);
        fc.addSource(ro);
        player = new SourcePlayer(bufferSize, bufferSizeJavaSound, srate);
        player.addSource(fc);
        player.setNChannels(2);
        player.setPriority(Thread.MAX_PRIORITY);

        String[] names = {"F1 ", "F2 ", "BW "};
        double[] val = {400, 1000, 1};
        double[] min = {1, 400, .1};
        double[] max = {10000, 16000, 10};
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
                        bp1.setF1((float) this.val[k]);
                        bp2.setF1((float) this.val[k]);
                        break;
                    case 1:
                        bp1.setF2((float) this.val[k]);
                        bp2.setF2((float) this.val[k]);
                        break;
                    case 2:
                        bp1.setBW((float) this.val[k]);
                        bp2.setBW((float) this.val[k]);
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


