package ca.utoronto.therappy;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by simeon on 2015-04-05.
 */

public class signalWatcher {

    private double lastTimestamp;
    private double[] position, velocity;

    private double furthestPosition;

    private Timer positionTimer;
    private static final int positionTimerPeriod = 5;

    private boolean leftOrigin, hitTarget, backToOrigin;
    private static final String TAG = signalWatcher.class.getSimpleName();



    public signalWatcher() {
        lastTimestamp = 0;
        position = new double[] {0, 0, 0};
        velocity = new double[] {0, 0, 0};

        positionTimer = new Timer();
        positionTimer.scheduleAtFixedRate(new MyTask(this, positionTimerPeriod), 0, positionTimerPeriod);
    }

    public void onDestroy() {
        positionTimer.cancel();
    }

    public void resetToZero() {
        position[0] = 0;
        position[1] = 0;
        position[2] = 0;

        velocity[0] = 0;
        velocity[1] = 0;
        velocity[2] = 0;
    }

    public double getFurthestPosition() {
        return this.furthestPosition;
    }

    public void onSensorChanged(float[] acceleration, double eventTimestamp) {
        this.velocity[0] += acceleration[0] * (eventTimestamp - this.lastTimestamp) * 10E-9;
        this.velocity[1] += acceleration[1] * (eventTimestamp - this.lastTimestamp) * 10E-9;
        this.velocity[2] += acceleration[2] * (eventTimestamp - this.lastTimestamp) * 10E-9;

        this.lastTimestamp = eventTimestamp;
    }

    public double[] getPosition() {
        return this.position;
    }

    public double[] getVelocity() {
        return this.velocity;
    }

    public boolean isBackToOrigin() {
        return backToOrigin;
    }

    public void onPositionTimerTick(double interval) {
        this.position[0] += this.velocity[0] * interval;
        this.position[1] += this.velocity[1] * interval;
        this.position[2] += this.velocity[2] * interval;

        // update furthest position
        double absposition = Math.sqrt(Math.pow(this.position[0], 2) + Math.pow(this.position[1], 2) + Math.pow(this.position[2], 2));
        this.furthestPosition = absposition > this.furthestPosition ? absposition : this.furthestPosition;

        double absvelocity = Math.sqrt(Math.pow(this.velocity[0], 2) + Math.pow(this.velocity[1], 2) + Math.pow(this.velocity[2], 2));

        // check status
        if(!leftOrigin && absvelocity > 0.5) {
            leftOrigin = true;
            Log.i(TAG, "left origin");
        }
        if(leftOrigin && !hitTarget && absvelocity < 0.5) {     // TODO: these need to be tweaked
            Log.i(TAG, "hit target");
            hitTarget = true;
        }
        if(leftOrigin && hitTarget && !backToOrigin && absposition < 0.03) {
            Log.i(TAG, "backToOrigin");
            backToOrigin = true;
        }
    }

    private class MyTask extends TimerTask {

        signalWatcher theclass;
        double interval;

        public MyTask(signalWatcher theclass, double interval) {
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
