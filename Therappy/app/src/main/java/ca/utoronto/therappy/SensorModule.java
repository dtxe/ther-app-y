package ca.utoronto.therappy;

import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;


public class SensorModule extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SensorEventListener, View.OnClickListener, MessageApi.MessageListener{

    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope;
    private BufferedWriter writer;
    private FileWriter fwriter;
    private File sensorFiles;
    private Button btnStart, btnStop, btnWear;
    private boolean started = false;
    private final int bufferSize = 2048;
    private final File root = android.os.Environment.getExternalStorageDirectory();
    TextView title,ax,ay,az, rx, ry, rz;
    RelativeLayout layout;

    private GoogleApiClient mGoogleApiClient;
    private static final String WEAR_MESSAGE_PATH = "/message";
    private static final String DATA_MESSAGE_PATH = "/sensordata";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_module);

        initGoogleApiClient();

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

        try {
            sensorFiles = new File(root + "/therappy" + System.currentTimeMillis() + ".txt");
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

    public void setData(byte[] message)
    {
        ByteBuffer bufferedData = ByteBuffer.allocate(message.length);
        char type;
        float x, y, z;
        long time;
        bufferedData.put(message, 0, message.length);
        type = bufferedData.getChar();
        x = bufferedData.getFloat();
        y = bufferedData.getFloat();
        z = bufferedData.getFloat();
        time = bufferedData.getLong();

        if(type == 'a')
        {
            ax.setText("X axis" + "\t\t" + x);
            ay.setText("Y axis" + "\t\t" + y);
            az.setText("Z axis" + "\t\t" + z);
        }
        else if (type == 'g')
        {
            rx.setText("X axis" + "\t\t" + x);
            ry.setText("Y axis" + "\t\t" + y);
            rz.setText("Z axis" + "\t\t" + z);
        }
        if (started) {
            try {
                writer.write(time + "," + type + "," + x + "," + y + "," + z + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        /*synchronized (this) {
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
        }*/
    }

    @Override
    public void onClick(View view) {
        NotificationCompat.Builder notificationBuilder;
        int notificationId = 001;
        NotificationManagerCompat notificationManager =  NotificationManagerCompat.from(SensorModule.this);
        switch(view.getId())
        {
            case R.id.btnStart:
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                // start recording
                notificationBuilder =  new NotificationCompat.Builder(SensorModule.this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("therappy")
                        .setContentText("Recording in progress!");

                notificationManager.notify(notificationId, notificationBuilder.build());
                sendMessage(WEAR_MESSAGE_PATH, "START");
                started = true;
                break;
            case R.id.btnStop:
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                notificationBuilder =  new NotificationCompat.Builder(SensorModule.this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("therappy")
                        .setContentText("Recording stopped!");

                notificationManager.notify(notificationId, notificationBuilder.build());
                sendMessage(WEAR_MESSAGE_PATH, "STOP");
                // stop recording. flush buffer and save file.
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
                // begin streaming sensor data
                notificationBuilder =  new NotificationCompat.Builder(SensorModule.this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("therappy")
                        .setContentText("Streaming Data!");

                notificationManager.notify(notificationId, notificationBuilder.build());
                sendMessage(WEAR_MESSAGE_PATH, "READ");
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
        mGoogleApiClient.connect();
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
        mGoogleApiClient.disconnect();
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
                writer.flush();
                writer.close();
                fwriter.close();
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

    /** Comm protocols **/
    private void initGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint){
        // do something
        //sendMessage(WEAR_MESSAGE_PATH, "READ");
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
                if( messageEvent.getPath().equalsIgnoreCase(DATA_MESSAGE_PATH) ) {
                    setData(messageEvent.getData());
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
