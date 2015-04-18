package ca.utoronto.therappy;

/**
 * Created by Andrew on 26/03/2015.
 */
public class sensorPoint implements Comparable<sensorPoint> {
    public static final int DATA_ROTATIONVEC = 2;
    public static final int DATA_ACCELERATION = 1;
    public static final int DATA_UNSPECIFIED = 0;

    public final long time;
    public final float[] value;
    public final int datatype;

    public sensorPoint(long time, float[] val) {
        this(time, val, DATA_UNSPECIFIED);
    }

    public sensorPoint(long time, float[] val, int datatype) {
        this.time = time;
        this.value = val;
        this.datatype = datatype;
    }

    // allows for sorting of sensorPoint data using native libraries.
    @Override
    public int compareTo(sensorPoint p) {
        long ptime = p.time;
        if(this.time > ptime){
            return 1;
        } else if (this.time == ptime){
            return 0;
        } else if(this.time < ptime){
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        String output = "" + this.time;

        for(int kk = 0; kk < this.value.length; kk++) {
            output += "," + this.value[kk];
        }

        return output;
    }

}
