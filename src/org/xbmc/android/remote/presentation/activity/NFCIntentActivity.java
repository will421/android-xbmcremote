package org.xbmc.android.remote.presentation.activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.xbmc.android.remote.R;
import org.xbmc.android.remote.business.Command;
import org.xbmc.android.util.ClientFactory;
import org.xbmc.android.util.HostFactory;
import org.xbmc.android.util.NFCHelper;
import org.xbmc.api.business.DataResponse;
import org.xbmc.api.business.INotifiableManager;
import org.xbmc.api.data.IControlClient;
import org.xbmc.api.data.IControlClient.ICurrentlyPlaying;
import org.xbmc.api.object.Host;
import org.xbmc.api.type.SeekType;
import org.xbmc.httpapi.WifiStateException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.TimeUtils;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

public class NFCIntentActivity extends Activity {

	//private static int HOST_STATE_SAME = 0x00; 
	private static int HOST_STATE_DIFF = 0x10; //0 if same, 1 if diff
	//private static int HOST_STATE_PLAYING = 0x0;
	private static int HOST_STATE_NOTPLAYING = 0x8; //0 if playing, 1 if not playing
	//private static int HOST_STATE_OTHER_PLAYING = 0x0;
	private static int HOST_STATE_OTHER_NOTPLAYING = 0x4;//0 if playing, 1 if not playing
	//private static int SOMEONE_ON_KINECT  = 0x0;
	private static int NOBODY_ON_KINECT = 0x2; //0 if someone on kinect,1 if nobody
	//private static int SOMETHING_ON_REMOTE = 0x0;
	private static int NOTHING_ON_REMOTE = 0x1; //0 if something, 1 if nothing

	private String TAG = this.getClass().getSimpleName();

	
	int mId = 5 ;
	private ICurrentlyPlaying playing;
	private ICurrentlyPlaying playingOther;
	private IControlClient cc;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		playing = null;
		playingOther = null;


		Intent intent = this.getIntent();


