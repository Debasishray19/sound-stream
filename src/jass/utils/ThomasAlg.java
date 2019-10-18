package jass.utils;

/** Implement Thomas alg. forthe solution of tridiagonal linear systems.
    @author Kees van den Doel (kvdoel@cs.ubc.ca)
*/
public class ThomasAlg {
    /**
       Solve tridiagonal system
       a(i)x(i-1)  + b(i)x(i) + c(i)x(i+1) = d(i),    i = 0,..,n-1
       a(0) = 0 and c(n-1) = 0. (Note that x(-1) and x(n) are unused.)
       d holds the solution, b is modified.
       Return false if there is a problem.
     */
    public static final boolean thomas(double[] a, double[] b, double[] c, double[] d, int n) {
        for(int i=1;i<n;i++) {
            if(b[i-1]==0) {
                return false;
            }
            double m = a[i]/b[i-1];
            b[i] -= m*c[i-1];
            d[i] -= m*d[i-1];
        }
        d[n-1] = d[n-1]/b[n-1];
        for(int i=n-2;i>=0;i--) {
            d[i]=(d[i]-c[i]*d[i+1])/b[i];
        }
        return true;
    }
}

class ThomasTest {
    public static void main(String[] argv) {
        int n=10;
        double[] a = new double[n];
        double[] b = new double[n];
        double[] bb = new double[n];
        double[] c = new double[n];
        double[] d = new double[n];
        double[] dd = new double[n];
        for(int i=0;i<n;i++) {
            a[i] = Math.random()-.5;
            b[i] = Math.random()-.5;
            bb[i] = b[i];
            c[i] = Math.random()-.5;
            d[i] = Math.random()-.5;
            dd[i] = d[i];
        }
        a[0] = 0;
        c[n-1]=0;
        boolean ret = ThomasAlg.thomas(a,b,c,d,n);
        if(!ret) {        
            System.out.println("error");
        }
        double res;
        int i=0;
        res = bb[i]*d[i]+c[i]*d[i+1] -dd[i];
        System.out.println(res);
        for(i=1;i<n-1;i++) {
            res = a[i]*d[i-1]+bb[i]*d[i]+c[i]*d[i+1] -dd[i];
            System.out.println(res);
        }
        i=n-1;
        res = a[i]*d[i-1]+bb[i]*d[i] -dd[i];
        System.out.println(res);
    }
}
