/* Wearable portion of the app
*  Created by: Andrew Wong
*  Created for: Jose Zariffa
*  For the purposes of completing BME489
*/

package ca.utoronto.therappy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.ByteBuffer;
import java.util.List;


public class MainActivity extends Activity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener, View.OnClickListener {

    /* sensor variables */
    private SensorManager mSensorManager;                   // sensor manager
    private Sensor mAccelerometer, mRotation;               // accelerometer and rotation vector sensor variables
    private float[] rotmatrix = new float[16];              // rotation matrix

    // Linear interpolation stuffs
    private float[][] bufferAccl = new float[128][];        // buffered acceleration values, making this large enough that it'll hold everything
    private long[] bufferAcclTime = new long[128];
    private int bufferAcclCounter = 0;

    private float[] bufferRot;                              // the previous rotation value, buffered.
    private long bufferRotTime = 0;

    /* debug variables */
    private static final String TAG = MainActivity.class.getSimpleName();

    /* communication variables */
    private GoogleApiClient mGoogleApiClient;
    private static final String WEAR_MESSAGE_PATH = "/message";
    private static final String DATA_MESSAGE_PATH = "/sensordata";
    private static final String INSTRUCTION_MESSAGE_PATH = "/instruction";              // instruction data header
    private String currInstruction;

    /* for recording */
    private static final int COUNT = 64;                                                        // size of buffer (in number of samples)
    private static ByteBuffer MessageBuffer = ByteBuffer.allocate((8 + 2 + 4*3)*COUNT);         // message buffer
    //private static ByteBuffer MessageBuffer = ByteBuffer.allocate((8 + 4*3)*COUNT);         // message buffer
    private static int cycle;                                                               // current number of items in buffer
    private boolean started;
    private signalWatcher watcher;

    private ImageButton btnLoad;

    /*  onCreate
     *  Input:  Bundle savedInstanceState - previous saved state
     *  Output: void
     *
     *  This function is called when this thread is created. It sets up all sensors needed for
     *  sending data to the phone
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set up some basic stuff
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // start sensor manager and register sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_GAME);

        // pre-clear variables
        for(int i = 0; i < 16; i++){
            this.rotmatrix[i] = 0;
        }
        MessageBuffer.clear();
        cycle = 0;
        started = false;

        // initialize acceleration buffer
        clearBufferAccl();

        // initiate communication
        initGoogleApiClient();

        // set up UI elements
        btnLoad = (ImageButton)findViewById(R.id.btnLoading);
        btnLoad.setOnClickListener(this);
        btnLoad.setEnabled(true);
        watcher = new signalWatcher();
        sendMessage(WEAR_MESSAGE_PATH, "");



    }

    /* stopMeasuring
     * Input:   void
     * Output:  void
     *
     * This function is called when the companion app no longer needs to measure data. It will
     * unregister all sensors, send the remaining data and close everything. Then close the app.
     */
    public void stopMeasuring() {
        mSensorManager.unregisterListener(this);
        MessageBuffer.clear();
        watcher.onDestroy();
        finish();
    }

    /* Sensors Protocols */

    /* onAccuracyChanged
     * Input:   Sensor sensor - sensor in which the accuracy has changed
     *          int accuracy  - current sensor accuracy
     * Output:  void
     *
     * Method is required for the SensorEventListener. It is not used in this app.
     */
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    // clear the acceleration buffer once it has been processed
    private void clearBufferAccl() {
        for(int kk = 0; kk < this.bufferAccl.length; kk++) {
            this.bufferAccl[kk] = null;
            this.bufferAcclTime[kk] = 0;
        }
        this.bufferAcclCounter = 0;
    }



    /*  onSensorChanged
     *  Input:  SensorEvent event - contains the sensor which has changed and the new values
     *  Output: void
     *
     *  This function is called whenever a sensor value has changed. The function extracts the
     *  value of the current sensor data, determines the type of sensor it came from, and places
     *  the message into the message buffer to send to the phone.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Many sensors return 3 values, one for each axis.
        float[] data = new float[4];
        data[0] = event.values[0];
        data[1] = event.values[1];
        data[2] = event.values[2];
        data[3] = 0;
        long time = event.timestamp;

        // check sensor type
        switch (event.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                if (started) {
                    // store into buffer
                    this.bufferAccl[this.bufferAcclCounter] = data;
                    this.bufferAcclTime[this.bufferAcclCounter] = time;
                    this.bufferAcclCounter++;
                }

                break;

            case Sensor.TYPE_ROTATION_VECTOR:
                if (started) {
                    // get linear coefficients for interpolation.
                    float Ax, Ay, Az;
                    Ax = (data[0] - this.bufferRot[0]) / (time - this.bufferRotTime);
                    Ay = (data[1] - this.bufferRot[1]) / (time - this.bufferRotTime);
                    Az = (data[2] - this.bufferRot[2]) / (time - this.bufferRotTime);


                    // float arrays are default zeroed upon initialization. not that it matters.
                    float[] tempRot = new float[3];
                    float[] tempAccl = new float[4];
                    float[] rotmatrix = new float[16], trotmatrix = new float[16];      // NOTE: not sure if matrix can be transposed in place.

                    // loop over acceleration values
                    for (int kk = 0; kk < this.bufferAcclCounter; kk++) {
                        // calculate interpolated rotation values
                        tempRot[0] = Ax * (this.bufferAcclTime[kk] - this.bufferRotTime) + this.bufferRot[0];
                        tempRot[1] = Ay * (this.bufferAcclTime[kk] - this.bufferRotTime) + this.bufferRot[1];
                        tempRot[2] = Az * (this.bufferAcclTime[kk] - this.bufferRotTime) + this.bufferRot[2];

                        // calculate rotation matrix
                        SensorManager.getRotationMatrixFromVector(rotmatrix, tempRot);
                        Matrix.transposeM(trotmatrix, 0, rotmatrix, 0);        // apparently transposing a rotation matrix is a more computationally effective way of inverting

                        // rotate the acceleration vector
                        Matrix.multiplyMV(tempAccl, 0, trotmatrix, 0, this.bufferAccl[kk], 0);

                        // give this to signalWatcher
                        watcher.onSensorChanged(tempAccl, this.bufferAcclTime[kk]);
                    }
                } // if (started)

                // >> BEGIN debugging monitoring
                if(watcher.isBackToOrigin()){
                    Log.i(TAG, "Back to origin!");
                    sendMessage(DATA_MESSAGE_PATH, watcher.getFurthestPosition() + "");
                }
                // >> END debugging monitoring

                // update the buffer
                this.bufferRot = data;
                this.bufferRotTime = time;

                break;  // case Sensor.TYPE_ROTATION_VECTOR:

            default:
                break;
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnLoading:
                sendMessage(WEAR_MESSAGE_PATH, "");
                break;
            default:
                break;

        }
    }

    /* Communications Protocols */

