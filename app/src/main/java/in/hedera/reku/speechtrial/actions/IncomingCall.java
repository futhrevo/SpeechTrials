package in.hedera.reku.speechtrial.actions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import in.hedera.reku.speechtrial.NotifyService;

import static in.hedera.reku.speechtrial.NotifyService.ACTION_PHONE_STATE;

public class IncomingCall extends BroadcastReceiver {
    String sender = "Unknown Number";
    Boolean isStarred = false;
    Context c;
    Intent i;

    public static final String INCOMING_CALL_IS_STARRED = "isStarredSender";
    public static final String INCOMING_CALL_INTENT_SENDER = "sender";
    @Override
    public void onReceive(Context context, Intent intent) {

        c = context;
        i= intent;
        try{
            // TELEPHONY MANAGER class object to register one listner
            TelephonyManager tmgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            //Create Listner
            MyPhoneStateListener PhoneListener = new MyPhoneStateListener();

            // Register listener for LISTEN_CALL_STATE
            tmgr.listen(PhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }catch (Exception e){
            Log.e("Phone Receive Error", " " + e);
        }
    }

    private class MyPhoneStateListener extends PhoneStateListener {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            Log.d("MyPhoneListener",state+"   incoming no:"+incomingNumber);

            if (state == 1) {
                Uri personUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(incomingNumber));

                Cursor cur = c.getContentResolver().query(personUri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.STARRED}, null, null, null);

                if (cur.moveToFirst()) {
                    int nameIndex = cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                    int starIndex = cur.getColumnIndex(ContactsContract.PhoneLookup.STARRED);

                    sender = cur.getString(nameIndex);
                    isStarred = cur.getInt(starIndex) == 1;
                    Log.e("MyPhoneListener", String.valueOf(isStarred));
                }
                cur.close();
                Intent serv = new Intent(c, NotifyService.class);
                serv.setAction(ACTION_PHONE_STATE);
                serv.putExtra(INCOMING_CALL_INTENT_SENDER, sender);
                serv.putExtra(INCOMING_CALL_IS_STARRED, isStarred);
                c.startService(serv);

            }
        }
    }
}
