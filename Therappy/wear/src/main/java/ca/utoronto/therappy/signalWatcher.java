package ca.utoronto.therappy;

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

    public void onSensorChanged(double[] acceleration, double eventTimestamp) {
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

    public void onPositionTimerTick(double interval) {
        this.position[0] += this.velocity[0] * interval;
        this.position[1] += this.velocity[1] * interval;
        this.position[2] += this.velocity[2] * interval;
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
