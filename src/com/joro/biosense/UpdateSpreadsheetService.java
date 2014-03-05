package com.joro.biosense;




import java.util.ArrayList;
import java.util.Date;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.pras.SpreadSheet;
import com.pras.SpreadSheetFactory;
import com.pras.WorkSheet;
import com.pras.table.Record;

public class UpdateSpreadsheetService extends Service{
	static final String TAG = "UpdaterService";
	public static final String UPDATE_INTENT = "com.joro.biosense.UPDATE_INTENT";
    private Intent intent;
	
	public static final int MESSAGE_G_CONNECTING = 6;
    public static final int  MESSAGE_G_NOT_CONNECTED = 7;
	public static final int  MESSAGE_G_CONNECTED = 8;
	public static final String  STATUS = "status";
	public static final int MESSAGE_STATUS = 9;
	
    private SpreadSheetFactory SSFactory;
	private SpreadSheet SSheet;
	private WorkSheet WSheet;
	private Timeline timeline=new Timeline();
	private ArrayList<SpreadSheet> spreadsheets;
	private ArrayList<WorkSheet> worksheets;
	public Record record;
	public int position=0;
	private boolean exists; 
	private Cursor mCursor;
	private Handler handler;
	
	public static final String C_ID = "_id";
	public static final String C_CREATED_AT = "created_at";
	public static final String C_PULSE = "pulse";
	public static final String C_OXY = "oxy";
	public static final String C_USER = "user";
	
	static final int DELAY = 60000;
	private boolean runFlag = false;
	private boolean errorFlag = false;
	private Updater updater;
	public Context con;
	int counter = 0;
	//private Updater updater;
	private ResultsData dbHelper;
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		this.updater = new Updater();
		dbHelper=new ResultsData(this);
		dbHelper.open();
		//mHandler=timeline.mHandler;
		con=getApplicationContext();
		
