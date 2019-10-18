package jass.generators;
import jass.engine.*;
import java.io.*;

/** Vibration model of object, capable of playing sound. Modal parameters are 
    fed through  a  of butterworth filter so if you change them they change slowly.
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class StickyModalObjectWithOneContact extends ModalObjectWithOneContact {

    protected Butter2LowFilter[] butterFiltersR2; // filter modal parameters
    protected Butter2LowFilter[] butterFiltersAmp; // filter modal parameters
    protected Butter2LowFilter[] butterFiltersCos; // filter modal parameters
    protected float butterLowPassFreq = .25f; // lowpass frequency of butterworth filter in Hz
    protected float[] parQueue = new float[2]; // for filtering
    final static int QUEUESIZE = 2;
    final static int OFFSET = 0;
    final static int NRELAXATIONS = 100;
    protected  boolean hasBeenConverged = false;

    /** Reset parameter filter
     */
    public void resetParameterFilter() {
        hasBeenConverged = false;
    }

    /** Set lowpass freq through which modal parameters are fed
        @param freq lowpass cutoff of butterworth filter
    */    
    public void setLowPassControlFilter(float freq) {
        butterLowPassFreq =  freq;
        int nf = modalModel.nfUsed;
        for(int i=0;i<nf;i++) {
            butterFiltersR2[i].setCutoffFrequency(freq);
            butterFiltersCos[i].setCutoffFrequency(freq);
            butterFiltersAmp[i].setCutoffFrequency(freq);
        }
    }

    /** Get lowpass freq through which modal parameters are fed
        @return lowpass cutoff of butterworth filter
    */    
    public float getLowPassControlFilter() {
        return butterLowPassFreq;
    }

    /** old values of low-level parameters */
    protected float[] R2_old;
    protected float[] twoRCosTheta_old;
    protected float[] ampR_old; 

    /** Constructor for derived classes to call super
        @param bufferSize Buffer size used for real-time rendering.
    */
    public StickyModalObjectWithOneContact(int bufferSize) {
        super(bufferSize);
    }
    
    /** Create and initialize, but don't set any modal parameters.
        @param srate sampling rate in Hertz.
        @param nf number of modes.
        @param np number of locations.
        @param bufferSize Buffer size used for real-time rendering.
    */
    public StickyModalObjectWithOneContact(float srate,int nf,int np,int bufferSize) {
        super(srate,nf,np,bufferSize);
        allocateOldData(nf,np);
    }

    /** Create and initialize with provided modal data.
        @param m modal model to load.
        @param srate sampling rate in Hertz.
        @param bufferSize Buffer size used for real-time rendering.
    */
    public StickyModalObjectWithOneContact(ModalModel m,float srate,int bufferSize) {
        super(m,srate,bufferSize);
        allocateOldData(modalModel.nf,modalModel.np);
    }

    /** Allocate old data.
        @param nf number of modes.
        @param np number of locations.
    */
    protected void allocateOldData(int nf,int np) {
        R2_old = new float[nf];
        twoRCosTheta_old = new float[nf];
        ampR_old = new float[nf];
        float controlRate = (float)(srate/bufferSize);
        butterFiltersR2 = new Butter2LowFilter[nf];
        butterFiltersCos = new Butter2LowFilter[nf];
        butterFiltersAmp = new Butter2LowFilter[nf];
        for(int i=0;i<nf;i++) {
            butterFiltersR2[i] = new Butter2LowFilter(controlRate);
            butterFiltersCos[i] = new Butter2LowFilter(controlRate);
            butterFiltersAmp[i] = new Butter2LowFilter(controlRate);
            butterFiltersR2[i].setCutoffFrequency(butterLowPassFreq);
            butterFiltersCos[i].setCutoffFrequency(butterLowPassFreq);
            butterFiltersAmp[i].setCutoffFrequency(butterLowPassFreq);
        }
    }
    
    /** Apply external force[] and compute response through bank of modal filters.
        Filter modal parameters through the butterworth filter so they change slowly
        @param output user provided output buffer.
        @param force input force.
        @param nsamples number of samples to compute.
    */
    protected void computeModalFilterBank(float[] output, float[] force, int nsamples) {
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
	
        if(hasBeenConverged) {
            for(int i=0;i<nf;i++) {
                parQueue[1] = R2[i];
                parQueue[0] = R2_old[i];
                butterFiltersR2[i].filter(parQueue,parQueue,QUEUESIZE,OFFSET);
                //System.out.println("0:"+parQueue[0]+" 1:"+parQueue[1]);
                R2_old[i] = parQueue[1];
                parQueue[1] = twoRCosTheta[i];
                parQueue[0] = twoRCosTheta_old[i];
                butterFiltersCos[i].filter(parQueue,parQueue,QUEUESIZE,OFFSET);
                twoRCosTheta_old[i] = parQueue[1];
                parQueue[1] = ampR[i];
                parQueue[0] = ampR_old[i];
                butterFiltersAmp[i].filter(parQueue,parQueue,QUEUESIZE,OFFSET);
                ampR_old[i] = parQueue[1];
            }
        } else {
            for(int i=0;i<nf;i++) {
                for(int k=0;k<NRELAXATIONS;k++) {
                    parQueue[1] = R2[i];
                    parQueue[0] = R2[i];
                    butterFiltersR2[i].filter(parQueue,parQueue,QUEUESIZE,OFFSET);
                }
                for(int k=0;k<NRELAXATIONS;k++) {
                    parQueue[1] = twoRCosTheta[i];
                    parQueue[0] = twoRCosTheta[i];
                    butterFiltersCos[i].filter(parQueue,parQueue,QUEUESIZE,OFFSET);
                }
                for(int k=0;k<NRELAXATIONS;k++) {
                    parQueue[1] = ampR[i];
                    parQueue[0] = ampR[i];
                    butterFiltersAmp[i].filter(parQueue,parQueue,QUEUESIZE,OFFSET);
                }
                R2_old[i] = R2[i];
                twoRCosTheta_old[i] = twoRCosTheta[i];
                ampR_old[i] = ampR[i];
            }
            /*
            for(int i=0;i<nf;i++) {
                butterFiltersR2[i].reset(R2[i]);
                butterFiltersCos[i].reset(twoRCosTheta[i]);
                butterFiltersAmp[i].reset(ampR[i]);
            }
            */
            hasBeenConverged = true;
        }


        for(int i=0;i<nf;i++) {
            float tmp_twoRCosTheta = twoRCosTheta_old[i];
            float tmp_R2 = R2_old[i];
            float tmp_a = ampR_old[i];
            float tmp_yt_1 = yt_1[i];
            float tmp_yt_2 = yt_2[i];

            for(int k=0;k<nsamples;k++) {
                float ynew = tmp_twoRCosTheta * tmp_yt_1 -
                    tmp_R2 * tmp_yt_2 + tmp_a * force[k];
                // commenting out the force[k] changes performance 650->718
                tmp_yt_2 = tmp_yt_1;
                tmp_yt_1 = ynew;
                output[k] += ynew;
            }
            yt_1[i] = tmp_yt_1;
            yt_2[i] = tmp_yt_2;
        }
    }

}
