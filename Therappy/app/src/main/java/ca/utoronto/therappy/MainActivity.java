package ca.utoronto.therappy;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends ActionBarActivity implements OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /*  startSensorModule
     *  starts intent for the sensor module section
     */
    public void startSensorModule(View view)  {
        Intent intent = new Intent(this, SensorModule.class);
        startActivity(intent);
    }

    /*  startViewProgress
     *  starts intent for the view progress section
     */
    public void startViewProgress(View view) {
        Intent activityIntent = new Intent(this, DrawGraph.class);
        startActivity(activityIntent);
    }

    @Override
    public void onClick(View view) {
        // do something when the user interacts with the UI
        // note: this app has the intent calls within the XML
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
