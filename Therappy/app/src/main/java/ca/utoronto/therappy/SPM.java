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
        //finish();
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
            double[][] results;
            data_accl = new ArrayList<>();

            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            try {
                // check if the raw data file exists...
                sensorFiles = new File(fileName + ".txt");
                if (!sensorFiles.exists()) {
                    Log.i(TAG, "Problem opening file...exiting");
                    publishProgress("File not found");
                    cancel(true);
                }
                // create the new output file
                spmFile = new File(fileName + "-output.txt");
                if(!spmFile.exists()){
                    // for some reason, this is always called...
                    Log.i(TAG, "Problem creating output file...exiting");
                    publishProgress("Cannot create new file");
                    //cancel(true);
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
                    } /*else if (sensorData[1].compareTo("r") == 0) {
                        data_rota.add(new sensorPoint(time-t0, new double[]{x, y, z}, sensorPoint.DATA_ROTATIONVEC));
                    }*/
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // ensure arrays are sorted properly
            if(data_accl.isEmpty()){
                cancel(true);
            }else {
                Collections.sort(data_accl);
            }
            //Collections.sort(data_rota);

            publishProgress("file read. processing signals.");

            // CALL SENSOR MODULE HERE
            SPM_FunctionalWorkspace sigProcInstance = new SPM_FunctionalWorkspace(data_accl);
            results = sigProcInstance.doChurnData();
            /*
            // retrieve results
            double xyarea = sigProcInstance.getXYplane();
            double xzarea = sigProcInstance.getXZplane();
            double yzarea = sigProcInstance.getYZplane();
            double fwvol = sigProcInstance.getWorkspaceVolume();
            */
            // now onto Joel's stuff!
            try {
                int len = results[0].length;
                for (int i = 0; i < len; i++) {
                    writer.write(results[0][i] + "," + results[1][i] + "," + results[2][i]);
                    writer.newLine();
                }
            }catch(Exception e){
                e.printStackTrace();
            }

            /*
             * line 1: timestamp
             * line 2: FW volume
             * line 3: X-Y area
             * line 4: X,Y coordinates for X-Y area
             * line 5: X-Z area
             * line 6: X,Y coordinates for X-Z area
             * line 7: Y-Z area
             * line 8: X,Y coordinates for Y-Z area
             */

            // close writing and reading.
            try {
                writer.flush();
                writer.close();
                fwriter.flush();
                writer.close();
                reader.close();
                freader.close();
            }catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String... progress) {
            status.setText(progress[0]);
        }

        protected void onPostExecute(){
            status.setText("Complete!");
        }

        protected void onCancelled(){
        }
    }


}
