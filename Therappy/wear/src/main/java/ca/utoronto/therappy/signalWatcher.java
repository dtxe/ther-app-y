package ca.utoronto.therappy;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by simeon on 2015-04-05.
 */

public class signalWatcher {

    private long lastTimestamp;               // last onSensorChanged timestamp
    private float[] position, velocity, acceleration;        // current position and velocity

    private int avgAccleration_ctr, longavgAccleration_ctr;
    private final static int avgAccleration_ctr_max = 20;       // TODO: verify the width of accl peak
    private final static int longavgAccleration_ctr_max = 500;       // TODO: verify the width of accl peak
    private float[][] avgAcceleration, longavgAcceleration;

    private double furthestPosition;            // keep track of furthest position to return (ie. target position)
    private float[] avgVelocity;

    private Timer positionTimer;                // integrate position every so often...
    private static final int positionTimerPeriod = 3;       // this is the so often...

    private static final String TAG = signalWatcher.class.getSimpleName();

    private int currentStatus;                  // keep track of what stage we're in (do things when returned to origin)
    private final static int BEGIN_AT_ORIGIN    = 0,
                             HAS_LEFT_ORIGIN    = 1,
                             HAS_HIT_TARGET     = 2,
                             HAS_LEFT_TARGET    = 3,
                             HAS_HIT_ORIGIN     = 4;

    private int counter;

    private final static float event_timediv = (float) 1E-9, timer_timediv = (float) 1E-3;      // convert time values into seconds


    public signalWatcher() {
        this.lastTimestamp = 0;

        this.position = new float[] {0, 0, 0};
        this.velocity = new float[] {0, 0, 0};
        this.acceleration = new float[] {0, 0, 0};
        this.avgVelocity = new float[] {0, 0, 0};

        this.avgAcceleration = new float[avgAccleration_ctr_max][];
        this.longavgAcceleration = new float[longavgAccleration_ctr_max][];

        this.furthestPosition = 0;

        this.positionTimer = new Timer();
        this.positionTimer.scheduleAtFixedRate(new MyTask(this, positionTimerPeriod), 0, positionTimerPeriod);

        this.currentStatus = BEGIN_AT_ORIGIN;

        this.counter = 0;
        this.avgAccleration_ctr = 0;
        this.longavgAccleration_ctr = 0;
    }

    // stop the position integration timer
    public void onDestroy() {
        this.positionTimer.cancel();
    }

    // assume the watch returns to a marked "origin"
    // reset accumulators to zero at the origin to deal with integration drift
    public void resetToZero() {
        this.position[0] = 0;
        this.position[1] = 0;
        this.position[2] = 0;

        this.velocity[0] = 0;
        this.velocity[1] = 0;
        this.velocity[2] = 0;

        this.acceleration[0] = 0;
        this.acceleration[1] = 0;
        this.acceleration[2] = 0;
    }

    public double getFurthestPosition() {
        return this.furthestPosition;
    }

    public void onSensorChanged(float[] acceleration, long eventTimestamp) {
        this.acceleration[0] = acceleration[0];
        this.acceleration[1] = acceleration[1];
        this.acceleration[2] = acceleration[2];

        this.avgAcceleration[this.avgAccleration_ctr] = this.acceleration;
        this.avgAccleration_ctr++;
        if(this.avgAccleration_ctr == avgAccleration_ctr_max) {
            this.avgAccleration_ctr = 0;
        }

        this.longavgAcceleration[this.longavgAccleration_ctr] = this.acceleration;
        this.longavgAccleration_ctr++;
        if(this.longavgAccleration_ctr == longavgAccleration_ctr_max){
            this.longavgAccleration_ctr = 0;
        }

        // update event timestamp
        this.lastTimestamp = eventTimestamp;
    }

    public float[] getPosition() {
        return this.position;
    }

    public float[] getVelocity() {
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

        // STEP: take mean of accl accumulator
        float[] avgAccl = new float[]{0, 0, 0};
        for(int kk = 0; kk < avgAccleration_ctr_max; kk++) {
            avgAccl[0] += avgAcceleration[kk][0];
            avgAccl[1] += avgAcceleration[kk][1];
            avgAccl[2] += avgAcceleration[kk][2];
        }
        avgAccl[0] = avgAccl[0] / avgAccleration_ctr_max;
        avgAccl[1] = avgAccl[1] / avgAccleration_ctr_max;
        avgAccl[2] = avgAccl[2] / avgAccleration_ctr_max;

        float[] longavgAccl = new float[]{0, 0, 0};
        for(int kk = 0; kk < longavgAccleration_ctr_max; kk++){
            longavgAccl[0] += longavgAcceleration[kk][0];
            longavgAccl[1] += longavgAcceleration[kk][1];
            longavgAccl[2] += longavgAcceleration[kk][2];
        }
        longavgAccl[0] = longavgAccl[0] / longavgAccleration_ctr_max;
        longavgAccl[1] = longavgAccl[1] / longavgAccleration_ctr_max;
        longavgAccl[2] = longavgAccl[2] / longavgAccleration_ctr_max;

        avgAccl[0] = avgAccl[0] - longavgAccl[0];
        avgAccl[1] = avgAccl[1] - longavgAccl[1];
        avgAccl[2] = avgAccl[2] - longavgAccl[2];

        //thresholdByValue(avgAccl, (float) 0.1);

        // STEP: integrate acceleration and velocity
        this.velocity[0] = this.velocity[0] + (avgAccl[0] * interval * timer_timediv);
        this.velocity[1] = this.velocity[1] + (avgAccl[1] * interval * timer_timediv);
        this.velocity[2] = this.velocity[2] + (avgAccl[2] * interval * timer_timediv);

        this.position[0] = this.position[0] + (this.velocity[0] * interval * timer_timediv);
        this.position[1] = this.position[1] + (this.velocity[1] * interval * timer_timediv);
        this.position[2] = this.position[2] + (this.velocity[2] * interval * timer_timediv);

        // keep track of rolling average
        this.avgVelocity[0] = (float)0.99*this.avgVelocity[0] + (float)0.01*this.velocity[0];
        this.avgVelocity[1] = (float)0.99*this.avgVelocity[1] + (float)0.01*this.velocity[1];
        this.avgVelocity[2] = (float)0.99*this.avgVelocity[2] + (float)0.01*this.velocity[2];

        // update furthest position
        float absposition = vectornorm(this.position);
        this.furthestPosition = absposition > this.furthestPosition ? absposition : this.furthestPosition;

        float absvelocity = vectornorm(this.velocity);
        float absavgvelocity = vectornorm(this.avgVelocity);

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
            vector[0] = vector[0] > -threshold ? 0 : vector[0];
        if(vector[1] > 0)
            vector[1] = vector[1] < threshold ? 0 : vector[1];
        else
            vector[1] = vector[1] > -threshold ? 0 : vector[1];
        if(vector[0] > 0)
            vector[2] = vector[2] < threshold ? 0 : vector[2];
        else
            vector[2] = vector[2] > -threshold ? 0 : vector[2];
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
