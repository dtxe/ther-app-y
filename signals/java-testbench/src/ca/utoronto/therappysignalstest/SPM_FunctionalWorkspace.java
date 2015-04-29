package ca.utoronto.therappysignalstest;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;



/**
 * Created by simeon on 2015-03-14.
 */

public class SPM_FunctionalWorkspace {

    // Constants
    private final static double LONGTERM_WND_LENGTH = 3.0;
    private final static double time_div = 1E-9;       // timestamp is in nanoseconds

    // INPUT
    private ArrayList<sensorPoint> data_input;

    // Interim
    private double [][] resampled_data;
    private int [][] resampled_idx;
    private double meandiff;

    private double [][] data_accl, data_rota;
    private double[] time_accl, time_rota;
    private double[][] time_split;

    // OUTPUTS
    private double [] fitmeasures; // fwvol, xyarea, yzarea, xzarea;
    private double [] maxpositions;
    private ArrayList<double[]> position;

    // when creating the signal processing module, must provide acceleration data
    public SPM_FunctionalWorkspace(ArrayList<sensorPoint> data_input) {
        // pass the loaded acceleration data here
        // the vector is pre-rotated
        this.data_input = data_input;
    }

    // do the whole signals processing thing here.
    public void doChurnData () {
        // do signals processing stuff

        Collections.sort(this.data_input);

        // STEP: remove duplicates and separate data
        doSeparateData();

        // STEP: preprocess everything
        this.doPreprocessing();


        // STEP: run signals processing code on it.
        ArrayList<double[]> position = new ArrayList<>();
        this.maxpositions = new double[this.resampled_idx.length];      // this stores the max position within each interval

        for(int kk = 0; kk < this.resampled_idx.length; kk++) {
            int[] curr = this.resampled_idx[kk];

            double[][] tempposition;
            tempposition = this.doIntegration(curr[0], curr[1]-curr[0]);

            // transfer new position vectors into array
            position.ensureCapacity(position.size() + tempposition[0].length);

            for(int jj = 0; jj < tempposition[0].length; jj++) {
                position.add(new double[]{tempposition[0][jj], tempposition[1][jj], tempposition[2][jj]});

                // within each interval, find max position
                double vecnorm = Math.sqrt(Math.pow(tempposition[0][jj],2) + Math.pow(tempposition[1][jj],2) + Math.pow(tempposition[2][jj],2));
                this.maxpositions[kk] = Math.max(vecnorm, this.maxpositions[kk]);
            }

        }

        this.position = position;




        // fit areas to get metrics
        doFitTargets();
    }

    protected void doInterpRotate() {

    }

    // separate the input sensorPoint data into individual components for processing
    protected void doSeparateData() {

        // ensure sorted
        Collections.sort(this.data_input);

        ArrayList<Integer> idx_accl = new ArrayList<>(), idx_gyro = new ArrayList<>(), idx_split = new ArrayList<>();

        // add first segment
        idx_split.add(-1);

        // count the number of each item
        for(int kk = 0; kk < this.data_input.size(); kk++) {
            int type = this.data_input.get(kk).datatype;

            if(type == sensorPoint.DATA_ACCELERATION) {
                idx_accl.add(kk);
            }
            else if(type == sensorPoint.DATA_ROTATIONVEC) {
                idx_gyro.add(kk);
            }
            else if(type == sensorPoint.TRACE_BREAK) {
                idx_split.add(kk);
            }
        }

        // remove duplicates
        idx_accl = doRemoveDuplicates(idx_accl);
        idx_gyro = doRemoveDuplicates(idx_gyro);


        // >>> take data out of sensor points
        // - acceleration
        this.data_accl = new double[3][idx_accl.size()];
        this.time_accl = new double[idx_accl.size()];

        for(int kk = 0; kk < idx_accl.size(); kk++) {
            sensorPoint sp = this.data_input.get(idx_accl.get(kk));

            this.data_accl[0][kk] = sp.value[0];
            this.data_accl[1][kk] = sp.value[1];
            this.data_accl[2][kk] = sp.value[2];

            this.time_accl[kk] = sp.time * time_div;
        }

        // - rotation
        this.data_rota = new double[3][idx_gyro.size()];
        this.time_rota = new double[idx_gyro.size()];

        for(int kk = 0; kk < idx_gyro.size(); kk++) {
            sensorPoint sp = this.data_input.get(idx_gyro.get(kk));

            this.data_rota[0][kk] = sp.value[0];
            this.data_rota[1][kk] = sp.value[1];
            this.data_rota[2][kk] = sp.value[2];

            this.time_rota[kk] = sp.time * time_div;
        }

        // - split
        this.time_split = new double[idx_split.size()][2];

        for(int kk = 0; kk < idx_split.size(); kk++) {
            this.time_split[kk][0] = this.data_input.get(idx_split.get(kk)+1).time * time_div;

            if(kk+2 >= idx_split.size()) {
                this.time_split[kk][1] = this.data_input.get(this.data_input.size()-1).time * time_div;
            } else {
                this.time_split[kk][1] = this.data_input.get(idx_split.get(kk+1)-1).time * time_div;
            }
        }

      //  this.data_input.clear();
        //this.data_input = null;
    }

