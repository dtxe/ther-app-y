package ca.utoronto.therappy;

import java.util.ArrayList;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Created by simeon on 2015-03-14.
 */

public class SPM_FunctionalWorkspace {
    private ArrayList<sensorPoint> data_accl;
    double fwvol, xyarea, yzarea, xzarea;

    // when creating the signal processing module, must provide acceleration data
    public SPM_FunctionalWorkspace(ArrayList<sensorPoint> data_accl) {
        // pass the loaded acceleration data here
        // the vector is prerotated
        this.data_accl = data_accl;
    }

    // do the whole signals processing thing here.
    public void doChurnData () {
        // STEP: remove duplicated acceleration values
        this.data_accl = removeDuplicates(this.data_accl);

        // take data out of arraylist/sensorpoint
        double[][] thedata = new double[3][this.data_accl.size()];
        double[] thetime = new double[this.data_accl.size()];

        for(int kk = 0; kk < this.data_accl.size(); kk++) {
            // retrieve sensorPoint from the ArrayList
            sensorPoint tempsp = this.data_accl.get(kk);

            // store the timestamp in a vector
            thetime[kk] = tempsp.getTime();

            // store the values in respective accl vector
            double[] tempdata = tempsp.getValue();
            thedata[0][kk] = tempdata[0];
            thedata[1][kk] = tempdata[1];
            thedata[2][kk] = tempdata[2];
        }
        this.data_accl.clear();
        this.data_accl = null;

        // STEP: do linear interpolation of the data
        //  - find sampling frequency
        double meandiff = StatUtils.mean(diff(thetime));
        meandiff = meandiff / 5;                            // oversample by 5x

        //  - generate new time vector
        int resampled_length = (int) Math.floor(thetime[thetime.length-1] / meandiff);
        double[] resampled_time = new double[resampled_length];

        for(int kk = 0; kk < resampled_length; kk++) {
            resampled_time[kk] = meandiff * kk;
        }

        //  - resample each acceleration dimension
        double[][] resampled_data = new double[3][];
        for(int kk = 0; kk < 3; kk++) {
            resampled_data[kk] = interp1(resampled_time, thetime, thedata[kk]);
        }

        //  - free for garbage collect
        thedata = null;
        thetime = null;

        // STEP: filter data
        for(int kk = 0; kk < 3; kk++) {
            double normalized_hicutoff = 30 * meandiff / 2;
            resampled_data[kk] = doFilterNoDC_FFT(resampled_data[kk], normalized_hicutoff);
        }
    }

    // remove values with duplicated time stamps
    // TODO: perhaps consider averaging.
    protected ArrayList<sensorPoint> removeDuplicates(ArrayList<sensorPoint> input) {

        ArrayList<Integer> duplicatedTimes = new ArrayList<Integer>();

        // loop through sensor points and mark duplicated time stamps for removal
        for(int kk = 1; kk < input.size(); kk++) {
            if(input.get(kk).compareTo(input.get(kk-1)) == 0) {
                // if the timestamps are equal, then mark it for dropping
                duplicatedTimes.add(kk);
            }
        }

        // remove all items marked for removal
        // we can't remove while searching, because then the size of the arraylist would change, that that makes things complicated.
        // - search backwards and delete, so that it doesn't upset the indexing
        for(int kk = duplicatedTimes.size()-1; kk >= 0; kk--) {
            input.remove((int) duplicatedTimes.get(kk));
        }

        return input;
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

    // imitates MATLAB diff command. takes the difference between elements of a vector
    protected double[] diff(double[] input) {
        double[] output = new double[input.length - 1];

        for(int kk = 0; kk < input.length - 1; kk++) {
            output[kk] = input[kk+1] - input[kk];
        }

        return output;
    }

    // return the computed workspace volume
    public double getWorkspaceVolume () {

        return this.fwvol;
    }

    // return the computed XY plane area
    public double getXYplane() {

        return this.xyarea;
    }

    public double getYZplane() {

        return this.yzarea;
    }

    public double getXZplane() {

        return this.xzarea;
    }
}
