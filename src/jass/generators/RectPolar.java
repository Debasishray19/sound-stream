
package jass.generators;

/* calculates to and from 
   polar and rectangular coordinates
   in the frequency domain.
   @author Reynald Hoskinson (reynald@cs.ubc.ca)
 */
public class RectPolar
{
    // speed of sound in meters per second. 
    static final float speedOfSound = 340.29f;

    float headRadius = 0.3f; // meters
    // need only 1dim array for phase? 
    // or calculate all of them at same time? 

    double [] phase;
    double [] magnitude;

    int windowSize;
    
    public RectPolar(float headRadius, int windowSize) {
	this.headRadius = headRadius;
	this.windowSize = windowSize;
	phase = new double[windowSize];
	magnitude = new double[windowSize];
    }

    /* 
       The paper is a little unclear here.
       First they say that the "complex part of the measured HRTF" 
       is prone to noise and other errors. Then they say they replace the 
       phase, but as far as I understand, 
       the phase is not the same as the complex part. 
       
       They replace the phase with a value that gives the correct interaural 
       time difference for a sphere

       radius(mag + cos mag) (cos theta)/c
    */

    public double calcPhase(double mag, double theta){
	return headRadius*(mag + Math.cos(mag))*Math.cos(theta)/speedOfSound;
    }

    // given real and imaginary parts of complex number, 
    // caclulates their magnitude in polarMag

    public void getPolarMag(double [] real, double [] imag, 
			    double [] polarMag) {
	
	for (int i = 0; i < real.length; i++) {
	    polarMag[i] = Math.sqrt(real[i]*real[i] + imag[i]*imag[i]);
	}
    }
	
    // convert from polar coordinates, given in first two arrays, 
    // to rectangular, given in next two
    public void getRect(double [] mag, double [] phase, double [] real, 
			double [] imag) {
	for (int i = 0; i < mag.length; i++ ) {
	    real[i] = mag[i]*Math.cos(phase[i]);
	    imag[i] = mag[i]*Math.sin(phase[i]);
	}
    }

    // multiply 
    public void multiplyRect(double [] a, double [] aI, 
			     double [] b, double [] bI) {
	// assume for now same length
	// puts result into b
	for (int i = 0; i < a.length; i++ ){
	    b[i] = a[i]*b[i] - aI[i]*bI[i];
	    bI[i] = aI[i]*b[i] + a[i]*bI[i];
	}
    }

    /* 
     * given the fresh fft'd HRIR
     * 1) converts it to polar coordinates
     * 2) calculates new phase component based on spherical head
     * 3) converts that back to rectangular

     replaces input values 
    */
    public void constructHRTF(double [] realPart, double [] imagPart) 
    {
	// get magnitude
	getPolarMag(realPart, imagPart, magnitude);
	
	// construct phase
	for (int i =0; i < magnitude.length; i++) {
	    //   phase[i] = Math.atan2(imagPart[i], realPart[i]);
	    phase[i] = calcPhase(magnitude[i], Math.atan2
		      (imagPart[i], realPart[i]));
	}
	// incidentally, why is it atan2(y,x), instead of (x, y)?
	// the other way worked pretty well!
	// convert back to rectangular coordinates. 
	getRect(magnitude, phase, realPart, imagPart);
    }

    // test out!
    public static void main(String [] args) {
	float headRadius = 0.3f;
	int length = 64;
	RectPolar rp = new RectPolar(headRadius, length);
	double [] bleh = new double[length];
	double [] bleh_img = new double[length];
	for (int i = 0; i < length; i++) {
	    bleh[i] = i/5.0;
	    bleh_img[i] = 0;
	}


	System.out.println("original");
	for (int i =0; i < length; i++) {
	    System.out.println(bleh[i] + " " + bleh_img[i]);
	}
	FFT fft = new FFT(6);
	fft.doFFT(bleh, bleh_img, false);

	System.out.println("after");
	rp.constructHRTF(bleh, bleh_img);

	fft.doFFT(bleh, bleh_img, true);
	for (int i =0; i < length; i++) {
	    System.out.println(bleh[i] + " " + bleh_img[i]);
	}
    }
}
