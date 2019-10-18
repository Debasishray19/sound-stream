package jass.utils;
import jass.generators.*;

/**
   Plot spectral response of a jass.generators.Filter and display.
   Also compute resonance frequencies ("formants").
*/


public class FormantsPlotter {
    protected IRPFilter irpFilter=null;
    protected double[][] plotData = null;
    protected PlotGraph plotGraph = null;
    protected float srate;
    // for formant finding
    protected int[] formantIndex;
    protected int MAX_FORMANTS;
    protected int nFormants = 0;
    //    protected int bits = 12;
    protected int bits = 15;

    protected int n = 1<<bits; // FFT window size
    protected int topleft_x=600;
    protected int topleft_y=0;

    public FormantsPlotter() {
	MAX_FORMANTS = 100;
        formantIndex = new int[MAX_FORMANTS];
    }
    
    public void setLocation(int topleft_x,int topleft_y) {
        this.topleft_x = topleft_x;
        this.topleft_y = topleft_y;
    }
    
    public void close() {
        plotGraph.close();
    }

    public void setNumFormants(int num) {
        MAX_FORMANTS = num;
        formantIndex = new int[MAX_FORMANTS];
    }
    
    
    public void dumpData(Filter filter, float srate) {
        int np = (int)(n/2); // up to Nyquist rate
        if(irpFilter == null) {
            irpFilter = new IRPFilter();
        }
        float[][]res = irpFilter.computeIRP(filter,bits,srate);
	System.out.println("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        for(int i=0;i<np;i++) {
            System.out.println(res[i][1]+" "+res[i][0]);
        }
	System.out.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
    }
    
    public void plotFormants(Filter filter, float srate) {
        int np = (int)(n/3); // up to Nyquist rate
        if(irpFilter == null) {
            irpFilter = new IRPFilter();
        }
        float[][]res = irpFilter.computeIRP(filter,bits,srate);
        if(plotData==null) {
            plotData = new double[2][np];
        }
        for(int i=0;i<np;i++) {
            plotData[0][i] = res[i][1]; //x
            plotData[1][i] = res[i][0]; //y (i.e., dB)
        }
        if(plotGraph == null) {
            System.out.println("CREATE new graph");
            plotGraph = new PlotGraph(plotData);
            plotGraph.setCloseChoice(0); //0 hide, 1 exit
            plotGraph.rescaleX(.5);
            plotGraph.rescaleY(.5);
        } else {
            plotGraph.initialise(plotData);
        }
        plotGraph.setLocation(topleft_x,topleft_y);
        plotGraph.setLine(1);
        plotGraph.setPoint(0);

        // find formants
	String str = "Formants: ";
	String str2 = "Bandwidth: ";
	quadratic q = new quadratic();
        nFormants = 0;
        for(int i=1;i<np-1;i++) {
            double prev = plotData[1][i-1];
            double next = plotData[1][i+1];
            double db = plotData[1][i];

	    double x0 = plotData[0][i-1];
	    double x1 = plotData[0][i];
	    double x2 = plotData[0][i+1];

	    if (nFormants<MAX_FORMANTS) {
		q.set(x0, x1, x2, prev, db, next);
		if (q.containsPeak()) {
		    double upper3dB;
		    double lower3dB;
		    //System.out.println("found peak @ x="+q.getPeakX()+" y="+q.getPeakY());
		    str+= (int)q.getPeakX() + "Hz, ";
		    //use these lines instead for 2 decimal points:
		    //int dec = (int)(100*(q.getPeakX()-(int)q.getPeakX()));
		    //str += (int)q.getPeakX() + "."+dec+"Hz, ";
		    
		    formantIndex[nFormants++] = i;
		    upper3dB = q.getUpper3dB();
		    lower3dB = q.getLower3dB();
		    if (q.getUpper3dB() > x2) {
			//System.out.println("upper out of range... do search");
			upper3dB = search(q.getPeakY(), i+1, np, true);
			}
		    
		    if (q.getLower3dB() < x0) {
			//System.out.println("lower out of range... do search");
			lower3dB = search(q.getPeakY(), i-1, np, false);
		    }
		    if ( (upper3dB == -1) || (lower3dB ==-1) )
		    {
			str2+= -1 + "Hz, ";
		    }
		    else
		    {
			str2 += (int)(upper3dB - lower3dB)+"Hz, ";
			//use these two lines instead for 2 decimal points.
			//int dec = (int)(100*((upper3dB-lower3dB)-(int)(upper3dB-lower3dB)));
			//str2 += (int)(upper3dB - lower3dB) + "."+dec+"Hz, ";
		    }
		}
	    }
	}
        plotGraph.setGraphTitle(str);
	plotGraph.setGraphTitle2(str2);
        plotGraph.plot();
    }  
    
