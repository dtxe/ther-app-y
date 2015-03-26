package ca.utoronto.therappy;

/**
 * Created by Andrew on 26/03/2015.
 * Object class for a complex number. Note object is immutable after creation
 */
public class Complex {

    private final double re, im;

    public Complex(double re, double im){
        this.re = re;
        this.im = im;
    }

    public Complex(int re){
        this.re = re;
        this.im = 0;
    }

    public Complex plus(Complex z){
        return new Complex(this.re + z.re, this.im + z.im);
    }

    public Complex minus(Complex z){
        return new Complex(this.re - z.re, this.im - z.im);
    }

    public Complex times(Complex z){
        return new Complex(this.re * z.re - this.im * z.im, this.re * z.im + this.im * z.re);
    }

    public Complex times(double a){
        return new Complex(this.re * a, this.im * a);
    }

    public Complex conjugate(){
        return new Complex(this.re, -this.im);
    }

    public String toString(){
        return this.re + "" + this.im + "i";
    }
}
