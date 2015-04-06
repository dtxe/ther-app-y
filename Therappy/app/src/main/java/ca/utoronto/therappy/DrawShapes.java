package ca.utoronto.therappy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by joel on 02-Mar-15.
 */
public class DrawShapes extends View {

    // yCoor gives the magnitude of the volume space, will need to normalize it later
    // Most of this data will be read from a file instead.
    private List<Float> xYRangeSpaceList = new ArrayList<Float>();
    private List<Float> xZRangeSpaceList = new ArrayList<Float>();
    private List<Float> yZRangeSpaceList = new ArrayList<Float>();
    private List<Float> volumeSpaceList = new ArrayList<Float>();
    private List<Float> xYDetailedSpaceXList = new ArrayList<Float>();
    private List<Float> xYDetailedSpaceYList = new ArrayList<Float>();
    private List<Float> xZDetailedSpaceXList = new ArrayList<Float>();
    private List<Float> xZDetailedSpaceZList = new ArrayList<Float>();
    private List<Float> yZDetailedSpaceYList = new ArrayList<Float>();
    private List<Float> yZDetailedSpaceZList = new ArrayList<Float>();
    private List<String> dateString = new ArrayList<String>();
    private float[] yCoordinates = {300, 350, 500, 600, 620, 640, 600, 700, 720, 750, 740, 755};
    //x and y store the actual coordinates
    private float[] xActualCoor = new float[12];
    private float[] yActualCoor = new float[12];
    private float[] xYRangeSpace = {10, 12, 12, 14, 15, 14, 16, 18, 17, 19, 18, 18};
    private float[] xZRangeSpace = {8, 8, 8, 9, 9, 9, 11, 10, 8, 8, 9, 10};
    private float[] yZRangeSpace = {7, 9, 8, 9, 9, 9, 11, 10, 8, 8, 9, 10};
    private float[] xYDetailedSpaceX = {-10f, -9f, -8.8f, -8.5f, -8.0f, -7f, -6.4f, -6.2f, -5f, -4.3f,
            -3.7f, -3.2f, -2.5f, -1.4f, -0.5f, 0f, 0.5f, 1.2f, 1.3f, 1.7f, 2.5f, 2.8f, 3.4f, 4.2f, 5.1f, 5.8f,
            6.6f, 7.3f, 7.9f, 8.2f, 8.6f, 9.2f};
    private float[] xYDetailedSpaceY = {0f, 0.34f, 0.81f, 1.54f, 2.32f, 3.23f, 3.85f, 4.34f, 5.22f, 5.92f,
            6.66f, 7.21f, 7.87f, 8.88f, 9.34f, 9.7f, 9.21f, 9.11f, 8.43f, 8.10f, 7.52f, 6.45f, 5.83f, 5.12f, 4.23f, 3.67f,
            3.01f, 2.33f, 1.87f, 1.23f, 0.56f, 0f};
    // To potentially store dates that they performed the task
    private String[] dates = {"Feb 05, 2015","Feb 07, 2015", "Feb 08, 2015", "Feb 09, 2015", "Feb 10, 2015",
            "Feb 11, 2015", "Feb 13, 2015", "Feb 14, 2015", "Feb 15, 2015", "Feb 16, 2015", "Feb 17, 2015", "Feb 19, 2015"};
    private final File root = android.os.Environment.getExternalStorageDirectory();
    // Storing the variables that tells what dates to show
    private String firstDate = "";
    private String lastDate = "";
    private int firstPosition = -1;
    private int lastPosition = -1;
    private static final String TAG = MainActivity.class.getSimpleName();
    private FileReader freader;
    private File sensorFiles;
    private BufferedReader reader;
    private boolean fileNotYetRead = true;

    // Stores the value of the current graph that is being shown
    private int graphCounter = 0;
    // How much you have to swipe to change the graph
    private String currentPage = "General";
    private int width = 0;
    private int height = 0;

    public DrawShapes(Context context) {
        super(context);
    }

    public DrawShapes(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int fileNumber = 1;
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);

        if (fileNotYetRead) {
            while (readFile("values" + String.valueOf(fileNumber))) {
                fileNumber++;
            }
            fileNotYetRead = false;
        }

