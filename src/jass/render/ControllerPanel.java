package jass.render;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class ControllerPanel extends javax.swing.JPanel {
    protected static final double MAX_SLIDERVAL = 10000;
    protected static final int NSLIDERS = 21; // defaults
    protected static final int NBUTTONS = 4; // defaults
    protected double[] val; //initial slider values
    protected String[] names; //slider labels
    protected double[] min,max; //slider ranges
    protected int nsliders = NSLIDERS;
    protected int nbuttons = NBUTTONS;
    private JScrollPane jScrollPane; // holds sliders
    
    private JPanel sliderPanel; // sliders are added to this
    private JPanel buttonPanel;
    private JPanel labelPanel;
    private JPanel valuePanel;
    private JPanel topPanel; // this contains sliders labels and textfields
    private JPanel sliderPanelHolder; // this holds the toppanel
    
    private static final String DEFAULT_SLIDERDISPLAYFORMAT = "%5.2e";
    private String sliderDisplayFormat = DEFAULT_SLIDERDISPLAYFORMAT;
    private static final int NBUTTONSPERROW = 4;

    /** Set the values of the sliders and call its handlers. */
    public void setSliders(double[] val,double[] min,double[] max,String[] names) {
        setValues(val,min,max,names);
        for(int i=0;i<val.length;i++) {
            onSlider(i);
        }
    }
    
    /**
     * s is a printf like format string
    */
    public void setSliderDisplayFormat(String s) {
        sliderDisplayFormat = s;
    }

    /** Set button names */
    public void setButtonNames(String[] names) {
        for(int i=0;i<names.length;i++) {
            setButtonName(names[i],i);
        }
    }
    
    /** Set button name */
    public void setButtonName(String name,int k) {
        jButton[k].setText(name);
    }

    /** Set the values of the sliders (will not call its handlers) */
    public void setValues(double[] val,double[] min,double[] max,String[] names) {
        for(int i=0;i<val.length;i++) {
            this.names[i] = names[i];
            this.val[i] = val[i];
            this.min[i] = min[i];
            this.max[i] = max[i];
            int x = (int)(MAX_SLIDERVAL * (val[i]-min[i])/(max[i]-min[i]));
            jSlider[i].setValue(x);
            jLabel[i].setText(names[i]);
            String vals = String.format(sliderDisplayFormat,val[i]);
            jTextField[i].setText("  "+vals+"  ");
        }
    }

    private void initValues() {
        val = new double[nsliders];
        min = new double[nsliders];
        max = new double[nsliders];
        names = new String[nsliders];
        for(int i=0;i<nsliders;i++) {
            names[i] = "";
            val[i]=-1.32e23;
            min[i] = 0;
            max[i] = 1;
        }
    }

    /** Creates new form Controller with nsl sliders and nbut buttons*/
    public ControllerPanel(int nsl,int nbut) {
        super ();
        this.nsliders = nsl;
        this.nbuttons = nbut;
    
        jSlider = new javax.swing.JSlider[nsliders];
        jTextField = new javax.swing.JTextField[nsliders];
        jButton = new javax.swing.JButton[nbuttons];
        jLabel = new javax.swing.JLabel[nsliders];
        initComponents ();
    }

    /** Member class to handle slider events */
    class LabeledMouseMotionAdapter extends java.awt.event.MouseMotionAdapter {
        private int label;
        
        public LabeledMouseMotionAdapter(int label) {
            super();
            this.label = label;
        }
        
        public void mouseDragged (java.awt.event.MouseEvent evt) {
            jSliderMouseDragged (label,evt);
        }
    }
    
    /** Member class to handle button events */
    class LabeledMouseAdapter extends java.awt.event.MouseAdapter {
        private int label;
        
        public LabeledMouseAdapter(int label) {
            super();
            this.label = label;
        }
        public void mousePressed (java.awt.event.MouseEvent evt) {
            jButtonMousePressed (label,evt);
        }
    }

    private void initComponents () {
        
        GridBagConstraints gridBagConstraints;
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        sliderPanel = new JPanel();
        labelPanel = new JPanel();
        valuePanel = new JPanel();
        topPanel = new JPanel(); // sliders + labels + values
        sliderPanelHolder = new JPanel();
        sliderPanelHolder.setLayout(new BorderLayout()); // for use in scrollpane
        sliderPanelHolder.add(topPanel);
        topPanel.setLayout(new BoxLayout(topPanel,BoxLayout.X_AXIS));
        jScrollPane = new JScrollPane(sliderPanelHolder);
        add(jScrollPane);
        topPanel.add(sliderPanel);
        topPanel.add(labelPanel);
        topPanel.add(valuePanel);
        buttonPanel = new JPanel();
        
        buttonPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        topPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

        add(buttonPanel);
        sliderPanel.setLayout (new java.awt.GridLayout (nsliders, 1));
        labelPanel.setLayout (new java.awt.GridLayout (nsliders, 1));
        valuePanel.setLayout (new java.awt.GridLayout (nsliders, 1));
        //buttonPanel.setLayout (new java.awt.GridLayout (2, 1));
        buttonPanel.setLayout (new java.awt.GridBagLayout());
        for(int i=0;i<nsliders;i++) {
            jSlider[i] = new javax.swing.JSlider();
            jLabel[i] = new JLabel();
            jTextField[i] = new javax.swing.JTextField();
        }
        for(int i=0;i<nbuttons;i++) {
            jButton[i] = new javax.swing.JButton ();
        }
        for(int i=0;i<nsliders;i++) {
            jSlider[i].setMaximum ((int)MAX_SLIDERVAL);
            sliderPanel.add (jSlider[i]);
            jTextField[i].setEditable (false);
            String s = String.format(sliderDisplayFormat, -23.32e-23);
            jTextField[i].setText (s);
            jTextField[i].setBackground(Color.WHITE);
            //jTextField[i].setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            jTextField[i].setMargin(new Insets(0,0,0,0));
            jLabel[i].setText("");
            jLabel[i].setHorizontalAlignment(SwingConstants.TRAILING);
            //jTextField[i].setHorizontalAlignment(JTextField.CENTER);
            labelPanel.add (jLabel[i]);
            //jLabel[i].setBorder(BorderFactory.createLineBorder(Color.black));
            valuePanel.add (jTextField[i]);
        }

        for(int i=0;i<nsliders;i++) {
            jSlider[i].addMouseMotionListener (new LabeledMouseMotionAdapter(i));
        }

        for(int i=0;i<nbuttons;i++) {
            jButton[i].addMouseListener (new LabeledMouseAdapter(i));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = i%NBUTTONSPERROW;
            gridBagConstraints.gridy = i/NBUTTONSPERROW;
            gridBagConstraints.weightx = .1;
            gridBagConstraints.weighty = .1;
            gridBagConstraints.anchor = GridBagConstraints.CENTER;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.insets = new Insets(0, 0, 0, 0);
            buttonPanel.add (jButton[i],gridBagConstraints);
        }

        initValues();
        setValues(val,min,max,names);
    }

    /** Save slider states to file */
    public void saveToFile(String fn) {
        try {
            BufferedWriter br = new BufferedWriter(new FileWriter(fn));
            double x = 0;
            for(int i=0;i<nsliders;i++) {
                br.write(names[i]); br.newLine();
                br.write(new Double(val[i]).toString()); br.newLine();
                br.write(new Double(min[i]).toString()); br.newLine();
                br.write(new Double(max[i]).toString()); br.newLine();
            }
            br.close();
        } catch(Exception e) {
            System.out.println(this+" "+e);
        }
    }
    
    /** Load slider states from file and call handlers */
    public void loadFromFile(String fn) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fn));
            double x = 0;
            for(int i=0;i<nsliders;i++) {
                names[i] = br.readLine();
                val[i] = (float)(new Double(br.readLine()).doubleValue());
                min[i] = (float)(new Double(br.readLine()).doubleValue());
                max[i] = (float)(new Double(br.readLine()).doubleValue());
            }
            br.close();
            setValues(val,min,max,names);
            for(int i=0;i<nsliders;i++) {
                onSlider(i);
            }
        } catch(Exception e) {
            System.out.println(this+" "+e);
        }
    }
    
    /** User should override this handler*/
    public void onSlider(int slider) {;}

    /** User should override this handler*/
    public void onButton(int button) {;}
    
    private void jButtonMousePressed (int k, java.awt.event.MouseEvent evt) {
        onButton(k);
    }
    
    private void jSliderMouseDragged (int k,java.awt.event.MouseEvent evt) {
        double sval = jSlider[k].getValue()/MAX_SLIDERVAL;
        val[k] = (float)(sval*(max[k]-min[k])+min[k]);
        String vals = String.format(sliderDisplayFormat,val[k]);
        jTextField[k].setText("  "+vals+"  ");
        onSlider(k);
    }
    
    protected javax.swing.JSlider[] jSlider;
    protected javax.swing.JTextField[] jTextField;
    protected javax.swing.JLabel[] jLabel;
    protected javax.swing.JButton[] jButton;

}
