package ca.utoronto.therappy;

import android.util.Log;

import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;

/**
 * Created by simeon on 2015-04-05.
 */

public class signalWatcher {

    // TODO: new algorithm thing.
    /* new algorithm:
        - put the dump of acceleration into a buffer
        - every time [ ] msec of information is available, process.
     */

    private long lastTimestamp;               // last time integration step has run
    private sensorPoint position, velocity;

    private double furthestPosition;            // keep track of furthest position to return (ie. target position)

    private ArrayList<sensorPoint> bufferAccl, bufferReAccl, bufferVel;
    private static final long bAccl_short_len = (long) 0.3E9, bAccl_long_len = (long) 5E9, bVel_len = (long) 0.5E9;

    private static final int interpLength = 90;

    private Timer positionTimer;                            // integrate position every so often...
    private static final int positionTimerPeriod = 3;       // this is the so often...

    private static final String TAG = signalWatcher.class.getSimpleName();

    private int currentStatus;                  // keep track of what stage we're in (do things when returned to origin)
    private final static int BEGIN_AT_ORIGIN    = 0,
                             HAS_LEFT_ORIGIN    = 1,
                             HAS_HIT_TARGET     = 2,
                             HAS_LEFT_TARGET    = 3,
                             HAS_HIT_ORIGIN     = 4;

    private int counter;        // for debugging

    private final static float event_timediv = (float) 1E-9, timer_timediv = (float) 1E-3;      // convert time values into seconds


    public signalWatcher() {
        this.lastTimestamp = 0;

        this.position = new sensorPoint(0, new float[] {0, 0, 0});
        this.velocity = new sensorPoint(0, new float[] {0, 0, 0});

        this.bufferAccl = new ArrayList<>((int) (bAccl_short_len * 1E-9 * 50)); // time x (in seconds) x 50 samples/sec = samples / time
        this.bufferVel = new ArrayList<>((int) (bVel_len * 1E-9 * 50)); // time x (in seconds) x 50 samples/sec = samples / time

        this.furthestPosition = 0;

        this.positionTimer = new Timer();
        this.positionTimer.scheduleAtFixedRate(new MyTask(this, positionTimerPeriod), 0, positionTimerPeriod);

        this.currentStatus = BEGIN_AT_ORIGIN;

        this.counter = 0;
    }

    // stop the position integration timer
    public void onDestroy() {
        this.positionTimer.cancel();
    }

    // assume the watch returns to a marked "origin"
    // reset accumulators to zero at the origin to deal with integration drift
    public void resetToZero() {
        this.position = new sensorPoint(0, new float[] {0, 0, 0});
        this.velocity = new sensorPoint(0, new float[] {0, 0, 0});

        this.bufferAccl.clear();
        this.bufferVel.clear();
    }

    public double getFurthestPosition() {
        return this.furthestPosition;
    }

    public void onSensorChanged(float[] acceleration, long eventTimestamp) {
        this.bufferAccl.add(new sensorPoint(eventTimestamp, acceleration));
    }

    public sensorPoint getPosition() {
        return this.position;
    }

    public sensorPoint getVelocity() {
        return this.velocity;
    }

    // watch onSensorChanged, or some other interval-ed thing can poll this for whether one full "tap" has been completed
    public boolean isBackToOrigin() {
        return this.currentStatus == HAS_HIT_ORIGIN;
    }

    public int getCurrentStatus() {
        return this.currentStatus;
    }