    /*  initGoogleApiClient
     *  Input:  null
     *  Output: void
     *
     *  This function initializes the Google Api Client used to communicate with the phone.
     */
    private void initGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();
    }

    /* onConnected
     * Input:   Bundle bundle
     * Output:  void
     *
     * Function is called when the GoogleApiClient has connected with the phone. it adds the wearable
     * listener to the phone to listen for further commands.
     */
    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        sendMessage(WEAR_MESSAGE_PATH, "");
    }

    /* onConnectionSuspended
     * Input:   int i
     * Output:  void
     *
     * Called when the connection between the phone and watch is suspended. Not used.
     */
    @Override
    public void onConnectionSuspended(int i) {
    }

    /*  onMessageReceived
     *  Input:  MessageEvent messageEvent - the message and its associated information
     *  Output: void
     *
     *  This function is called when it receives a message through the MessageApi. It determines
     *  the source of the message, and then extracts the payload to determine the correct function
     *  call.
     */
    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        final String msg = new String(messageEvent.getData());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (messageEvent.getPath().equalsIgnoreCase(WEAR_MESSAGE_PATH)) {
                    if(msg.equalsIgnoreCase("START")) {
                        Log.i(TAG, "Recording has started");
                        started = true;
                    }
                    else if(msg.equalsIgnoreCase("STOP")) {
                        Log.i(TAG, "Recording has stopped");
                        started = false;
                        finish();
                    }
                }
                else if (messageEvent.getPath().equalsIgnoreCase(INSTRUCTION_MESSAGE_PATH)) {
                    // do something
                    if(msg.equalsIgnoreCase("READY")){
                        btnLoad.setEnabled(false);
                        currInstruction = "START";
                        Log.i(TAG, "Recording!");
                    }
                    else if(msg.equalsIgnoreCase("FLUSH")){
                        MessageBuffer.compact();
                        sendMessage(DATA_MESSAGE_PATH, MessageBuffer);
                        MessageBuffer.clear();
                        sendMessage(INSTRUCTION_MESSAGE_PATH, "END");
                        Log.i(TAG, "End of transmission");
                    }
                }
            }
        });
    }

    /*  sendMessage
     *  Input:  String path - message path or header
     *          String message - message data
     *          ByteBuffer message - message data (for the sensors)
     *  Output: void
     *
     *  This function is called when a message needs to be sent. It requires the header information
     *  and the payload to send the data. Two different methods are present, depending on the type
     *  of data being sent. ByteBuffer is used for sensor data, String is used for all other data.
     */
    private void sendMessage(final String path, final ByteBuffer message) {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                List<Node> nodes = getConnectedNodesResult.getNodes();
                for (Node node : nodes) {
                    Log.i(TAG, "WEAR sending to " + node);
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.array()).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.i(TAG, "WEAR Result " + sendMessageResult.getStatus());
                        }
                    });
                }
            }
        });
    }
    private void sendMessage(final String path, final String message) {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                List<Node> nodes = getConnectedNodesResult.getNodes();
                for (Node node : nodes) {
                    Log.i(TAG, "WEAR sending to " + node);
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes()).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.i(TAG, "WEAR Result " + sendMessageResult.getStatus());
                        }
                    });
                }
            }
        });
    }

    /* System commands */

    /*  Called when the state of the watch changes. On any suspended commands (pause, destroy,
     *  finish), the app will de-register all sensors and listeners to save battery. On any resume
     *  commands (resume), the app will re-register all the sensors and listeners.
     *
     */

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_FASTEST);
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        if (mGoogleApiClient != null) {
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.disconnect();
        }
        watcher.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        }
        watcher.onDestroy();
    }

    @Override
    public void finish(){
        super.finish();
        watcher.onDestroy();
        if(mGoogleApiClient != null) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.disconnect();
        }
    }
}