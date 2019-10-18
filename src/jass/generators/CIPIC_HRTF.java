/**
   class which encapsulates most of CIPIC data structure 
   for HRIR
       @author Reynald Hoskinson (reynald@cs.ubc.ca)
*/
package jass.generators;

public class CIPIC_HRTF {

    final boolean DEBUG = true;

    int       elmax = 50;
    int    [] elindices;
    double [] elevations;
    double [] azimuths;
    double [] azimuths_err;

    double elerr;
    double azerr;
    int [] returnValue;

    public CIPIC_HRTF() {

	elindices =    new int[elmax];
	elevations =   new double[elmax];
	
	azimuths =     new double[25];
	azimuths_err =    new double[25];
	returnValue = new int[2];

	int i;
	for (i = 0; i < elmax; i++) {
	    elindices[i] = i;
	    elevations[i] = -45 + 5.625*(elindices[i]-1);
	}
	azimuths[0] = -80.0;
	azimuths[1] = -65.0;
	azimuths[2] = -55.0;
      	i = 3;
	for (int j = -45; j < 50; j+=5) {
	    azimuths[i++] = j;
	}
	azimuths[i++] = 55;
	azimuths[i++] = 65;
	azimuths[i] = 80;
    }

    // gets a coordinate in th HRTF for specific azimuth, elevation 
    // need to interpolate
    // from CIPIC
    // errors are recorded, and can be looked at later. 
    
    /* returns coordinates of azimuth, elevation
       closest to those given as inputs
     */
    public int[] getNearest(double azimuth, double elevation) {
	//	System.out.println("getnearest: az: " + azimuth + " elev " + elevation);
	if ((azimuth < -90.0) || (azimuth > 90.0)) {
	    System.out.println("error: invalid azimuth");
	}
	
	double el = Math.round((elevation+45.0)/5.625);
	el = Math.max(el, 1.0);
	elerr = elevation - elevations[(int)el];	

	double min = 180f;
	int    min_index = 0;
	for (int i = 0; i < azimuths.length; i++) {
	    azimuths_err[i] = Math.abs(Math.abs(azimuths[i] - azimuth));
	    if (azimuths_err[i] < min ) {
		min = azimuths_err[i];
		min_index = i;
	    }
	}
	azerr = min;
	returnValue[0] = min_index;  // azimuth
	returnValue[1] = (int)el -1;
	// System.out.println("return : " + returnValue[0] + " " + returnValue[1]); 
	return returnValue;
    }

    public int[] getNearestPVALDEG(double azimuth, double elevation) {
	//	System.out.println("getnearest: az: " + azimuth + " elev " + elevation);
	 double az = pvaldeg(azimuth);
	if ((az < -90.0) || (az > 90.0)) {
	    System.out.println("error: invalid azimuth");
	}
	
	double elev = pvaldeg(elevation);
	double el = Math.round((elev+45.0)/5.625);
	el = Math.max(el, 1.0);
       
	// el = Math.min(el, elmax);
	
	// is this really true? aren't I pdvadeg'ing elev twice? 
	if (DEBUG) {
	    if (el > 50.0) {
		System.out.println("el: " + el);    
	    }
	    elerr = pvaldeg(elev - elevations[(int)el]);	
	}
	double min = 180f;
	int    min_index = 0;
	for (int i = 0; i < azimuths.length; i++) {
	    azimuths_err[i] = Math.abs(pvaldeg(Math.abs(azimuths[i] - az)));
	    if (azimuths_err[i] < min ) {
		min = azimuths_err[i];
		min_index = i;
	    }
	}
	az = min_index;
	azerr = min;
	returnValue[0] = min_index;
	returnValue[1] = (int)el -1;
	// System.out.println("return : " + returnValue[0] + " " + returnValue[1]); 
	return returnValue;
    }

    // returns azimuth error for last pulse caclulated
    public double getAzimuthError() {
	return azerr;	
    }

    // returns elevation error for last pulse caclulated
    public double getElevationError() {
	return elerr;	
    }

    /* 
       finds a principal value of an angle, converting it to the range
       [-90, 270]
    */
    public double pvaldeg(double angle) {
	
	double dtr = Math.PI/180.0;
	/* 
	   atan2 returns the theta component of the point (r, theta) 
	   in polar coordinates that corresponds to the point (x, y) 
	   in Cartesian coordinates.
	*/
	double angl = Math.atan2(Math.sin(angle*dtr), Math.cos(angle*dtr))/dtr;
	
	if (angl < -90.0) {
	    angl = angle + 360.0;
	}
	return angl;
    }
}
