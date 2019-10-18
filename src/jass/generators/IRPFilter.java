package jass.generators;

/**
   Compute impulse respones of filter.
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class IRPFilter {
    private FFTFloat fft=null;
    private int bits=0; // len 2^bits FFT
    // input to filter
    private float[] x = null;
    private float[] xim = null;
    private float[][]res = null;
    private Filter filter = null;

    public static void main(String[] argv) {
        int n = 4096;
        int b=11;
        float fs=44100;
        Filter filter = new Filter() {
                double srate = 44100;
                double freq = 440;
                double d = 1000;
                public void filter(float [] output, float[] input, int nsamples, int inputOffset) {
                    for(int i=0;i<nsamples;i++) {
                        output[i] = (float)(Math.sin(2*Math.PI*440*i/srate)*Math.exp(-d*i/srate));
                    }
                }
            };
        IRPFilter ip = new IRPFilter();
        float[][] res = ip.computeIRP(filter, b, fs);
        for(int i=0;i<n/2;i++) {
            System.out.println(res[i][0]+" "+res[i][1]);
        }

        res = ip.computeIRP(filter, b, fs);
        for(int i=0;i<n/2;i++) {
            System.out.println(res[i][0]+" "+res[i][1]);
        }
    }
    
    /** Compute IRP as dB magnitude of Fourier transform.
        @param filter filter
        @param bits 2^bits length IRP generated
        @param srate sampling rate in Hz
        @return array[2^bits][2] with array[i][0] dB and array[i][1] freq. of i.
    */
    public float[][] computeIRP(Filter filter, int bits, float srate) {
        if(filter != this.filter || bits != this.bits) {
            this.bits = bits;
            this.filter = filter;
            allocate();
        }
        int n = 1<<bits;

        for(int i=0;i<n;i++) {
            x[i] = 0;
            xim[i]=0;
        }
        x[0]=1;
        int offset=0;
        filter.filter(x,x,n,offset);
        
        /*
        System.out.println("==============================");
        for(int i=0;i<n;i++) {
            System.out.println(x[i]);
        }
        System.out.println("==============================");
        */
        
        fft.doFFT(x,xim,false);

        //System.out.println("==============================");
        for(int i=0;i<n;i++) {
            if(x[i]*x[i]+xim[i]*xim[i] > 0) {
                res[i][0] = 200+(float)(10*Math.log10(x[i]*x[i]+xim[i]*xim[i]));
            } else {
                res[i][0] = (float) (-110);
            }
            res[i][1] = i*srate/n;
            //System.out.println(res[i][0] + " " + res[i][1]);
        }
        //System.out.println("==============================");
        return res;
    }

    private void allocate() {
        fft = new FFTFloat(bits);
        int n = 1<<bits;
        x = new float[n];
        xim = new float[n];
        res = new float[n][2];
    }
    
    //public voiterd filter(float [] output, float[] input, int nsamples, int inputOffset);
}