		Log.d(TAG, "OnCreated");
	}

	/**NOT CURRENTLY USED-Should be used to bind activity to service
	 * @author Joro
	 *
	 */
	public class LocalBinder extends Binder {
		UpdateSpreadsheetService getService() {
            return UpdateSpreadsheetService.this;
        }
    }
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		if(!runFlag){
			
			
			this.runFlag=true;
			((BiosenseApplication) getApplication()).setServiceRunning(true);
			this.updater.start();
			Log.d(TAG, "OnStarted");
		}
		
		return START_STICKY;
	}



	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		this.updater.interrupt();
		this.updater=null;
		this.runFlag=false;
		
			intent = new Intent(UPDATE_INTENT); 
		if(errorFlag){
			intent.putExtra(STATUS, "Not Connected - Problem Connecting - Check Login"); 
			this.sendBroadcast(intent);
			((BiosenseApplication) getApplication()).setStatusUpload("Not Connected - Problem Connecting - Check Login");
		}else{
			intent.putExtra(STATUS, "Not Connected"); 
			this.sendBroadcast(intent);
			((BiosenseApplication) getApplication()).setStatusUpload("Not Connected");
		}
		
		
		errorFlag=false;
		((BiosenseApplication) getApplication()).setServiceRunning(false);
		
		
	}
	
	/** NOT CURRENTLY USED-Should be used to register the handler from an activity.
	 * @param serviceHandler The desired handler being used.
	 */
	public void registerHandler(Handler serviceHandler) {
	    handler = serviceHandler;
	}
	//Thread that performs the actual updater of the updater service
	/**
	 * @author Joro
	 *
	 */
	public class Updater extends Thread{

		public Updater(){
			super("UpdaterService-Updater");
		}
		
		public void run(){
			UpdateSpreadsheetService updaterService = UpdateSpreadsheetService.this;
			
			Log.d(TAG, "Running Background Thread");
			
		try{	
				intent = new Intent(UPDATE_INTENT); 
				intent.putExtra(STATUS, "Connecting..."); 
				updaterService.sendBroadcast(intent);
				BiosenseApplication biosense = (BiosenseApplication) updaterService.getApplication();
				biosense.setStatusUpload("Connecting...");
				// TODO Auto-generated method stub
				
				String[] ColumnNames = {"time_recorded", "pulse", "oxygen_percentage"};
				Date date = new Date();
				String username=new String();
				String password=new String();
				username=biosense.getPrefs().getString("login", null);
				password=biosense.getPrefs().getString("password", null);
				String WorksheetName = new String(date.getDate() + ", " + date.getMonth() + " ," + (date.getYear()+1900));
				String SpreadsheetName = new String("norin_device_results");
				Log.i(TAG, username+ ", " +password);	
				SSFactory = SpreadSheetFactory.getInstance(username, password);
				Log.i(TAG, "Got Spreadsheet");	
				spreadsheets = SSFactory.getSpreadSheet(SpreadsheetName, true);
				if(spreadsheets==null){
					Log.i(TAG, "Spreadsheet doesn't exist");	
					SSFactory.createSpreadSheet(SpreadsheetName);
					if(SSFactory.getSpreadSheet(SpreadsheetName, true)==null){
						
						updaterService.errorFlag=true;
						Log.d(TAG, "Insider");
					}
					SSheet=SSFactory.getSpreadSheet(SpreadsheetName, true).get(0);
				}else{
					Log.i(TAG, "Spreadsheet Exists");	
					SSheet=spreadsheets.get(0);
				}
				
				worksheets=SSheet.getWorkSheet(WorksheetName, true);
				if(worksheets==null){
					SSheet.addListWorkSheet(WorksheetName,40, ColumnNames);
					WSheet=SSheet.getWorkSheet(WorksheetName, true).get(0);
					Log.i(TAG, "Created List Worksheet");
				}else{
					WSheet=worksheets.get(0);
					Log.i(TAG, "Worksheet Already there");
				}
				intent = new Intent(UPDATE_INTENT); 
				intent.putExtra(STATUS, "Uploading Results..."); 
				updaterService.sendBroadcast(intent);
				biosense.setStatusUpload("Uploading Results...");
				
				Log.i(TAG, "Got Worksheets");
	
		}catch(Exception e){
				Log.e(TAG, "Problem connecting/creating spreadsheets");
				
		}
			
	
	   try{   	
				mCursor = dbHelper.getResultsByUploadStatus();
				//mCursor.moveToPosition(position);
				mCursor.moveToNext();
				
				while(mCursor.isAfterLast()==false){
				    if(((BiosenseApplication) updaterService.getApplication()).networkConnected(con)){
						//***MAKE SURE YOU ADD THE DAation())
						//***MAKE SURE YOU ADD THE DATA
						record=new Record();
					    record.addData("timerecorded", mCursor.getString(mCursor.getColumnIndex(C_CREATED_AT)));
					    Log.i(TAG, "Time: " + mCursor.getString(mCursor.getColumnIndex(C_CREATED_AT)));
					    record.addData("pulse", mCursor.getString(mCursor.getColumnIndex(C_PULSE)));
					    Log.i(TAG, "Pulse " + mCursor.getString(mCursor.getColumnIndex(C_PULSE)));
					    record.addData("oxygenpercentage", mCursor.getString(mCursor.getColumnIndex(C_OXY)));
					    Log.i(TAG, "Oxygen Percentage " + mCursor.getString(mCursor.getColumnIndex(C_OXY)));
					    WSheet.addListRow(record.getData());
						//Log.i(TAG, getString(mCursor.getPosition()));
						Log.i(TAG, "Added Row");
						
						String[] created = new String[] {mCursor.getString(mCursor.getColumnIndex(C_CREATED_AT))};
						dbHelper.uploadedResults(created);
						
						mCursor = dbHelper.getResultsByUploadStatus();
						mCursor.moveToNext();
						//position=mCursor.getPosition();
				    }else{
				    	break;
				    }
				}
			
		 }catch(Exception e){
				Log.e(TAG, "Problem uploading data");
					
		 }
		 	updaterService.runFlag=false;
			stopSelf();
		
		}
	}//End of updater class thread

	public void updateStatus() {
		// TODO Auto-generated method stub
		
	}
	
	

}