package com.joro.biosense;

import java.util.Date;

import com.joro.biosense.charts.LiveChart;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

//Main Screen Activity
/**
 * @author Joro
 * 
 */
public class Timeline extends Activity {

	private static final String TAG = "Timeline";
	private static final boolean D = true;

	public static final String NEW_RESULT_INTENT = "com.joro.biosense.NEW_RESULT_INTENT";
	private Intent intent;
	
	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Key name received from UpdateSpreadsheetService Broadcast
	public static final String STATUS = "status";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	// Insecure connection will not be used
	// private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;

	// Layout Views
	private ListView mConversationView;
	private TextView mTextView;
	public TextView mTextViewG;
	private Button graphButton;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;

	// Local Bluetooth adapter
	private static BluetoothAdapter mBluetoothAdapter = null;

	// Member object for the chat services
	private BluetoothCommService mChatService = null;

	// Intent filter use to create filter for UpdateSpreadsheetService broadcast
	private IntentFilter filter;

	// Results data to access SQLite Database
	private ResultsData dbHelper;

	// Application context to be stored in this variable
	private static Context context;

	// Cursor adapter
	Cursor mCursor;
	SimpleCursorAdapter dataListAdapter;
	// Arrays used to set adapter between listview row and cursor
	static final String[] FROM = { ResultsData.C_CREATED_AT, ResultsData.C_OXY,
			ResultsData.C_PULSE }; //
	static final int[] TO = { R.id.textTime, R.id.textOxiRead,
			R.id.textPulseRead };

	UpdateSpreadsheetService mService;
	// Broadcast receiver that receives data upload updates
	UpdateReceiver receiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		BiosenseApplication biosense = (BiosenseApplication) getApplication();

		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// Setting up UpdateReceiver broadcast receiver
		receiver = new UpdateReceiver();
		filter = new IntentFilter("com.joro.biosense.UPDATE_INTENT");

		// Setting layout of from xml resource file
		setContentView(R.layout.timeline);
		context = getApplicationContext();

		// Open Database helper
		dbHelper = new ResultsData(this);
		dbHelper.open();

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Set initial link textview to specific string value
		mTextView = (TextView) findViewById(R.id.textConnectivityStatus);
		mTextViewG = (TextView) findViewById(R.id.textGoogleStatus);
		graphButton = (Button) findViewById(R.id.buttonGraph);
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		// If preferences aren't set when application is started
		// user is redirected to preference page
		if (biosense.getPrefs().getString("login", null) == null
				|| biosense.getPrefs().getString("password", null) == null) { //
			startActivity(new Intent(this, PrefActivity.class)); //
			Toast.makeText(this, R.string.msgSetupPrefs, Toast.LENGTH_LONG)
					.show(); //
		}

		graphButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (dbHelper.getResultsByTime().moveToFirst()) {
					startActivity(new Intent(Timeline.this, LiveChart.class));
				} else {
					Toast.makeText(Timeline.this,
							R.string.notifNoAvailableData, Toast.LENGTH_LONG)
							.show();
				}
			}
		});

	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		registerReceiver(receiver, filter);
		if (D)
			Log.e(TAG, "+ ON RESUME +");
		// Bluetooth Communication service is started only if it hasn't been
		// started already.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothCommService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}

		BiosenseApplication biosense = (BiosenseApplication) getApplication();
		mTextViewG.setText(biosense.getStatusUpdate());
	}

	/**
     * 
     */
	private void setupChat() {
		Log.d(TAG, "setupChat()");

		// Set the cursor adapter to the listview row.
		mCursor = dbHelper.getResultsByTime();
		startManagingCursor(mCursor);
		mConversationView = (ListView) findViewById(R.id.listViewMeasure);
		dataListAdapter = new SimpleCursorAdapter(this, R.layout.row, mCursor,
				FROM, TO);
		mConversationView.setAdapter(dataListAdapter);

		// Initialize the BluetoothCommService to perform bluetooth connections
		mChatService = new BluetoothCommService(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	/**
	 * NOT CURRENTLY USED-Used if device should be made discoverable to other
	 * Bluetooth devices for pairing.
	 * 
	 */
	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message to connected device. In current format, it is only used
	 * to send the initial message to Norin measurement device to set up the
	 * desired output format of the data.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothCommService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothCommService to write
			byte[] send = message.getBytes();
			mChatService.write(send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			// mOutEditText.setText(mOutStringBuffer);
		}
	}

	// Can't use this in Gingerbread. Only available in Honeycomb
	/*
	 * private final void setStatus(int resId) { final ActionBar actionBar =
	 * getActionBar(); actionBar.setSubtitle(resId); }
	 * 
	 * private final void setStatus(CharSequence subTitle) { final ActionBar
	 * actionBar = getActionBar(); actionBar.setSubtitle(subTitle); }
	 */

	// The Handler that gets information back from the BluetoothCommService
	/**
	 * Handler which receives messages from BluetoothCommService and recognizes
	 * state changes in the Bluetooth communication or read/write commands to
	 * and from the bluetooth device.
	 * 
	 */
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothCommService.STATE_CONNECTED:

					mTextView.setText(getString(R.string.title_connected_to,
							mConnectedDeviceName));
					if (((BiosenseApplication) getApplication())
							.networkConnected(context) == false) {
						Toast.makeText(Timeline.this, R.string.textNoNetwork,
								Toast.LENGTH_SHORT).show();
					} else if (!((BiosenseApplication) getApplication())
							.serviceIsRunning()) {

						Intent msgIntent = new Intent(Timeline.this,
								UpdateSpreadsheetService.class);
						startService(msgIntent);

						// mConversationArrayAdapter.clear();
					}
					break;
				case BluetoothCommService.STATE_CONNECTING:

					mTextView.setText(getString(R.string.title_connecting));
					break;
				case BluetoothCommService.STATE_LISTEN:
				case BluetoothCommService.STATE_NONE:

					mTextView.setText(getString(R.string.title_not_connected));
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				Log.i(TAG, "Timeline: Writing");
				String writeMessage = new String(toHexString(writeBuf));

				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// Construct a string from the valid bytes in the buffer
				String[] readMessage = createOutput(readBuf);
				Date date = new Date();

				// This might effect the accuracy of the timing in the graphing
				// activity
				if (!(readMessage[0] == "poor" || readMessage[0] == "0" || readMessage[1] == "0")) {
					dbHelper.addResult(date.toString(), readMessage[2],
							readMessage[0], readMessage[1], "false");
					intent = new Intent(NEW_RESULT_INTENT); 
					intent.putExtra("PULSE", readMessage[0]); 
					context.sendBroadcast(intent);
					fillData();
				}

				break;
			case MESSAGE_DEVICE_NAME:
				// Display the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				// Displays toast with message
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	/**
	 * After the startActivityForResult is called to start and activity, the
	 * result is passed on to this method and the Bluetooth is either turned on,
	 * or a connection is initiated to a Bluetooth device.
	 * 
	 * @param requestCode
	 *            Request code from requesting activity can either request a
	 *            connection to a Bluetooth device, or request for the Bluetooth
	 *            to be turned on.
	 * @param resultCode
	 *            Results code can either confirm or deny the requested code.
	 * @param data
	 *            Intent containing device address extra.
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
		// No insecure connection will be available - Leaving it in case of
		// future implementation
		/*
		 * case REQUEST_CONNECT_DEVICE_INSECURE: // When DeviceListActivity
		 * returns with a device to connect if (resultCode ==
		 * Activity.RESULT_OK) { connectDevice(data, false); } break;
		 */
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	/**
	 * @param data
	 *            Intent containing device address extra.
	 * @param secure
	 *            This boolean determines if the connection is secure or not. At
	 *            this moment, the secure connection is hard coded.
	 */
	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device, true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		Log.d(TAG, "Inflating menu");
		// Menu layout can be found in xml resource
		getMenuInflater().inflate(R.menu.menu, menu);
		Log.d(TAG, "Menu inflated");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub

		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.itemPref:
			serverIntent = new Intent(this, PrefActivity.class);
			startActivity(serverIntent
					.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
			break;
		case R.id.connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
			break;
		// No insecure connect scan will be available
		case R.id.itemToggleService:
			// Check if service is already running
			if (((BiosenseApplication) getApplication()).serviceIsRunning() == false) {
				// Check if there is data to update
				if (dbHelper.dataToUpdate() == false
						|| dbHelper.getResultsByTime().moveToFirst() == false) {
					Toast.makeText(this, R.string.textNothingToUpdate,
							Toast.LENGTH_SHORT).show();
					// Check if there is a network connection
				} else if (!((BiosenseApplication) getApplication())
						.networkConnected(context)) {
					Toast.makeText(this, R.string.textNoNetwork,
							Toast.LENGTH_SHORT).show();
				} else {
					// Start service if all previous conditions are met
					Intent msgIntent = new Intent(this,
							UpdateSpreadsheetService.class);
					startService(msgIntent);

				}
			} else {
				// If service is already running, then selecting this option
				// will stop service
				Intent msgIntent = new Intent(this,
						UpdateSpreadsheetService.class);
				stopService(msgIntent);
			}
			break;
		case R.id.clearResults:
			// If service is running then we cannot clear results
			if (((BiosenseApplication) getApplication()).serviceIsRunning()) {
				Toast.makeText(this, R.string.notifServiceIsRunning,
						Toast.LENGTH_LONG).show();
			} else if (dbHelper.getResultsByTime().moveToFirst() == false) {
				// If there is no data in the database then we cannot clear it
				Toast.makeText(this, R.string.notifNoData, Toast.LENGTH_LONG)
						.show();
			} else if (dbHelper.dataToUpdate() == true) {
				// If there is data that hasn't been updated, the user is
				// prompted for permission
				// to delete the data
				alertDialogBuilder("There are still results that haven't been uploaded"
						+ " to Google Spreadsheets. Are you sure you wan't to delete all results?");
			} else {
				// If all data has been updated and service isn't running, user
				// is prompted for
				// permission to delete the data
				alertDialogBuilder("Are you sure you want to delete the results?");
			}
		}
		return true;

	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		// TODO Auto-generated method stub
		// When menu is opened, the itemToggleService icon to start or stop the
		// UploadSpreadsheetService is changed based on whether the service is
		// running or not.
		if (((BiosenseApplication) getApplication()).serviceIsRunning()) {

			menu.findItem(R.id.itemToggleService).setTitle(
					R.string.titleItemStopService);
			menu.findItem(R.id.itemToggleService).setIcon(
					android.R.drawable.ic_media_pause);
		} else {

			menu.findItem(R.id.itemToggleService).setTitle(
					R.string.titleItemStartService);

			menu.findItem(R.id.itemToggleService).setIcon(
					android.R.drawable.ic_media_play);

		}

		return true;
	}

	/**
	 * Creates a dialog box which allows the user to choose a yes or no option
	 * if they want to clear the results that are displayed in the Timeline.
	 * 
	 * @param message
	 *            Message displayed when dialog is displayed.
	 */
	private void alertDialogBuilder(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message)
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dbHelper.clearResults();
								fillData();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * BroadcastReceiver that receives intents from UpdateSpreadsheetService and
	 * then passes on intent to updateUI method which displays message attached
	 * to intent.
	 * 
	 * @author Joro
	 * 
	 */
	class UpdateReceiver extends BroadcastReceiver { //
		@Override
		public void onReceive(Context context, Intent intent) { //
			updateUI(intent);
			Log.d("TimelineReceiver", "onReceived");
		}
	}

	/**
	 * Updates textview which displays the current status of the Google
	 * Spreadsheets update process. The intent comes from the
	 * UpdateSpreadsheetService.
	 * 
	 * @param intent
	 *            Intent received from update receiver
	 */
	private void updateUI(Intent intent) {
		String gConnectStatus = intent.getStringExtra(STATUS);
		Log.d(TAG, gConnectStatus);
		mTextViewG = (TextView) findViewById(R.id.textGoogleStatus);
		mTextViewG.setText(gConnectStatus);

	}

	/**
	 * Relevant data is extracted from bytes that are sent from the Bluetooth
	 * device and is then converted to a string array which can easily be
	 * outputed.
	 * 
	 * @param bytes
	 *            Array of bytes sent by bluetooth device.
	 * @return String array containing pulse, oxygen percentage and username
	 */
	public String[] createOutput(byte[] bytes) {

		String username = new String();
		// Setting up application to access shared preferences
		username = ((BiosenseApplication) getApplication()).getPrefs()
				.getString(username, null);

		String[] finalMessage = { "", "", "" };
		int val = byteArrayToInt(bytes, 0);
		// First to masks are for heart rate measurements
		int mask1 = 0x03000000;
		int mask2 = 0x007F0000;
		// Second mask is for oxygen level
		int mask3 = 0x0000FF00;
		// Third mask is for Smart Point Algorithm (quality measurement)
		int mask4 = 0x00000020;

		// Shifting of bits to the right
		int pulse = (val & mask1) >> 17 | (val & mask2) >> 16;
		int oxy = (val & mask3) >> 8;
		int quality = (val & mask4) >> 5;

		if (oxy <= 100 && quality == 1) {
			finalMessage[0] = String.valueOf(pulse);
			finalMessage[1] = String.valueOf(oxy);
			finalMessage[2] = username;
		} else {
			finalMessage[0] = "poor";
			finalMessage[1] = "poor";
			finalMessage[2] = username;
		}
		return finalMessage;
	}

	/**
	 * Updates results displayed in listview by getting an updated cursor and
	 * updating the simple cursor adapter.
	 */
	private void fillData() {
		mCursor = dbHelper.getResultsByTime();
		startManagingCursor(mCursor);

		dataListAdapter = new SimpleCursorAdapter(this, R.layout.row, mCursor,
				FROM, TO);
		mConversationView.setAdapter(dataListAdapter);
	}

	/**
	 * @param b
	 *            Byte to be converted
	 * @param offset
	 *            Offset of byte
	 * @return Converted integer
	 */
	public static int byteArrayToInt(byte[] b, int offset) {
		int value = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			value += (b[i + offset] & 0x000000FF) << shift;
		}
		return value;
	}

	/**
	 * @param bytes
	 *            Byte to be converted to hexadecimal string
	 * @return Hexadecimal string
	 */
	public static String toHexString(byte[] bytes) {
		char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'A', 'B', 'C', 'D', 'E', 'F' };
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v / 16];
			hexChars[j * 2 + 1] = hexArray[v % 16];
		}
		return new String(hexChars);
	}

}