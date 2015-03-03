package org.xbmc.android.util;

import java.io.IOException;
import java.nio.charset.Charset;

import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.util.Log;

public class NFCHelper {
	private static final String TAG = NFCHelper.class.getSimpleName();
	public static String mimeSuffixe = "kodinfc";
	public static String NFCmimeType = "app/"+mimeSuffixe;
	
	public static boolean writeTag(Context context, Tag tag, byte[] data) {
		Log.v(TAG, "Debut de writeTag");
	    // Record to launch Play Store if app is not installed
	    NdefRecord appRecord = NdefRecord.createApplicationRecord(context.getPackageName());
	    //appRecord = NdefRecord.createUri(data);
	 
	    // Record with actual data we care about
	    NdefRecord relayRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
	                                            new String(NFCmimeType).getBytes(Charset.forName("UTF8")),
	                                            null, data);
	    // Complete NDEF message with both records
	    NdefMessage message = new NdefMessage(new NdefRecord[] {relayRecord/*, appRecord*/});
	    
	 
	    try {
	        // If the tag is already formatted, just write the message to it
	        Ndef ndef = Ndef.get(tag);
	        if(ndef != null) {
	        	Log.v(TAG, "Tag already formated");
	            ndef.connect();
	 
	            // Make sure the tag is writable
	            if(!ndef.isWritable()) {
	            	Log.e(TAG, "The tag isn't writable");
	                //DialogUtils.displayErrorDialog(context, R.string.nfcReadOnlyErrorTitle, R.string.nfcReadOnlyError);
	                return false;
	            }
	 
	            // Check if there's enough space on the tag for the message
	            int size = message.toByteArray().length;
	            if(ndef.getMaxSize() < size) {
	            	Log.e(TAG, "There is not enough space");
	                //DialogUtils.displayErrorDialog(context, R.string.nfcBadSpaceErrorTitle, R.string.nfcBadSpaceError);
	                return false;
	            }
	 
	            try {
	                // Write the data to the tag
	                ndef.writeNdefMessage(message);
	                
	                //DialogUtils.displayInfoDialog(context, R.string.nfcWrittenTitle, R.string.nfcWritten);
	                return true;
	            } catch (TagLostException tle) {
	            	Log.e(TAG, "TagLostException");
	                //DialogUtils.displayErrorDialog(context, R.string.nfcTagLostErrorTitle, R.string.nfcTagLostError);
	                return false;
	            } catch (IOException ioe) {
	            	Log.e(TAG, "IOException");
	                //DialogUtils.displayErrorDialog(context, R.string.nfcFormattingErrorTitle, R.string.nfcFormattingError);
	                return false;
	            } catch (FormatException fe) {
	            	Log.e(TAG, "FormatException");
	                //DialogUtils.displayErrorDialog(context, R.string.nfcFormattingErrorTitle, R.string.nfcFormattingError);
	                return false;
	            }
	        // If the tag is not formatted, format it with the message
	        } else {
	            NdefFormatable format = NdefFormatable.get(tag);
	            if(format != null) {
	                try {
	                    format.connect();
	                    format.format(message);
	                    Log.v(TAG, "Tag not formatted");
	                    //DialogUtils.displayInfoDialog(context, R.string.nfcWrittenTitle, R.string.nfcWritten);
	                    return true;
		            } catch (TagLostException tle) {
		            	Log.e(TAG, "TagLostException");
		                //DialogUtils.displayErrorDialog(context, R.string.nfcTagLostErrorTitle, R.string.nfcTagLostError);
		                return false;
		            } catch (IOException ioe) {
		            	Log.e(TAG, "IOException");
		                //DialogUtils.displayErrorDialog(context, R.string.nfcFormattingErrorTitle, R.string.nfcFormattingError);
		                return false;
		            } catch (FormatException fe) {
		            	Log.e(TAG, "FormatException");
		                //DialogUtils.displayErrorDialog(context, R.string.nfcFormattingErrorTitle, R.string.nfcFormattingError);
		                return false;
		            }
	            } else {
	            	Log.e(TAG, "nfcNoNdefError");
	                //DialogUtils.displayErrorDialog(context, R.string.nfcNoNdefErrorTitle, R.string.nfcNoNdefError);
	                return false;
	            }
	        }
	    } catch(Exception e) {
	    	Log.e(TAG, "Unknow error");
	        //DialogUtils.displayErrorDialog(context, R.string.nfcUnknownErrorTitle, R.string.nfcUnknownError);
	    }
	 
	    return false;
	}
}
