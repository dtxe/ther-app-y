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

        private sensorPoint[] points;

        protected void onPreExecute(){
            super.onPreExecute();
            status.setText("Starting signals processing. \nPlease wait");
        }

        protected Void doInBackground(String... params){
            /*
            String fileName = params[0];
            String nextLine;
            int size = 0;
            int temp;
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
                    size++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            points = new sensorPoint[size];
            try{
                temp = 0;
                while((nextLine = reader.readLine()) != null) {
                    points[temp] = new sensorPoint(nextLine);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            publishProgress("going");
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
