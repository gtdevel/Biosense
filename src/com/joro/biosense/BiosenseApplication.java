package com.joro.biosense;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
//**This class is used as a start up class which holds general information which is useful to 
//**whole application. Every time the application starts, it runs through this code. 
//**The shared preferences of the application are set up here and the flag for the running service
//**is set here so that all the components in the application can have access to it.
public class BiosenseApplication extends Application implements OnSharedPreferenceChangeListener {
	private static final String TAG = "BiosenseApplication";
	private SharedPreferences pref;
	private boolean serviceRunning=false;
	public static Context context;
	private ResultsData dbHelper;
	private boolean changesToCollaborators=false;
	private String statusOfUpdate=new String("Not connected");
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		//Setting shared preferences and on shared preferences listener
		this.pref = PreferenceManager.getDefaultSharedPreferences(this);
		this.pref.registerOnSharedPreferenceChangeListener(this);
		context=getApplicationContext();
		Log.i(TAG, "OnCreated");
	}



	@Override
	public void onTerminate() {
		// TODO Auto-generated method stub
		super.onTerminate();
		Log.i(TAG, "OnTerminated");
	}
	
	/**
	 * @return
	 */
	public SharedPreferences getPrefs(){
		Log.i(TAG, "Returned Preferences");
		return pref;
		
	}
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// TODO Auto-generated method stub
		if(key=="collabCheck1"){
			changesToCollaborators=true;
		}else if(key=="collabCheck2"){
			changesToCollaborators=true;
		}else if(key=="collabCheck3"){
			changesToCollaborators=true;
		}else if(key=="collabEmail1"){
			changesToCollaborators=true;
		}else if(key=="collabEmail2"){
			changesToCollaborators=true;
		}else if(key=="collabEmail3"){
			changesToCollaborators=true;
		}
			
		Log.i(TAG, "Shared Preferences Changed");
	}
	
	//Used to check if there are changes to the collaborators
	/**
	 * @return
	 */
	public boolean changesToCollaborators(){
		return this.changesToCollaborators;
	}
	
	/**
	 * @return
	 */
	public boolean collaboratorsAreChecked(){
		if(pref.getBoolean("collabCheck1", false)||pref.getBoolean("collabCheck2", false)||pref.getBoolean("collabCheck3", false)){
			return true;
		}else{
			return false;
		}	
	}
	//Used to see if the updater service is running
	/**
	 * @return
	 */
	public boolean serviceIsRunning(){
		return serviceRunning;
	}
	//Updater service sets this when it changes states (Actiive to innactive or vice versa).
	/**
	 * @param serviceRunning
	 */
	public void setServiceRunning(boolean serviceRunning){
		this.serviceRunning = serviceRunning;
	}
	
	
	
	 /**
	 * @param con
	 * @return
	 */
	public boolean networkConnected(Context con){
			 	ConnectivityManager connectivityManager;
			    NetworkInfo wifiInfo, mobileInfo;
		        try{
		            connectivityManager = (ConnectivityManager) con.getSystemService(Context.CONNECTIVITY_SERVICE);
		            wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		            mobileInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);   
		 
		            if(wifiInfo.isConnected() || mobileInfo.isConnected())
		            {
		                return true;
		            }
		        }
		        catch(Exception e){
		            System.out.println("CheckConnectivity Exception: " + e.getMessage());
		        }
		 
		        return false;
		    
	 }

	public String getStatusUpdate(){
		return statusOfUpdate;
	}

	public void setStatusUpload(String string) {
		// TODO Auto-generated method stub
		statusOfUpdate=string;
		
	}
	
		
	
}
