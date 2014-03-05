package com.joro.biosense;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

/**NOT CURRENTLY IN USE - Receives broadcast from device when network connection is established.
 * @author Joro
 *
 */
public class NetworkReceiver extends BroadcastReceiver{
	public static final String TAG="NetworkReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		//if true, the network is down
		/*boolean isNetworkDown=intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

		if(isNetworkDown){
			Log.d(TAG, "onReceive: NOT connected, stopping update service");
			//Broadcast receivers aren't subclasses of contexts, therefore we need to include them
			context.stopService(new Intent(context, UpdateSpreadsheetService.class));
		}else{
			Log.d(TAG, "onReceive: IS connected, starting update service");
			context.startService(new Intent(context, UpdateSpreadsheetService.class));
			
		}*/
	}

}
