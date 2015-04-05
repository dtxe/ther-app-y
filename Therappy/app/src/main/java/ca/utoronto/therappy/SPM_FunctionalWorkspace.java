package ca.utoronto.therappy;

import java.util.ArrayList;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Created by simeon on 2015-03-14.
 */

public class SPM_FunctionalWorkspace {

    private ArrayList<sensorPoint> data_accl, data_rota;

    public SPM_FunctionalWorkspace(ArrayList<sensorPoint> data_accl, ArrayList<sensorPoint> data_rota) {
        // pass the loaded acceleration and rotation data here
        this.data_accl = data_accl;
        this.data_rota = data_rota;
    }

    public double getWorkspaceVolume () {

        return 0;
    }

    public double getXYplane() {

        return 0;
    }

    public double getYZplane() {

        return 0;
    }

    public double getXZplane() {

        return 0;
    }

    public void doChurnData () {
        // do the whole signals processing thing here.


    }

    // do 1D linear interpolation of data, similar to matlab interp1 command
    protected double[] interp1(double [] newTime, double[] oldTime, double[] oldX) {
        // fit linear interpolator model to provided data
        PolynomialSplineFunction psfmodel = (new LinearInterpolator()).interpolate(oldTime, oldX);

        // allocate new data vector of same length
        int num_samples = newTime.length;
        double[] newX = new double[num_samples];

        // get new data values using model
        for(int tt = 0; tt < num_samples; tt++) {
            newX[tt] = psfmodel.value(newTime[tt]);
        }

        // return
        return newX;
    }

    // low pass filter data using an FFT, and removing DC components
    protected double[] doFilterNoDC_FFT(double[] datain, double hicutoff) {
        // filter the signal using an FFT / iFFT algorithm, removing the DC component, and any
        // components above the specified hicutoff
        //      hicutoff should be provided as normalized frequency
        //
        // Essentially, this function will:
        //   - transform the signal into frequency space (using FFT)
        //   - create a vector of multiplication ratios
        //   - zero out the multiplication ratio vectors that we want to filter out
        //   - element-wise multiply the ratio vector with the frequency space of signal
        //   - do inverse FFT to recover filtered signal

        int num_samples = datain.length;

        // find corresponding index
        int hicutoffidx = (int) Math.ceil(num_samples * hicutoff);

        // FFT MAGICKS HAPPENS HERE
        // ***************
        FastFourierTransformer fftengine = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex [] fftoutput = fftengine.transform(datain, TransformType.FORWARD);
        // ***************

        // create a vector of things to zero out, set everything to 1
        double[] zeroidx = new double[num_samples];
        for (int kk = 0; kk < num_samples; kk++) {
            zeroidx[kk] = 1;
        }

        // zero out DC component
        zeroidx[0] = 0;

        // zero out high frequency components above the hicutoff
        int vecmiddle = (int) Math.ceil(num_samples/2);
        for(int kk = hicutoffidx; kk < vecmiddle; kk++) {
            zeroidx[kk] = 0;
        }

        // mirror the zero-out vector (since FFT is mirrored)
        for(int kk = 0; kk < vecmiddle; kk++) {
            zeroidx[num_samples - kk - 1] = zeroidx[kk];
        }

        // actually zero out the components that need to be zeroed out
        for(int kk = 0; kk < num_samples; kk++) {
            fftoutput[kk] = fftoutput[kk].multiply(zeroidx[kk]);
        }

        // INVERSE FFT MAGICKS HAPPENS HERE
        // ***************
        Complex[] cpx_output = fftengine.transform(fftoutput, TransformType.INVERSE);
        // ***************

        // get real part of the FFT output.
        double[] output = new double[num_samples];
        for(int kk = 0; kk < num_samples; kk++) {
            output[kk] = cpx_output[kk].getReal();
        }

        return output;
    }
}