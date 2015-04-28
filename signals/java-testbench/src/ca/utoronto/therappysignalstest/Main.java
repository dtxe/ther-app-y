package ca.utoronto.therappysignalstest;

import java.io.*;
import java.util.ArrayList;


public class Main {
    private final static String sep = File.separator;

    public static void main(String[] args) throws IOException {
        // ** PARAMETERS **
        String filePrefix = sep + ".." + sep + "assets2" + sep,
               filename;

        // ** LESS USEFUL PARAMETERS **
        int bufferSize = 2048;    // size of write buffer


        // Variables
        String nextLine;
        long time = 0, t_last = 0, t0 = 0;
        float x = 0, y = 0, z = 0;
        boolean open = false;

        ArrayList<sensorPoint> data_accl = new ArrayList<>();

        ArrayList<double[]> results;


        // ********************************************



        try {

            // enter filename
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter filename: ");
            filename = br.readLine();

            // construct paths
            String currentDir = System.getProperty("user.dir");
            String outputFile = currentDir+filePrefix+filename+"-output.txt",
                    inputFile = currentDir+filePrefix+filename+".txt";

            FileWriter fwriter = new FileWriter(outputFile, false);
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
                if (!open) {
                    if(time == 0) {     // if this is the first line, AND time = 0, then skip
                        continue;          // TODO: remove this once data files don't begin with 0
                    }
                    t0 = time;
                    open = true;
                }


                // parse sensor type then add to corresponding arrayList
                if (sensorData[1].compareTo("a") == 0) {
                    data_accl.add(new sensorPoint(time - t0, new float[]{x, y, z}, sensorPoint.DATA_ACCELERATION));
                } else if (sensorData[1].compareTo("N") == 0) {
                    // divider sensorPoint has time equal to the last time point
                    data_accl.add(new sensorPoint(t_last - t0 + 1, new float[]{0, 0, 0}, sensorPoint.TRACE_BREAK));
                }

                // update the last timestamp variable
                t_last = time;
            }


            // # Calculate Data
            SPM_FunctionalWorkspace sigProcInstance = new SPM_FunctionalWorkspace(data_accl);
            sigProcInstance.doChurnData();

            results = sigProcInstance.getPosition();


            // # Save results
            int len = results.size();
            for (int i = 0; i < len; i++) {
                bwriter.write(results.get(i)[0] + "," + results.get(i)[1] + "," + results.get(i)[2]);
                bwriter.newLine();
            }


            // # Clean up
            bwriter.flush();
            fwriter.flush();
            bwriter.close();
            fwriter.close();

            breader.close();
            freader.close();


            System.out.println("all done! :D");

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