		dealWithTagIntent(intent);
	}

	private void dealWithTagIntent(Intent intent){
		Log.v(this.getClass().getSimpleName(), "resolveIntent : "+intent.getAction().toString());
		String action = intent.getAction();
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)){
			if (intent.getType() !=null &&  intent.getType().equals(NFCHelper.NFCmimeType));
			{
				Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
				NdefRecord relayRecord = ((NdefMessage)rawMsgs[0]).getRecords()[0];
				final Host host = HostFactory.getHostFromCompressedLightJson(relayRecord.getPayload());
				String nfcData = host.toJson();
				Log.d(this.getClass().getName(), "Message lu : "+nfcData);
				host.toCompressedLightJson();
				if(!checkIfHostExist(host)){
					addHostDialog(host);
				}
				else{
					exec(host);
				}
			}
		}
	}

	private void addHostDialog(final Host host){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.Do_you_want_to_add_this_host))
		.setTitle(getString(R.string.tag_nfc));
		builder.setPositiveButton(getString(R.string.add), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				HostFactory.addHost(getBaseContext(), host);
				Toast.makeText(getBaseContext(), "Host "+host.name+" added", Toast.LENGTH_SHORT).show();
				finish();
			}
		});
		builder.setNegativeButton(getString(R.string.ignore), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}	
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private int initState(Host host,SharedPreferences pref){
		int state = 0;

		if(HostFactory.host ==null){ //Workaround for when remote not connected
			HostFactory.host = host;
			ClientFactory.resetClient(host);
		}


		if(! host.toLightJson().equals(HostFactory.host.toLightJson()))
			state += HOST_STATE_DIFF;

		playing = cc.getCurrentlyPlaying(new MyNotifiable());
		if((state & HOST_STATE_DIFF) != 0){
			Host hostSave = HostFactory.host;
			ClientFactory.resetClient(host);
			playingOther = cc.getCurrentlyPlaying(new MyNotifiable());
			ClientFactory.resetClient(hostSave);
		}

		if(playing.getFilename().isEmpty())
			state+= HOST_STATE_NOTPLAYING;

		if(playingOther ==null || playingOther.getFilename().isEmpty())
			state+= HOST_STATE_OTHER_NOTPLAYING;

		//Check kinect
		if(playingOther == null || !checkKinect())
			state+= NOBODY_ON_KINECT;

		//Check notification
		String filepath =pref.getString(getString(R.string.prefId_save_playing_file_name), null);
		if(filepath ==null)
			state+= NOTHING_ON_REMOTE;

		return state;
	}

	private void saveFile(SharedPreferences pref){
		SharedPreferences.Editor editor = pref.edit();
		editor.putString(getString(R.string.prefId_save_playing_file_name), playing.getFilename());
		editor.putFloat(getString(R.string.prefId_save_playing_seek),(float) playing.getPercentage());
		editor.commit();

		
		cc.stop(new MyNotifiable());
		
		int seconds1 = playing.getTime();
		int seconds2 = playing.getDuration();
		String time = String.format("%02d:%02d:%02d/%02d:%02d:%02d", 
				TimeUnit.SECONDS.toHours(seconds1),
				TimeUnit.SECONDS.toMinutes(seconds1) -  
				TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(seconds1)), // The change is in this line
				TimeUnit.SECONDS.toSeconds(seconds1) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds1)),
				TimeUnit.SECONDS.toHours(seconds2),
				TimeUnit.SECONDS.toMinutes(seconds2) -  
				TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(seconds2)), // The change is in this line
				TimeUnit.SECONDS.toSeconds(seconds2) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds2))); 

		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.icon)
		.setContentTitle(URLUtil.guessFileName(playing.getFilename(),null,null))
		.setContentText(time);

		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, HomeActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(HomeActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
						);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		
		mNotificationManager.notify(mId, mBuilder.build());
	}
	
	private void applySavedFile(SharedPreferences sharedPref,Host host){
		String filepath = sharedPref.getString(getString(R.string.prefId_save_playing_file_name), null);
		if(filepath==null) return;
		Float p = sharedPref.getFloat(getString(R.string.prefId_save_playing_seek), 0);
		
		
		ClientFactory.resetClient(host);
		cc.playFile(new MyNotifiable(), filepath, 1);
		cc.seek(new MyNotifiable(), SeekType.absolute, p);

		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(mId);
		
		cleanSavedFile(this);
	}


	private void exec(Host host){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		if(! pref.getBoolean("setting_follow_me", false)){
			finish();
			return;
		}

		try {
			cc = ClientFactory.getControlClient(new MyNotifiable(), this);
		} catch (WifiStateException e1) {
			e1.printStackTrace();
		}

		int state = initState(host,pref);

		Log.d(this.getClass().getSimpleName(), "state :"+state);

		if((state & HOST_STATE_DIFF) != 0){ //Another host is tagged
			if((state & HOST_STATE_NOTPLAYING) == 0){  //There is something playing on the connected host
				if((state & HOST_STATE_OTHER_NOTPLAYING) != 0){ //There is nothing playing on the tagged hot
					if((state & NOBODY_ON_KINECT) !=0){ //There is nobody on connected host kinect
						//case22-23
						switchPlaying(host,true,10);
			
					}
					else { //There is someone on kinect
						//case20-21
						switchPlaying(host,false,10);
					}
				}
			}
			else{ //There is nothing playing on the connected host
				{
					if((state & NOTHING_ON_REMOTE)== 0) //There is something on the remote
					{
						applySavedFile(pref, host);
					}
				}
			}
		} else { //This host is tagged
			if((state & HOST_STATE_NOTPLAYING) == 0){ //There is something playing on the connected host
				if((state & NOTHING_ON_REMOTE) != 0) { //Nothing on the remote
					//case7
					saveFile(pref);
				} else { //Something on the remote
					//case6
					askForSaveFile(pref);
					return;
				}
			} else{ //There is nothing playing
				if((state & NOTHING_ON_REMOTE) == 0) { //Something on the remote
					//case14
					applySavedFile(pref,host);
				}
			}
		}
		finish();

	}

	private boolean checkKinect(){
		Socket s = new Socket();
		try {
			s.connect(new InetSocketAddress(HostFactory.host.addr, 8420),500);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		BufferedReader i = null;
		try {
			i = new BufferedReader(new InputStreamReader(s.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		String res = "";
		try {
			res = i.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		if("0".equals(res)){
			return false;
		}
		else if(("1").equals(res)){
			return true;
		}
		return false;
	}
	
	private void switchPlaying(Host host,boolean pause,int secondsBack){
		//Stop this from playing //See jsonrpc-ControlClient @120
		String f =  playing.getFilename();
		double p;
		if(playing.getTime()<secondsBack)
			p = 0;
		else
			p = (playing.getTime()-secondsBack)  * 100 / (float)playing.getDuration();
	
		/*boolean isPlaying = playing.isPlaying();
		if(isPlaying && pause)
			cc.pause(new MyNotifiable());*/
		if(pause)
			cc.stop(new MyNotifiable());


		//Then connect to the other //See HomeContoller @160
		Log.i(TAG, "Switching host to " + (host == null ? "<null>" : host.addr) + ".");
		HostFactory.saveHost(this.getApplicationContext(), host);
		Toast.makeText(this.getApplicationContext(), "Changed host to " + host.toString() + ".", Toast.LENGTH_SHORT).show();
		ClientFactory.resetClient(host);


		//launch playURL ? playFile ?
		cc.playFile(new MyNotifiable(), f, 1);
		cc.seek(new MyNotifiable(), SeekType.absolute, p);
	}

	
	public void askForSaveFile(final SharedPreferences pref){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		//builder.setMessage(getString(R.string.Do_you_want_to_add_this_host))
		builder.setMessage("Do you want to save this file ?")
		.setTitle(getString(R.string.tag_nfc));
		builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				saveFile(pref);
				finish();
			}
		});
		builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}	
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private boolean checkIfHostExist(Host host){
		ArrayList<Host> list = HostFactory.getHosts(this);
		for(Host h :list){
			if(h.toLightJson().equals(host.toLightJson()))
				return true;
		}
		return false;
	}

	public static void cleanSavedFile(Context c){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c);
		SharedPreferences.Editor editor = pref.edit();
		editor.remove(c.getString(R.string.prefId_save_playing_file_name));
		editor.remove(c.getString(R.string.prefId_save_playing_seek));
		editor.commit();
	}
	
 	public static class MyNotifiable implements INotifiableManager{

		public void onFinish(DataResponse<?> response) {
			// TODO Auto-generated method stub
			Log.d(this.getClass().getSimpleName(), "debug");
		}

		public void onWrongConnectionState(int state, Command<?> cmd) {
			// TODO Auto-generated method stub
			Log.d(this.getClass().getSimpleName(), "debug");
		}

		public void onError(Exception e) {
			// TODO Auto-generated method stub
			e.printStackTrace();
		}

		public void onMessage(String message) {
			// TODO Auto-generated method stub
			Log.d(this.getClass().getSimpleName(), "debug");
		}

		public void onMessage(int code, String message) {
			// TODO Auto-generated method stub
			Log.d(this.getClass().getSimpleName(), "debug");
		}

		public void retryAll() {
			// TODO Auto-generated method stub
			Log.d(this.getClass().getSimpleName(), "debug");
		}

	}

}
