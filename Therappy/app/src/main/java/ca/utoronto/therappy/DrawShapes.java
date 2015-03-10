package ca.utoronto.therappy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by joel on 02-Mar-15.
 */
public class DrawShapes extends View {

    /* yCoor gives the magnitude of the volume space, will need to normalize it later */
    private float[] yCoordinates = {300, 350, 500, 600, 750};
    private float[] yCoordinatesNorm = new float[5];
    /* To potentially store dates that they performed the task */
    private String[] dates = {"5/02", "6/02", "7/02", "8/02", "9/02"};
    /* To store the current calculated volume space */
    private String[] volume = new String[5];
    /*    private Path path = new Path();*/

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
        /* xCoordinates are constant, so do not need them */
        drawGraphLine(canvas, viewWidth, viewHeight);

        /* To store the current calculated volume space */
        for (int i=0; i<yCoordinates.length; i++) {
            volume[i] = String.valueOf(yCoordinates[i]);
        }
        /* Draw out the volume space values */
        drawGraphText(canvas, "Original Volume Space:", viewWidth/8, viewHeight/6, Color.BLACK, viewWidth/23);
        drawGraphText(canvas, "Current Volume Space:", viewWidth/8, viewHeight/5, Color.BLACK, viewWidth/23);
        drawGraphText(canvas, volume[0] + "mL", viewWidth*3/5, viewHeight/6, Color.BLACK, viewWidth/23);
        drawGraphText(canvas, volume[4] + "mL", viewWidth*3/5, viewHeight/5, Color.GREEN, viewWidth/23);
    }

    private void drawGraphLine(Canvas canvas, int viewWidth, int viewHeight) {
        Paint graphColor = makePaint("Line", Color.MAGENTA, viewWidth/120);
        Paint axisColor = makePaint("Line", Color.BLACK, viewWidth/86);

        /*First normalize the value */
        int maxIndex = findMax(yCoordinates);
/*        int minIndex = findMin(yCoordinates);*/
        for(int i=0; i<yCoordinates.length; i++) {
            /* Intermediate float */
            float interValue = yCoordinates[i]/yCoordinates[maxIndex];
            yCoordinatesNorm[i] = (int) ((interValue)*viewHeight*3/7);
        }
/*         Go to the starting position *//*
        path.moveTo(, viewHeight - yCoorNorm[0]);
*//*         For drawing path lines *//*
        path.lineTo(xCoor[1], viewHeight - yCoorNorm[1]);
        path.lineTo(xCoor[2], viewHeight - yCoorNorm[2]);
        path.lineTo(xCoor[3], viewHeight - yCoorNorm[3]);
        path.lineTo(xCoor[4], viewHeight - yCoorNorm[4]);*/

        /* Drawing the x axis*/
        canvas.drawLine(viewWidth/6 - viewWidth/160, viewHeight*5/6,
                viewWidth*5/6, viewHeight*5/6, axisColor);
        /* Draw the y axis */
        canvas.drawLine(viewWidth/6, viewHeight*5/6,
                viewWidth/6, viewHeight/3, axisColor);

        /* Get the maximum scale of the y-axis */
/*        int yAxisRange = (int) (yCoordinates[maxIndex] - yCoordinates[0]);*/

        /* Say labels have 5 y-axis and x-axis labels */
        for (int i=0; i<yCoordinates.length ; i++){
            /* x-axis label */
            drawGraphText(canvas,  dates[i], (i+1)*viewWidth/7, viewHeight*7/8, Color.BLACK, viewWidth/40);
            /* y-axis label */
            drawGraphText(canvas, String.valueOf(yCoordinates[maxIndex]*i/4), viewWidth/12, viewHeight*5/6 - viewHeight*i*3/28, Color.BLACK, viewWidth/40);
        }

        /* Draw the Volume and Date Axis Titles */
        drawGraphText(canvas, "Volume(mL)", viewWidth/9, viewHeight*4/13, Color.BLACK, viewWidth/40);
        drawGraphText(canvas, "Date", viewWidth*3/7, viewHeight*10/11, Color.BLACK, viewWidth/40);

        /* Drawing the Volume Space trend*/
        for (int i=1; i<yCoordinates.length; i++) {
            canvas.drawLine(viewWidth/6+(i-1)*viewWidth/7, viewHeight*5/6 - yCoordinatesNorm[i-1],
                    viewWidth/6 + (i)*viewWidth/7, viewHeight*5/6 - yCoordinatesNorm[i], graphColor);
        }

/*        canvas.drawPath(path, graphColor);*/
    }

    private void drawGraphText(Canvas canvas, String text, int xCoor, int yCoor, int color, int size) {
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

/*    public static int findMin(float[] array) {
        int indexOfMin = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] < array[indexOfMin]) {
                indexOfMin = i;
            }
        }
        return indexOfMin;
    }*/
/*      For drawing rectangular bars

        drawGraphRect(canvas, viewWidth,
                viewHeight, viewWidth/6, viewHeight/2);
        drawGraphRect(canvas, viewWidth,
                viewHeight, viewWidth*2/6, viewHeight/2);
        drawGraphRect(canvas, viewWidth,
                viewHeight, viewWidth*3/6, viewHeight/2);
        drawGraphRect(canvas, viewWidth,
                viewHeight, viewWidth*4/6, viewHeight/2);
        drawGraphRect(canvas, viewWidth,
                viewHeight, viewWidth*5/6, viewHeight/2);
        drawGraphText(canvas, viewWidth, viewHeight);*/


/*    private void drawGraphRect(Canvas canvas, int viewWidth,
                                int viewHeight, int xcoor, int ycoor) {
        Paint squareColor = makePaint(Color.MAGENTA);
        canvas.drawRect(xcoor,ycoor,xcoor+viewWidth/10,ycoor+viewHeight/2, squareColor);

    }*/

    private Paint makePaint(String type, int color, int width) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);

        if (type.equals("Line")) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(width);
        }
        else {
            p.setTextSize(width);
        }
        return(p);
    }

    public void changeCoordinates(float[] coordinates) {
        for (int i=0; i<yCoordinates.length; i++) {
            yCoordinates[i] = coordinates[i];
        }
    }
}