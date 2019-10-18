package jass.generators;

/* 
    @author Reynald Hoskinson (reynald@cs.ubc.ca)   
   give it an azimuth and elevation
   subclass has to return HRTF data from the
   database. 
*/


public class PositionData {
    
    protected float azimuth;
    protected float elevation;
    
    protected float x;
    protected float y;
    protected float z;


    /* 
     * initialize 
     */

    public PositionData() {
	
    }
    public PositionData(float az, float el) {
	azimuth = az;
	elevation = el;
    }
    public PositionData(float x, float y, float z) {
	this.x = x;
	this.y = y;
	this.z = z;
    }

    public float  getAzimuth() {
	return azimuth;
    }
    public float  getElevation() {
	return elevation;
    }
    public float  getX() {
	return x;
    }
    public float  getY() {
	return y;
    }
    public float  getZ() {
	return z;
    }

    public void  setAzimuth(float az) {
	azimuth = az;
    }
    public void  setElevation(float el) {
	elevation = el;
    }
    public void  setX(float X) {
	x = X;
    }
    public void  setY(float Y) {
	y = Y;
    }
    public void  setZ(float Z) {
	z = Z;
    }
}
