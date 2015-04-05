package ca.utoronto.therappy;

import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import java.util.ArrayList;

public class SensorModule extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, MessageApi.MessageListener{

    /* for file writing */
    private BufferedWriter writer;          // Bufferwriter used to buffer write I/O data (reduces I/O calls)
    private FileWriter fwriter;             // writer used to write data to files
    private File sensorFiles;               // location of the files
    private final int bufferSize = 2048;    // size of write buffer
    private final File root = android.os.Environment.getExternalStorageDirectory();     // location of external directory

    /* UI variables */
    private Button lButton, bNext;            // UI buttons
    private ImageView ivInstruction;
    private TextView status, lstatus, lhint;            // UI title
    private RelativeLayout layout;      // UI layout
    private Intent intent;
    private View vloading, vmain;
    private ProgressBar loader;
    private AnimationDrawable frameAnimation;

    /* recording variables */
    private boolean started = false;                        // whether or not the app is recording data or not
    private int step = 0;                                   // current step number
    private int NUM_STEPS = 3;                              // number of steps
    private long time = System.currentTimeMillis();         // timestamp for the long

    /* communication variables */
    private GoogleApiClient mGoogleApiClient;                                           // communications protocol with the watch
    private static final String START_ACTIVITY = "/therappy-start_activity";            // start command for watch
    private static final String WEAR_MESSAGE_PATH = "/message";                         // watch message header
    private static final String DATA_MESSAGE_PATH = "/sensordata";                      // watch sensor data header
    private static final String INSTRUCTION_MESSAGE_PATH = "/instruction";              // instruction data header