    protected void doFitTargets() {
        // points for xy, yz, xz
        int[][] indices = new int[][]{new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8},
                new int[]{0, 1, 2, 3, 4}, new int[]{2, 5, 6}, new int[]{0, 7, 6, 8, 4}};
        this.fitmeasures = new double[indices.length];

        for (int aa = 0; aa < indices.length; aa++) {

            this.fitmeasures[aa] = 0;
            for (int bb = 0; bb < indices[aa].length; bb++) {
                this.fitmeasures[aa] += this.maxpositions[indices[aa][bb]];
            }
            this.fitmeasures[aa] /= (indices[aa].length+1);
        }

        // calculate sphere volume
        this.fitmeasures[0] = Math.PI * Math.pow(this.fitmeasures[0], 3) / 3.0;

        // calculate workspace areas
        for (int aa = 1; aa < indices.length; aa++) {
            this.fitmeasures[aa] = Math.PI * Math.pow(this.fitmeasures[aa], 2) / 2.0;
        }
    }


    /* perform acceleration preprocessing: all the steps up until integration.
       split into segments, resample, filter, subtract moving average   */
    protected void doPreprocessing() {


        // STEP: do linear interpolation of the data
        double startTime = Math.max(this.time_accl[0], this.time_rota[0]),
                endTime = Math.min(this.time_accl[this.time_accl.length - 1], this.time_rota[this.time_rota.length - 1]);

        //  - find sampling frequency
        double meandiff = StatUtils.mean(calculateDiff(this.time_accl));
        meandiff = meandiff / 5;                            // aim for oversample by 5x
        int resampled_length = (int) Math.floor((endTime - startTime) / meandiff);

        //  - turn resampled_length into closest higher power of 2
        resampled_length = (int) Math.pow(2, Math.ceil(  (Math.log(resampled_length)/Math.log(2)) - 0.1 ));
        meandiff = (endTime - startTime) / (resampled_length-1);

        //  - generate new time vector
        double[] resampled_time = new double[resampled_length];

        for(int kk = 0; kk < resampled_length; kk++) {
            resampled_time[kk] = (meandiff * kk) + startTime;
        }

        //  - resample each acceleration dimension
        double[][] resampled_data = new double[3][];
        for(int kk = 0; kk < 3; kk++) {
            resampled_data[kk] = calculateInterp1(resampled_time, this.time_accl, this.data_accl[kk]);
        }

        //  - free for garbage collect
        this.data_accl = null;
        this.time_accl = null;

        // STEP: rotate the acceleration vectors
        double[][] resampled_rotation = new double[3][];
        for(int kk = 0; kk < 3; kk++) {
            resampled_rotation[kk] = calculateInterp1(resampled_time, this.time_rota, this.data_rota[kk]);
        }

        for(int tt = 0; tt < resampled_length; tt++) {
            double q0 = Math.sqrt(1 - Math.pow(resampled_rotation[0][tt], 2) - Math.pow(resampled_rotation[1][tt], 2) - Math.pow(resampled_rotation[2][tt], 2));
            Rotation rotator = new Rotation(q0, resampled_rotation[0][tt], resampled_rotation[1][tt], resampled_rotation[2][tt], false);

            double[] output = new double[3];
            rotator.applyTo(new double[]{resampled_data[0][tt], resampled_data[1][tt], resampled_data[2][tt]}, output);

            resampled_data[0][tt] = output[0];
            resampled_data[1][tt] = output[1];
            resampled_data[2][tt] = output[2];
        }


        // *****************************************

        // STEP: filter data
        for(int kk = 0; kk < 3; kk++) {
            double normalized_hicutoff = 30 * meandiff / 2;
            resampled_data[kk] = calculateFilterNoDC_FFT(resampled_data[kk], normalized_hicutoff);
        }
        // *****************************************


        int half_longterm_wnd_length = (int) Math.round((LONGTERM_WND_LENGTH / 2) / meandiff);

        // STEP: subtract longer term moving average
        double[][] resampled_debiased_data = new double[3][resampled_length];

        for(int kk = 0; kk < 3; kk++) {
            for(int tt = 0; tt < resampled_length; tt++) {
                int idxBegin = Math.max(0, tt-half_longterm_wnd_length),
                    idxLength = Math.min(resampled_length-1, tt+half_longterm_wnd_length) - idxBegin;
                resampled_debiased_data[kk][tt] = resampled_data[kk][tt] - StatUtils.mean(resampled_data[kk], idxBegin, idxLength);
            }
        }
        // *****************************************


        // STEP: convert separator times into indices
        this.resampled_idx = new int[this.time_split.length][2];

        for(int kk = 0; kk < this.resampled_idx.length; kk++) {
            this.resampled_idx[kk][0] = (int) Math.ceil((this.time_split[kk][0] - resampled_time[0]) / meandiff);
            this.resampled_idx[kk][1] = (int) Math.floor((this.time_split[kk][1] - resampled_time[0]) / meandiff);
        }
        // *****************************************

        this.resampled_data = resampled_debiased_data;
        this.meandiff = meandiff;
    }


    // perform integration over a certain interval to get position
    protected double[][] doIntegration(int segmentBegin, int segmentLength) {

        // STEP: integrate acceleration twice to get position
        //  - integrate accl to get velocity
        double[][] velocity = new double[3][segmentLength];
        for(int kk = 0; kk < 3; kk++) {
            // initial velocity is zero + change in first time step
            velocity[kk][0] = (meandiff * this.resampled_data[kk][segmentBegin]);

            // loop through time steps and add changes
            for(int tt = 1; tt < segmentLength; tt++) {
                velocity[kk][tt] = velocity[kk][tt-1] + (meandiff * this.resampled_data[kk][tt + segmentBegin]);
            }
        }

        //  - integrate velocity to get position
        double[][] position = new double[3][segmentLength];
        for(int kk = 0; kk < 3; kk++) {

            // initial position is zero + change in first time step
            position[kk][0] = (meandiff * velocity[kk][0]);

            for(int tt = 1; tt < segmentLength; tt++) {
                position[kk][tt] = position[kk][tt-1] + (meandiff * velocity[kk][tt]);
            }
        }
        // *****************************************

        return position;

        // TODO: correct position based on return-to-zero
    }


    // calculate the moving average of a 3 x N matrix of values in the N dimension.
    protected double[][] calculateMovingAverage(double[][] input, int numsamples) {
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

            output[0][kk] = output[0][kk] / numsamples;
        }

        return output;
    }

    // remove values with duplicated time stamps
    // TODO: perhaps consider averaging duplicates instead
    protected ArrayList<Integer> doRemoveDuplicates(ArrayList<Integer> idx) {

        ArrayList<Integer> duplicatedTimes = new ArrayList<>();

        // loop through sensor points and mark duplicated time stamps for removal
        for(int kk = 1; kk < idx.size(); kk++) {
            if(this.data_input.get(idx.get(kk)).time == this.data_input.get(idx.get(kk-1)).time) {
                duplicatedTimes.add(kk);
            }
        }

        // remove all items marked for removal
        // we can't remove while searching, because then the size of the arraylist would change, that that makes things complicated.
        // - search backwards and delete, so that it doesn't upset the indexing
        for(int kk = duplicatedTimes.size()-1; kk >= 0; kk--) {
            idx.remove((int) duplicatedTimes.get(kk));
        }

        return idx;
    }

    // do 1D linear interpolation of data, similar to matlab interp1 command
    protected double[] calculateInterp1(double[] newTime, double[] oldTime, double[] oldX) {
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
    protected double[] calculateFilterNoDC_FFT(double[] datain, double hicutoff) {
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
    protected double[] calculateDiff(double[] input) {
        double[] output = new double[input.length - 1];

        for(int kk = 0; kk < input.length - 1; kk++) {
            output[kk] = input[kk+1] - input[kk];
        }

        return output;
    }

    // return the computed workspace volume
    public double getWorkspaceVolume () {

        return this.fitmeasures[0];
    }

    // return the computed XY plane area
    public double getXYplane() {

        return this.fitmeasures[1];
    }

    public double getYZplane() {

        return this.fitmeasures[2];
    }

    public double getXZplane() {

        return this.fitmeasures[3];
    }

    public ArrayList<double[]> getPosition() {
        return this.position;
    }

    public double[] getMaxpositions() {
        return this.maxpositions;
    }


    // calculate the z-score of data
    public static double[] calculateZScore(double[] data) {
        double [] output = new double[data.length];
        double popmean = StatUtils.mean(data);
        double popstd = Math.sqrt(StatUtils.variance(data, popmean));

        for(int kk = 0; kk < data.length; kk++) {
            output[kk] = (data[kk] - popmean) / popstd;
        }

        return output;
    }

    // return all the values between the middle prctile-th percentiles of the data
    public static double[] calculateThresholdPrctile(double[] data, double prctile) {
        boolean[] output_temp = new boolean[data.length];
        int newlength = 0;

        double edgeprctile = (1-prctile)/2;
        double thresholdlow = StatUtils.percentile(data, edgeprctile),
               thresholdhigh = StatUtils.percentile(data, 1-edgeprctile);


        // find data within bounds
        for(int kk = 0; kk < data.length; kk++) {
            if(data[kk] >= thresholdlow && data[kk] <= thresholdhigh) {
                output_temp[kk] = true;
                newlength++;
            } else {
                output_temp[kk] = false;
            }

        }

        // copy data between thresholds to new array
        double[] output = new double[newlength];
        int counter = 0;
        for(int kk = 0; kk < data.length; kk++) {
            if(output_temp[kk]) {
                output[counter] = data[kk];
                counter++;
            }
        }


        return output;
    }
}
