package ca.utoronto.therappy;

import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.CountDownTimer;
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
    private Button lButton, bNext;                              // UI buttons
    private ImageView ivInstruction;
    private TextView status, lstatus, lhint, tapNext;           // UI title
    private RelativeLayout layout;                              // UI layout
    private Intent intent;
    private View vloading, vmain;
    private ProgressBar loader;
    private AnimationDrawable frameAnimation;

    /* recording variables */
    private int started = 0;                                    // whether or not the app is recording data or not. 1 = calibration, 2 = recording
    private int step = 0, stage = 0;                            // current step number
    private int NUM_STEPS = 5, NUM_STAGE = 3;                   // number of steps
    private long time = System.currentTimeMillis();             // timestamp for the long

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
        lstatus = (TextView)findViewById(R.id.load_status);
        loader = (ProgressBar)findViewById(R.id.progressBar);
        lhint = (TextView)findViewById(R.id.tvHint);
        lButton = (Button)findViewById(R.id.load_button);
        ivInstruction = (ImageView)findViewById(R.id.instruction);
        lButton.setOnClickListener(this);
        bNext = (Button)findViewById(R.id.sm_button);
        bNext.setOnClickListener(this);
        tapNext = (TextView)findViewById(R.id.nextText);

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
                if(stage == 0){
                    countdown();
                    startCalibrating();
                }
                else if(stage > NUM_STAGE){
                    sendMessage(INSTRUCTION_MESSAGE_PATH, "LASTFLUSH");
                }
                else {
                    sendMessage(INSTRUCTION_MESSAGE_PATH, "FLUSH");
                }
                break;
            default:
                break;
        }
    }

    private void countdown(){
        vloading.setVisibility(View.VISIBLE);
        vmain.setVisibility(View.GONE);
        bNext.setEnabled(false);
        lhint.setText("Please wait!");
        new CountDownTimer(5000, 1000) {

            public void onTick(long millisUntilFinished) {
                lstatus.setText("Hold steady...\n"+ (millisUntilFinished/1000));
            }

            public void onFinish() {
                sendMessage(INSTRUCTION_MESSAGE_PATH, "CALIBFLUSH");
                Log.i(TAG,"finished countdown");
            }
        }.start();
    }

    private void startCalibrating(){
        sendMessage(WEAR_MESSAGE_PATH, "START");
        started = 1;
    }

    private void startRecording(){
        stage++;
        vloading.setVisibility(View.GONE);
        vmain.setVisibility(View.VISIBLE);
        bNext.setEnabled(true);
        sendMessage(WEAR_MESSAGE_PATH, "START");
        getNextInstruction();
        started = 2;
    }

    private void stopRecording(){
        // stop recording. flush buffer and save file.
        Log.i(TAG, "Finished recording. Saving files now");
        sendMessage(WEAR_MESSAGE_PATH, "END");
        try{
            writer.close();
            fwriter.close();
        } catch (IOException e) {
            Log.i(TAG, "I/O issue at flush and close");
            e.printStackTrace();
        }
        started = 0;
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

            try {
                writer.write(time + "," + type + "," + x + "," + y + "," + z);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
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
    private void getNextInstruction() {
        try {
            writer.write("0,N,0,0,0");
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // set up the stage/step
        step++;
        String source = "stage" + stage + "step" + step;
        status.setText("Stage " + stage + " of " + NUM_STAGE + "\nStep " + step + " of " + NUM_STEPS);
        tapNext.setText("Tap screen to continue...");
        Log.i(TAG, "Next step!");

        // setup the animations
        ivInstruction.setImageResource(getResources().getIdentifier(source,"drawable",getPackageName()));
        frameAnimation = (AnimationDrawable)ivInstruction.getDrawable();
        frameAnimation.setCallback(ivInstruction);
        frameAnimation.setVisible(true, true);
        frameAnimation.start();
        if(step == NUM_STEPS){
            stage++;
            step = 0;
            switch(stage){
                case 1: NUM_STEPS = 5;
                    break;
                case 2: NUM_STEPS = 2;
                    break;
                case 3: NUM_STEPS = 2;
                    break;
                default: NUM_STEPS = 0;
                    break;
            }
        }
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
                final String path = messageEvent.getPath();
                // if the message is sensor data, send it to be recorded
                if (messageEvent.getPath().equalsIgnoreCase(DATA_MESSAGE_PATH)) {
                    if(started == 2)
                        setData(messageEvent.getData());
                }
                // if it is connection data, set the label
                else if (path.equalsIgnoreCase(WEAR_MESSAGE_PATH)) {
                    vloading.setVisibility(View.GONE);
                    lButton.setEnabled(false);
                    lButton.setVisibility(View.GONE);
                    vmain.setVisibility(View.VISIBLE);
                    status.setText("Ready!");
                    Log.i(TAG, "connected to wear");
                    sendMessage(INSTRUCTION_MESSAGE_PATH, "READY");
                    tapNext.setText("Tap to start");
                }
                else if (path.equalsIgnoreCase(INSTRUCTION_MESSAGE_PATH)){
                    if(msg.equalsIgnoreCase("START")){
                        Log.i(TAG, "message for recording start");
                    }
                    else if(msg.equalsIgnoreCase("END")){
                        Log.i(TAG, "message for recording end");
                        stopRecording();
                    }
                    else if(msg.equalsIgnoreCase("FLUSHED")){
                        getNextInstruction();
                    }
                    else if (msg.equalsIgnoreCase("CALIBRATED")){
                        startRecording();
                    }
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
