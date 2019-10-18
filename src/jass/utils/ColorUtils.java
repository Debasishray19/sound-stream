package jass.utils;

/** Color utils
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class ColorUtils {
    static private double[] rgb;
    static private double[] lab;
    static private double[] lch;
        static private float[] lch_f;
    static private double[] xyz;
    static final double x_wp = .950456;
    static final double y_wp = 1;
    static final double z_wp = 1.088754;
    static final double th = .008856; // threshold
    
    static {
        rgb = new double[3];
        lab = new double[3];
        lch = new double[3];
        lch_f = new float[3];
        xyz = new double[3];
    }

    public static void main(String[] args) {
        double a,b,an;
        a = 0;
        b= 0;
        an = 360*arctan(a,b)/(Math.PI*2);
        System.out.println(an);
    }

    
    // arctan(a/b) in 0-2pi
    private static double arctan(double a, double b) {
        double an;
        if(a == 0) {
            if(b>0) {
                an = Math.PI/2;
            } else if(b<0) {
                an = Math.PI*3./2;
            } else {
                an = 0;
            }
        } else if(a>=0) {
            an =  Math.atan(b/a);
        } else {
            an = Math.PI + Math.atan(b/a);
        }
        if(an<0) {
            an += 2*Math.PI;
        }
        return an;
    }

    private static double f(double x) {
        double ret;
        if(x>th) {
            ret = Math.pow(x,1./3);
        } else {
            ret = 7.787*x+16./116;
        }
        return ret;
    }
    
    /** Convert rgb to L*a*b*
        @param rgb double[3] of rgb values 0-1
        @return double[3] of L*a*b*
     */
    public static double[] Rgb2lab(double[] rgb) {
        xyz[0] = .412453*rgb[0]+.357580*rgb[1]+.180423*rgb[2];
        xyz[1] = .212671*rgb[0]+.715160*rgb[1]+.072169*rgb[2];
        xyz[2] = .019334*rgb[0]+.119193*rgb[1]+.950227*rgb[2];
        double x_n = xyz[0]/x_wp;
        double y_n = xyz[1]/y_wp;
        double z_n = xyz[2]/z_wp;
        if(y_n>th) {
            lab[0] = 116.*Math.pow(y_n,1./3)-16.;
        } else {
            lab[0]=903.3*y_n;
        }
        lab[1] = 500.*(f(x_n)-f(y_n));
        lab[2] = 500.*(f(y_n)-f(z_n));
        return lab;
    }
   
    /** Convert L*a*b* to LCH (normalized luminance, chromaticity, hue;)
        Assume -100<a,b<100, truncate if outside these limits
        @param lab double[3] of L*a*b*
        @return double[3] of LCH
     */
    public static double[] Lab2lch(double[] lab) {
        lch[0] = lab[0]/100.;
        double a = lab[1];
        double b = lab[2];
        if(a>100) {
            a=100;
        } else if(a<-100) {
            a = -100;
        }
        if(b>100) {
            b=100;
        } else if(b<-100) {
            b = -100;
        }
        lch[1] = Math.sqrt((a*a+b*b)/20000.);
        lch[2] = arctan(a,b)/(Math.PI*2);
        return lch;
    }
    
    /** Convert RGB to LCH (normalized luminance, chromaticity, hue)
        @param rgb double[3] of rgb
        @return double[3] of LCH
     */
    public static double[] Rgb2lch(double[] rgb) {
        lab = Rgb2lab(rgb);
        lch = Lab2lch(lab);
        return lch;
    }

    
    /** Convert RGB to LCH (normalized luminance, chromaticity, hue)
        @param rgb_f float[3] of rgb
        @return float[3] of LCH
     */
    public static float[] Rgb2lch(float[] rgb_f) {
        rgb[0] = rgb_f[0];
        rgb[1] = rgb_f[2];
        rgb[2] = rgb_f[2];
        lab = Rgb2lab(rgb);
        lch = Lab2lch(lab);
        lch_f[0] = (float)lch[0];
        lch_f[1] = (float)lch[1];
        lch_f[2] = (float)lch[2];
        return lch_f;
    }
}
