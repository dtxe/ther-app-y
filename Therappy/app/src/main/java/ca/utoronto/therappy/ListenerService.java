package ca.utoronto.therappy;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Andrew on 28/02/2015.
 */
public class ListenerService extends WearableListenerService {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DATA_MESSAGE_PATH = "/sensordata";

    @Override
    public void onMessageReceived(MessageEvent messageEvent ) {
        super.onMessageReceived(messageEvent);
        final String msg = new String(messageEvent.getData());
        Log.i(TAG, "msg received: " + msg);
        if( messageEvent.getPath().equalsIgnoreCase(DATA_MESSAGE_PATH) ) {
            setData(messageEvent.getData());
            sendBroadcastMessage();
        }

    }
    // Send an Intent with an action named "custom-event-name". The Intent sent should
// be received by the ReceiverActivity.
    private void sendBroadcastMessage() {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent("custom-event-name");
        // You can also include some extra data.
        intent.putExtra("message", "This is my message!");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void setData(byte[] message){

    }

}
