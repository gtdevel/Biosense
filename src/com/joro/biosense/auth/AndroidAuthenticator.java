package com.joro.biosense.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.Service;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.joro.biosense.BiosenseApplication;
import com.pras.auth.Authenticator;

/** NOT CURRENTLY USED-This should be used if you want to always use a default login that is registered
 * to the mobile device.
 * @author Joro
 *
 */
public class AndroidAuthenticator implements Authenticator {
	private final String TAG = "AndroidAuthenticator";
	Activity activity;
	Service service;
	AccountManager manager;
	private String mService = null;
	private String auth_token = "";
	
	//Changes made
	public AndroidAuthenticator(Activity activity){
		this.activity = activity;
		manager = AccountManager.get(BiosenseApplication.context);
	}
	//Changes made
	public AndroidAuthenticator(Service service){
		this.service = service;
		manager = AccountManager.get(BiosenseApplication.context);
	}
	
	public String getAuthToken(String service) 
	{
		if(service == null){
			throw new IllegalAccessError("No Service name defined, Can't create Auth Token...");
		}
		
		if(mService != null && !mService.equals(service)){
			// Reset previous Token
			manager.invalidateAuthToken("com.google", auth_token);
		}
			
		Account[] acs = manager.getAccountsByType("com.google");
		Log.i(TAG, "Num of Matching account: "+ acs.length);
		
		if(acs == null || acs.length == 0){
			Toast.makeText(this.activity.getApplicationContext(), "No Google Account Added...", Toast.LENGTH_LONG).show();
			return "";
		}
		
		for(int i=0; i<acs.length; i++){
			if(acs[i].type.equals("com.google"))
			{
				// The first Gmail Account will be selected
				Log.i(TAG, "Selected Google Account "+ acs[i].name);
				AccountManagerFuture result = (AccountManagerFuture)(manager.getAuthToken(acs[i], service, null, activity, null, null));
				
				try{
					Bundle b = (Bundle)result.getResult();
					auth_token = b.getString(AccountManager.KEY_AUTHTOKEN);
					Log.i(TAG, "Auth_Token: "+ auth_token);
					return auth_token;
				}catch(Exception ex){
					Log.i(TAG, "Error: "+ ex.toString());
				}
			}
		}
		Log.i(TAG, "Problem in getting Auth Token...");
		return "";
	}

}
