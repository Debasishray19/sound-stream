package VTNT_JASS;


import jass.render.*;
import jass.generators.*;
import jass.utils.*;
import java.awt.*;
import javax.swing.*;
import matlabcontrol.*;
import matlabcontrol.extensions.*;
import processing.core.*;

public class VTNTDemo implements Runnable {

    static final String aboutStr = "This demo uses a numerical solution of the linearized 1D Navier-Stokes PDE as well as a lip model. Select various vowels by clicking the buttons in the control panel. This will  generate a  particular vocal  tract shape  matching the  vowel and synthesize  the  sound of  exciting  such a  vocal  tract  shape with  a Rosenberg type glottal excitation. The  airway model is morphed in order to fit this  tube model.  Start the timeline in order  to see the airway change.   You can  also tweak  reflection  coefficients at  the lip  and glottis,  attenuation (damping)  and the  4 glottal  parameters  for the excitation. The formants window plots  the spectrum of the sound.  After you move  the sliders you need to  click the formants button  to see the update.\n The particular vowels implemented here are the six Russian vowels as described in \"Acoustics Theory of Speech Production\", Chapter 2.3, Gunnar Fant, 1970. Nasal tract is controlled by parameters Velum (0 nasal tract is closed, 1 vocal tract is closed) and M-N balance which determines the mix out the nose and mouth sound sources. Finally the geometry can be specified with the sliders.";
    
    int  nTubeSections;    
    double[] tract;// = new double[nTubeSections]; // for presets
    //static final double tubeLength=-1;
    String[] args;// = {".17","44100",".10","6","5",".5"};
    SourcePlayer player;
    Controller a_controlPanel; // VT
    Controller a_controlPanelNasal; // nasal tract
    Controller a_controlPanelRosenberg; // Rosenberg glottal model
    Controller a_controlPanelTwoMass; // Ishizak-Flanagan model
    //Airway airway=null;
    FormantsPlotter formantsPlotter;
    boolean useTwoMassModel = true; // or if false use Rosenberg model
    float srate;
    boolean slideStarted = false;

    public String getAbout() {
        return aboutStr;
    }

    private boolean haltMe=false;
    
    public void detach() {
        halt();
        System.out.println("halt!!");
    }

    public void halt() {
        haltMe=true;
        player.stopPlaying();
        a_controlPanel.dispose();
        a_controlPanelRosenberg.dispose();
        a_controlPanelTwoMass.dispose();
        a_controlPanelNasal.dispose();
        if(formantsPlotter != null) {
            formantsPlotter.close();
        }
    }
    
    public VTNTDemo(String[] args) {
        super();
        this.args=args;
        Thread thread = new Thread(this);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
        /*
        while(airway==null) {
            try {
                Thread.sleep(50);
            } catch(Exception e){}
        }
        addModel(airway);
        */
    }
    
    /****************IMPLEMENT PROCESSING*****************/
    public static class MySketch extends PApplet{
    	/***LINK: https://processing.github.io/processing-javadocs/core/index.html?processing/core/PApplet.html***/
    	//Define the global variables
    	static float circleX=0;
    	static float circleY=0;
    	float diameter=50;

    	public void settings(){
    	  size(700,700);
    	  
    	}//End of void setup()

    	public void draw(){
    	  //Set up the background as black
    	  background(0);
    	  /***Create a Circle***/
    	  fill(255,0,0);
    	  ellipse(circleX, circleY, diameter,diameter);
    	  
    	  if(circleX<diameter/2)
    	      circleX = diameter/2;
    	      
    	  if(circleX>(width - diameter/2))
    	      circleX = width - diameter/2;  
    	      
    	  if(circleY<diameter/2)
    	      circleY = diameter/2;
    	      
    	  if(circleY>(height - diameter/2))
    	      circleY = height - diameter/2;
    	  
    	  textSize(25);
    	  text("Freq:",20,30);
    	  text((int)circleX, 85,30);
    	  
    	  text("Gain:",150,30);
    	  text((int)circleY, 215,30);
    	      
    	}//End of void draw()