    // every so often... integrate the velocity to update position vector
    // check if a full "tap" has been completed
    public void onPositionTimerTick(float interval) {
        // TODO: may have to create copy of buffer to prevent race condition

        int idx_begin = 0,          // ArrayList index of values within the current time interval that we care about
            idx_accl_short = 0,     // ArrayList index+1 for last accl value within the short-time average (ie. starting index for values NOT within st avg)
            idx_accl_long = 0;      // ArrayList index+1 for last accl value within the long-term average
        long temp_time,             // retrieved time value from previous sensor point
             temp_timediff;
        sensorPoint temp_sp;

        float[] accl_avg_short = new float[] {0, 0, 0},
                accl_avg_long  = new float[] {0, 0, 0};

        long currentTimestamp = this.lastTimestamp + bAccl_short_len;

        // ensure sorted
        Collections.sort(this.bufferAccl);

        // create time vector
        long[] resampleTime = new long[interpLength];
        final long timestep = bAccl_short_len / interpLength;

        for(int kk = 0; kk < interpLength; kk++) {
            resampleTime[kk] = this.lastTimestamp + (kk * timestep);
        }

        // loop through time vector, find nearest points, and interpolate results
        sensorPoint[] resampleData = new sensorPoint[interpLength];
        int bufferCounter = this.bufferAccl.size()-1;

        for(int kk = 0; kk < interpLength; kk++) {
            int resampleCounter = interpLength - kk - 1;

            // decrement bufferCounter until we find the surrounding points
            while(this.bufferAccl.get(bufferCounter).time > resampleTime[resampleCounter]) {
                bufferCounter--;
            }

            // so now bufferCounter+1 is the larger value, bufferCounter is the smaller.

            sensorPoint smallerVals = this.bufferAccl.get(bufferCounter),
                          largerVals  = this.bufferAccl.get(bufferCounter+1);

            // fit coefficients
            float[] A = new float[3];
            A[0] = (largerVals.value[0] - smallerVals.value[0]) / (largerVals.time - smallerVals.time);
            A[1] = (largerVals.value[1] - smallerVals.value[1]) / (largerVals.time - smallerVals.time);
            A[2] = (largerVals.value[2] - smallerVals.value[2]) / (largerVals.time - smallerVals.time);

            // get fit data
            float[] newpoints = new float[3];
            newpoints[0] = A[0] * (resampleTime[resampleCounter] - smallerVals.time) + smallerVals.value[0];
            newpoints[1] = A[1] * (resampleTime[resampleCounter] - smallerVals.time) + smallerVals.value[1];
            newpoints[2] = A[2] * (resampleTime[resampleCounter] - smallerVals.time) + smallerVals.value[2];

            // create sensorPoint
            resampleData[resampleCounter] = new sensorPoint(resampleTime[resampleCounter], newpoints);
        }




        this.lastTimestamp = currentTimestamp;

        // check status
        if(this.currentStatus == BEGIN_AT_ORIGIN && absvelocity > 2) {
            this.currentStatus = HAS_LEFT_ORIGIN;
            Log.i(TAG, "left origin");
        } else if(this.currentStatus == HAS_LEFT_ORIGIN && absavgvelocity < 0.5) {     // TODO: these need to be tweaked
            this.currentStatus = HAS_HIT_TARGET;
            Log.i(TAG, "hit target");
        } else if(this.currentStatus == HAS_HIT_TARGET && absvelocity > 2) {
            this.currentStatus = HAS_LEFT_TARGET;
            Log.i(TAG, "left target");
        } else if(this.currentStatus == HAS_LEFT_TARGET && absavgvelocity < 0.4) {
            this.currentStatus = HAS_HIT_ORIGIN;        // yay we're done!
            Log.i(TAG, "backToOrigin");
        }

        if(this.counter == 100) {
            Log.i(TAG, "avgaccl: " + avgAccl[0] + ", " + avgAccl[1] + ", " + avgAccl[2]);
            Log.i(TAG, "avgvelocity: " + this.avgVelocity[0] + ", " + this.avgVelocity[1] + ", " + this.avgVelocity[2]);
            Log.i(TAG, "position: " + this.position[0] + ", " + this.position[1] + ", " + this.position[2]);

            this.counter = 0;
        }
        this.counter++;
    }

    protected void thresholdByValue(float[] vector, float threshold) {
        if(vector[0] > 0)
            vector[0] = vector[0] < threshold ? 0 : vector[0];
        else
            vector[0] = vector[0] > -1.0f * threshold ? 0 : vector[0];
        if(vector[1] > 0)
            vector[1] = vector[1] < threshold ? 0 : vector[1];
        else
            vector[1] = vector[1] > -1.0f * threshold ? 0 : vector[1];
        if(vector[2] > 0)
            vector[2] = vector[2] < threshold ? 0 : vector[2];
        else
            vector[2] = vector[2] > -1.0f * threshold ? 0 : vector[2];
    }

    // get the magnitude of the vector in 3d space.
    protected float vectornorm(float[] vector) {
        return (float) Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2) + Math.pow(vector[2], 2));
    }

    // this is a TimerTask for whenever
    private class MyTask extends TimerTask {

        signalWatcher theclass;
        long interval;

        public MyTask(signalWatcher theclass, long interval) {
            this.theclass = theclass;
            this.interval = interval;
        }

        @Override
        public void run() {
            try {
                theclass.onPositionTimerTick(interval);
            } catch (Throwable t) {
                // RAWR
            }
        }
    }
}
