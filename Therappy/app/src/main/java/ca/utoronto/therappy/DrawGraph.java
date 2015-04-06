package ca.utoronto.therappy;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 02-Mar-15.
 */
public class DrawGraph extends Activity {
    private Button button;
    private Button button2;
    private int buttonCount1 = 1;
    private DrawShapes mDrawingArea;
    private Spinner spinner;
    private CustomOnItemSelectedListener listener1;
    private CustomOnItemSelectedListener listener2;
    private Spinner spinnerDate1;
    private Spinner spinnerDate2;
    // Have to read the actual dates that were tested from a string here.
    private String[] dates = {"Feb 05, 2015","Feb 07, 2015", "Feb 08, 2015", "Feb 09, 2015", "Feb 10, 2015",
            "Feb 11, 2015", "Feb 13, 2015", "Feb 14, 2015", "Feb 15, 2015", "Feb 16, 2015", "Feb 17, 2015", "Feb 19, 2015"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_layout);
        mDrawingArea =
                (DrawShapes)findViewById(R.id.sketchSpace);
        addItemsOnSpinner();
        addItemsOnSpinner2();
        addListenerOnSpinnerItemSelection();
        listener1.setView(mDrawingArea);
        listener2.setView(mDrawingArea);

        Drawable dr2 = getResources().getDrawable(R.drawable.button_pressed);
        dr2.setColorFilter(Color.parseColor("#70CCCCCC"), PorterDuff.Mode.SRC_ATOP);
        button2 = (Button) findViewById(R.id.left_button);
        button2.setBackgroundResource(R.drawable.button_pressed);
        button2.setBackgroundDrawable(dr2);
    }

    // add items into spinner dynamically
    public void addItemsOnSpinner() {

        spinnerDate1 = (Spinner) findViewById(R.id.spinner_date1);
        List<String> list = new ArrayList<String>();
        for (int i=0; i<dates.length; i++) {
            list.add(dates[i]);
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDate1.setAdapter(dataAdapter);
        // Sets the default value to 10 before the most recent measurement
        if (dates.length >= 10) {
            spinnerDate1.setSelection(dates.length - 10);
        }
        else {
            spinnerDate1.setSelection(0);
        }
    }

    // add items into spinner dynamically
    public void addItemsOnSpinner2() {

        spinnerDate2 = (Spinner) findViewById(R.id.spinner_date2);
        List<String> list = new ArrayList<String>();
        for (int i=0; i<dates.length; i++) {
            list.add(dates[i]);
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDate2.setAdapter(dataAdapter);
        // Sets the default value to the most recent measurement
        spinnerDate2.setSelection(dates.length-1);
    }

    public void addListenerOnSpinnerItemSelection() {
        listener1 = new CustomOnItemSelectedListener();
        listener2 = new CustomOnItemSelectedListener();
        spinnerDate1.setOnItemSelectedListener(listener1);
        spinnerDate2.setOnItemSelectedListener(listener2);
    }

    public void setGeneral(View view) {
        mDrawingArea.setPage("General");
        mDrawingArea.invalidate();

        if(buttonCount1 == 0) {
            // Need to set the left_button to the on state
            Drawable dr = getResources().getDrawable(R.drawable.button_pressed);
            dr.setColorFilter(Color.parseColor("#70CCCCCC"), PorterDuff.Mode.SRC_ATOP);
            button = (Button) findViewById(view.getId());
            button.setBackgroundResource(R.drawable.button_pressed);
            button.setBackgroundDrawable(dr);

            // Need to set the right_button to the off state as well
            Drawable dr2 = getResources().getDrawable(R.drawable.button_pressed);
            dr2.setColorFilter(Color.parseColor("#00CCCCCC"), PorterDuff.Mode.SRC_ATOP);
            button2 = (Button) findViewById(R.id.right_button);
            button2.setBackgroundResource(R.drawable.button_pressed);
            button2.setBackgroundDrawable(dr2);
            buttonCount1 = 1;
        }
    }

    public void setSpecific(View view) {
        mDrawingArea.setPage("Specific");
        mDrawingArea.invalidate();

        if(buttonCount1 == 1) {
            // Need to set the right_button to the on state
            Drawable dr = getResources().getDrawable(R.drawable.button_pressed);
            dr.setColorFilter(Color.parseColor("#70CCCCCC"), PorterDuff.Mode.SRC_ATOP);
            button = (Button) findViewById(view.getId());
            button.setBackgroundResource(R.drawable.button_pressed);
            button.setBackgroundDrawable(dr);

            // Need to set the left_button to the off state as well
            Drawable dr2 = getResources().getDrawable(R.drawable.button_pressed);
            dr2.setColorFilter(Color.parseColor("#00CCCCCC"), PorterDuff.Mode.SRC_ATOP);
            button2 = (Button) findViewById(R.id.left_button);
            button2.setBackgroundResource(R.drawable.button_pressed);
            button2.setBackgroundDrawable(dr2);
            buttonCount1 = 0;
        }
    }
}
