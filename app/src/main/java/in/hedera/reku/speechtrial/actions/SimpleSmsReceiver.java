package in.hedera.reku.speechtrial.actions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;


public class SimpleSmsReceiver extends BroadcastReceiver {

    public static final String INCOMING_SMS_LOCAL_BROADCAST = "incomingSms";

    private static final String TAG = SimpleSmsReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle pudsBundle = intent.getExtras();
        Object[] pdus = (Object[]) pudsBundle.get("pdus");
        SmsMessage messages = SmsMessage.createFromPdu((byte[]) pdus[0]);
        Log.i(TAG, messages.getMessageBody());
        String callMessage = messages.getMessageBody();
        String sender = messages.getDisplayOriginatingAddress();

        Uri personUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, messages.getOriginatingAddress());

        Cursor cur = context.getContentResolver().query(personUri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        if( cur.moveToFirst() ) {
            int nameIndex = cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);

            sender = cur.getString(nameIndex);
        }
        cur.close();

        Toast.makeText(context, "SMS Received : "+messages.getMessageBody(),
                Toast.LENGTH_LONG) .show();

        Intent intentLocal = new Intent(INCOMING_SMS_LOCAL_BROADCAST);
        intentLocal.putExtra("sender", sender);
        intentLocal.putExtra("message", callMessage);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intentLocal);
    }
}
