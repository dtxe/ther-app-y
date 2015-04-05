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
import java.util.ArrayList;
import java.util.Collections;

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

        private ArrayList<sensorPoint> data_accl, data_rota;

        protected void onPreExecute(){
            super.onPreExecute();
            status.setText("Starting signals processing. \nPlease wait");
        }

        protected Void doInBackground(String... params){
            String fileName = params[0];
            String nextLine;
            long time = 0;
            double t0 = 0;
            float x = 0, y = 0, z = 0;
            boolean open = false;

            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            try {
                sensorFiles = new File(fileName + ".txt");
                if (!sensorFiles.exists()) {
                    Log.i(TAG, "Problem opening file...exiting");
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
                // read all lines in data file
                while((nextLine = reader.readLine()) != null) {
                    String sensorData[] = nextLine.split(",");
                    try {
                        time = Long.parseLong(sensorData[0]);
                        x = Float.parseFloat(sensorData[2]);
                        y = Float.parseFloat(sensorData[3]);
                        z = Float.parseFloat(sensorData[4]);
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    // if this is the first line read, set initial time
                    if(!open){
                        t0 = (double)time;
                        open = true;
                    }

                    // parse sensor type then add to corresponding arrayList
                    if(sensorData[1].compareTo("a") == 0) {
                        data_accl.add(new sensorPoint(time-t0, new double[]{x, y, z}, sensorPoint.DATA_ACCELERATION));
                    } else if (sensorData[1].compareTo("r") == 0) {
                        data_rota.add(new sensorPoint(time-t0, new double[]{x, y, z}, sensorPoint.DATA_ROTATIONVEC));
                    }

                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // ensure arrays are sorted properly
            Collections.sort(data_accl);
            Collections.sort(data_rota);

            publishProgress("file read. processing signals.");

            // CALL SENSOR MODULE HERE
            SPM_FunctionalWorkspace sigProcInstance = new SPM_FunctionalWorkspace(data_accl);
            sigProcInstance.doChurnData();

            // retrieve results
            double xyarea = sigProcInstance.getXYplane();
            double xzarea = sigProcInstance.getXZplane();
            double yzarea = sigProcInstance.getYZplane();
            double fwvol = sigProcInstance.getWorkspaceVolume();

            // now onto Joel's stuff!

            return null;
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
