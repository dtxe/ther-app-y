package ca.utoronto.therappysignalstest;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

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

    private final static double time_div = 1E-9;       // timestamp is in nanoseconds

    private ArrayList<sensorPoint> data_accl;
    double fwvol, xyarea, yzarea, xzarea;

    // when creating the signal processing module, must provide acceleration data
    public SPM_FunctionalWorkspace(ArrayList<sensorPoint> data_accl) {
        // pass the loaded acceleration data here
        // the vector is prerotated
        this.data_accl = data_accl;
    }

    // do the whole signals processing thing here.
    public ArrayList<double[]> doChurnData () {
        // do signals processing stuff

        // STEP: remove duplicated acceleration values
        this.data_accl = removeDuplicates(this.data_accl);

        // STEP: split into sections
        ArrayList<int[]> sectionIndices = new ArrayList<>();

        // - add first segment
        sectionIndices.add(new int[] {-1, 0});

        // - loop through the arraylist looking for Ns
        for(int kk = 0; kk < this.data_accl.size(); kk++) {
            if(this.data_accl.get(kk).datatype == sensorPoint.TRACE_BREAK) {
                sectionIndices.add(new int[]{kk, 0});
            }
        }

        // - loop through indices add find segment length
        for(int kk = 0; kk < sectionIndices.size(); kk++) {
            int[] curr = sectionIndices.get(kk);
            curr[0] = curr[0] + 1;                                      // set index to first sensorPoint that isn't 'N'

            if(kk+1 >= sectionIndices.size())
                curr[1] = this.data_accl.size() - curr[0];              // if this is the last index...
            else
                curr[1] = sectionIndices.get(kk+1)[0] - curr[0] - 1;    // calculate the length of the segment
        }

        // STEP: run signals processing code on it.
        ArrayList<double[]> position = new ArrayList<>();

        for(int kk = 0; kk < sectionIndices.size(); kk++) {
            int[] curr = sectionIndices.get(kk);

            double[][] tempposition = null;

            try {
                tempposition = doDeadReckoning(curr[0], curr[1]);
            } catch(Exception ex) {
                System.out.println("Current section: " + kk);
                ex.printStackTrace();
            }

            // transfer new position vectors into array         TODO: this may not be necessary if we're fitting
            position.ensureCapacity(position.size() + tempposition.length);
            for(int jj = 0; jj < tempposition[0].length; jj++) {
                position.add(new double[]{tempposition[0][jj], tempposition[1][jj], tempposition[2][jj]});
            }
        }

        // clean up for garbage collector
        this.data_accl.clear();
        this.data_accl = null;


        return position;




        // fit areas to get metrics

    }

    protected double[] doFitTargets(double[][] position) {

        return null;
    }


    // this is the bulk of the signals processing code. It removes duplicated time points,
    // resamples the data, filters using FFT, then integrates acceleration to get position.
    protected double[][] doDeadReckoning(int segmentBegin, int segmentLength) {

        // if we are asked to process a segment which is longer than the data acceleration vector,
        // signal that this isn't going to work out.
        if (segmentBegin+segmentLength-1 >= this.data_accl.size()) {
            return null;
        }

        // take data out of arraylist/sensorpoint
        double[][] thedata = new double[3][segmentLength];
        double[] thetime = new double[segmentLength];

        for(int kk = 0; kk < segmentLength; kk++) {
            // retrieve sensorPoint from the ArrayList
            sensorPoint tempsp = this.data_accl.get(kk + segmentBegin);

            // store the timestamp in a vector
            thetime[kk] = tempsp.time * time_div;      // convert to seconds

            // store the values in respective accl vector
            float[] tempdata = tempsp.value;
            thedata[0][kk] = tempdata[0];
            thedata[1][kk] = tempdata[1];
            thedata[2][kk] = tempdata[2];
        }
        // *****************************************

        // STEP: do linear interpolation of the data
        //  - find sampling frequency
        double meandiff = StatUtils.mean(diff(thetime));
        meandiff = meandiff / 5;                            // aim for oversample by 5x
        int resampled_length = (int) Math.floor((thetime[thetime.length-1] - thetime[0]) / meandiff);

        //  - turn resampled_length into closest higher power of 2          TODO: might need to add option to pad with zeros if it gets too long
        resampled_length = (int) Math.pow(2, Math.ceil(  (Math.log(resampled_length)/Math.log(2)) - 0.1 ));
        meandiff = (thetime[thetime.length-1] - thetime[0]) / (resampled_length-1);

        //  - generate new time vector
        double[] resampled_time = new double[resampled_length];

        for(int kk = 0; kk < resampled_length; kk++) {
            resampled_time[kk] = (meandiff * kk) + thetime[0];
        }

        //  - resample each acceleration dimension
        double[][] resampled_data = new double[3][];
        for(int kk = 0; kk < 3; kk++) {
            resampled_data[kk] = interp1(resampled_time, thetime, thedata[kk]);
        }

        //  - free for garbage collect
        thedata = null;
        thetime = null;
        // *****************************************

        // STEP: filter data
        for(int kk = 0; kk < 3; kk++) {
            double normalized_hicutoff = 30 * meandiff / 2;
            resampled_data[kk] = doFilterNoDC_FFT(resampled_data[kk], normalized_hicutoff);
        }
        // *****************************************

        resampled_data = getMovingAverage(resampled_data, 25);      // get moving average over 5 pre-resampled accl samples
        resampled_length = resampled_data.length;


        // STEP: integrate acceleration twice to get position
        //  - integrate accl to get velocity
        double[][] velocity = new double[3][resampled_length];
        for(int kk = 0; kk < 3; kk++) {
            // initial velocity is zero + change in first time step
            velocity[kk][0] = (meandiff * resampled_data[kk][0]);

            // loop through time steps and add changes
            for(int tt = 1; tt < resampled_length; tt++) {
                velocity[kk][tt] = velocity[kk][tt-1] + (meandiff * resampled_data[kk][tt]);
            }
        }

        //  - integrate velocity to get position
        double[][] position = new double[3][resampled_length];
        for(int kk = 0; kk < 3; kk++) {

            // initial position is zero + change in first time step
            position[kk][0] = (meandiff * velocity[kk][0]);

            for(int tt = 1; tt < resampled_length; tt++) {
                position[kk][tt] = position[kk][tt-1] + (meandiff * velocity[kk][tt]);
            }
        }
        // *****************************************

        return position;
    }


    // calculate the moving average of a 3 x N matrix of values in the N dimension.
    protected double[][] getMovingAverage(double[][] input, int numsamples) {
        double[][] output = new double[3][];
        for(int kk = 0; kk < 3; kk++) {
            output[kk] = new double[input[0].length - numsamples + 1];
        }

        for(int kk = 0; kk < output.length; kk++) {
            for(int jj = 0; jj < numsamples; jj++) {
                output[0][kk] += input[0][kk+jj];
                output[1][kk] += input[1][kk+jj];
                output[2][kk] += input[2][kk+jj];
            }
        }

        return output;
    }

    // remove values with duplicated time stamps
    // TODO: perhaps consider averaging.
    protected ArrayList<sensorPoint> removeDuplicates(ArrayList<sensorPoint> input) {

        ArrayList<Integer> duplicatedTimes = new ArrayList<>();

        // ensure the data is sorted.
        Collections.sort(input);

        // loop through sensor points and mark duplicated time stamps for removal
        for(int kk = 1; kk < input.size(); kk++) {
            if(input.get(kk).time == input.get(kk-1).time                       // if the timestamps are equal, then mark it for dropping
                    && input.get(kk).datatype != sensorPoint.TRACE_BREAK) {     // unless it's a trace break (ie. signals a return to origin)
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
