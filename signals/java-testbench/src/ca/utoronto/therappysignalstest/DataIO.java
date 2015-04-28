package ca.utoronto.therappysignalstest;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Created by simeon on 2015-04-23.
 */
public class DataIO {

    private FileReader freader;
    private BufferedReader breader;

    private final static int bufferSize = 2048;


    public DataIO(String inputFile) {
        try {
            freader = new FileReader(inputFile);
            breader = new BufferedReader(freader, bufferSize);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


}
