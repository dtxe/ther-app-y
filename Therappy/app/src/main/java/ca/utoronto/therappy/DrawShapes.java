package ca.utoronto.therappy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by joel on 02-Mar-15.
 */
public class DrawShapes extends View {

    // yCoor gives the magnitude of the volume space, will need to normalize it later
    // Most of this data will be read from a file instead.
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
    private String[] dates = {"05/02/15","07/02/15", "08/02/15", "09/02/15", "10/02/15",
            "11/02/15", "13/02/15", "14/02/15", "15/02/15", "16/02/15", "17/02/15", "19/02/15"};
    // Storing the variables that tells what dates to show
    private String firstDate = "";
    private String lastDate = "";
    private int firstPosition = -1;
    private int lastPosition = -1;

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
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);

        if (currentPage.equals("General")) {
            if (graphCounter == 0) {
                drawGeneralGraph(canvas, viewWidth, viewHeight, getRangeArray(yCoordinates, firstPosition, lastPosition));
                drawGraphText(canvas, " Volume Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            } else if (graphCounter == 1) {
                drawGeneralGraph(canvas, viewWidth, viewHeight, getRangeArray(xYRangeSpace, firstPosition, lastPosition));
                drawGraphText(canvas, "     X-Y Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            } else if (graphCounter == 2) {
                drawGeneralGraph(canvas, viewWidth, viewHeight, getRangeArray(xZRangeSpace, firstPosition, lastPosition));
                drawGraphText(canvas, "     X-Z Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            } else if (graphCounter == 3) {
                drawGeneralGraph(canvas, viewWidth, viewHeight, getRangeArray(yZRangeSpace, firstPosition, lastPosition));
                drawGraphText(canvas, "     Y-Z Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            }
        }
        else {
            if (graphCounter == 0) {
                drawGeneralGraph(canvas, viewWidth, viewHeight, getRangeArray(yCoordinates, firstPosition, lastPosition));
                drawGraphText(canvas, " Volume Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            } else if (graphCounter == 1) {
                drawDetailed2DSpace(canvas, xYDetailedSpaceX, xYDetailedSpaceY);
                drawGraphText(canvas, "     X-Y Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            } else if (graphCounter == 2) {
                drawDetailed2DSpace(canvas, xYDetailedSpaceX, xYDetailedSpaceY);
                drawGraphText(canvas, "     X-Z Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            } else if (graphCounter == 3) {
                drawDetailed2DSpace(canvas, xYDetailedSpaceX, xYDetailedSpaceY);
                drawGraphText(canvas, "     Y-Z Space", viewWidth*3/10, viewHeight*7/8+20, Color.BLACK, 60);
            }

        }
    }

    private float[] getRangeArray(float[] array, int firstPos, int lastPos){
        int length = lastPos - firstPos;
        int count = 0;
        if(firstPos == -1 || lastPos == -1 || length<1){
            // Set the default view
            float[] tempArray = new float[10];
            for(int i=0; i<10; i++) {
                tempArray[i] = array[array.length+i-10];
            }
            // Have to reset the last position if we are setting it to default
            lastPosition = dates.length - 1;
            return tempArray;
        }
        else {
            float[] tempArray = new float[length+1];
            for(int i=firstPos; i<lastPos+1; i++) {
                tempArray[count] = array[i];
                count++;
            }
            return tempArray;
        }

    }

    private void drawGeneralGraph(Canvas canvas, int viewWidth, int viewHeight, float[] coordinates) {
        //Paint graphColor = makePaint("Line", Color.MAGENTA, viewWidth / 120);
        float[] tempCoorNorm = new float[coordinates.length];
        Path path = new Path();
        Paint pathPaint = makePaint("Path", Color.MAGENTA, 255);

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

    private void drawDetailed2DSpace(Canvas canvas, float[] xCoor, float[] yCoor) {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        // Need to find whether |xMin| or |xMax| is bigger
        int xMax = 0;
        int xMin = 0;
        // Store the biggest number from the array
        float xGreatest = 0;
        // Only need yMax since assumed that only x-axis has negative values
        float yMax = 0;
        xMax = findMax(xCoor);
        xMin = findMin(xCoor);
        yMax = findMax(yCoor);

        drawChangeSpace(canvas, viewWidth, viewHeight);

        // Store the number of axis points you want
        String[] tempString = new String[5];
        Paint graphColor = makePaint("Line", Color.MAGENTA, viewWidth / 120);

        if (xCoor[xMax] > -xCoor[xMin]) {
            xGreatest = xCoor[xMax];
        }
        else{
            xGreatest = -xCoor[xMin];
        }
        // I have to change this to make it more streamlined
        // Max i value is depending on the number of axis variables you want
        for (int i = 0; i < 5; i++) {
            tempString[i] = String.valueOf(-xGreatest + i*(xGreatest/2));
        }

        // Calculate the actual coordinates for the graph
        float[] tempActualX = new float[xYDetailedSpaceX.length];
        float[] tempActualY = new float[xYDetailedSpaceY.length];
        float[] tempActualX2 = new float[xYDetailedSpaceX.length];
        float[] tempActualY2 = new float[xYDetailedSpaceY.length];

        // Getting the Actual X and Y coordinates
        for (int i=0; i<xYDetailedSpaceX.length; i++){
            // min length is viewWidth/6, max length is view Width*5/6
            // min height is viewHeight/3, max height is viewHeight*5/6
            if (xYDetailedSpaceX[i] < 0){
                tempActualX[i] = viewWidth/2 - Math.abs(xYDetailedSpaceX[i])/xGreatest*viewWidth/3;
                tempActualY[i] = viewHeight*23/36 - xYDetailedSpaceY[i]/yMax*viewHeight/2;
                tempActualX2[i] = tempActualX[i];
                tempActualY2[i] = tempActualY[i] * 25/36;
            }
            else{
                tempActualX[i] = viewWidth/2 + xYDetailedSpaceX[i]/xGreatest*viewWidth/3;
                tempActualY[i] = viewHeight*23/36 - xYDetailedSpaceY[i]/yMax*viewHeight/2;
                tempActualX2[i] = tempActualX[i];
                tempActualY2[i] = tempActualY[i] * 25/36;
            }
        }

        // Drawing the points in the detailed graph with the actual coordinates
        Path path = new Path();
        Path path2 = new Path();

        // Last property is to set transparency
        Paint pathPaint = makePaint("Path", Color.MAGENTA, 255);
        Paint pathPaint2 = makePaint("Path", Color.MAGENTA, 60);
        // For first point, have to make sure it starts from the axis
        path.moveTo(viewWidth/6, viewHeight*23/36);
        path2.moveTo(viewWidth/6, viewHeight*23/36);

        for (int i=1; i<xYDetailedSpaceX.length; i++) {
            path.lineTo(tempActualX[i-1], tempActualY[i-1]);
            path2.lineTo(tempActualX2[i-1], tempActualY2[i-1]);
            // If its the last point, close the path
            if (i == (xYDetailedSpaceX.length - 1)){
                // Have to make sure if hits the axis at 0
                path.lineTo(tempActualX[i], tempActualY[i]);
                path.lineTo(tempActualX[i], viewHeight*23/36);
                path.close();
                path2.lineTo(tempActualX2[i], tempActualY2[i]);
                path2.lineTo(tempActualX2[i], viewHeight*23/36);
                path2.close();
            }
        }
        canvas.drawPath(path, pathPaint);
        canvas.drawPath(path2, pathPaint2);

        draw2DAxis(canvas, viewWidth, viewHeight, tempString, yMax);
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
                    (float) (viewHeight * 103 / 144));
            drawGraphText(canvas, xValues[lastPosition-((i)*mulTen)], viewWidth / 7 +
                    ((length-1-(i*mulTen)) * interXValue) - viewWidth / 20, viewHeight * 103 / 144, Color.BLACK, unitSize);
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

    private void draw2DAxis(Canvas canvas, int viewWidth, int viewHeight, String[] xValues, float maxYScale) {
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
        for (int i = 0; i < xValues.length; i++) {
            /* x-axis label */
            drawGraphText(canvas, xValues[i], (i + 1) * viewWidth / 6 - viewWidth/50,
                    viewHeight * 49 / 72, Color.BLACK, unitSize);
            /* y-axis label */
            if (i>0) {
                drawGraphText(canvas, String.valueOf((int) (maxYScale * i / 4)), viewWidth * 4 / 9,
                        viewHeight * 23 / 36 - viewHeight * i * 3 / 28, Color.BLACK, unitSize);
            }
        }

        /* Draw the Volume and Date Axis Titles */
        drawGraphText(canvas, "X-Range(cm)", viewWidth * 3 / 7, viewHeight * 24 / 33, Color.BLACK, titleSize);
        drawGraphText(canvas, "Y-Range(cm)", viewWidth * 3 / 7, viewHeight / 9, Color.BLACK, titleSize);
    }

    private void drawGraphText(Canvas canvas, String text, float xCoor, float yCoor, int color, int size) {
        Paint textPaint = makePaint("Text", color, size);
        canvas.drawText(text, xCoor, yCoor, textPaint);
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
}