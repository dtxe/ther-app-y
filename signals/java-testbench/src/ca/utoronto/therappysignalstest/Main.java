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


        // enter filename
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("THIS WILL PROCESS AND OVERWRITE THE ENTIRE FOLDER!!");
        System.out.print("Enter folder: ");
        filename = br.readLine();

        String currentDir = System.getProperty("user.dir");
        File folder = new File(currentDir+filePrefix+filename);

        File[] listfiles = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("^therappy\\d+?\\.txt$");
            }
        });

        double[][] allmaxpositions = new double[listfiles.length][];

        for(int ff = 0; ff < listfiles.length; ff++) {
            // Variables
            String nextLine;
            long time = 0, t_last = 0, t0 = 0;
            float x = 0, y = 0, z = 0;
            boolean open = false;

            ArrayList<sensorPoint> data_accl = new ArrayList<>();

            ArrayList<double[]> results;

            // extract file prefix
            String thisfilename = listfiles[ff].getName();
            thisfilename = thisfilename.split("\\.(?=[^\\.]+$)")[0];


            // ********************************************

            try {


                // construct paths
                String metricsFile = currentDir+filePrefix+filename+sep+thisfilename+"-metrics.txt",
                        outputFile = currentDir+filePrefix+filename+sep+thisfilename+"-output.txt",
                        inputFile = currentDir+filePrefix+filename+sep+thisfilename+".txt";

                System.out.println("Loading: " + inputFile);

                FileWriter fwriter = new FileWriter(outputFile, false);
                BufferedWriter bwriter = new BufferedWriter(fwriter, bufferSize);

                FileWriter fwriter2 = new FileWriter(metricsFile, false);
                BufferedWriter bwriter2 = new BufferedWriter(fwriter2, bufferSize);

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
                    } else if(sensorData[1].compareTo("r") == 0) {
                        data_accl.add(new sensorPoint(time - t0, new float[]{x, y, z}, sensorPoint.DATA_ROTATIONVEC));
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

                // # Save fancy metrics
                bwriter2.write("# Raw direction measurements");
                bwriter2.newLine();

                double[] maxposition = sigProcInstance.getMaxpositions();
                String tempout = "";
                for(int kk = 0; kk < maxposition.length; kk++) {
                    tempout += ","+maxposition[kk];
                }
                bwriter2.write(tempout.substring(1));
                bwriter2.newLine();

                bwriter2.write("------------------------");
                bwriter2.newLine();
                bwriter2.write("# Workspace Measurements");
                bwriter2.newLine();

                bwriter2.write(sigProcInstance.getWorkspaceVolume() + "," +
                        sigProcInstance.getXYplane() + "," +
                        sigProcInstance.getYZplane() + "," +
                        sigProcInstance.getXZplane());
                bwriter2.newLine();

                allmaxpositions[ff] = maxposition;


                // # Clean up
                bwriter.flush();
                bwriter2.flush();
                fwriter.flush();
                fwriter2.flush();

                bwriter.close();
                bwriter2.close();
                fwriter.close();
                fwriter2.close();

                breader.close();
                freader.close();


                System.out.println("all done! :D");

            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }

        FileWriter fsumwriter = new FileWriter(currentDir+filePrefix+filename+sep+"summary.csv", false);
        BufferedWriter bsumwriter = new BufferedWriter(fsumwriter, bufferSize);

        for(int aa = 0; aa < allmaxpositions.length; aa++) {
            if(allmaxpositions[aa] != null) {
                for (int bb = 0; bb < allmaxpositions[aa].length; bb++) {
                    bsumwriter.write(allmaxpositions[aa][bb] + ",");
                }
                bsumwriter.newLine();
            }
        }

        bsumwriter.flush();
        fsumwriter.flush();
        bsumwriter.close();
        fsumwriter.close();

    }



}
