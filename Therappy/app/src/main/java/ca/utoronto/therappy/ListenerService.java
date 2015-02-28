package ca.utoronto.therappy;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Andrew on 28/02/2015.
 */
public class ListenerService extends WearableListenerService {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    public void onMessageReceived(MessageEvent messageEvent ) {
        final String msg = new String(messageEvent.getData());
        Log.i(TAG, "msg received: " + msg);
    }

}