    /* debug variables */
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);       // ensure screen (and app) stays on

        vloading = findViewById(R.id.loadingLayout);
        vmain = findViewById(R.id.sensorModuleLayout);

        vloading.setVisibility(View.VISIBLE);
        vmain.setVisibility(View.GONE);

        // initiate comm protocol with watch
        initGoogleApiClient();
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

        // get Intent from MainActivity
        intent = getIntent();

        // set up UI elements
        lstatus = (TextView) findViewById(R.id.load_status);
        loader = (ProgressBar) findViewById(R.id.progressBar);
        lhint = (TextView)findViewById(R.id.tvHint);
        lButton = (Button)findViewById(R.id.load_button);
        ivInstruction = (ImageView) findViewById(R.id.instruction);
        lButton.setOnClickListener(this);
        bNext = (Button)findViewById(R.id.sm_button);
        bNext.setOnClickListener(this);

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
        } catch (IOException e){
            e.printStackTrace();
        }

        // setup UI text elements
        status=(TextView)findViewById(R.id.tvStatus);
        sendMessage(START_ACTIVITY, "");
        Log.i(TAG, "calling wear");
        lstatus.setText("Searching for wear...\nPlease wait");
        lhint.setText("Hint: try waking up the watch by tapping on it");
    }

    /*  onClick
     *  Input:  View view
     *  Output: void
     *
     *  Called when the user interacts with the UI. onClick is used to determine what to do.
     */
    @Override
    public void onClick(View view) {
        switch(view.getId())
        {
            case R.id.load_button:
                sendMessage(START_ACTIVITY, "");
                Log.i(TAG, "calling wear manually");
                break;
            case R.id.sm_button:
                if(step == 0){
                    startRecording();
                }
                else if(step == NUM_STEPS){
                    sendMessage(INSTRUCTION_MESSAGE_PATH, "flush");
                }
                else {
                    getNextInstruction();
                }
                break;
            default:
                break;
        }
    }

    private void startRecording(){
        sendMessage(WEAR_MESSAGE_PATH, "START");
        getNextInstruction();
        started = true;
    }

    private void stopRecording(){
        sendMessage(WEAR_MESSAGE_PATH, "STOP");
        // stop recording. flush buffer and save file.
        try{
            writer.flush();
            fwriter.flush();
            writer.close();
            fwriter.close();
        } catch (IOException e) {
            Log.i(TAG, "I/O issue at flush and close");
            e.printStackTrace();
        }
        started = false;
        intent.putExtra("location", sensorFiles.toString());
        setResult(RESULT_OK,intent);
        finish();
    }

    /*  setData
     *  Input:  byte[] message - sensor data from the watch in a byte array (buffered)
     *  Output: void
     *
     *  This function will take the sensor data from the watch, and save it to the open file.
     */
    public void setData(byte[] message) {
        /* setup local variables to translate the buffer message into useable information */
        char type = 'x';
        float x = 0, y = 0, z = 0;
        long time;
        ByteBuffer buffer;

        buffer = ByteBuffer.wrap(message);            // place the message in the buffer
        buffer.rewind();                              // rewind buffer to start from 0

        while(buffer.hasRemaining()) {                // message has form: time, type, x, y, z
            time = buffer.getLong();
            type = buffer.getChar();
            x = buffer.getFloat();
            y = buffer.getFloat();
            z = buffer.getFloat();

            if (started) {      // save data only if the recording has started
                try {
                    //writer.write(time + "," + x + "," + y + "," + z);
                    writer.write(time + "," + type + "," + x + "," + y + "," + z);
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*  getNextInstruction
     *  Input:  void
     *  Output: void
     *
     *  This function retrieves the next instruction in the sequence, displays the appropriate
     *  visual aid, and sends the correct next sequence to the watch.
     */
    public void getNextInstruction() {
        step++;
        String STEPNAME = "ERROR";
        String source = null;
        switch(step){
            case 1: STEPNAME = "Stir the cauldron";
               source = "xy_instruction";
                break;
            case 2: STEPNAME = "Paint the rainbow";
                source = "xz_instruction";
                break;
            case 3: STEPNAME = "Ninja chop";
                source = "yz_instruction";
                break;
            default:
                break;
        }
        status.setText("Step " + step + "of" + NUM_STEPS + "\n" + STEPNAME);
        Log.i(TAG, "Next step!");
        // setup the animations
        ivInstruction.setImageResource(getResources().getIdentifier(source,"drawable",getPackageName()));
        frameAnimation = (AnimationDrawable)ivInstruction.getDrawable();
        frameAnimation.setCallback(ivInstruction);
        frameAnimation.setVisible(true, true);
        frameAnimation.start();
    }

    /* settings/option menu commands */
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

    /** Comm protocols **/

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
                .addOnConnectionFailedListener(this)
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
    public void onConnected(Bundle connectionHint){
        // do something
    }

    /* onConnectionSuspended
     * Input:   int i
     * Output:  void
     *
     * Called when the connection between the phone and watch is suspended. UI changes to notify user.
     */
    @Override
    public void onConnectionSuspended(int i) {
        // do something
        status.setText("not connected to wear");
    }

    /* onConnectionFailed
     * Input:   ConnectionResult connectionResult
     * Output:  void
     *
     * Called when the connection between the phone and watch failed. UI changes to notify user.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // do something
        status.setText("not connected to wear");
    }

    /*  sendMessage
     *  Input:  String path - message path or header
     *          String message - message data
     *  Output: void
     *
     *  This function is called when a message needs to be sent. It requires the header information
     *  and the payload to send the data. Two different methods are present, depending on the type
     *  of data being sent. ByteBuffer is used for sensor data, String is used for all other data.
     */
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

    /*  onMessageReceived
     *  Input:  MessageEvent messageEvent - the message and its associated information
     *  Output: void
     *
     *  This function is called when it receives a message through the MessageApi. It determines
     *  the source of the message, and then extracts the payload to determine the correct function
     *  call.
     */
    @Override
    public void onMessageReceived( final MessageEvent messageEvent ) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final String msg = new String(messageEvent.getData());
                // if the message is sensor data, send it to be recorded
                if (messageEvent.getPath().equalsIgnoreCase(DATA_MESSAGE_PATH)) {
                    Log.i(TAG, "data rec'd: " + msg);
                    setData(messageEvent.getData());
                }
                // if it is connection data, set the label
                else if (messageEvent.getPath().equalsIgnoreCase(WEAR_MESSAGE_PATH)) {
                    vloading.setVisibility(View.GONE);
                    lButton.setEnabled(false);
                    lButton.setVisibility(View.GONE);
                    vmain.setVisibility(View.VISIBLE);
                    status.setText("Ready!");
                    Log.i(TAG, "connected to wear");
                    sendMessage(INSTRUCTION_MESSAGE_PATH, "READY");
                }
                else if (messageEvent.getPath().equalsIgnoreCase(INSTRUCTION_MESSAGE_PATH)){
                    // do something
                    if(msg.equalsIgnoreCase("START")){
                        Log.i(TAG, "message for recording start");
                        //startRecording();
                    }
                    else if(msg.equalsIgnoreCase("END")){
                        Log.i(TAG, "message for recording end");
                        stopRecording();
                    }
                }
            }
        });
    }

    // for sending notifications on the phone (not used)
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

    /* System commands */

    /*  Called when the state of the watch changes. On any suspended commands (pause, destroy,
     *  finish), the app will de-register all sensors and listeners to save battery. On any resume
     *  commands (resume), the app will re-register all the sensors and listeners.
     *
     */

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
}
