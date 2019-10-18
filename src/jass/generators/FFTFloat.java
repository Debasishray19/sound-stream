// Fast Fourier Transform (FFT) Code
// Java implementation by: Craig A. Lindley
// Last Update: 02/27/99

/* libfft.c - fast Fourier transform library
**
** Copyright (C) 1989 by Jef Poskanzer.
**
** Permission to use, copy, modify, and distribute this software and its
** documentation for any purpose and without fee is hereby granted, provided
** that the above copyright notice appear in all copies and that both that
** copyright notice and this permission notice appear in supporting
** documentation.  This software is provided "as is" without express or
** implied warranty.
*/

package jass.generators;

public class FFTFloat {

    public static void main(String[] argv) {
        int n = 4096;
        int b=12;
        FFTFloat fft = new FFTFloat(b);
        float fs=44100;
        float f = 840;
        float[] y = new float[n];
        float[] yim = new float[n];
        float[] yf = new float[n];
        for(int i=0;i<n;i++) {
            y[i] = (float)Math.sin(TWOPI*f*i/fs);
            yim[i] = 0;
            yf[i]=0;
        }
        fft.doFFT(y,yim,false);
        for(int i=0;i<n/2;i++) {
            yf[i] = (float)(10*Math.log10(y[i]*y[i]+yim[i]*yim[i]));
            float freq=i*fs/n;
            System.out.println(yf[i]+" "+freq);
        }
        
    }
	/**
	 * This is a Java implementation of the fast Fourier transform
	 * written by Jef Poskanzer. The copyright appears above.
	 */

	private static final float TWOPI = (float)(2.0 * Math.PI);
	
	// Limits on the number of bits this algorithm can utilize
	private static final int LOG2_MAXFFTSIZE = 15; // was 15

	private static final int MAXFFTSIZE = 1 << LOG2_MAXFFTSIZE;

	/**
	 * FFT class constructor
	 * Initializes code for doing a fast Fourier transform
	 *
	 * @param int bits is a power of two such that 2^b is the number
	 * of samples.
	 */
	public FFTFloat(int bits) {

		this.bits = bits;

		if (bits > LOG2_MAXFFTSIZE) {
			System.out.println("" + bits + " is too big");
			System.exit(1);
		}
		for (int i = (1 << bits) - 1; i >= 0; --i) {
			int k = 0;
			for (int j = 0; j < bits; ++j) {
				k *= 2;
				if ((i & (1 << j)) != 0)
					k++;
			}
			bitreverse[i] = k;
		}
	}

	/**
	 * A fast Fourier transform routine
	 *
	 * @param float [] xr is the real part of the data to be transformed
	 * @param float [] xi is the imaginary part of the data to be transformed
	 * (normally zero unless inverse transofrm is effect).
	 * @param boolean invFlag which is true if inverse transform is being
	 * applied. false for a forward transform.
	 */
	public void doFFT(float [] xr, float [] xi, boolean invFlag) {
		int n, n2, i, k, kn2, l, p;
		float ang, s, c, tr, ti;

		n2 = (n = (1 << bits)) / 2;

		for (l = 0; l < bits; ++l) {
			for (k = 0; k < n; k += n2) {
				for (i = 0; i < n2; ++i, ++k) {
					p = bitreverse[k / n2];
					ang = TWOPI * p / n;
					c = (float)Math.cos(ang);
					s = (float)Math.sin(ang);
					kn2 = k + n2;
					
					if (invFlag)
						s = -s;

					tr = xr[kn2] * c + xi[kn2] * s;
					ti = xi[kn2] * c - xr[kn2] * s;

					xr[kn2] = xr[k] - tr;
					xi[kn2] = xi[k] - ti;
					xr[k] += tr;
					xi[k] += ti;
				}
			}
			n2 /= 2;
		}

		for (k = 0; k < n; k++) {
			if ((i = bitreverse[k]) <= k)
				continue;

			tr = xr[k];
			ti = xi[k];
			xr[k] = xr[i];
			xi[k] = xi[i];
			xr[i] = tr;
			xi[i] = ti;
		}

		// Finally, multiply each value by 1/n, if this is the forward
		// transform.
		if (!invFlag) {
			float f = 1.0f / n;

			for (i = 0; i < n ; i++) {
				xr[i] *= f;
				xi[i] *= f;
			}
		}
	}
	// Private class data
	public int bits;
	private int [] bitreverse = new int[MAXFFTSIZE];
}
