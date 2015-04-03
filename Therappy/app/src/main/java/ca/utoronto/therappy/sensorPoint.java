package ca.utoronto.therappy;

/**
 * Created by Andrew on 26/03/2015.
 */
public class sensorPoint implements Comparable<sensorPoint> {
    public static final int DATA_ROTATIONVEC = 2;
    public static final int DATA_ACCELERATION = 1;

    private final double time;
    private final double[] value;
    private final int datatype;

    public sensorPoint(double time, double[] val, int datatype) {
        this.time = time;
        this.value = val;
        this.datatype = datatype;
    }

    public double getTime() {
        return this.time;
    }

    public double[] getValue() {
        return this.value;
    }

    public int getDataType() {
        return this.datatype;
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
