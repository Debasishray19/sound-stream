package jass.render;
import java.io.*;
import java.awt.*;
import javax.swing.*;

public class Controller2 extends ControllerPanel {
    private JFrame jFrame;

    /** Creates new form Controller with nsl sliders and nbut buttons
     This is the contructor for backwards compatibility */
                
    private void init() {
        FlowLayout layout = new FlowLayout();
        layout.setHgap(1);
        layout.setVgap(1);
        jFrame.getContentPane().setLayout(new BorderLayout());
        jFrame.getContentPane().add(this);
        //jFrame.setPreferredSize(new Dimension(450, 110));
        jFrame.setLocationRelativeTo(null);

        jFrame.pack();     
    }
    
    public Controller2(java.awt.Frame parent,boolean modal,int nsl,int nbut) {
        super(nsl,nbut);
        jFrame = new JFrame(parent.getTitle());
        init();
    }
    
    /** Creates new form Controller with nsl sliders and nbut buttons
    This is the new contructor  */
    public Controller2(String title,int nsl, int nbut) {
        super(nsl, nbut);
        jFrame = new JFrame(title);
        init();
    }

    private void closeDialog(java.awt.event.WindowEvent evt) {
        jFrame.setVisible (false);
        jFrame.dispose();
    }
    
    public void addWindowListener(java.awt.event.WindowAdapter windowAdapter) {
        jFrame.addWindowListener(windowAdapter);
    }
    
    public void setVisible(boolean v) {
        super.setVisible(v);
        jFrame.setVisible(v);
        jFrame.pack();
    }
    
    public JFrame getJFrame() {
        return jFrame;
    }
    
}
