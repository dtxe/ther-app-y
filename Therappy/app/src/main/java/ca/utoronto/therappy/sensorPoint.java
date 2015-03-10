package ca.utoronto.therappy;

/**
 * Created by Andrew on 09/03/2015.
 */
public class sensorPoint {
    public Long time;
    public char type;
    public float x, y, z;

    public sensorPoint(String id){
        String sensorData[] = id.split(",");
        try {
            this.time = Long.parseLong(sensorData[0]);
            this.type = sensorData[1].charAt(0);
            this.x = Float.parseFloat(sensorData[2]);
            this.y = Float.parseFloat(sensorData[3]);
            this.z = Float.parseFloat(sensorData[4]);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public sensorPoint(Long time, char type, float x, float y, float z){
        this.time = time;
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Long getTime(){
        return this.time;
    }

    public char getType(){
        return this.type;
    }

    public float getX(){
        return this.x;
    }

    public float getY(){
        return this.y;
    }

    public float getZ(){
        return this.z;
    }

    public String toString(){
        return time + "," + type + "," + x + "," + y + "," + z;
    }
}
