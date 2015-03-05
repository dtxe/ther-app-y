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
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.WindowManager;
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


public class MainActivity extends Activity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener {

    /* sensor variables */
    private SensorManager mSensorManager;                   // sensor manager
    private Sensor mAccelerometer, mGyroscope;              // accelerometer and gyroscope sensor variables

    /* debug variables */
    private static final String TAG = MainActivity.class.getSimpleName();

    /* communication variables */
    private GoogleApiClient mGoogleApiClient;
    private static final String WEAR_MESSAGE_PATH = "/message";
    private static final String DATA_MESSAGE_PATH = "/sensordata";

    /* for recording */
    static final int COUNT = 32;                                                        // size of buffer (in number of samples)
    static ByteBuffer MessageBuffer = ByteBuffer.allocate((8 + 2 + 4*3)*COUNT);         // message buffer
    static int cycle = 0;                                                               // current number of items in buffer

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
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);

        // initiate communication
        initGoogleApiClient();

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
        MessageBuffer.compact();
        sendMessage(DATA_MESSAGE_PATH, MessageBuffer);
        MessageBuffer.clear();
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
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        long time = SystemClock.elapsedRealtimeNanos();
        char type = 'x';

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                type = 'a';
                break;
            case Sensor.TYPE_GYROSCOPE:
                type = 'g';
                break;
            default:
                break;
        }
        if(type != 'x') {
            MessageBuffer.putLong(time).putChar(type).putFloat(x).putFloat(y).putFloat(z).array();
            cycle++;
            if(cycle == COUNT){
                sendMessage(DATA_MESSAGE_PATH, MessageBuffer);
                MessageBuffer.clear();
                cycle = 0;
            }
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
                    }
                    else if (msg.equalsIgnoreCase("STOP")) {
                        Log.i(TAG, "Recording has stopped");
                        stopMeasuring();
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
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
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
    }

    @Override
    public void finish(){
        super.finish();
        if(mGoogleApiClient != null) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.disconnect();
        }
    }
}