        if (currentPage.equals("General")) {
            if (graphCounter == 0) {
                drawGeneralGraph(canvas, viewWidth, viewHeight, getRangeArray(volumeSpaceList, firstPosition, lastPosition));
                drawGraphText(canvas, " Volume Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            } else if (graphCounter == 1) {
                drawGeneralGraph(canvas, viewWidth, viewHeight, getRangeArray(xYRangeSpaceList, firstPosition, lastPosition));
                drawGraphText(canvas, "     X-Y Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            } else if (graphCounter == 2) {
                drawGeneralGraph(canvas, viewWidth, viewHeight, getRangeArray(xZRangeSpaceList, firstPosition, lastPosition));
                drawGraphText(canvas, "     X-Z Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            } else if (graphCounter == 3) {
                drawGeneralGraph(canvas, viewWidth, viewHeight, getRangeArray(yZRangeSpaceList, firstPosition, lastPosition));
                drawGraphText(canvas, "     Y-Z Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            }
        }
        else {
            if (graphCounter == 0) {
                drawSpecificVolume(canvas, viewWidth, viewHeight);
                drawGraphText(canvas, "Summary Page", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            } else if (graphCounter == 1) {
                drawDetailed2DSpace(canvas, xYDetailedSpaceXList, xYDetailedSpaceYList, "X-Range(cm)", "Y-Range(cm)");
                drawGraphText(canvas, "     X-Y Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            } else if (graphCounter == 2) {
                drawDetailed2DSpace(canvas, xZDetailedSpaceXList, xZDetailedSpaceZList, "X-Range(cm)", "Z-Range(cm)");
                drawGraphText(canvas, "     X-Z Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            } else if (graphCounter == 3) {
                drawDetailed2DSpace(canvas, yZDetailedSpaceYList, yZDetailedSpaceZList, "Y-Range(cm)", "Z-Range(cm)");
                drawGraphText(canvas, "     Y-Z Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            }

        }
    }

    private float[] getRangeArray(List<Float> list, int firstPos, int lastPos){
        int length = lastPos - firstPos;
        int count = 0;
        if(firstPos == -1 || lastPos == -1 || length<1){
            // Set the default view
            float[] tempArray = new float[10];
            if (length > 9) {
                for (int i = 0; i < 10; i++) {
                    tempArray[i] = list.get(list.size() + i - 10);
                }
            }
            else {
                for (int i = 0; i < length; i++) {
                    tempArray[i] = list.get(list.size() + i - length);
                }
            }
            // Have to reset the last position if we are setting it to default
            lastPosition = dates.length - 1;
            return tempArray;
        }
        else {
            float[] tempArray = new float[length+1];
            for(int i=firstPos; i<lastPos+1; i++) {
                tempArray[count] = list.get(i);
                count++;
            }
            return tempArray;
        }

    }

    private void drawGeneralGraph(Canvas canvas, int viewWidth, int viewHeight, float[] coordinates) {
        float[] tempCoorNorm = new float[coordinates.length];
        Path path = new Path();
        Paint pathPaint = makePaint("Path", getResources().getColor(R.color.colorPrimary), 255);

        drawChangeSpace(canvas, viewWidth, viewHeight);

        // Set the width and height values so that onTouchEvent knows what viewWidth and viewHeight are
        width = viewWidth;
        height = viewHeight;

        /*First normalize the value */
        int maxIndex = findMax(coordinates);
/*        int minIndex = findMin(yCoordinates);*/
        // MAX height is viewHeight*3/7
        for (int i = 0; i < coordinates.length; i++) {
            /* Intermediate float */
            float interValue = coordinates[i] / coordinates[maxIndex];
            tempCoorNorm[i] = (int) ((interValue) * viewHeight * 3 / 7);
        }
        // Have to normalize the x values too, should go from 1/6 to 5/6
        float interXValue = (viewWidth*4/6)/(coordinates.length - 1);

        // Store the actual coordinates here
        for (int i = 0; i < coordinates.length; i++) {
            xActualCoor[i] = viewWidth / 6 + (i) * interXValue;
            yActualCoor[i] = viewHeight * 23 / 36 - tempCoorNorm[i];
        }

        path.moveTo(xActualCoor[0], yActualCoor[0]);
        for (int i = 1; i < yCoordinates.length; i++) {
            path.lineTo(xActualCoor[i], yActualCoor[i]);
            if (i == coordinates.length - 1){
                path.lineTo(xActualCoor[i], viewHeight*23/36);
                path.lineTo(xActualCoor[0], viewHeight*23/36);
                path.close();
                canvas.drawPath(path, pathPaint);
            }
        }

        drawAxis(canvas, viewWidth, viewHeight, dates, coordinates[maxIndex], interXValue, coordinates.length);
    }

    private void drawSpecificVolume(Canvas canvas, int viewWidth, int viewHeight){
        int unitSize = viewWidth/25;
        Paint paint = makePaint("Path", getResources().getColor(R.color.colorPrimary), 255);
        Paint paint2 = makePaint("Path", getResources().getColor(R.color.colorPrimaryBright), 150);
        drawChangeSpace(canvas, viewWidth, viewHeight);
        canvas.drawRect(viewWidth*27/72, viewHeight*2/24, viewWidth*46/72, viewHeight*16/24, paint);
        canvas.drawRect(viewWidth*47/72, viewHeight*2/24, viewWidth*66/72, viewHeight*16/24, paint2);

        drawGraphText(canvas, "Vol Space(mL)", viewWidth/15, viewHeight*6/24, Color.BLACK, unitSize);
        drawGraphText(canvas, "X-Y Space(mL)", viewWidth/15, viewHeight*9/24, Color.BLACK, unitSize);
        drawGraphText(canvas, "X-Z Space(mL)", viewWidth/15, viewHeight*12/24, Color.BLACK, unitSize);
        drawGraphText(canvas, "Y-Z Space(mL)", viewWidth/15, viewHeight*15/24, Color.BLACK, unitSize);
        drawGraphText(canvas, firstDate, viewWidth*7/18, viewHeight*3/24, Color.WHITE, unitSize);
        drawGraphText(canvas, lastDate, viewWidth*2/3, viewHeight*3/24, Color.WHITE, unitSize);
        // Volume Space values
        drawGraphText(canvas, String.valueOf(volumeSpaceList.get(firstPosition)), viewWidth*7/18 + viewWidth/12, viewHeight*6/24, Color.WHITE, unitSize);
        drawGraphText(canvas, String.valueOf(volumeSpaceList.get(lastPosition)), viewWidth*2/3 + viewWidth/12, viewHeight*6/24, Color.WHITE, unitSize);

        // X-Y Space values
        drawGraphText(canvas, String.valueOf(xYRangeSpaceList.get(firstPosition)), viewWidth*7/18 + viewWidth/12, viewHeight*9/24, Color.WHITE, unitSize);
        drawGraphText(canvas, String.valueOf(xYRangeSpaceList.get(lastPosition)), viewWidth*2/3 + viewWidth/12, viewHeight*9/24, Color.WHITE, unitSize);

        // X-Z Space values
        drawGraphText(canvas, String.valueOf(xZRangeSpaceList.get(firstPosition)), viewWidth*7/18 + viewWidth/12, viewHeight*12/24, Color.WHITE, unitSize);
        drawGraphText(canvas, String.valueOf(xZRangeSpaceList.get(lastPosition)), viewWidth*2/3 + viewWidth/12, viewHeight*12/24, Color.WHITE, unitSize);

        // Y-Z Space values
        drawGraphText(canvas, String.valueOf(yZRangeSpaceList.get(firstPosition)), viewWidth*7/18 + viewWidth/12, viewHeight*15/24, Color.WHITE, unitSize);
        drawGraphText(canvas, String.valueOf(yZRangeSpaceList.get(lastPosition)), viewWidth*2/3 + viewWidth/12, viewHeight*15/24, Color.WHITE, unitSize);
    }

    private void drawChangeSpace(Canvas canvas, int viewWidth, int viewHeight){
        Path path = new Path();
        Paint pathPaint = makePaint("Path", Color.BLACK, 255);

        // Draw out the arrow
        path.moveTo(viewWidth/8, viewHeight*19/20);
        path.lineTo(viewWidth/8, viewHeight*19/20 - viewHeight/25);
        path.lineTo(viewWidth/4, viewHeight*19/20 - viewHeight/25);
        path.lineTo(viewWidth/4, viewHeight*4/5 + viewHeight/25);
        path.lineTo(viewWidth/8, viewHeight*4/5 + viewHeight/25);
        path.lineTo(viewWidth/8, viewHeight*4/5);
        path.lineTo(viewWidth/15, viewHeight*7/8);
        path.close();
        canvas.drawPath(path, pathPaint);

        // Draw out the arrow
        path.moveTo(viewWidth*7/8, viewHeight*19/20);
        path.lineTo(viewWidth*7/8, viewHeight*19/20 - viewHeight/25);
        path.lineTo(viewWidth*6/8, viewHeight*19/20 - viewHeight/25);
        path.lineTo(viewWidth*6/8, viewHeight*4/5 + viewHeight/25);
        path.lineTo(viewWidth*7/8, viewHeight*4/5 + viewHeight/25);
        path.lineTo(viewWidth*7/8, viewHeight*4/5);
        path.lineTo(viewWidth*14/15, viewHeight*7/8);
        path.close();
        canvas.drawPath(path, pathPaint);
    }

    private void drawDetailed2DSpace(Canvas canvas, List<Float> xCoor, List<Float> yCoor, String xAxis, String yAxis) {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        // Need to find whether |xMin| or |xMax| is bigger
        int xMax = 0;
        int xMin = 0;

        //First have to know what arrays I'm actually displaying
        float[] tempXValues = new float[10];
        float[] tempYValues = new float[10];

        for (int i=0; i<5; i++){
            tempXValues[i] = xCoor.get(i+(firstPosition*5));
            tempYValues[i] = yCoor.get(i+(firstPosition*5));
            tempXValues[i+5] = xCoor.get(i+(lastPosition*5));
            tempYValues[i+5] = yCoor.get(i+(lastPosition*5));
        }

        // Store the biggest number from the array
        float xGreatest = 0;
        // Only need yMax since assumed that only x-axis has negative values
        int yMax = 0;
        float yGreatest = 0;
        xMax = findMax(tempXValues);
        xMin = findMin(tempXValues);
        yMax = findMax(tempYValues);
        yGreatest = tempYValues[yMax];

        drawChangeSpace(canvas, viewWidth, viewHeight);

        // Store the number of axis points you want
        String[] tempString = new String[5];
        if (tempXValues[xMax] > -tempXValues[xMin]) {
            xGreatest = tempXValues[xMax];
        }
        else{
            xGreatest = -tempXValues[xMin];
        }
        // I have to change this to make it more streamlined
        // Max i value is depending on the number of axis variables you want
        for (int i = 0; i < 5; i++) {
            tempString[i] = String.valueOf(-xGreatest + i*(xGreatest/2));
        }

        // Calculate the actual coordinates for the graph
        float[] tempActualX = new float[10];
        float[] tempActualY = new float[10];

        // Getting the Actual X and Y coordinates
        for (int i=0; i<tempActualX.length; i++){
            // min length is viewWidth/6, max length is view Width*5/6
            // min height is viewHeight/3, max height is viewHeight*5/6
            if (yCoor.get(i) < 0){
                // Then set to 0
                yCoor.set(i, Float.valueOf(0));
            }
            if (xCoor.get(i) < 0){
                tempActualX[i] = viewWidth/2 - Math.abs(tempXValues[i])/xGreatest*viewWidth/3;
                tempActualY[i] = viewHeight*23/36 - tempYValues[i]/yGreatest*viewHeight*3/7;
            }
            else{
                tempActualX[i] = viewWidth/2 + tempXValues[i]/xGreatest*viewWidth/3;
                tempActualY[i] = viewHeight*23/36 - tempYValues[i]/yGreatest*viewHeight*3/7;
            }
        }

        // Drawing the points in the detailed graph with the actual coordinates
        Path path = new Path();
        Path path2 = new Path();

        // Last property is to set transparency
        Paint pathPaint = makePaint("Path", getResources().getColor(R.color.colorPrimary), 255);
        Paint pathPaint2 = makePaint("Path", getResources().getColor(R.color.colorPrimaryBright), 150);
        // For first point, have to make sure it starts from the axis
        path.moveTo(viewWidth/6, viewHeight*23/36);
        path2.moveTo(viewWidth/6, viewHeight*23/36);

        for (int i=1; i<5; i++) {
            path.lineTo(tempActualX[i-1], tempActualY[i-1]);
            path2.lineTo(tempActualX[i+5-1], tempActualY[i+5-1]);
            // If its the last point, close the path
            if (i == 4){
                // Have to make sure if hits the axis at 0
                path.lineTo(tempActualX[i], tempActualY[i]);
                path.lineTo(tempActualX[i], viewHeight*23/36);
                path.close();
                path2.lineTo(tempActualX[i+5], tempActualY[i+5]);
                path2.lineTo(tempActualX[i+5], viewHeight*23/36);
                path2.close();
            }
        }
        // Assume the new is bigger
        canvas.drawPath(path, pathPaint);
        canvas.drawPath(path2, pathPaint2);

        draw2DAxis(canvas, viewWidth, viewHeight, tempString, yMax, xAxis, yAxis);
    }

    private void drawAxis(Canvas canvas, int viewWidth, int viewHeight, String[] xValues,
                          float maxScale, float interXValue, int length) {
        Paint axisColor = makePaint("Line", Color.BLACK, viewWidth / 86);
        int titleSize = viewWidth/30;
        int unitSize = viewWidth/35;
        int numYUnits = 5;

        /* Drawing the x axis*/
        canvas.drawLine(viewWidth / 6 - viewWidth / 160, viewHeight * 23 / 36,
                viewWidth * 5 / 6 + viewWidth/40, viewHeight * 23 / 36, axisColor);
        /* Draw the y axis */
        canvas.drawLine(viewWidth / 6, viewHeight * 23 / 36,
                viewWidth / 6, viewHeight * 5 / 36, axisColor);

        int mulTen = 1;

        if (length > 10) {
            mulTen = length / 10 + 1;
        }

        int numValues = length / mulTen;
        if ((length % mulTen) > 0) {
            numValues++;
        }

        for (int i = 0; i < numValues; i++) {
            canvas.save();
            canvas.rotate(-45f, (float) (viewWidth / 7 + ((length-1-(i*mulTen)) * interXValue)),
                    (float) (viewHeight * 100 / 144));
            char temp = xValues[lastPosition-((i)*mulTen)].charAt(0);
            char temp2 = xValues[lastPosition-((i)*mulTen)].charAt(1);
            char temp3 = xValues[lastPosition-((i)*mulTen)].charAt(2);
            String month = String.valueOf(temp) + String.valueOf(temp2) + String.valueOf(temp3);
            temp = xValues[lastPosition-((i)*mulTen)].charAt(4);
            temp2 = xValues[lastPosition-((i)*mulTen)].charAt(5);
            String day = String.valueOf(temp) + String.valueOf(temp2);
            String date = month + " " + day;

            drawGraphText(canvas, date, viewWidth / 7 +
                    ((length-1-(i*mulTen)) * interXValue) - viewWidth / 20, viewHeight * 100 / 144, Color.BLACK, unitSize);
            canvas.restore();
        }

        for (int i = 0; i < numYUnits; i++){
            /* y-axis label */
            drawGraphText(canvas, String.valueOf((int) (maxScale * i / 4)), viewWidth / 12,
                    viewHeight * 94 / 144 - viewHeight * i * 3 / 28, Color.BLACK, unitSize);
        }

        /* Draw the Volume and Date Axis Titles */
        drawGraphText(canvas, "Volume(mL)", viewWidth / 11, viewHeight * 8 / 78, Color.BLACK, titleSize);
        // drawGraphText(canvas, "Date", viewWidth * 3 / 7, viewHeight * 25 / 33, Color.BLACK, titleSize);
    }

    private void draw2DAxis(Canvas canvas, int viewWidth, int viewHeight, String[] xValues,
                            float maxYScale, String xAxis, String yAxis) {
        Paint axisColor = makePaint("Line", Color.BLACK, viewWidth / 86);

        int titleSize = viewWidth/30;
        int unitSize = viewWidth/35;
        int numYUnits = 5;

        /* Drawing the x axis*/
        canvas.drawLine(viewWidth/6 - viewWidth / 160, viewHeight * 23 / 36,
                viewWidth * 5 / 6 + viewWidth/160, viewHeight * 23 / 36, axisColor);
        /* Draw the y axis */
        canvas.drawLine(viewWidth/2, viewHeight * 23 / 36,
                viewWidth/2, viewHeight * 5 / 36, axisColor);

        /* Get the maximum scale of the y-axis */
/*        int yAxisRange = (int) (yCoordinates[maxIndex] - yCoordinates[0]);*/

        /* Say labels have 5 y-axis and x-axis labels */
        for (int i = 0; i < 5; i++) {
            /* x-axis label */
            drawGraphText(canvas, xValues[i], (i + 1) * viewWidth / 6 - viewWidth/50,
                    viewHeight * 49 / 72, Color.BLACK, unitSize);
            /* y-axis label */
            if (i>0) {
                drawGraphText(canvas, String.valueOf((maxYScale * i / 4)), viewWidth * 4 / 9,
                        viewHeight * 23 / 36 - viewHeight * i * 3 / 28, Color.BLACK, unitSize);
            }
        }

        /* Draw the Volume and Date Axis Titles */
        drawGraphText(canvas, xAxis, viewWidth * 3 / 7, viewHeight * 24 / 33, Color.BLACK, titleSize);
        drawGraphText(canvas, yAxis, viewWidth * 3 / 7, viewHeight / 9, Color.BLACK, titleSize);
    }

    private void drawGraphText(Canvas canvas, String text, float xCoor, float yCoor, int color, int size) {
        Paint textPaint = makePaint("Text", color, size);
        canvas.drawText(text, xCoor, yCoor, textPaint);
    }


    public static int findMax(List<Float> array) {
        int indexOfMax = 0;
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i) > array.get(indexOfMax)) {
                indexOfMax = i;
            }
        }
        return indexOfMax;
    }

    public static int findMax(float[] array) {
        int indexOfMax = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > array[indexOfMax]) {
                indexOfMax = i;
            }
        }
        return indexOfMax;
    }

    public static int findMin(List<Float> array) {
        int indexOfMin = 0;
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i) < array.get(indexOfMin)) {
                indexOfMin = i;
            }
        }
        return indexOfMin;
    }

    public static int findMin(float[] array) {
        int indexOfMin = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] < array[indexOfMin]) {
                indexOfMin = i;
            }
        }
        return indexOfMin;
    }

    private Paint makePaint(String type, int color, int width) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);

        if (type.equals("Line")) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(width);
        } else if (type.equals("Path")){
            p.setStyle(Paint.Style.FILL);
            p.setAlpha(width);
        }
        else {
            p.setTextSize(width);
        }
        return (p);
    }

    public void sendRequest(String message) {
        firstDate = message;
    }

    public void sendRequest2(String message) {
        lastDate = message;
    }

    public void sendPosition(int pos) {
        firstPosition = pos;
    }

    public void sendPosition2(int pos) {
        lastPosition = pos;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // if the user touches a particular point, do something
                if ((x > width / 15 && x < width / 4) && (y > height * 4 / 5 && y < height * 19 / 20)) {
                    graphCounter--;
                    if (graphCounter < 0) {
                        graphCounter = 3;
                    }
                    invalidate();
                } else if ((x > width * 6 / 8 && x < width * 14 / 15) && (y > height * 4 / 5 && y < height * 19 / 20)) {
                    graphCounter++;
                    graphCounter = graphCounter % 4;
                    invalidate();
                }
        }
        return true;
    }

    public void setPage(String page) {
        currentPage = page;
    }

    private boolean readFile(String fileName) {
        final int bufferSize = 2048;
        String nextLine;
        // To keep track of what line I'm in
        int count = 0;

        //fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        try {
            sensorFiles = new File(root, "/therappy/" + fileName + ".csv");
            if (!sensorFiles.exists()) {
                Log.i(TAG, "Problem opening file...exiting");
                return false;
            }
            freader = new FileReader(sensorFiles);
            reader = new BufferedReader(freader, bufferSize);
        } catch(IOException e){
            e.printStackTrace();
        }

        try{
            // read all lines in data file
            while((nextLine = reader.readLine()) != null) {
                String sensorData[] = nextLine.split(",");
                try {
                    if (count == 0) {
                        // This is the date of the measurement
                        dateString.add(sensorData[0]);
                    }
                    else if (count == 1) {
                        volumeSpaceList.add(Float.parseFloat(sensorData[0]));
                    }
                    else if (count == 2) {
                        xYRangeSpaceList.add(Float.parseFloat(sensorData[0]));
                    }
                    else if (count > 2 && count < 8) {
                        xYDetailedSpaceXList.add(Float.parseFloat(sensorData[0]));
                        xYDetailedSpaceYList.add(Float.parseFloat(sensorData[1]));
                    }
                    else if (count == 8) {
                        xZRangeSpaceList.add(Float.parseFloat(sensorData[0]));
                    }
                    else if (count > 8 && count < 14) {
                        xZDetailedSpaceXList.add(Float.parseFloat(sensorData[0]));
                        xZDetailedSpaceZList.add(Float.parseFloat(sensorData[2]));
                    }
                    else if (count == 14) {
                        yZRangeSpaceList.add(Float.parseFloat(sensorData[0]));
                    }
                    else if (count > 14 && count < 18) {
                        yZDetailedSpaceYList.add(Float.parseFloat(sensorData[1]));
                        yZDetailedSpaceZList.add(Float.parseFloat(sensorData[2]));
                        if (count == 17) {
                            // To make it a nice 5 values
                            yZDetailedSpaceYList.add(Float.parseFloat(sensorData[1]));
                            yZDetailedSpaceZList.add(Float.parseFloat(sensorData[2]));
                            yZDetailedSpaceYList.add(Float.parseFloat(sensorData[1]));
                            yZDetailedSpaceZList.add(Float.parseFloat(sensorData[2]));
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            freader.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }

}