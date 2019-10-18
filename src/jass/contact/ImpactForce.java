package jass.contact;
import jass.engine.*;
import jass.generators.*;

public interface ImpactForce extends Source {
    void setImpactGain(float gain);
    public void setImpactDuration(float dur);
    public void bang(float force);
}


