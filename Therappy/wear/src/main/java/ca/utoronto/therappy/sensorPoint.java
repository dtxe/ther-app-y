package ca.utoronto.therappy;

/**
 * Created by Andrew on 26/03/2015.
 */
public class sensorPoint implements Comparable<sensorPoint> {
    public static final int DATA_ROTATIONVEC = 2;
    public static final int DATA_ACCELERATION = 1;

    private final long time;
    private final float[] value;
    private final int datatype;

    public sensorPoint(long time, float[] val, int datatype) {
        this.time = time;
        this.value = val;
        this.datatype = datatype;
    }

    public long getTime() {
        return this.time;
    }

    public float[] getValue() {
        return this.value;
    }

    public int getDataType() {
        return this.datatype;
    }

    // allows for sorting of sensorPoint data using native libraries.
    @Override
    public int compareTo(sensorPoint p) {
        long ptime = p.getTime();
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
