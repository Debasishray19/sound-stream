/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jass.generators;
import jass.engine.*;
import jass.generators.*;
/**
 * Implements a sticky control. Must be attached to Mixer only.
 * Has null buffer which Mixer detects (but no other generators)
 * @author kees
 */
public class StickyControl extends Out {

    /** Sampling rate in Hertz of Out. */
    public float srate;
    protected double xc=0; // controller value
    protected double x=0;  //sticky value
    protected double T=1; // delay time constant
    protected double dt; // delta t for delay implementation
    
    public StickyControl(float srate,int bufferSize) {
        super(bufferSize);
        buf = null;
        this.srate = srate;
        dt = bufferSize/srate;
    }
    public double getT() {
        return T;
    }
    public void setT(double T) {
        this.T = T;
    }   
    public double getXc() {
        return xc;
    }
    public void setXc(double xc) {
        this.xc = xc;
    }
    public void setX(double x) {
        this.x = x;
    }
    public double getX() {
        return x;
    }

    protected void computeBuffer() {
        x = xc/(1+T/dt) + x/(1+dt/T);
    }
    
}
