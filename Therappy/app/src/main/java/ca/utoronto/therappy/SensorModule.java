package ca.utoronto.therappy;

import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
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
import java.util.StringTokenizer;


public class SensorModule extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, MessageApi.MessageListener{

    private BufferedWriter writer;
    private FileWriter fwriter;
    private File sensorFiles;
    private Button btnStart, btnStop, btnWear;
    private boolean started = false;
    private final int bufferSize = 2048;
    private final File root = android.os.Environment.getExternalStorageDirectory();
    TextView title;
    RelativeLayout layout;

    private GoogleApiClient mGoogleApiClient;
    private static final String START_ACTIVITY = "/therappy-start_activity";
    private static final String WEAR_MESSAGE_PATH = "/message";
    private static final String DATA_MESSAGE_PATH = "/sensordata";
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_module);

        initGoogleApiClient();

        Intent intent = getIntent();
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

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
            sensorFiles = new File(root, "therappy");
            if(!sensorFiles.exists()){
                if(!sensorFiles.mkdir()){
                    Log.i(TAG, "Problem creating folder...exiting");
                    finish();
                }
            }
            sensorFiles = new File(sensorFiles + "/therappy" + System.currentTimeMillis() + ".txt");
            fwriter = new FileWriter(sensorFiles, true);
            writer = new BufferedWriter(fwriter, bufferSize);
        } catch (IOException e){
            e.printStackTrace();
        }

        layout = (RelativeLayout)findViewById(R.id.sensorModuleLayout);

        title=(TextView)findViewById(R.id.name);
    }

    public void setData(byte[] message)
    {
        /* setup local variables to translate the buffer message into useable information */
        char type = 'x';
        float x = 0, y = 0, z = 0;
        long time;
        ByteBuffer buffer;

        buffer = ByteBuffer.wrap(message);                                                          // place the message in the buffer
        buffer.rewind();                                                                            // rewind buffer to start from 0

        while(buffer.hasRemaining()) {                                                              // message has form: time, type, x, y, z
            time = buffer.getLong();
            type = buffer.getChar();
            x = buffer.getFloat();
            y = buffer.getFloat();
            z = buffer.getFloat();

            Log.i(TAG, "Data is: " + time + "," + type + "," + x + "," + y + "," + z);

            if (started) {                                                                          // save data only if the recording has started
                try {
                    writer.write(time + "," + type + "," + x + "," + y + "," + z);
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onClick(View view) {

        //start something

        //end something */
        switch(view.getId())
        {
            case R.id.btnStart:
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                sendMessage(WEAR_MESSAGE_PATH, "START");
                started = true;
                break;
            case R.id.btnStop:
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                sendMessage(WEAR_MESSAGE_PATH, "STOP");
                // stop recording. flush buffer and save file.
                
                try{
                    writer.flush();
                    fwriter.flush();
                    writer.close();
                    fwriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                started = false;
                finish();
                break;
            case R.id.wearButton:
                sendMessage(START_ACTIVITY, "");
                Log.i(TAG, "calling wear");
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        mGoogleApiClient.disconnect();
        if(writer != null) {
            try {
                writer.flush();
                fwriter.flush();
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
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint){
        // do something
    }

    @Override
    public void onConnectionSuspended(int i) {
        // do something
        title.setText("not connected to wear");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // do something
        title.setText("not connected to wear");
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

    @Override
    public void onMessageReceived( final MessageEvent messageEvent ) {
       runOnUiThread( new Runnable() {
            @Override
            public void run() {
                if( messageEvent.getPath().equalsIgnoreCase(DATA_MESSAGE_PATH) ) {
                    final String msg = new String(messageEvent.getData());
                    Log.i(TAG, "data rec'd: " + msg);
                    setData(messageEvent.getData());
                }
                else if (messageEvent.getPath().equalsIgnoreCase(WEAR_MESSAGE_PATH)){
                    title.setText("connected to wear");
                }
            }
        });
    }

    public void sendNotifications(String title, String text){
        NotificationCompat.Builder notificationBuilder;
        int notificationId = 001;
        NotificationManagerCompat notificationManager =  NotificationManagerCompat.from(SensorModule.this);
        notificationBuilder =  new NotificationCompat.Builder(SensorModule.this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(text);
        notificationManager.notify(notificationId, notificationBuilder.build());
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
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
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
