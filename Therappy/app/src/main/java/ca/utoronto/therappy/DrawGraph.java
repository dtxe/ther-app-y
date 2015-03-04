package ca.utoronto.therappy;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * Created by joel on 02-Mar-15.
 */
public class DrawGraph extends Activity {
    private DrawShapes mDrawingArea;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_layout);
        mDrawingArea =
                (DrawShapes)findViewById(R.id.sketchSpace);
    }
    /** Handles events for the button. Redraws the ShapeView. */
    public void redraw(View view) {
        mDrawingArea.invalidate();
    }
}
