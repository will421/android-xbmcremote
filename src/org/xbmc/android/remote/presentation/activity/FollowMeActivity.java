package org.xbmc.android.remote.presentation.activity;

import java.util.ArrayList;
import java.util.HashMap;

import javax.jmdns.impl.HostInfo;

import org.xbmc.android.remote.R;
import org.xbmc.android.remote.business.provider.HostProvider.Hosts;
import org.xbmc.android.util.HostFactory;
import org.xbmc.android.util.NFCHelper;
import org.xbmc.api.object.Host;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.database.DataSetObserver;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.Toast;
import android.nfc.NfcAdapter;


public class FollowMeActivity extends Activity{
	private boolean writing = false;
	protected NfcAdapter mNfcAdapter;
	private PendingIntent pendingIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.followme);
		
		Button writeButton = (Button) findViewById(R.id.buttonWriteNfc);
		writeButton.setOnTouchListener(new OnTouchListener() {
			
			public boolean onTouch(View v, MotionEvent event) {
				writing = true;
				return false;
			}
		});
		
		//Toast.makeText(this, getPackageName(), Toast.LENGTH_SHORT).show();
		this.mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
			Toast.makeText(this, "no nfc", Toast.LENGTH_SHORT).show();
		}
		
		pendingIntent = PendingIntent.getActivity(
			    this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		ndef = new IntentFilter();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mNfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
		
	}
	
	public void onPause() {
	    super.onPause();
	    mNfcAdapter.disableForegroundDispatch(this);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		String action = intent.getAction();
		Log.v(this.getClass().getSimpleName(), "resolveIntent : "+intent.getAction().toString());
		Log.v(this.getClass().getSimpleName(), "Lecture d'un intent de type "+intent.getType());
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)){
			if(writing){
				Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
				byte[] nfcMessage;
				nfcMessage =HostFactory.host.toCompressedLightJson();
				NFCHelper.writeTag(this, tag, nfcMessage);
				Toast.makeText(this, "Writed", Toast.LENGTH_SHORT).show();
				writing = false;
			}
			else {
				
			}
		}
		super.onNewIntent(intent);
	}
}
