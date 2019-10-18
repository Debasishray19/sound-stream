package jass.generators;
import jass.engine.*;
import java.io.*;

/** Vibration model of object, capable of playing sound.
    Changes in freq damping and gains are linearly changed over one buffer rather than abruptly
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class ModalObjectWithOneContactInterpolated extends ModalObjectWithOneContact {

    /** The transfer function of a reson filter is H(z) = 1/(1-twoRCosTheta/z + R2/z*z). */
    
    protected float[] R2_new;
    protected float[] twoRCosTheta_new;

    /** Reson filter gain. */
    protected float[] ampR_new; 

    /** Constructor for derived classes to call super
        @param bufferSize Buffer size used for real-time rendering.
     */
    public ModalObjectWithOneContactInterpolated(int bufferSize) {
        super(bufferSize);
    }
    
    /** Create and initialize, but don't set any modal parameters.
        @param srate sampling rate in Hertz.
        @param nf number of modes.
        @param np number of locations.
        @param bufferSize Buffer size used for real-time rendering.
    */
    public ModalObjectWithOneContactInterpolated(float srate,int nf,int np,int bufferSize) {
                super(bufferSize);
        this.srate = srate;
        modalModel = new ModalModel(nf,np);
        allocate(nf,np);
        allocate_new(nf,np);
        tmpBuf = new float[bufferSize];
    }

    /** Create and initialize with provided modal data.
        @param m modal model to load.
        @param srate sampling rate in Hertz.
        @param bufferSize Buffer size used for real-time rendering.
    */
    public ModalObjectWithOneContactInterpolated(ModalModel m,float srate,int bufferSize) {
        super(bufferSize);
        this.srate = srate;
        modalModel = m;
        allocate(modalModel.nf,modalModel.np);
        allocate_new(m.nf,m.np);
        computeFilter();
        tmpBuf = new float[bufferSize];
    }

    /** Allocate data for new filter values
        @param nf number of modes.
        @param np number of locations.
    */
    protected void allocate_new(int nf,int np) {
        R2_new = new float[nf];
        twoRCosTheta_new = new float[nf];
        ampR_new = new float[nf];
    }

    /** Compute the reson coefficients from the modal model parameters.
        Cache values used in {@link #setLocation}.
    */
    public void computeResonCoeff() {
        for(int i=0;i<modalModel.nf;i++) {
            float tmp_r = (float)(Math.exp(-modalModel.dscale*modalModel.d[i]/srate));
            R2_new[i] = tmp_r*tmp_r;
            twoRCosTheta_new[i] = (float)(2*Math.cos(2*Math.PI*modalModel.fscale*
                                                 modalModel.f[i]/srate)*tmp_r);
            c_i[i] = (float)(Math.sin(2*Math.PI*modalModel.fscale*
                                      modalModel.f[i]/srate)*tmp_r);
        }        
    }

    /** Compute gains. Check also if any frequency is above Nyquist rate.
        If so set its gain to zero.
     */
    protected void computeLocation() {
        for(int i=0;i<modalModel.nf;i++) {
            if(modalModel.fscale*modalModel.f[i] < srate/2 && modalModel.fscale*modalModel.f[i] > 50) {
                ampR_new[i] = modalModel.ascale *c_i[i] *
                    (b1* modalModel.a[p1][i] + b2* modalModel.a[p2][i] +
                     b3* modalModel.a[p3][i]);
            } else {
                ampR_new[i] = 0; // kill stuff above nyquist rate
            }
        }   
    }
    
    
    /** Apply external force[] and compute response through bank of modal filters.
        Interpolate filter parameters over the buffer. c[k]=k*(c_new-c_old)/nsamples + c_old
        @param output user provided output buffer.
        @param force input force.
        @param nsamples number of samples to compute.
    */
    protected void computeModalFilterBank(float[] output, float[] force, int nsamples) {
        // filter parameters are:
        // twoRCosTheta[nf]; R2[nf]; ampR[nf]
        boolean isnul = true;
        for(int k=0;k<nsamples;k++) {
            output[k] = 0;
            if(Math.abs(force[k])>=eps) {
                isnul = false;
            }
        }
        int nf = modalModel.nfUsed;
        if(isnul) {
            for(int i=0;i<nf;i++) {
                if(Math.abs(yt_1[i]) >= eps || Math.abs(yt_2[i]) >= eps) {
                    isnul = false;
                    break;
                }
            }
        }
        if(isnul) {
            return;
        }

        for(int i=0;i<nf;i++) {
            float tmp_twoRCosTheta_old = twoRCosTheta[i];
            float tmp_twoRCosTheta_new = twoRCosTheta_new[i];
            float tmp_twoRCosTheta_coeff = (tmp_twoRCosTheta_new-tmp_twoRCosTheta_old)/nsamples;
            float tmp_R2_old = R2[i];
            float tmp_R2_new = R2_new[i];
            float tmp_R2_coeff = (tmp_R2_new-tmp_R2_old)/nsamples;
            float tmp_a_old = ampR[i];
            float tmp_a_new = ampR_new[i];
            float tmp_a_coeff = (tmp_a_new-tmp_a_old)/nsamples;
            float tmp_yt_1 = yt_1[i];
            float tmp_yt_2 = yt_2[i];
            for(int k=0;k<nsamples;k++) {
                // optimize by taking */ out ofthe loop
                float ynew = (tmp_twoRCosTheta_old+tmp_twoRCosTheta_coeff*k/nsamples)*tmp_yt_1 -
                    (tmp_R2_old+tmp_R2_coeff*k/nsamples) * tmp_yt_2 +
                    (tmp_a_old + tmp_a_coeff*k/nsamples) * force[k];
                tmp_yt_2 = tmp_yt_1;
                tmp_yt_1 = ynew;
                output[k] += ynew;
            }
            yt_1[i] = tmp_yt_1;
            yt_2[i] = tmp_yt_2;
            twoRCosTheta[i] = twoRCosTheta_new[i];
            R2[i] = R2_new[i];
            ampR[i] = ampR_new[i];
        }
    }

}
