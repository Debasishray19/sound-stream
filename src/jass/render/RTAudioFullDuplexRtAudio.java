package jass.render;
import javax.sound.sampled.*;

/**
   Utility class to read/write audio in real-time using native libs from RtAudio C++ classes
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class RTAudioFullDuplexRtAudio extends Thread {
    private float srate;
    private int bitsPerFrame;
    private int nchannels;
    private boolean signed;
    private static final boolean bigEndian = false;
    private long nativeObjectPointer = 0;
    private int buffersizeJASS;
    private float[] readBuffer;

    /** Constructor. Uses native audio write. Also specify RtAudio tweak parameter numberofbuffers used
        Needs librtaudio.so (LINUX) or rtaudio.dll (Windows)
        @param srate sampling rate in Hertz.
        @param nchannels number of audio channels.
        @param buffersizeJass jass buffersize
        @param numRtAudioBuffers number of rtaudio buffers (0 is lowest)
     */
    public RTAudioFullDuplexRtAudio(float srate,int nchannels,int buffersizeJASS,int numRtAudioBuffers) {
        // load shared library with native sound implementations
        try {
            System.loadLibrary("rtaudio");
        } catch(UnsatisfiedLinkError e) {
                System.out.println("Could not load shared library rtaudio: "+e);
        }
        readBuffer = new float[buffersizeJASS];
        this.buffersizeJASS = buffersizeJASS;
        initAudioNative(srate,nchannels,buffersizeJASS,numRtAudioBuffers);
    }

    /** Initialize native sound using RtAudio, setting buffersize and an internal RtAudio buffersize
        @param nchannels number of audio channels.
        @param srate sampling rate in Hertz.
        @param buffersizeJASS buffers will be rendered in these chunks
        @param nRtaudioBuffers internal buffers, 0 = lowest possiblse
        @return long representing C++ object pointer
    */
    public native long initNativeSound(int nchannels,int srate,int buffersizeJASS, int nRtaudioBuffers);
    
    /** Close native sound
        This is a native method and needs librtaudio.so (LINUX) or rtaudio.dll (Windows) or whatever its called on MAC OSX
        @param nativeObjectPointer representing C++ object pointer
    */
    public native void closeNativeSound(long nativeObjectPointer);

    /** write/read a buffer of floats to native sound.
        This is a native method and needs librtaudio.so (LINUX) or rtaudio.dll (Windows)
        @param nativeObjectPointer representing C++ object pointer.
        @param outbuf array of floats with sound buffer to be output to audio card
        @param outbuflen length of buffer.
        @param readbuf array of floats with sound buffer to be read from audio input
        @param readbuflen length of read buffer.
    */
    public native void writeReadNativeSoundFloat(long nativeObjectPointer,float[] outbuf,int outbuflen,float[] readbuf, int readbuflen);
    
    private void initAudioNative(float srate,int nchannels,int buffersizeJASS,int numRtAudioBuffers) { 
        this.srate = srate;
        this.bitsPerFrame = bitsPerFrame;
        this.nchannels = nchannels;
        this.signed = signed;
        // get pointer to C++ object
        nativeObjectPointer = initNativeSound(nchannels,(int)srate,buffersizeJASS,numRtAudioBuffers); 
        
        //This will ensure that close() gets called before the program exits
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("The shutdown hook in RTAUdioFullDuplex is executing");
                close();
                try{	
                    sleep(100);
                } catch(Exception e) {
                    System.out.print("The sleep function is broken");
                }
            }
	    });
	
    }

    /** Read audio buffer from input queue and block if queue is empty.
        @param y buffer to write to.
        @param nsamples number of samples required.
    */
    public void read(float [] y,int nsamples) {
        if(nsamples > buffersizeJASS) {
            nsamples = buffersizeJASS;
        }
        // readBuffer is kept up to dat by SourcePlayer thread, caches is always previous result
        // of tick()ing RtAudio object
        for(int i=0;i<nsamples;i++) {
            y[i] = readBuffer[i];
        }
    }

    /** Write audio buffer to output queue and block if queue is full.
        @param y output buffer.
    */
    public void write(float [] y) {
        writeReadNativeSoundFloat(nativeObjectPointer,y,y.length,readBuffer,readBuffer.length);
    }
    
    //** Close resources */
    public void close() {
        System.out.println("RTAudioFullDuplexRtAudio.close() will call closeNativeSound: nativeObjectPointer= "+nativeObjectPointer);
	if(nativeObjectPointer != 0) {
	    closeNativeSound(nativeObjectPointer);
	    nativeObjectPointer = 0;
	}
	try{
	    sleep(100);
	} catch(Exception e) {
	}
    }
    
}
