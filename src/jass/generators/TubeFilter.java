package jass.generators;

/** A filter corresponding to a tube of a given length and shape.
    Excitation enters one side ("glottis") and sound is produced at the other
    end. Besides shape data it needs the glottal reflection coefficient and
    the total damping factor, modeling losses in the system. This class offers
    also a higher level interface to set the tube parameters through the TubeShape.
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public class TubeFilter extends KellyLochbaumFilter {

    protected TubeShape tubeShape;

    private final double c; // speed of sound
    
    /** Create and initialize.
        @param srate sampling rate in Hertz.
        @param ts a tube shape to build the filter out of
        @param c speed of sound in m/s
     */
    public TubeFilter(float srate, TubeShape ts,double c) {
        // figure out how many segments are needed for the filter
        super(srate,(int)(2*ts.getLength()*srate/c));
        this.c=c;
        tubeShape = ts;
        setKLRadii();
    }

    /** Change tube parameters (radii) and change filter accordingly
        Assumes caller has the TubeModel object and maintains it
     */
    public void changeTubeModel() {
        setKLRadii();
    }

    /**
       Set the radii in the filter according to the Tubemodel
    */
    protected void setKLRadii() {
        double len = tubeShape.getLength();
        for(int k=0;k<nTubeSections;k++) {
            double xl = (len/nTubeSections)*k;  // left boundary of k'th segment
            double xr = xl + len/nTubeSections; // right boundary
            double xc = (xl+xr)/2; // center
            cylRadius[k]=tubeShape.getRadius(xc);

        }
        computeKCoeff();
    }

}
