package jass.render;

import java.io.*;
import javax.sound.sampled.*;

/**
Convert raw audio file to .wav. A raw file is basically a wav file without header.
@author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
    
public class ConvertRawToWav {

    
    public static void convertRawToWav(double srate,String fn) throws Exception {
        String fnout = fn+".wav";
        FileInputStream inStream = new FileInputStream(new File(fn));
        File out = new File(fnout);
        int bytesAvailable = inStream.available();
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = false;
        boolean bigEndian = false;
        AudioFormat audioFormat = new AudioFormat((float)srate, sampleSizeInBits, channels, signed, bigEndian); 
        AudioInputStream  audioInputStream = new AudioInputStream(inStream,audioFormat,bytesAvailable/2);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
        audioInputStream.close();
        inStream.close();
    }
    
    public static void main (String args[]) throws Exception {
        double srate = 44100.;
        srate = Double.parseDouble(args[1]);
        String fn = args[0];
        convertRawToWav(srate,fn);
        
    }
}
