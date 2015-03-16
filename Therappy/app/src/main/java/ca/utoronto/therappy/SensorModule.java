package ca.utoronto.therappy;


import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;



public class SensorModule extends ActionBarActivity implements SensorEventListener, View.OnClickListener{

    /* for file writing */
    private BufferedWriter writer;          // Bufferwriter used to buffer write I/O data (reduces I/O calls)
    private FileWriter fwriter;             // writer used to write data to files
    private File sensorFiles;               // location of the files
    private final int bufferSize = 2048;    // size of write buffer
    private final File root = android.os.Environment.getExternalStorageDirectory();     // location of external directory

    /* UI variables */
    private Button btnStart, btnStop, btnPrev, btnNext;            // UI buttons
    private ImageView ivInstruction;
    private TextView status;            // UI title
    private RelativeLayout layout;      // UI layout
    private Intent intent;


    /* recording variables */
    private boolean started = false;                        // whether or not the app is recording data or not
    private int step = 0;                                   // current step number
    private int NUM_STEPS = 3;                              // number of steps
    private long time = System.currentTimeMillis();         // timestamp for the long

    /* sensor variables */
    private SensorManager mSensorManager;                   // sensor manager
    private Sensor mAccelerometer, mGyroscope, mRotation;              // accelerometer and gyroscope sensor variables

    /* debug variables */
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_module);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);       // ensure screen (and app) stays on

        intent = getIntent();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_FASTEST);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnNext = (Button) findViewById(R.id.btnNext);
        btnPrev = (Button) findViewById(R.id.btnPrev);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnPrev.setOnClickListener(this);

        btnPrev.setEnabled(false);
        btnPrev.setEnabled(true);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);

        // create file in folder called therappy. if folder doesn't exist, create it
        try {
            sensorFiles = new File(root, "therappy");
            if(!sensorFiles.exists()){
                if(!sensorFiles.mkdir()){
                    Log.i(TAG, "Problem creating folder...exiting");        // if we can't create the folder, exit
                    finish();
                }
            }
            sensorFiles = new File(sensorFiles + "/therappy" + time + ".txt");
            // setup writers, using a nested writer in buffered writer
            fwriter = new FileWriter(sensorFiles, true);
            writer = new BufferedWriter(fwriter, bufferSize);
            Log.i(TAG,"New file created at " + sensorFiles.toString());
        } catch (IOException e){
            e.printStackTrace();
        }

        layout = (RelativeLayout)findViewById(R.id.sensorModuleLayout);
        status = (TextView)findViewById(R.id.tvStatus);
        ivInstruction = (ImageView) findViewById(R.id.instruction);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // Many sensors return 3 values, one for each axis.
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        long currTime = event.timestamp;
        char type = 'x';
        switch (event.sensor.getType())
        {
            case Sensor.TYPE_ACCELEROMETER:
                type = 'a';
                break;
            case Sensor.TYPE_GYROSCOPE:
                type = 'g';
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                type = 'r';
            default:
                break;
        }

        if (started && type != 'x') {      // save data only if the recording has started
            try {
                writer.write(currTime + "," + type + "," + x + "," + y + "," + z);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId())
        {
            case R.id.btnStart:
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                started = true;
                if(step < NUM_STEPS) {
                    step++;
                }
                drawInstruction();
                break;
            case R.id.btnStop:
                stopRecording();
                break;
            case R.id.btnPrev:
                if(step > 1){
                    step--;
                }
                drawInstruction();
                break;
            case R.id.btnNext:
                if(step < NUM_STEPS) {
                    step++;
                }
                drawInstruction();
                break;
            default:
                break;
        }
    }

    private void drawInstruction(){
        String STEPNAME = "ERROR";
        switch(step){
            case 1: STEPNAME = "Stir the cauldron";
                break;
            case 2: STEPNAME = "Paint the rainbow";
                break;
            case 3: STEPNAME = "Ninja chop";
                break;
            default:
                break;
        }
        status.setText("Step " + step + "of" + NUM_STEPS + "\n" + STEPNAME);
        if (step == NUM_STEPS){
            btnNext.setEnabled(false);
        }
        if(step > 0){
            btnPrev.setEnabled(true);
            String source = "drawable/instruction" + step;
            ivInstruction.setImageDrawable(getResources().getDrawable(getResources().getIdentifier(source, null, getPackageName())));
        }

    }

    private void stopRecording(){
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        // stop recording. flush buffer and save file.
        try{
            writer.flush();
            writer.close();
            fwriter.close();
        } catch (IOException e) { 
            e.printStackTrace();
        }
        started = false;
        intent.putExtra("location", sensorFiles.toString());
        setResult(RESULT_OK,intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_FASTEST);
        try {
            fwriter = new FileWriter(sensorFiles, true);
            writer = new BufferedWriter(fwriter, bufferSize);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        if(writer != null) {
            try {
                writer.flush();
                writer.close();
                fwriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        if(writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sensor_module, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
