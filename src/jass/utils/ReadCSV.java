package jass.utils;

import java.io.*;

public class ReadCSV {
    private String[][] cell;
       
    public static void main(String args[]) {
        ReadCSV r=null;
        try {
            r = new ReadCSV("tmp.csv");
        } catch(Exception e) {
            System.out.println(e);
        }
        String[][] s = r.getCells();
        
        int nrows = s.length;
        int ncols = s[0].length;
        for(int i=0;i<nrows;i++) {
             for(int j=0;j<ncols;j++) {
                //System.out.println(s[i][j]);
             }
        }
        try {
            r.saveCells("tmp2.csv");
            } catch(Exception e) {
            System.out.println(e);
        }
    }
    
    public ReadCSV() {
         cell = null;
    }
    
    public ReadCSV(String fn) throws FileNotFoundException, IOException {
        LineNumberReader r = new LineNumberReader(new FileReader(new File(fn)));
        String s;
        int cnt = 0;
        while ((s = r.readLine()) != null) {
            cnt++;
        }
        cell = new String[cnt][];
        r = new LineNumberReader(new FileReader(new File(fn)));
        cnt = 0;
        while ((s = r.readLine()) != null) {
            if (s.length() > 0) {
                cell[cnt] = s.split(",", -1);
                for(int i=0;i<cell[cnt].length;i++) {
                    cell[cnt][i] = cell[cnt][i].trim();
                }
                //System.out.println("XX"+s+"SS"+s.length());
                cnt++;
            }
        }
    }
    
    public void saveCells(String fn) throws FileNotFoundException, IOException {
        PrintStream p = new PrintStream(new File(fn));
        String[][] s = getCells();
        int nrows = s.length;
        for (int i = 0; i < nrows; i++) {
            int ncols = s[i].length;
            for (int j = 0; j < ncols - 1; j++) {
                String str = s[i][j].trim();
                p.print(str);
                p.print(",");
            }
            p.print(s[i][ncols - 1]);
            p.println();
        }
    }

    public void setCells(String[][] cell) {
        this.cell = cell;
    }
    
    public String[][] getCells() {
        return cell;
    }
}

