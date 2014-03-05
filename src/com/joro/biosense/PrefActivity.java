package com.joro.biosense;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

/** Preference activity lets the user choose preference that will apply throughout the application.
 * @author Joro
 *
 */
public class PrefActivity extends PreferenceActivity{
	private static final String TAG = "PrefActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		Log.i(TAG, "Preference On Created");
		super.onCreate(savedInstanceState);
		// Preference resource contains all of the desired preferences
		addPreferencesFromResource(R.xml.prefs);
	}
	

}
