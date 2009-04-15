package edu.nyu.cs.omnidroid.ui;

import java.util.ArrayList;

import edu.nyu.cs.omnidroid.util.AGParser;
import edu.nyu.cs.omnidroid.util.UGParser;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity used to present a list of Applications which are registered in Omnidroid configuration.
 * The user can then select an application whose actions can be thrown using our Omnihandler.
 * 
 * @author acase
 *
 */
public class ActionThrower extends ListActivity {
  private static AGParser ag;
  private String appName;
  private String eventName;
  
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(this.getLocalClassName(), "onCreate");
		super.onCreate(savedInstanceState);

    // Initialize our AGParser
    ag = new AGParser(getApplicationContext());

    // See what application we want to handle events for from the
    // intent data passed to us.
    Intent i = getIntent();
    Bundle extras = i.getExtras();
    if (extras != null) {
      appName = extras.getString(AGParser.KEY_APPLICATION);
      eventName = extras.getString(UGParser.KEY_EventName);
    } else {
      // TODO (acase): Throw exception
    }

    // TODO: Filter by only apps that contain actions
    // Getting the Events from AppConfig
    ArrayList<String> pkgNames;
    pkgNames = ag.readLines(AGParser.KEY_APPLICATION);
    // Display possible actions
    setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, pkgNames));
    getListView().setTextFilterEnabled(true);
  }


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
    Log.i(this.getLocalClassName(), "onListItemClicked");

    // TODO: Build URI dynamically
		// Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
		// String action = getIntent().getAction();
		// Launch activity to view/edit the currently selected item
    Intent i = new Intent();
    // TODO (acase): Choose Filter page
    i.setClass(this.getApplicationContext(), edu.nyu.cs.omnidroid.ui.ActionThrowerActions.class);
    TextView tv = (TextView) v;
    i.putExtra(AGParser.KEY_APPLICATION, appName);
    i.putExtra(UGParser.KEY_EventName, eventName);
    i.putExtra(UGParser.KEY_ActionApp, tv.getText());
    startActivity(i);
	}

}