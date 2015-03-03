package org.xbmc.android.remote.presentation.activity;

import java.util.ArrayList;

import org.xbmc.android.util.HostFactory;
import org.xbmc.android.util.NFCHelper;
import org.xbmc.api.object.Host;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class NFCIntentActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = this.getIntent();
		String action = intent.getAction();
		Log.v(this.getClass().getSimpleName(), "resolveIntent : "+intent.getAction().toString());
		Log.v(this.getClass().getSimpleName(), "Lecture d'un intent de type "+intent.getType());
		
		String res = new String();
		
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)){
			if (intent.getType() !=null &&  intent.getType().equals(NFCHelper.NFCmimeType));
			{
		        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		        NdefRecord relayRecord = ((NdefMessage)rawMsgs[0]).getRecords()[0];
		        final Host host = HostFactory.getHostFromCompressedLightJson(relayRecord.getPayload());
		        String nfcData = host.toJson();
		        Log.d(this.getClass().getName(), "Message lu : "+nfcData);
		        
		        if(!checkIfHostExist(host))
		        {
		        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        	builder.setMessage("Do you want to add this host ?")
		        			.setTitle("Tag NFC");
		        	builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									HostFactory.addHost(getBaseContext(), host);
									Toast.makeText(getBaseContext(), "New host, adding", Toast.LENGTH_SHORT).show();
									finish();
								}
							});
		        	builder.setNegativeButton("Ignore", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							finish();
						}	
					});
		        	AlertDialog dialog = builder.create();
		        	dialog.show();
		        }
		        else
		        {
		        	finish();
		        	
		        }

		        //Check kinect
		        
		        //Stop this from playing //See jsonrpc-ControlClient @120
		        //Then connect to the other //See HomeContoller @160
		        //launch playURL ? playFile ?
		        
			}
		
		}
		
		//finish();
	}
	
	private boolean checkIfHostExist(Host host){
		ArrayList<Host> list = HostFactory.getHosts(this);
		for(Host h :list){
			if(h.toLightJson().equals(host.toLightJson()))
				return true;
		}
		return false;
	}
	
	
}
