package ca.utoronto.therappy;

/**
 * Created by joel on 09-Mar-15.
 */

import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

/**
 * Created by joel on 09-Mar-15.
 */
public class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
    private DrawShapes mDrawingArea;

    public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {

        Spinner spinner = (Spinner) parent;
        if(spinner.getId() == R.id.spinner_date1)
        {
            //This might not be necessary if I can just send position instead
            mDrawingArea.sendRequest(parent.getItemAtPosition(pos).toString());
            mDrawingArea.sendPosition(pos);
            mDrawingArea.invalidate();
        }
        else if(spinner.getId() == R.id.spinner_date2)
        {
            mDrawingArea.sendRequest2(parent.getItemAtPosition(pos).toString());
            mDrawingArea.sendPosition2(pos);
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