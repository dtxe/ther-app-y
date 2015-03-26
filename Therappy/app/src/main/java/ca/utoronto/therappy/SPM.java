package ca.utoronto.therappy;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;

/**
 * Created by Andrew on 09/03/2015.
 */
public class SPM extends ActionBarActivity{

    private ProgressBar spinner;
    private Intent intent;
    private String fileName;


    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView status;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading_layout);
        intent = getIntent();
        spinner = (ProgressBar)findViewById(R.id.progressBar);
        status = (TextView)findViewById(R.id.load_status);
        fileName = intent.getStringExtra("fileName");

        new SPMCalculate().execute(fileName);
    }

    private class SPMCalculate extends AsyncTask<String, String, Void> {

        /* for file writing */
        private BufferedWriter writer;          // Bufferwriter used to buffer write I/O data (reduces I/O calls)
        private FileWriter fwriter;             // writer used to write data to files
        private File sensorFiles, spmFile;               // location of the files
        private FileReader freader;
        private BufferedReader reader;
        private final int bufferSize = 2048;    // size of write buffer

        private ArrayList<sensorPoint> ax, ay, az, rx, ry, rz;

        protected void onPreExecute(){
            super.onPreExecute();
            status.setText("Starting signals processing. \nPlease wait");
        }

        protected Void doInBackground(String... params){
            String fileName = params[0];
            String nextLine;
            long time = 0;
            double t0 = 0;
            char type = 'x';
            float x, y, z;
            boolean open = false;

            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            try {
                sensorFiles = new File(fileName + ".txt");
                if (!sensorFiles.exists()) {
                    Log.i(TAG, "Problem opening file...exiting");        // if we can't create the folder, exit
                    publishProgress("File not found");
                    cancel(true);
                }
                spmFile = new File(fileName + "-output.txt");
                if(!spmFile.exists()){
                    Log.i(TAG, "Problem creating output file...exiting");
                    publishProgress("Cannot create new file");
                    cancel(true);
                }
                fwriter = new FileWriter(spmFile, true);
                writer = new BufferedWriter(fwriter, bufferSize);
                freader = new FileReader(sensorFiles);
                reader = new BufferedReader(freader, bufferSize);
            } catch(IOException e){
                e.printStackTrace();
            }
            publishProgress("Opening file");
            try{
                while((nextLine = reader.readLine()) != null) {
                    String sensorData[] = nextLine.split(",");
                    try {
                        time = Long.parseLong(sensorData[0]);
                        type = sensorData[1].charAt(0);
                        x = Float.parseFloat(sensorData[2]);
                        y = Float.parseFloat(sensorData[3]);
                        z = Float.parseFloat(sensorData[4]);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    if(!open){
                        t0 = (double)time;
                        open = true;
                    }
                    switch(type){
                        case 'a':
                            ax.add(new sensorPoint(time-t0, x));
                            ax.add(new sensorPoint(time-t0, y));
                            ax.add(new sensorPoint(time-t0, z));
                            break;
                        case 'r':
                            rx.add(new sensorPoint(time-t0, x));
                            rx.add(new sensorPoint(time-t0, y));
                            rx.add(new sensorPoint(time-t0, z));
                            break;
                        default: publishProgress("Issue parsing data");
                            Log.i(TAG, "Error opening dataset: type not found");
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // ensure arrays are sorted properly
            Collections.sort(ax);
            Collections.sort(ay);
            Collections.sort(az);
            Collections.sort(rx);
            Collections.sort(ry);
            Collections.sort(rz);

            publishProgress("doing something");
            return null;
        }

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
            Complex fftoutput = fft(datain);
            // ***************

            // create a vector of things to zero out, set everything to 1
            double[] zeroidx = Array(num_samples);
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
                fftoutput[kk] = fftoutput[kk] * zeroidx[kk];
            }


            // INVERSE FFT MAGICKS HAPPENS HERE
            // ***************
            double[] output  = real(ifft(fftoutput));
            // ***************

            return output;
        }

        protected void onProgressUpdate(String progress) {
            status.setText(progress);
        }

        protected void onPostExecute(){
            status.setText("Complete!");
        }

        protected void onCancelled(){
        }
    }


}
