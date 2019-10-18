package jass.generators;

/** Describes a 1-d tube of varying radius, specified by a bunch of equidistant radii,
    starting at begin and ending at end. So there is one more radius than segments.
    Deals with geometry only
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public class TubeModel implements TubeShape {

    private int nRadii; 
    private double[] r; //radii
    private double length;

    /** Create tube defined by a set of radii from glottis to lip end.
        Then interpolate the shape.
        @param nRadii number of segments
    */
    public TubeModel(int nRadii) {
        this.nRadii = nRadii;
        r = new double[nRadii];
    }

    /** Set the length
        @param length length
    */
    public void setLength(double length) {
        this.length = length;
    }
    
    /** Set the k'th radius
        @param k index of radius (starting at 0)
        @param r radius
    */
    public void setRadius(int k,double r) {
        this.r[k] = r;
    }
        
    /** Get the k'th radius
        @param k index of radius (starting at 0)
        @return radius
    */
    public double getRadius(int k) {
        return this.r[k];
    }
    
    /** Get length (units not specified)
        @return length of tube
    */
    public double getLength() {
        return length;
    }

    /** Get radius at point 0<x<length by interpolation of the data
        @param x where to get radius
        @return radius at x
     */
    public double getRadius(double x) {
        double t = x/length;
        if(t<0) {
            t=0;
        } else if(t>=1) {
            t=.999999;
        }
        // do linear interpolation
        int nSegments = nRadii-1;
        int segment = (int)(t*nSegments);
        double residue  = t*nSegments - segment; //0-1
        double left_r = r[segment];
        double right_r = r[segment+1];
        double r_interp = left_r*(1-residue)+right_r*residue;
        return r_interp;
    }

}