    	public void mouseDragged(){
    	  circleX = mouseX;
    	  circleY=mouseY;
    	}//End of mouseDragged
    	
    	
        public static void main(String[] args) {
    		String[] varProcessing = {"processJavaInteg"};
    		MySketch mysketch = new MySketch();
    		PApplet.runSketch(varProcessing, mysketch);
            new VTNTDemo(args);
        }
    	
    }
    /********************END***************************/

    public void run() {
        int bufferSize = 256;//512;
        int bufferSizeJavaSound = 1024*8;
        //        int nchannels = 1;
        int nTubeSectionsNasal;
        
        if(args.length != 6) {
            System.out.println("Usage: java VTNTDemo .17 srate nasalLen nNasalSections nTubeSections cflNumber");
            return;
        }
        double tubeLength = Double.parseDouble(args[0]);
        double tubeLengthNasal = Double.parseDouble(args[2]);
        
        nTubeSectionsNasal = Integer.parseInt(args[3]); // only for control
        nTubeSections = Integer.parseInt(args[4]); // only for control
        
        tract = new double[nTubeSections];
        srate = (float) Double.parseDouble(args[1]);
        double cflNumber = Double.parseDouble(args[5]);
        // TubeModel will decide how many segments are needed and interpolate
        final TubeModel tm = new TubeModel(nTubeSections);
        final TubeModel tmNasal = new TubeModel(nTubeSectionsNasal);
        tm.setLength(tubeLength);
        tmNasal.setLength(tubeLengthNasal);
        player = new SourcePlayer(bufferSize,bufferSizeJavaSound,srate);
        double c= 350; // vel. of sound
        double minLen = .15;
        double minLenNasal = tubeLengthNasal;
        final RightLoadedWebsterTube filter = new RightLoadedWebsterTube(srate,tm,minLen,tmNasal,minLenNasal,cflNumber);
        filter.useLipModel = !filter.useLipModel; // set to false

        final RightLoadedWebsterTube filterCopy = new RightLoadedWebsterTube(srate,tm,minLen,tmNasal,minLenNasal,cflNumber);
        filterCopy.setOutputVelocity(true); // to display formants correctly
        filterCopy.useLipModel = !filterCopy.useLipModel; // set to false (will be reset later on)

        final FilterContainer filterContainer = new FilterContainer(srate,bufferSize,filter);
        final GlottalWave source = new GlottalWave(srate,bufferSize);
        final TwoMassModel twoMassSource = new TwoMassModel(bufferSize,srate);
        final RandOut randOut= new RandOut(bufferSize);
        final Silence silence= new Silence(bufferSize);

        filterCopy.setFlowNoiseLevel(0); // no noise for spectrum

        try {
            if(useTwoMassModel) {
                filter.setTwoMassModel(twoMassSource);
                filterContainer.addSource(source); // add Rosenberg source also
                //filterContainer.addSource(randOut); // add Rand source
            } else {
                filterContainer.addSource(source);
            }
            player.addSource(filterContainer);
            //player.addSource(twoMassSource);
            
        } catch(Exception e) {}
        
        preset("a");
        for(int i=0;i<nTubeSections;i++) {
            tm.setRadius(i,tract[i]);
        }
        //airway.init(tm);
        filter.changeTubeModel();
        filterCopy.changeTubeModel();

        // set up control panels
        
        // Vocal tract control panel:        
        int nbuttons = 4 + 7 + 2;
        final int nAuxSliders = 5;
        final int nSliders = nTubeSections+nAuxSliders;
        String[] names = new String[nSliders];
        double[] val = new double[nSliders];
        double[] min = new double[nSliders];
        double[] max = new double[nSliders];
        final int tubelengthSliderIndex = nAuxSliders-1;
        //names[0] = "f1 ";
        //val[0] = 250; min[0] = 10; max[0] = 800;
        //names[1] = "f2 ";
        //val[1] = 2000; min[1] = 801; max[1] = 10000;

        names[0] = "u_xx mult"; 
        val[0] = 1; min[0] = 0.0; max[0] = 20;
        names[1] = "u mult";
        val[1] = 1; min[1] = 0.0; max[1] = 20;
        names[2] = "wall coeff ";
        val[2] = 1; min[2] = 0; max[2] = 5;
        names[3] = "lipCf ";
        val[3] = 1; min[3] = .05; max[3] = 30;
        names[4] = "length ";
        val[4] = tubeLength; min[4] = .15; max[4] = tubeLength*4;
        
        double minA = 1;
        double maxA = 20;
        for(int k=nAuxSliders;k<nSliders;k++) {
            names[k] = "A("+new Integer(k-nAuxSliders).toString() + ") ";
            val[k] = 1;
            min[k] = minA;
            max[k] = maxA;
            double r=Math.sqrt(val[k]/Math.PI);
            tm.setRadius(k-nAuxSliders,r/100); // in meters
            //tmAirway.setRadius(k-nAuxSliders,r); // in cm!
        }

        a_controlPanel = new Controller(new java.awt.Frame ("Vocal Tract"),
                                        false,val.length,nbuttons) {
                private static final long serialVersionUID = 1L;

                boolean muted=false;

                private void handleReset() {
                        filter.reset();
                        twoMassSource.reset();
                        player.resetAGC();
                        muted = !muted;
                        player.setMute(muted);
                        player.resetAGC();
                        try {
                            Thread.sleep(100);
                        } catch(Exception e){};
                        muted = !muted;
                        player.setMute(muted);
                        player.resetAGC();
                }
                
                public void onButton(int k) {
                    switch(k) {
                    case 0: 
                        handleReset();
                        slideStarted = !slideStarted;
                        break;
                    case 1: {
                        FileDialog fd = new FileDialog(new Frame(),"Save");
                        fd.setMode(FileDialog.SAVE);
                        fd.setVisible(true);
                        saveToFile(fd.getFile());
                    }
                        break;
                    case 2: {
                        FileDialog fd = new FileDialog(new Frame(),"Load");
                        fd.setMode(FileDialog.LOAD);
                        fd.setVisible(true);
                        loadFromFile(fd.getFile());
                        handleReset();
                    }
                        break;
                    case 3: {
                        muted = !muted;
                        player.setMute(muted);
                        player.resetAGC();
                    }
                        break;
                        
                    case 4: {
                        preset("a");
                        double tubeLen = .17;
                        handlePresetChange(tubeLen);

                    }
                        break;
                    case 5: {
                        preset("o");
                        double tubeLen = .185;
                        handlePresetChange(tubeLen);
                    }
                        break;
                    case 6: {
                        preset("u");
                        double tubeLen = .195;
                        handlePresetChange(tubeLen);
                    }
                        break;
                    case 7: {
                        preset("i_");
                        double tubeLen = .19;
                        handlePresetChange(tubeLen);
                    }
                        break;
                    case 8: {
                        preset("i");
                        double tubeLen = .165;
                        handlePresetChange(tubeLen);
                    }
                        break;
                    case 9: {
                        preset("e");
                        double tubeLen = .165;
                        handlePresetChange(tubeLen);
                    }
                        break;
                    case 10: {
                        preset("-");
                        double tubeLen = .17;
                        handlePresetChange(tubeLen);
                    }
                        break;
                    case 11: { //plot formants
                        updateFormantsPlot();
                    }
                        break;
                    case 12: { //toggle lipmodel
                        filter.useLipModel = !filter.useLipModel;
                        filterCopy.useLipModel = filter.useLipModel;
                        if(filter.useLipModel) {
                            a_controlPanel.setButtonName("ToggleLipModel (is on)",12);
                        } else {
                            a_controlPanel.setButtonName("ToggleLipModel (is off)",12);
                        }
                        handleReset();
                    }
                        break;
                    }
                }
                
                private void handlePresetChange(double tubeLen) {
                    tm.setLength(tubeLen);
                    val[tubelengthSliderIndex] = tubeLen;
                    //min[tubelengthSliderIndex] = tubeLen;
                    for(int k=nAuxSliders;k<nSliders;k++) {
                        val[k] = 100*tract[k-nAuxSliders]; // in cm!
                        val[k] *= val[k];
                        val[k] *= Math.PI;
                    }
                    a_controlPanel.setSliders(val,min,max,names);
                    for(int i=0;i<nTubeSections;i++) {
                        tm.setRadius(i,tract[i]);
                    }
                    filter.changeTubeModel();
                    handleReset();
                    //updateFormantsPlot();
                    
                }
                
                private void updateFormantsPlot() {
                    filterCopy.changeTubeModel();
                    filterCopy.reset();
                    if(formantsPlotter == null) {
                        formantsPlotter = new FormantsPlotter();
                        formantsPlotter.setLocation(300,500);
                    }
                    formantsPlotter.plotFormants(filterCopy,srate);
                    //formantsPlotter.dumpData(filterCopy,srate);
                }

                
                public void onSlider(int k) {
                    switch(k) {

                    case 0:
                        filter.multDSecond = this.val[k];
                        filterCopy.multDSecond = this.val[k];
                        filter.changeTubeModel();
                        break;
                    case 1:
                        filter.multDWall = this.val[k];
                        filterCopy.multDWall = this.val[k];
                        break;
                    case 2:
                        filter.setWallPressureCoupling((double)this.val[k]);
                        filterCopy.setWallPressureCoupling((double)this.val[k]);
                        filter.changeTubeModel();
                        break;
                    case 3:
                    	filter.lipAreaMultiplier = (double)this.val[k];
                    	filterCopy.lipAreaMultiplier = (double)this.val[k];
                        filter.changeTubeModel();
                        break;
                    case 4:
                        tm.setLength((double)this.val[k]);
                        filter.changeTubeModel();
                        break;
                    default:
                        double r=Math.sqrt(val[k]/Math.PI);
                        tm.setRadius(k-nAuxSliders,r/100);// in meters
                        //tmAirway.setRadius(k-nAuxSliders,r);// in cm
                        filter.changeTubeModel();
                        break;
                    }
                }
            };

        a_controlPanel.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent e) {
                    System.out.println("Close handler called");
                    player.stopPlaying();
                }
            });
        
        a_controlPanel.setSliders(val,min,max,names);
        a_controlPanel.setButtonNames (new String[] {"Reset","Save","Load","(Un)mute","[a]","[o]","[u]","[i-]","[i]","[e]","[-]","Formants","ToggleLipModel (is on)"});
        a_controlPanel.setVisible(true);
        a_controlPanel.onButton(nbuttons-1); // put up formants
        


        // End Vocal tract control panel:        

        // Nasal tract control panel:
        int nbuttonsNasal = 2;
        final int nAuxSlidersNasal = 3;
        int nSlidersNasal = nTubeSectionsNasal+nAuxSlidersNasal;
        String[] namesNasal = new String[nSlidersNasal];
        final double[] valNasal = new double[nSlidersNasal];
        double[] minNasal = new double[nSlidersNasal];
        double[] maxNasal = new double[nSlidersNasal];
        namesNasal[0] = "Velum(0noNasal)";
        valNasal[0] = 0.0; minNasal[0] = 0; maxNasal[0] = 1;
        namesNasal[1] = "M-N Bal";
        valNasal[1] = .5; minNasal[1] = 0; maxNasal[1] = 1;
        namesNasal[2] = "NasalLen";
        valNasal[2] = .11; minNasal[2] = .1; maxNasal[2] = .18;
        double minANasal = .01;
        double maxANasal = 10;
        double[] dangHondaFig6 = {.7,1.5,5,1,.8,.5,.6,.8}; // 8 sliders
        int ii=0;
        for(int k=nAuxSlidersNasal;k<nSlidersNasal;k++,ii++) {
            namesNasal[k] = "A("+new Integer(k-nAuxSlidersNasal).toString() + ") ";
            valNasal[k] = dangHondaFig6[ii];
            minNasal[k] = minANasal;
            maxNasal[k] = maxANasal;
            double r=Math.sqrt(valNasal[k]/Math.PI);
            tmNasal.setRadius(k-nAuxSlidersNasal,r/100); // in meters
        }
        
        a_controlPanelNasal = new Controller(new java.awt.Frame ("Nasal Tract"),
                                        false,valNasal.length,nbuttonsNasal) {
                private static final long serialVersionUID = 2L;

                boolean muted=false;
                
                public void onButton(int k) {
                    switch(k) {
                    case 0: {
                        FileDialog fd = new FileDialog(new Frame(),"Save");
                        fd.setMode(FileDialog.SAVE);
                        fd.setVisible(true);
                        saveToFile(fd.getFile());
                    }
                        break;
                    case 1: {
                        FileDialog fd = new FileDialog(new Frame(),"Load");
                        fd.setMode(FileDialog.LOAD);
                        fd.setVisible(true);
                        loadFromFile(fd.getFile());
                    }
                        break;
                        
                    }
                }
            
                public void onSlider(int k) {
                    switch(k) {
                    case 0:
                        filter.velumNasal = this.val[k];
                        filterCopy.velumNasal = this.val[k];
                        break;
                    case 1:
                        filter.mouthNoseBalance = this.val[k];
                        filterCopy.mouthNoseBalance = this.val[k];
                        break;
                    case 2:
                        tmNasal.setLength((double)this.val[k]);
                        filter.changeTubeModel();
                        break;
                    default:
                        double r=Math.sqrt(this.val[k]/Math.PI);
                        tmNasal.setRadius(k-nAuxSlidersNasal,r/100);// in meters
                        filter.changeTubeModel();
                        break;
                    }
                }
            
            };
        
        a_controlPanelNasal.setSliders(valNasal,minNasal,maxNasal,namesNasal);
        a_controlPanelNasal.setButtonNames (new String[] {"Save","Load"});
        a_controlPanelNasal.setVisible(true);

        // End Nasal tract control panel:

        // Rosenberg glottal source control panel:
        int nbuttonsRosenberg = 2;
        int nSlidersRosenberg = 4;
        String[] namesRosenberg = new String[nSlidersRosenberg];
        double[] valRosenberg = new double[nSlidersRosenberg];
        double[] minRosenberg = new double[nSlidersRosenberg];
        double[] maxRosenberg = new double[nSlidersRosenberg];

        namesRosenberg[0] = "freq";
        valRosenberg[0] = 100; minRosenberg[0] = 0; maxRosenberg[0] = 200;
        namesRosenberg[1] = "openQ";
        valRosenberg[1] = .5; minRosenberg[1] = 0.001; maxRosenberg[1] = 1;
        namesRosenberg[2] = "slopeQ";
        valRosenberg[2] = 4; minRosenberg[2] = .15; maxRosenberg[2] = 10;
        namesRosenberg[3] = "gain";
        valRosenberg[3] = 0.0; minRosenberg[3] = 0; maxRosenberg[3] = 1;
        
        a_controlPanelRosenberg = new Controller(new java.awt.Frame ("Rosenberg Glottal Model"),
                                        false,valRosenberg.length,nbuttonsRosenberg) {
                private static final long serialVersionUID = 1L;
                
                public void onButton(int k) {
                    switch(k) {
                    case 0: {
                        FileDialog fd = new FileDialog(new Frame(),"Save");
                        fd.setMode(FileDialog.SAVE);
                        fd.setVisible(true);
                        saveToFile(fd.getFile());
                    }
                        break;
                    case 1: {
                        FileDialog fd = new FileDialog(new Frame(),"Load");
                        fd.setMode(FileDialog.LOAD);
                        fd.setVisible(true);
                        loadFromFile(fd.getFile());
                    }
                        break;
                    }
                }

                public void onSlider(int k) {
                    switch(k) {
                    case 0:
                        source.setFrequency((float)this.val[k]);
                        break;
                    case 1:
                        source.setOpenQuotient((float)this.val[k]);
                        break;
                    case 2:
                        source.setSpeedQuotient((float)this.val[k]);
                        break;
                    case 3:
                        source.setVolume((float)this.val[k]);
                        break;
                    default:
                        break;
                    }
                }
            };
        
        a_controlPanelRosenberg.setSliders(valRosenberg,minRosenberg,maxRosenberg,namesRosenberg);
        a_controlPanelRosenberg.setButtonNames (new String[] {"Save","Load"});
        a_controlPanelRosenberg.setVisible(true);
        // end Rosenberg panel

        //Ishizak-Flanagan twomass model panel
        int nbuttonsTwoMass = 2;
        int nSlidersTwoMass = 6;
        String[] namesTwoMass = new String[nSlidersTwoMass];
        double[] valTwoMass = new double[nSlidersTwoMass];
        double[] minTwoMass = new double[nSlidersTwoMass];
        double[] maxTwoMass = new double[nSlidersTwoMass];
        
        namesTwoMass[0] = "q(freq)";
        valTwoMass[0] = 1; minTwoMass[0] = .05; maxTwoMass[0] = 6;
        namesTwoMass[1] = "p-lung";
        valTwoMass[1] = 500; minTwoMass[1] = 0; maxTwoMass[1] = 6000;
        namesTwoMass[2] = "Ag0(cm^2)";
        valTwoMass[2] = -.005; minTwoMass[2] = -.5; maxTwoMass[2] = .5;
        namesTwoMass[3] = "noiseLevel";
        valTwoMass[3] = .3; minTwoMass[3] = 0; maxTwoMass[3] = 10;
        namesTwoMass[4] = "noiseFreq.";
        valTwoMass[4] = 1500; minTwoMass[4] = 200; maxTwoMass[4] = 10000;
        namesTwoMass[5] = "noiseBW";
        valTwoMass[5] = 6000; minTwoMass[5] = 250; maxTwoMass[5] = 10000;
        

        
        a_controlPanelTwoMass = new Controller(new java.awt.Frame ("TwoMass Glottal Model"),
                                        false,valTwoMass.length,nbuttonsTwoMass) {
                private static final long serialVersionUID = 1L;

                public void onButton(int k) {
                    switch(k) {
                    case 0: {
                        FileDialog fd = new FileDialog(new Frame(),"Save");
                        fd.setMode(FileDialog.SAVE);
                        fd.setVisible(true);
                        saveToFile(fd.getFile());
                    }
                        break;
                    case 1: {
                        FileDialog fd = new FileDialog(new Frame(),"Load");
                        fd.setMode(FileDialog.LOAD);
                        fd.setVisible(true);
                        loadFromFile(fd.getFile());
                    }
                        break;
                    }
                }

                public void onSlider(int k) {
                    switch(k) {
                    case 0:
                        // q factor of two mass model
                        twoMassSource.getVars().q = this.val[k];
                        //twoMassSource.getVars().setVars(); 
                        break;
                    case 1:
                        // lung pressure
                        twoMassSource.getVars().ps = this.val[k];
                        //twoMassSource.getVars().setVars(); 
                        break;
                    case 2:
                        // glottal rest area (displayed in cm^2)
                        twoMassSource.getVars().Ag0 = 1.e-4 * this.val[k];
                        //twoMassSource.getVars().setVars(); 
                        break;
                    case 3:
                        twoMassSource.setFlowNoiseLevel(this.val[k]);
                        break;
                    case 4:
                        twoMassSource.setFlowNoiseFrequency(this.val[k]);
                        break;
                    case 5:
                        twoMassSource.setFlowNoiseBandwidth(this.val[k]);
                        break;
                    default:
                        break;
                    }
                }
            };
        
        a_controlPanelTwoMass.setSliders(valTwoMass,minTwoMass,maxTwoMass,namesTwoMass);
        a_controlPanelTwoMass.setButtonNames (new String[] {"Save","Load"});
        a_controlPanelTwoMass.setVisible(true);
        // end Ishizak-Flanagan twomass model panel

        // add airflow monitor
        final JProgressBar progressBar = new JProgressBar(0, 1000);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        a_controlPanelTwoMass.add(progressBar);
        a_controlPanelTwoMass.getContentPane ().setLayout (new java.awt.GridLayout (nSlidersTwoMass+(nbuttonsTwoMass+2)/2, 2));
        a_controlPanelTwoMass.pack();
        
                
        // set locations of panels on screen     
        a_controlPanel.setLocation(new Point(740,10));
        Point p = a_controlPanel.getLocation();
        p.translate(430,0);
        a_controlPanelNasal.setLocation(p);
        p.translate(0,300);
        a_controlPanelRosenberg.setLocation(p);
        p.translate(0,150);
        a_controlPanelTwoMass.setLocation(p);

        player.start();

        try {
            Thread.sleep(1000);
        } catch(Exception e){};
        filter.reset();
        try {
            Thread.sleep(100);
        } catch(Exception e){};
        player.resetAGC();

        int sleepms = 100/100;
        double maxug = .00000001;
        double L0=.17;
        double LL = L0;
        double L1 = .20;
        double dL = .0002;
        boolean goUp=true;
        
        /********MATLAB CONNECTION**********/
        MatlabProxyFactoryOptions options =
                new MatlabProxyFactoryOptions.Builder()
                    .setUsePreviouslyControlledSession(true)
                    .build();
        
        MatlabProxyFactory factory = new MatlabProxyFactory(options);
        MatlabProxy proxy = null;
        
		try {
			proxy = factory.getProxy();
		} catch (MatlabConnectionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		/***************END*****************/
		/*************ARDUINO SETUP**********/
		try {
			proxy.eval("serial_port = serial('COM3')");
			proxy.eval("fopen(serial_port)");
			proxy.eval("flushinput(serial_port)");
		} catch (MatlabInvocationException e1) {
			// TODO Auto-generated catch block
			System.out.println("MARLAB-ARDUINO Exception");
			e1.printStackTrace();
		}
		/***************END**************/
		MySketch sketch_obj = new MySketch();
		
        while(!haltMe) {
            try {
                Thread.sleep(sleepms);
                if(slideStarted) {
                    tm.setLength(LL);
                    if(goUp) {
                        LL += dL;
                    } else {
                        LL -= dL;
                    }
                    if(LL<L0) {
                        goUp=true;
                    }
                    if(LL>L1) {
                        goUp=false;
                    }
                    double change_val =0;
                    int slider_count=0;
                    while(true) {
                    	Thread.sleep(500);
                    	//Evaluate the MATLAB Script
                    	proxy.eval("ImageP");
                    	//proxy.eval("sliderRead_Glottal"); // comment it for mouse interface
                    	
                    	slider_count=0;
                    	for(int k=nAuxSliders;k<nSliders;k++) {
                    		change_val = ((double[]) proxy.getVariable("Area"))[slider_count++];
                    		System.out.println(change_val);
                    		val[k] = change_val;
                    		if(val[k]<1)
                    			val[k]=1;
                    	}
                    	
                    	//SLIDER
//                    	valRosenberg[0] = ((double[]) proxy.getVariable("freq"))[0];
//                    	valRosenberg[3] = ((double[]) proxy.getVariable("gain"))[0];
                    	//END
                    	
                    	//MOUSE
                    	valRosenberg[0] = (sketch_obj.circleX/675)*1000;
                    	valRosenberg[3] = sketch_obj.circleY/675;

                        if(valRosenberg[0]>200 && valRosenberg[0]<300)
                    		valRosenberg[0]=200;
 					    else if(valRosenberg[0]>400 && valRosenberg[0]<600)
                    		valRosenberg[0]=400;
                    	//END
                    	
                    	
                    	
                    	a_controlPanelRosenberg.setSliders(valRosenberg,minRosenberg,maxRosenberg,namesRosenberg);
                    	a_controlPanel.setSliders(val,min,max,names);
                    	filter.changeTubeModel();
                    }
                    
                }

            } catch(Exception e) {}
        }
        
    }

    double[] fantData_a =  new double[] {5, 5, 5, 5, 6.5,       8, 8, 8, 8, 8,
                                                8, 8, 8, 6.5, 5,       4, 3.2, 1.6, 2.6, 2.6,
                                                2, 1.6, 1.3, 1, .65,   .65, .65, 1, 1.6, 2.6,
                                                4, 1, 1.3, 1.6, 2.6};
    double[] fantData_o =  new double[] {3.2,3.2,3.2,3.2,6.5,   13,13,16,13,10.5,
                                                10.5,8,8,6.5,6.5,       5,5,4,3.2,2,
                                                1.6,2.6,1.3,.65,.65,    1,1,1.3,1.6,2,
                                                3.2,4,5,5,1.3,          1.3,1.6,2.6};
    double[] fantData_u =  new double[] {.65,.65,.32,.32,2,  5,10.5,13,13,13,
                                                13,10.5,8,6.5,5,    3.2,2.6,2,2,2,
                                                1.6,1.3,2,1.6,1,     1,1,1.3,1.6,3.2,
                                                5,8,8,10.5,10.5,    10.5,2,2,2.6,      2.6};
    double[] fantData_i_ =  new double[] {6.5,6.5,2,6.5,8,   8,8,5,3.2,2.6,
                                                 2,2,1.6,1.3,1,     1,1.3,1.6,2.6,2,
                                                 4,5,6.5,6.5,8,     10.5,10.5,10.5,10.5,10.5,
                                                 13,13,10.5,10.5,6, 3.2,3.2,3.2,3.2};
    double[] fantData_i =  new double[] {4,4,3.2,1.6,1.3,              1,.65,.65,.65,.65,
                                                .65,.65,.65,1.3,2.6,          4,6.5,8,8,10.5,
                                                10.5,10.5,10.5,10.5,10.5,     10.5,10.5,10.5,8,8,
                                                2,2,2.6,3.2};
    double[] fantData_e =  new double[] {8,8,5,5,4,               2.6,2,2.6,2.6,3.2,
                                                4,4,4,5,5,               6.5,8,6.5,8,10.5,
                                                10.5,10.5,10.5,10.5,8,   8,6.5,6.5,6.5,6.5,
                                                1.3,1.6,2,2.6};
    
    double[] fantData__ =  new double[] {5, 5, 5, 5, 5,       5, 5, 5, 5, 5,
                                                5, 5, 5, 5, 5,       5, 5, 5, 5, 5,
                                                5, 5, 5, 5, 5,   5, 5, 5, 5, 5,
                                                5, 5, 5, 5, 5};

    public void preset(String p) {
        double[] f_a=null;
        if (p=="a") {
            f_a = fantData_a;
        }
		if (p=="o") {
            f_a = fantData_o;
        }
        if (p=="u") {
            f_a = fantData_u;
        }
        if (p=="i_") {
            f_a = fantData_i_;
        }
        if (p=="i") {
            f_a = fantData_i;
        }
        if (p=="e") {
            f_a = fantData_e;
        }
        if (p=="-") {
            f_a = fantData__;
        }
        // interpolate and invert Fant data
        double C = (f_a.length-1.)/(tract.length-1);
        for(int i=0;i<tract.length;i++) {
            double k = i*C;
            int ki = (int)k;
            double kfrac = k-ki;
            int i1 = ki;
            int i2 = i1+1;
            if(i2>f_a.length-1) {
                i2 = i1;
            }
           
            tract[tract.length-i-1] = Math.sqrt((f_a[i1]*(1-kfrac)+f_a[i2]*kfrac)/Math.PI)/100;
        }
    }
}