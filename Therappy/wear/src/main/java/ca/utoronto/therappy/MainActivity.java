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
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.ByteBuffer;


public class MainActivity extends Activity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener {

    private SensorManager mSensorManager;
    private Sensor mLinAccelerometer, mGyroscope;
    private LinearLayout mRectBackground;
    private RelativeLayout mRoundBackground;
    private TextView status, ax, ay, az, rx, ry, rz;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String WEAR_MESSAGE_PATH = "/message";
    private static final String DATA_MESSAGE_PATH = "/sensordata";
    private ArrayAdapter<String> mAdapter;
    private GoogleApiClient mGoogleApiClient;
    private boolean started = false;

    static final int COUNT = 32;
    static ByteBuffer MessageBuffer = ByteBuffer.allocate(2 + 4*3 + 8);
    static ByteBuffer GyroBuffer = ByteBuffer.allocate((4 * 3 * COUNT) + (8 * 1 * COUNT));

    static int accelerator_cycle = 0;
    static int gyro_cycle = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rect_activity_main);

        // start sensor manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //mAdapter = new ArrayAdapter<String>(this, R.layout.list_item);
        //mListView.setAdapter(mAdapter);

        // initiate communication
        initGoogleApiClient();
        status = (TextView) findViewById(R.id.status);
        ax = (TextView) findViewById(R.id.ax);
        ay = (TextView) findViewById(R.id.ay);
        az = (TextView) findViewById(R.id.az);
        rx = (TextView) findViewById(R.id.rx);
        ry = (TextView) findViewById(R.id.ry);
        rz = (TextView) findViewById(R.id.rz);

    }

    public void startMeasuring() {
        status.setText("Measuring");
        sendData();
        started = true;
    }

    public void stopMeasuring() {
        status.setText("Not Measuring");
        started = false;
        mSensorManager.unregisterListener(this);
        sendMessage(DATA_MESSAGE_PATH, MessageBuffer);
        MessageBuffer.clear();
    }

    public void sendData(){
        // start sensor recording
        status.setText("Data Enabled");
        mLinAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    /* Sensors Protocols */

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Many sensors return 3 values, one for each axis.
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        char type = 'x';

        switch (event.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                //display values using TextView
                ax.setText("X axis" + "\t\t" + x);
                ay.setText("Y axis" + "\t\t" + y);
                az.setText("Z axis" + "\t\t" + z);
                Log.i(TAG, "Accel x: " + x + " y: " + y + " z: " + z);
                type = 'a';
                break;
            case Sensor.TYPE_GYROSCOPE:
                rx.setText("X axis" + "\t\t" + x);
                ry.setText("Y axis" + "\t\t" + y);
                rz.setText("Z axis" + "\t\t" + z);
                type = 'g';
                break;
            default:
                break;
        }
        if(type != 'x' && started) {
            MessageBuffer.putChar(type).putFloat(x).putFloat(y).putFloat(z).putLong(System.currentTimeMillis()).array();
            sendMessage(DATA_MESSAGE_PATH, MessageBuffer);
            MessageBuffer.clear();
        }
    }

    /* Communications Protocols */

    private void initGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        if (mGoogleApiClient != null && !(mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting()))
            mGoogleApiClient.connect();
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        final String msg = new String(messageEvent.getData());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                status.setText("MSG REC'D");
                if (messageEvent.getPath().equalsIgnoreCase(WEAR_MESSAGE_PATH)) {
                    if(msg.equalsIgnoreCase("START")) {
                        status.setText("START MSG REC'D");
                        startMeasuring();
                    }
                    else if (msg.equalsIgnoreCase("STOP")) {
                        status.setText("STOP MSG REC'D");
                        stopMeasuring();
                    }
                    else if(msg.equalsIgnoreCase("READ")) {
                        status.setText("READ MSG REC'D");
                        sendData();
                    }
                }
            }
        });
    }

    public void sendMessage(final String path, final ByteBuffer message) {
        final byte[] data = message.array();
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                for(Node node : nodes.getNodes()) {
                    Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, node.getId(), path, data)
                        .setResultCallback(
                            new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(MessageApi.SendMessageResult sendMessageResult) {

                                    if (!sendMessageResult.getStatus().isSuccess()) {
                                        Log.e(TAG, "Failed to send message with status code: "
                                                + sendMessageResult.getStatus().getStatusCode());
                                    } else {
                                        Log.e(TAG, "data sent successfully");
                                    }
                                }
                            }
                        );
                }

                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        // creating something in the UI that notifies user that the thing is recording
                    }
                });
            }
        }).start();
    }

    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // do something
    }

    /* System commands */

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mLinAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
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
}