package jass.generators;
import jass.engine.*;
import jass.render.*;
import jass.generators.*;
import java.net.*;
import java.awt.*;

/**
   Provide a pitch shiftable object built of a set of recordings at 
   particular frequencies. At any given freq. the sound is produced
   by pitch shifting nearby samples to this ranges and mixing them.
   If f is the disired freq. we find the two samples y1,2 with freq. f1 and f2
   s.t. f1 < f < f2 . Both y1 and y2 are pitch shifted to f and then combined
   as y = (1-t)*y1_shifted + t*y2_shifted with 0<t<1 the obvious parameter.
   An optional overal multiplier of C(t) y can be provided which is of the
   form C = 1 + q*t*(1-t). Tweaking q for each sample interval can improve
   quality.
   The number of audio samples provided must be at least 2.
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class CrossfadeLoopBuffer extends Out {
    /** Array of buffers to loop */
    protected float[][] loopBuffer;

    /** Buffer lengths */
    protected int[] loopBufferLength;
    
     /** fGAin corrections (1 if audio files are ok)  */
    protected float[] gainCorrection;

    /** Playback volume. */
    protected float volume = 1;

    /** Loop f (frequency) through buffer. */
    protected float f = 1;
    
    protected float oldF = 1; // for in-buffer interpolation

     /** Natural loop frequencies of buffers */
    protected float[] fb;
    
    /** Current fractional position [0 1] of pointer in buffer. */
    protected float xLeft,xRight; // fractional offsets in frames

    /** Current integer position of pointer in buffer. */
    protected int ixLeft,ixRight; // frame index (not sample index fro 2 channels)
    
    /**  Current  fractional f [0  1]  of pointer  in  buffer  per sample. */
    protected float dxLeft,dxRight;
    
    /**  Current  integer f of pointer  in  buffer  per sample. */
    protected int dixLeft,dixRight;
    
    protected float aLeft; // gains of left contribution (right - 1-aLeft)
    
    //protected float[] sampleLevel; // |a| levels of buffers
    //protected float[] sampleCorrelation; // sampleCorrelation[i] =  integral y[i]*y[i+1]/integral y[i]
    
    protected float levelCorrection; // corrects interpolated sum of sources for level difference
    
    protected float[] qFactor; // qFactor[i] adds a non-linear tweak to the crossfade between i and i+1 (see top for description)
    
    protected int iLeft=-1; // left frame index of currently active loopbuffer pair fb[]

    /** Sampling rate in Hertz of Out. */
    public float srate;

    /** Sampling rate ratio, srateLoopBuffer/srate */
    private float srateRatio = 1;

    /** Sampling rate in Hertz of loaded buffers (must all be the same). */
    public float srateLoopBuffer;

    protected int nChannels;
    
    /**
       For derived classes
       @param bufferSize biffer size
     */
    public CrossfadeLoopBuffer(int bufferSize) {
        super(bufferSize); // this is the internal buffer size
    }
    
    /** Construct buffers from named files.
        @param srate sampling rate in Hertz.
        @param bufferSize bufferSize of this Out
        @param fn Audio files names.
        @param natural frequencies of the audio files.
    */
    public CrossfadeLoopBuffer(float srate,int bufferSize, String[] fn, float[] fb) {
        super(bufferSize); // this is the internal buffer size
        this.fb = fb;
        int nb = fb.length;
        gainCorrection = new float[nb];
        qFactor = new float[nb];
        AudioFileBuffer[] afBuffer = new AudioFileBuffer[nb];
        loopBuffer = new float[nb][];
        loopBufferLength = new int[nb];
        for(int i=0;i<nb;i++) {
            afBuffer[i] = new AudioFileBuffer(fn[i]);
            loopBuffer[i] = afBuffer[i].buf;
            loopBufferLength[i] = loopBuffer[i].length;
            gainCorrection[i]=1;
            qFactor[i] = 1;        
        }
        nChannels = afBuffer[0].nChannels;
        srateLoopBuffer = afBuffer[0].srate;
        this.srate = srate;
        srateRatio = srateLoopBuffer/srate;
        setF(f);
    }

    public float getQ(int k) {
        return qFactor[k];
    }
     
    public void setQ(int k,float val) {
        qFactor[k] = val;
    }
    
    public int getNChannels() {
        return nChannels;
    }
    
    /** Set force magnitude.
        @param val Volume.
    */
    public void setVolume(float val) {
        volume = val;
    }

    /** Set loopspeed.
        @param f Loop freq. 
    */
    public synchronized void setF(float f) {
        this.f = f;       
    }
    
    // find index of left fb[] interval in which f lies
    private void findFInterval(float f) {
        int iLeftOld = iLeft;
        if(iLeft !=-1) {
            if(f>=fb[iLeft] && f<=fb[iLeft+1]) {
                return;
            }
        }
        int nb = fb.length;
         boolean notFound = true;
        if(f<fb[0]) {
            iLeft = 0;
            notFound = false;
        } else if(f>fb[nb-1]) {
            iLeft = nb-2;
            notFound = false;
        }
        // do binary search     
        if (notFound) {
            iLeft = 0;
        }
        int iRight = nb-1;
        while(notFound) {
            int i = (iLeft+iRight)/2;
            if(f < fb[i]) {
                iRight = i;
            } else {
                iLeft = i;
            }
            if(iRight == iLeft+1) {
                notFound = false;
            }
        }  
        if(iLeft==iLeftOld+1) {
            xLeft = xRight;
            ixLeft = ixRight;
        } else if(iLeft==iLeftOld-1) {
            xRight = xLeft;
            ixRight = ixLeft;
        }
    }
        
    protected void calcRates(float f) {
        findFInterval(f);
        float fLeft = fb[iLeft];
        float fRight = fb[iLeft+1];
        //float levelRatio = sampleLevel[iLeft+1]/sampleLevel[iLeft];
        float rateLeft = (f/fLeft) * srateRatio;
        float rateRight = (f/fRight) * srateRatio;
        dixLeft = (int)rateLeft;
        dxLeft = rateLeft - dixLeft;
        dixRight = (int)rateRight;
        dxRight = rateRight - dixRight;
        aLeft = (fRight-f)/(fRight-fLeft);               
        if(aLeft<0) {
            aLeft = 0;
        } else if(aLeft>1) {
            aLeft = 1;
        }
        levelCorrection = 1 + aLeft*(1-aLeft)*qFactor[iLeft];
    }

    /** Get next sample value, interpolating in between sample points.
     */
    int next_index_left = 0;
    int next_index_right = 0;
    
    protected float getNextSampleStereo(int k,int bufsz) {
        float val=0;
        float valLeft=0;
        float valRight=0;
        if (k % 2 == 0) {
            float f = (this.f - oldF) * (k + 1) / bufsz + oldF;
            calcRates(f);
            ixLeft += dixLeft;
            xLeft += dxLeft;
            if (xLeft > 1.f) {
                xLeft -= 1.f;
                ixLeft++;
            }
            while (ixLeft >= loopBufferLength[iLeft]/2) {
                ixLeft -= loopBufferLength[iLeft]/2;
            }
            if (ixLeft == loopBufferLength[iLeft]/2 - 1) {
                next_index_left = 0;
            } else {
                next_index_left = ixLeft + 1;
            }
            valLeft = (1.f - xLeft) * loopBuffer[iLeft][2*ixLeft] + xLeft * loopBuffer[iLeft][2*next_index_left];

            ixRight += dixRight;
            xRight += dxRight;
            if (xRight > 1.f) {
                xRight -= 1.f;
                ixRight++;
            }
            while (ixRight >= loopBufferLength[iLeft + 1]/2) {
                ixRight -= loopBufferLength[iLeft + 1]/2;
            }
            if (ixRight == loopBufferLength[iLeft + 1]/2 - 1) {
                next_index_right = 0;
            } else {
                next_index_right = ixRight + 1;
            }
            valRight = (1.f - xRight) * loopBuffer[iLeft + 1][2*ixRight] + xRight * loopBuffer[iLeft + 1][2*next_index_right];
        } else {
            try {
                valLeft = (1.f - xLeft) * loopBuffer[iLeft][2*ixLeft+1] + xLeft * loopBuffer[iLeft][2*next_index_left+1];
                valRight = (1.f - xRight) * loopBuffer[iLeft + 1][2*ixRight+1] + xRight * loopBuffer[iLeft + 1][2*next_index_right+1];
            } catch(ArrayIndexOutOfBoundsException e) {
                System.out.println("ixLeft="+ixLeft+" "+next_index_left+" "+ixRight+" "+next_index_right);
            
            }            
        }
        val = (float) ((gainCorrection[iLeft] * valLeft * aLeft + gainCorrection[iLeft + 1] * valRight * (1 - aLeft)) * volume * levelCorrection);
        return val;
    }
   
    /** Get next sample value, interpolating in between sample points.
     */
    protected float getNextSample(int k, int bufsz) {
        float f = (this.f - oldF) * (k + 1) / bufsz + oldF;
        calcRates(f);
        ixLeft += dixLeft;
        xLeft += dxLeft;
        if (xLeft > 1.f) {
            xLeft -= 1.f;
            ixLeft++;
        }
        while (ixLeft >= loopBufferLength[iLeft]) {
            ixLeft -= loopBufferLength[iLeft];
        }
        int next_index_left;
        if (ixLeft == loopBufferLength[iLeft] - 1) {
            next_index_left = 0;
        } else {
            next_index_left = ixLeft + 1;
        }
        float valLeft = (1.f - xLeft) * loopBuffer[iLeft][ixLeft] + xLeft * loopBuffer[iLeft][next_index_left];

        ixRight += dixRight;
        xRight += dxRight;
        if (xRight > 1.f) {
            xRight -= 1.f;
            ixRight++;
        }
        while (ixRight >= loopBufferLength[iLeft + 1]) {
            ixRight -= loopBufferLength[iLeft + 1];
        }
        int next_index_right;
        if (ixRight == loopBufferLength[iLeft + 1] - 1) {
            next_index_right = 0;
        } else {
            next_index_right = ixRight + 1;
        }
        float valRight = (1.f - xRight) * loopBuffer[iLeft + 1][ixRight] + xRight * loopBuffer[iLeft + 1][next_index_right];
        float val = (float) ((gainCorrection[iLeft] * valLeft * aLeft + gainCorrection[iLeft + 1] * valRight * (1 - aLeft)) * volume * levelCorrection);
        return val;
    }

    /** Compute the next buffer.
     */
    public synchronized void computeBuffer() {
        int bufsz = getBufferSize();
        for(int k=0;k<bufsz;k++) {
            if(nChannels==1) {
                buf[k] = getNextSample(k,bufsz);
            } else if(nChannels==2) {
                buf[k] = getNextSampleStereo(k,bufsz);
            }
        }
        oldF = this.f;
    }
    
    // to go away:
    public ControllerPanel getEditorPanel(String name) {
        return getControlPanel();
    }
    
    public ControllerPanel getControlPanel() {
        final int nb = fb.length;
        double minV = -2;
        double maxV = 5;
        String[] names = new String[nb];
        double[] val = new double[nb];
        double[] min = new double[nb];
        double[] max = new double[nb];
        for(int i=0;i<nb-1;i++) {
            String f1 = String.format("%4.0f-",fb[i]);
            String f2 = String.format("%4.0f: ",fb[i+1]);
            names[i] = f1 + f2;
            val[i]=getQ(i);
            min[i]=minV;
            max[i]=maxV;
        }
        names[nb-1] = "RPM: ";
        val[nb-1] = fb[0];
        max[nb-1] =  fb[fb.length-1];
        min[nb-1] =  fb[0];
        int nbuttons = 2;
        ControllerPanel controlPanel = new ControllerPanel(val.length, nbuttons) {

            public void onButton(int k) {
                switch (k) {
                    case 0:
                         {
                            FileDialog fd = new FileDialog(new Frame(), "Save");
                            fd.setMode(FileDialog.SAVE);
                            fd.setVisible(true);
                            saveToFile(fd.getFile());
                        }
                        break;
                    case 1:
                         {
                            FileDialog fd = new FileDialog(new Frame(), "Load");
                            fd.setMode(FileDialog.LOAD);
                            fd.setVisible(true);
                            loadFromFile(fd.getFile());
                        }
                        break;
                }
            }

            public void onSlider(int k) {
                float v = (float) this.val[k];
                if(k<nb-1) {
                    setQ(k, v);
                } else {
                    setF(v);
                }
            }
        };
        controlPanel.setSliders(val, min, max, names);
        controlPanel.setButtonNames(new String[]{"Save","Load"});
        //controlPanel.setVisible(true);    
        return controlPanel;
    }
    
   
    
    static final int MIXER_CH_CAB = 0;
    static final int MIXER_CH_ENG = 1;
    static final int MIXER_CH_INT = 2;
    static final int MIXER_CH_EXH = 3;
    static final int MIXER_CH_RFF = 4;
    static final int MIXER_CH_LEVEL = 5;
    static final int MIXER_CH_RPM = 6;

    public static void main(String args[]) throws Exception {
        float srate = 44100.f;
        int bufferSize = 256;
        int bufferSizeJavaSound = 1024 * 8;
        final SourcePlayer player;
        
        final Mixer mixer = new Mixer(bufferSize, MIXER_CH_RPM+1);
        final LevelMeter levelMeter = new LevelMeter(bufferSize,200f);
        levelMeter.addSource(mixer);
        final StickyControl stickyControlSpeed = new StickyControl(srate,bufferSize);
        stickyControlSpeed.setT(.1);
        //player = new SourcePlayer(bufferSize, bufferSizeJavaSound, srate,"Java Sound Audio Engine");
        player = new SourcePlayer(bufferSize, bufferSizeJavaSound, srate);
        String[] fnCab = {"modelData/6430/stereo/cab800.wav","modelData/6430/stereo/cab1000.wav","modelData/6430/stereo/cab1200.wav","modelData/6430/stereo/cab1350.wav",
        "modelData/6430/stereo/cab1400.wav","modelData/6430/stereo/cab1600.wav",
        "modelData/6430/stereo/cab1800.wav","modelData/6430/stereo/cab2000.wav","modelData/6430/stereo/cab2200.wav","modelData/6430/stereo/cab2300.wav"};
        String[] fnEng = {"modelData/6430/stereo/eng800.wav", "modelData/6430/stereo/eng1000.wav", "modelData/6430/stereo/eng1200.wav",
            "modelData/6430/stereo/eng1400.wav", "modelData/6430/stereo/eng1600.wav",
            "modelData/6430/stereo/eng1800.wav", "modelData/6430/stereo/eng2000.wav", "modelData/6430/stereo/eng2200.wav", "modelData/6430/stereo/eng2300.wav"
        };
        String[] fnInt = {"modelData/6430/stereo/int800.wav", "modelData/6430/stereo/int1000.wav", "modelData/6430/stereo/int1200.wav",
            "modelData/6430/stereo/int1400.wav", "modelData/6430/stereo/int1600.wav",
            "modelData/6430/stereo/int1800.wav", "modelData/6430/stereo/int2000.wav", "modelData/6430/stereo/int2200.wav", "modelData/6430/stereo/int2300.wav"
        };
        String[] fnExh = {"modelData/6430/stereo/exh850.wav", "modelData/6430/stereo/exh1000.wav", "modelData/6430/stereo/exh1200.wav",
            "modelData/6430/stereo/exh1400.wav", "modelData/6430/stereo/exh1600.wav",
            "modelData/6430/stereo/exh1800.wav", "modelData/6430/stereo/exh2000.wav", "modelData/6430/stereo/exh2200.wav", "modelData/6430/stereo/exh2300.wav"
        };
        String[] fnRff = {"modelData/6430/stereo/rff800.wav", "modelData/6430/stereo/rff1000.wav", "modelData/6430/stereo/rff1200.wav",
            "modelData/6430/stereo/rff1400.wav", "modelData/6430/stereo/rff1600.wav",
            "modelData/6430/stereo/rff1800.wav", "modelData/6430/stereo/rff2000.wav", "modelData/6430/stereo/rff2200.wav", "modelData/6430/stereo/rff2300.wav"
        };
        //String[] fn = {"sine440.wav","sine800.wav"};
        float[] fbCab = {800,1000,1200,1350,1400,1600,1800,2000,2200,2300};
        float[] fbEng = {800,1000,1200,1400,1600,1800,2000,2200,2300};
        float[] fbInt = {800,1000,1200,1400,1600,1800,2000,2200,2300};
        float[] fbExh = {850,1000,1200,1400,1600,1800,2000,2200,2300};
        float[] fbRff = {800,1000,1200,1400,1600,1800,2000,2200,2300};
        //float[] fb = {440,800};
        final CrossfadeLoopBuffer cfCab = new CrossfadeLoopBuffer(srate,bufferSize,fnCab,fbCab);
        final CrossfadeLoopBuffer cfEng = new CrossfadeLoopBuffer(srate,bufferSize,fnEng,fbEng);
        final CrossfadeLoopBuffer cfInt = new CrossfadeLoopBuffer(srate,bufferSize,fnInt,fbInt);
        final CrossfadeLoopBuffer cfExh = new CrossfadeLoopBuffer(srate,bufferSize,fnExh,fbExh);
        final CrossfadeLoopBuffer cfRff = new CrossfadeLoopBuffer(srate,bufferSize,fnRff,fbRff);
        //final LoopBuffer lb = new LoopBuffer(srate,bufferSize,"sine440.wav");

        mixer.addSource(cfCab);
        mixer.addSource(cfEng);
        mixer.addSource(cfInt);  
        mixer.addSource(cfExh);
        mixer.addSource(cfRff);
        mixer.addSource(levelMeter);
        mixer.addSource(stickyControlSpeed);
        mixer.setGain(MIXER_CH_CAB, 0);
        mixer.setGain(MIXER_CH_ENG, 0);
        mixer.setGain(MIXER_CH_INT, 1);
        mixer.setGain(MIXER_CH_EXH, 0);
        mixer.setGain(MIXER_CH_RFF, 0);

        player.addSource(mixer);
        player.AGCOff();
        player.setVolume(1f);
        player.setNChannels(cfCab.getNChannels());
        System.out.println("ch="+cfCab.getNChannels());

        // Add control panel
        double freq = 800;
        stickyControlSpeed.setX(freq);
        String[] names = {"rpm ","cab ","eng ","int ","exh ","rff "};
        double[] val =   {freq,  0,     0,     1,     0,      0};
        double[] min =   {800,   0,     0,     0,     0,      0};
        double[] max =   {2300,  1,     1,     1,     1,      1};
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
                        stickyControlSpeed.setXc(this.val[k]);
                        break;
                    case 1:
                        mixer.setGain(MIXER_CH_CAB, (float) this.val[k]);
                        break;
                    case 2:
                        mixer.setGain(MIXER_CH_ENG, (float) this.val[k]);
                        break;
                    case 3:
                        mixer.setGain(MIXER_CH_INT, (float) this.val[k]);
                        break;
                    case 4:
                        mixer.setGain(MIXER_CH_EXH, (float) this.val[k]);
                        break;
                    case 5:
                        mixer.setGain(MIXER_CH_RFF, (float) this.val[k]);
                        break;
                }
            }
        };

        a_controlPanel.addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent e) {
                System.out.println("Close handler called");
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
        
        new Thread() {
            public void run() {
                int i = 0;
                boolean shouldStop = false;
                int interval = 100;
                while (!shouldStop) {
                    double v = stickyControlSpeed.getX();
                    cfCab.setF((float) stickyControlSpeed.getX());
                    cfEng.setF((float) stickyControlSpeed.getX());
                    cfInt.setF((float) stickyControlSpeed.getX());
                    cfExh.setF((float) stickyControlSpeed.getX());
                    cfRff.setF((float) stickyControlSpeed.getX());
                    //System.out.println(levelMeter.getDBLevel());
                    try {
                        Thread.sleep(interval);
                    } catch (Exception e) {
                    }

                }
            //System.out.println("thread exit");
            }
        }.start();

    }

}


