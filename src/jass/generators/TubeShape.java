package jass.generators;

/** Describes a 1-d tube of varying radius.
    Deals with geometry only
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public interface TubeShape {

    /** Get length of tube (units not specified)
        @return length of tube
    */
    public double getLength();

    /** Get radius at point 0<x<length
        @param x where to get radius
        @return radius at x 
     */
    public double getRadius(double x);

}
