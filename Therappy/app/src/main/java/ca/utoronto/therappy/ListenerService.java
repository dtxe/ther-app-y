package ca.utoronto.therappy;

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
        }
    }

    private void setData(byte[] message){

    }

}