    private double search(double max, int idx, int np, boolean up) 
	{
	    double x_tmp;
	    quadratic q = new quadratic();

	    double y0 = 0;
	    double y1 = 0;
	    double y2 = 0;

	    double x0 = 0;
	    double x1 = 0;
	    double x2 = 0;
		
	    if (up == true)
	    {
		for (int k=idx; k<np-1; k++) 
		{
		    y0 = plotData[1][k-1];
		    y1 = plotData[1][k];
		    y2 = plotData[1][k+1];
		
		    x0 = plotData[0][k-1];
		    x1 = plotData[0][k];
		    x2 = plotData[0][k+1];
				
		    q.set(x0, x1, x2, y0, y1, y2);
		    x_tmp = q.findX_left(max-3);//replace with 'findLower3dB()?
		    //System.out.println("UPPER @ "+(int)x_tmp + " k="+k+" x0="+(int)x0+" x1="+(int)x1);
		    //looking for the upper 3dB point, we first look at the left solution.
		    if ( (x_tmp >= x0) && (x_tmp <= x1) ) 
		    {
			return x_tmp;
		    }
		    else 
		    {
			x_tmp = q.findX_right(max-3);
			if ( (x_tmp >= x0) && (x_tmp <= x1) ) 
			{
			    return x_tmp;
			}
		    }
		}
	    }	
	    else
	    {
	    	for (int k=idx; k>0; k--)
	    	{
		    y0 = plotData[1][k-1];
		    y1 = plotData[1][k];
		    y2 = plotData[1][k+1];
		
		    x0 = plotData[0][k-1];
		    x1 = plotData[0][k];
		    x2 = plotData[0][k+1];
				
		    q.set(x0, x1, x2, y0, y1, y2);
		    // looking for the lower 3dB point, we first look at the RIGHT (closest) solution
		    x_tmp = q.findX_right(max-3);
		    //System.out.println("LOWER @ "+(int)x_tmp + " k="+k+" x0="+(int)x0+" x1="+(int)x1+"  ..tgt val="+(max-3));
		    if ( (x_tmp >= x0) && (x_tmp <= x1) ) 
		    {
			return x_tmp;
		    }
		    else 
		    {
			x_tmp = q.findX_left(max-3);
			if ( (x_tmp >= x0) && (x_tmp <= x1) ) 
			{
			    return x_tmp;
			}
		    }
	    	}
	    }
	    //if we didn't find anything, return -1;
	    return -1;
	}

    private class quadratic 
    {
	private double a;
	private double b;
	private double c;
	private boolean hasPeak;
	private double peakX;
	private double peakY;

	public quadratic() 
	{
	}

	public quadratic(double x0, double x1, double x2,
			 double y0, double y1, double y2) 
	{
	    set(x0,x1,x2,y0,y1,y2);
	}

	public void set(double x0, double x1, double x2,
		   double y0, double y1, double y2) 
	{

	    //uses lagrangian polynomials to find interpolation formula
	    //denominators for each term in the series
	    double l_0den = x0*x0 - x0*(x1+x2) + x1*x2;
	    double l_1den = x1*x1 - x1*(x0+x2) + x0*x2;
	    double l_2den = x2*x2 - x2*(x0+x1) + x0*x1;
	    if ( (l_0den != 0) && (l_1den != 0) && (l_2den != 0) )
	    {
		    //a = x^2 term; b = x^1; term c = x^0	
		    a = y0/l_0den + y1/l_1den + y2/l_2den;
		    b = -y0*(x1+x2)/l_0den - y1*(x0+x2)/l_1den - y2*(x0+x1)/l_2den;
		    c = y0*x1*x2/l_0den + y1*x0*x2/l_1den + y2*x0*x1/l_2den;

		    peakX = -0.5*b/a; //X value of possible maximum
		    peakY = a*peakX*peakX + b*peakX + c; //Y value of possible maximum

		    //maximum occurs when: 
		    //-concave down (2a < 0)
		    //-middle value larger than y0 and y2
		    //-X value is within x0 and x2
		    //System.out.println("a: "+a+" b: "+b+" c: "+c);
		    if ( (a<0) && (y1>y0) && (y1>y2) && (peakX>=x0) && (peakX <= x2) ) 
		    {
			hasPeak = true;
		    }      
		    else 
		    {
			hasPeak = false;
			peakX = -1;
		    }
	    }
	    else
	    {
		hasPeak = false;
		peakX = -1;
		peakY = 0;
	    }
	}

	public boolean containsPeak() 
	{
	    return hasPeak;
	}
	public double getUpper3dB() 
	{
	    //double shiftedC = c - peakY + 3;
	    //return (-b - Math.sqrt(b*b-4*a*shiftedC)) / (2*a);
	    return findX_right(peakY - 3);
	}
	public double getLower3dB() 
	{
	    //double shiftedC = c - peakY + 3;		    
	    //return (-b + Math.sqrt(b*b-4*a*shiftedC)) / (2*a);
	    return findX_left(peakY - 3);
	}
	public double getPeakX() 
	{
	    return peakX;
	}
	public double getPeakY() 
	{
	    return peakY;
	}
	public double findX_left(double Y) 
	{
	    double shiftedC = c - Y;
	    if (a!= 0)
	    {
		return (-b + Math.sqrt(b*b-4*a*shiftedC)) / (2*a);
	    }
	    else
	    {
		return -shiftedC/b;
	    }
	}
	public double findX_right(double Y) 
	{
	    double shiftedC = c - Y;
	    if (a!=0)
	    {
		return (-b - Math.sqrt(b*b-4*a*shiftedC)) / (2*a);
	    }
	    else
	    {
		return -shiftedC/b;
	    }
	}
    }

}
