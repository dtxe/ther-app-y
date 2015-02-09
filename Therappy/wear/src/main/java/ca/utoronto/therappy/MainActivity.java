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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.google.android.gms.wearable.MessageEvent;


public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mLinAccelerometer, mGyroscope;
    private LinearLayout mRectBackground;
    private RelativeLayout mRoundBackground;
    private TextView ax, ay, az, rx, ry, rz;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rect_activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLinAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        /*
        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mRectBackground = (LinearLayout) findViewById(R.id.rect_layout);
                mRoundBackground = (RelativeLayout) findViewById(R.id.round_layout);

                ax = (TextView) stub.findViewById(R.id.ax);
                ay = (TextView) stub.findViewById(R.id.ay);
                az = (TextView) stub.findViewById(R.id.az);
            }
        });*/
        ax = (TextView)findViewById(R.id.ax);
        ay = (TextView)findViewById(R.id.ay);
        az = (TextView)findViewById(R.id.az);
        rx = (TextView)findViewById(R.id.rx);
        ry = (TextView)findViewById(R.id.ry);
        rz = (TextView)findViewById(R.id.rz);
    }

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

        switch (event.sensor.getType())
        {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                //display values using TextView
                ax.setText("X axis" + "\t\t" + x);
                ay.setText("Y axis" + "\t\t" + y);
                az.setText("Z axis" + "\t\t" + z);
                break;
            case Sensor.TYPE_GYROSCOPE:
                rx.setText("X axis" + "\t\t" + x);
                ry.setText("Y axis" + "\t\t" + y);
                rz.setText("Z axis" + "\t\t" + z);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mLinAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }
/*
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/startactivity")) {

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }*/
}