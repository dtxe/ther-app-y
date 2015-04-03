package ca.utoronto.therappy;

/**
 * Created by Andrew on 26/03/2015.
 */
public class sensorPoint implements Comparable<sensorPoint> {
    private final double time;
    private final double[] value;

    public sensorPoint(double time, double[] val) {
        this.time = time;
        this.value = val;
    }

    public double getTime() {
        return this.time;
    }

    public double[] getValue() {
        return this.value;
    }

    @Override
    public int compareTo(sensorPoint p) {
        return (int) (this.time - p.time);
    }

    @Override
    public String toString() {
        return time + "," + value.toString();
    }

}
