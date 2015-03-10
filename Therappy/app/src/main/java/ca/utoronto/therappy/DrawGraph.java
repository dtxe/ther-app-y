package ca.utoronto.therappy;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;

/**
 * Created by joel on 02-Mar-15.
 */
public class DrawGraph extends Activity {
    private DrawShapes mDrawingArea;
    private Spinner spinner;
    private CustomOnItemSelectedListener listener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_layout);
        mDrawingArea =
                (DrawShapes)findViewById(R.id.sketchSpace);
        addListenerOnSpinnerItemSelection();
        listener.setView(mDrawingArea);
    }
    /** Handles events for the button. Redraws the ShapeView. */
    public void redraw(View view) {
        mDrawingArea.invalidate();
    }

    /*    // add items into spinner dynamically
    public void addItemsOnSpinner() {

        choice_spinner = (Spinner) findViewById(R.id.choice_spinner);
        List<String> list = new ArrayList<String>();
        list.add("list 1");
        list.add("list 2");
        list.add("list 3");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(dataAdapter);
    }*/

    public void addListenerOnSpinnerItemSelection() {
        listener = new CustomOnItemSelectedListener();
        spinner = (Spinner) findViewById(R.id.choice_spinner);
        spinner.setOnItemSelectedListener(listener);
    }
}
