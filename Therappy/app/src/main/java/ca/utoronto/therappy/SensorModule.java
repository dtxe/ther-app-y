package ca.utoronto.therappy;

import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;


public class SensorModule extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SensorEventListener, View.OnClickListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope;
    private BufferedWriter writer;
    private FileWriter fwriter;
    private File sensorFiles;
    private Button btnStart, btnStop, btnWear;
    private boolean started = false;
    private final int bufferSize = 2048;
    TextView title,ax,ay,az, rx, ry, rz;
    RelativeLayout layout;

    private GoogleApiClient mGoogleApiClient;
    private static final String START_ACTIVITY = "/start_activity";
    private static final String WEAR_MESSAGE_PATH = "/message";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_module);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addApi(Wearable.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();

        Intent intent = getIntent();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnWear = (Button) findViewById(R.id.wearButton);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnWear.setOnClickListener(this);

        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnWear.setEnabled(true);

        File root = android.os.Environment.getExternalStorageDirectory();

        try {
            sensorFiles = new File(root + "/therappy/therappy" + System.currentTimeMillis() + ".txt");
            fwriter = new FileWriter(sensorFiles, true);
            writer = new BufferedWriter(fwriter, bufferSize);
        } catch (IOException e){
            e.printStackTrace();
        }

        layout = (RelativeLayout)findViewById(R.id.sensorModuleLayout);

        title=(TextView)findViewById(R.id.name);
        ax=(TextView)findViewById(R.id.ax);
        ay=(TextView)findViewById(R.id.ay);
        az=(TextView)findViewById(R.id.az);
        rx=(TextView)findViewById(R.id.rx);
        ry=(TextView)findViewById(R.id.ry);
        rz=(TextView)findViewById(R.id.rz);

    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            // Many sensors return 3 values, one for each axis.
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            long currTime = System.currentTimeMillis();
            title.setText(R.string.app_name);
            switch (event.sensor.getType())
            {
                case Sensor.TYPE_ACCELEROMETER:
                    //display values using TextView
                    ax.setText("X axis" + "\t\t" + x);
                    ay.setText("Y axis" + "\t\t" + y);
                    az.setText("Z axis" + "\t\t" + z);

                    if (started) {
                        try {
                            writer.write(currTime + ",a," + x + "," + y + "," + z + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    rx.setText("X axis" + "\t\t" + x);
                    ry.setText("Y axis" + "\t\t" + y);
                    rz.setText("Z axis" + "\t\t" + z);

                    if (started) {
                        try {
                            writer.write(currTime + ",r," + x + "," + y + "," + z + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    break;
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
                break;
            case R.id.btnStop:
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                try{
                    writer.flush();
                    writer.close();
                    fwriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                started = false;
                finish();
                break;
            case R.id.wearButton:
                int notificationId = 001;
                // send a notification that recording is starting
                NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(SensorModule.this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("therappy")
                        .setContentText("Recording in progress!");

                NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(SensorModule.this);

                notificationManager.notify(notificationId, notificationBuilder.build());
                sendMessage(WEAR_MESSAGE_PATH, "");
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
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
        mGoogleApiClient.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sensor_module, menu);
        return true;
    }

    private void initGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addApi(Wearable.API)
            .build();
    }

    @Override
    public void onConnected(Bundle connectionHint){
        // do something
        sendMessage(START_ACTIVITY,"");
    }

    @Override
    public void onConnectionSuspended(int i) {
        // do something
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // do something
    }

    private void sendMessage(final String path, final String text) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, node.getId(), path, text.getBytes() ).await();
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

    public void onMessageReceived( final MessageEvent messageEvent ) {
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                if( messageEvent.getPath().equalsIgnoreCase( WEAR_MESSAGE_PATH ) ) {
                    // what happens when you receive a message
                    // g = gyro data
                    // a = lin accel data
                    messageEvent.getData(); //Byte[] need to convert to string or something...
                }
            }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop(){
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void finish() {
        // do something
        super.finish();

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
