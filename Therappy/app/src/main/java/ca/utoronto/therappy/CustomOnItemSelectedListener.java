package ca.utoronto.therappy;

/**
 * Created by joel on 09-Mar-15.
 */

import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

/**
 * Created by joel on 09-Mar-15.
 */
public class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
    private float[] coordinates = new float[5];
    private DrawShapes mDrawingArea;

    public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {

        if (parent.getItemAtPosition(pos).toString().equals("Volume Space")){
            Toast.makeText(parent.getContext(),
                    "Volume Space Read!",
                    Toast.LENGTH_SHORT).show();
            // Enter some coordinate changes here, in real practice will be reading from a file
            coordinates[0] = 300;
            coordinates[1] = 350;
            coordinates[2] = 500;
            coordinates[3] = 600;
            coordinates[4] = 750;

            mDrawingArea.changeCoordinates(coordinates);
            mDrawingArea.invalidate();
        }
        else if (parent.getItemAtPosition(pos).toString().equals("X-Y Space")){
            Toast.makeText(parent.getContext(),
                    "X-Y Space Read!",
                    Toast.LENGTH_SHORT).show();
            // Enter some coordinate changes here, in real practice will be reading from a file
            for (int i=0; i<coordinates.length; i++) {
                coordinates[i] = 300;
            }
            mDrawingArea.changeCoordinates(coordinates);
            mDrawingArea.invalidate();
        }
        else if (parent.getItemAtPosition(pos).toString().equals("X-Z Space")){
            Toast.makeText(parent.getContext(),
                    "X-Z Space Read!",
                    Toast.LENGTH_SHORT).show();
            // Enter some coordinate changes here, in real practice will be reading from a file
            for (int i=0; i<coordinates.length; i++) {
                coordinates[i] = 400;
            }
            mDrawingArea.changeCoordinates(coordinates);
            mDrawingArea.invalidate();
        }
        else if (parent.getItemAtPosition(pos).toString().equals("Y-Z Space")){
            Toast.makeText(parent.getContext(),
                    "Y-Z Space Read!",
                    Toast.LENGTH_SHORT).show();
            for (int i=0; i<coordinates.length; i++) {
                coordinates[i] = 500;
            }
            // Enter some coordinate changes here, in real practice will be reading from a file
            mDrawingArea.changeCoordinates(coordinates);
            mDrawingArea.invalidate();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    public void setView(DrawShapes view){
        mDrawingArea = view;
    }

}
