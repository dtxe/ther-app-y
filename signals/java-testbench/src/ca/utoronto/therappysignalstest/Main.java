package ca.utoronto.therappysignalstest;

import java.io.*;
import java.util.ArrayList;


public class Main {
    private final static String sep = File.separator;

    public static void main(String[] args) {
        // ** PARAMETERS **
        String filePrefix = ".." + sep + "assets2" + sep + "therappy";

        // ** LESS USEFUL PARAMETERS **
        int bufferSize = 2048;    // size of write buffer


        // Variables
        String nextLine;
        long time = 0, t0 = 0;
        float x = 0, y = 0, z = 0;
        boolean open = false;

        ArrayList<sensorPoint> data_accl = new ArrayList<>();

        double[][] results;


        // ********************************************
        // construct paths
        String currentDir = System.getProperty("user.dir");
        String outputFile = currentDir+filePrefix,
                inputFile = currentDir+filePrefix;


        try {

            FileWriter fwriter = new FileWriter(outputFile, true);
            BufferedWriter bwriter = new BufferedWriter(fwriter, bufferSize);

            FileReader freader = new FileReader(inputFile);
            BufferedReader breader = new BufferedReader(freader, bufferSize);

            // # Main Reading Loop
            while((nextLine = breader.readLine()) != null) {
                String sensorData[] = nextLine.split(",");
                try {
                    time = Long.parseLong(sensorData[0]);
                    x = Float.parseFloat(sensorData[2]);
                    y = Float.parseFloat(sensorData[3]);
                    z = Float.parseFloat(sensorData[4]);
                } catch (Exception e){
                    e.printStackTrace();
                }

                // if this is the first line read, set initial time
                if(!open){
                    t0 = time;
                    open = true;
                }

                // parse sensor type then add to corresponding arrayList
                if(sensorData[1].compareTo("a") == 0) {
                    data_accl.add(new sensorPoint(time-t0, new float[]{x, y, z}, sensorPoint.DATA_ACCELERATION));
                } else if(sensorData[1].compareTo("N") == 0) {
                    data_accl.add(new sensorPoint(time-t0, new float[]{0, 0, 0}, sensorPoint.TRACE_BREAK));
                }
            }

            // # Calculate Data
            SPM_FunctionalWorkspace sigProcInstance = new SPM_FunctionalWorkspace(data_accl);
            results = sigProcInstance.doChurnData();


            // # Save results
            int len = results[0].length;
            for (int i = 0; i < len; i++) {
                bwriter.write(results[0][i] + "," + results[1][i] + "," + results[2][i]);
                bwriter.newLine();
            }


            // # Clean up
            bwriter.flush();
            bwriter.close();
            fwriter.flush();
            fwriter.close();

            breader.close();
            freader.close();

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
