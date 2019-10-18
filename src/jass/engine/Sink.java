package jass.engine;
import java.util.*;

/**
   Interface for objects containing  multiple Sources.
   This object recieves input lines.
   @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/

public interface Sink {
    /** Add a Source
        @param s Source to add
        @return Object associated with the source (may be a controller, or whatever)
    */    
    Object addSource(Source s) throws SinkIsFullException;
    
    /** Remove a Source
        @param s Source to remove
    */
    void removeSource(Source s);

    /** Get array of sources.
        @return array of the Sources, null if there are none.
    */
    Source [] getSources();
}
