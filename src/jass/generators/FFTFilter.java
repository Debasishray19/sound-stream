package jass.generators;

import jass.render.*;

/** 
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class FFTFilter implements Filter {
    
    protected float srate;
    protected int bufferSize;
    protected float[] xr,xi;
    protected FFTFloat fft;

    public FFTFilter(float srate,int bufferSize,int bits) {
        super();
        this.srate = srate;
        this.bufferSize= bufferSize;
        init(bits);
    }
    
    protected void init(int bits) {
        xr = new float[bufferSize];
        xi = new float[bufferSize];
        fft = new FFTFloat(bits);
    }

    private float fScale(int fk) {
        //return 1;
        double b = .4;
        double f0=1000;
        int n = bufferSize/2;
        if(fk>=n) {
            fk -= n;
        }
        double f = srate*fk/bufferSize;
        return (float)Math.exp(-(f-f0)*(f-f0)/2*b);
    }
    
    /** Proces input (may be same as output).
    @param output user provided buffer for returned result.
    @param input user provided input buffer.
    @param nsamples number of samples written to output buffer.
    @param inputOffset where to start in circular buffer input. Must be 0 for this filter
     */
    public void filter(float[] output, float[] input, int nsamples, int inputOffset) {
        boolean invFlag = false;
        for (int i = 0; i < bufferSize; i++) {
            xi[i] = 0;
        }
        fft.doFFT(input, xi, invFlag);
        for (int k = 0; k < nsamples; k++) {
           input[k] *= fScale(k);
           xi[k] *= fScale(k);
        }
        invFlag = true;
        fft.doFFT(input, xi, invFlag);
        for (int i = 0; i < bufferSize; i++) {
            output[i] = input[i];
        }
    }
    
    public static void main(String args[]) throws Exception {
        float srate = 44100.f;
        int bufferSize = 256;
        int bits = 8;
        int bufferSizeJavaSound = 1024 * 8;
        final SourcePlayer player;
        final FFTFilter fftFilter = new FFTFilter(srate,bufferSize,bits);
        final FilterContainer fft = new FilterContainer(srate,bufferSize,fftFilter);
        RandOut ro = new RandOut(bufferSize);
        player = new SourcePlayer(bufferSize, bufferSizeJavaSound, srate);
        
        fft.addSource(ro);
        player.addSource(fft);
        
        String[] names = {"F1 ", "F2 ", "BW "};
        double[] val = {400, 1000,1};
        double[] min = {100, 400,.1};
        double[] max = {10000,12000, 10};
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
                        //hp.setF0((float)this.val[k]);
                        break;
                    case 1:
                        break;
                    case 2:
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
