package ca.utoronto.therappy;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by simeon on 2015-04-05.
 */

public class signalWatcher {

    private double lastTimestamp;               // last onSensorChanged timestamp
    private double[] position, velocity;        // current position and velocity

    private double furthestPosition;            // keep track of furthest position to return (ie. target position)
    private double[] avgVelocity;

    private Timer positionTimer;                // integrate position every so often...
    private static final int positionTimerPeriod = 5;       // this is the so often...

    private int currentStatus;                  // keep track of what stage we're in (do things when returned to origin)
    private final static int BEGIN_AT_ORIGIN    = 0,
                             HAS_LEFT_ORIGIN    = 1,
                             HAS_HIT_TARGET     = 2,
                             HAS_LEFT_TARGET    = 3,
                             HAS_HIT_ORIGIN     = 4;

    public signalWatcher() {
        lastTimestamp = 0;
        position = new double[] {0, 0, 0};
        velocity = new double[] {0, 0, 0};

        furthestPosition = 0;
        avgVelocity = new double[] {0, 0, 0};

        positionTimer = new Timer();
        positionTimer.scheduleAtFixedRate(new MyTask(this, positionTimerPeriod), 0, positionTimerPeriod);

        currentStatus = 0;
    }

    // stop the position integration timer
    public void onDestroy() {
        positionTimer.cancel();
    }

    // assume the watch returns to a marked "origin"
    // reset accumulators to zero at the origin to deal with integration drift
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
        // update velocity accumulator
        this.velocity[0] += acceleration[0] * (eventTimestamp - this.lastTimestamp) * 10E-9;
        this.velocity[1] += acceleration[1] * (eventTimestamp - this.lastTimestamp) * 10E-9;
        this.velocity[2] += acceleration[2] * (eventTimestamp - this.lastTimestamp) * 10E-9;

        // update event timestamp
        this.lastTimestamp = eventTimestamp;

        // keep track of rolling average
        this.avgVelocity[0] = 0.9*this.avgVelocity[0] + 0.1*this.velocity[0];
        this.avgVelocity[1] = 0.9*this.avgVelocity[1] + 0.1*this.velocity[1];
        this.avgVelocity[2] = 0.9*this.avgVelocity[2] + 0.1*this.velocity[2];
    }

    public double[] getPosition() {
        return this.position;
    }

    public double[] getVelocity() {
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
    public void onPositionTimerTick(double interval) {
        this.position[0] += this.velocity[0] * interval;
        this.position[1] += this.velocity[1] * interval;
        this.position[2] += this.velocity[2] * interval;

        // update furthest position
        double absposition = vectornorm(this.position);
        this.furthestPosition = absposition > this.furthestPosition ? absposition : this.furthestPosition;

        double absvelocity = vectornorm(this.velocity);
        double absavgvelocity = vectornorm(this.avgVelocity);

        // check status
        if(this.currentStatus == BEGIN_AT_ORIGIN && absvelocity > 2) {
            this.currentStatus = HAS_LEFT_ORIGIN;
        } else if(this.currentStatus == HAS_LEFT_ORIGIN && absavgvelocity < 0.4) {     // TODO: these need to be tweaked
            this.currentStatus = HAS_HIT_TARGET;
        } else if(this.currentStatus == HAS_HIT_TARGET && absvelocity > 2) {
            this.currentStatus = HAS_LEFT_TARGET;
        } else if(this.currentStatus == HAS_LEFT_TARGET && absavgvelocity < 0.4) {
            this.currentStatus = HAS_HIT_ORIGIN;        // yay we're done!
        }
    }

    // get the magnitude of the vector in 3d space.
    protected double vectornorm(double[] vector) {
        return Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2) + Math.pow(vector[2], 2));
    }

    // this is a TimerTask for whenever
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
