package in.hedera.reku.speechtrial.actions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;
import android.util.Log;

import in.hedera.reku.speechtrial.NotifyService;

import static in.hedera.reku.speechtrial.NotifyService.ACTION_SMS_RECEIVED;


public class SimpleSmsReceiver extends BroadcastReceiver {

    public static final String INCOMING_SMS_LOCAL_BROADCAST = "incomingSms";
    public static final String INCOMING_SMS_INTENT_SENDER = "sender";
    public static final String INCOMING_SMS_INTENT_MESSAGE = "message";
    public static final String INCOMING_SMS_NUMBER = "number";
    String sender = "Unknown Number";

    private static final String TAG = SimpleSmsReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle pudsBundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String str = "";
        Object[] pdus = (Object[]) pudsBundle.get("pdus");
        msgs = new SmsMessage[pdus.length];
        // For every SMS message received
        for (int i=0; i < msgs.length; i++) {
            // Convert Object array
            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
            // Fetch the text message
            str += msgs[i].getMessageBody().toString();
            // Newline 🙂
            str += "\n";
        }

        Log.i(TAG, str);
        String callMessage = str;
        String number = msgs[0].getOriginatingAddress();
        Uri personUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, number);

        Cursor cur = context.getContentResolver().query(personUri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        if( cur.moveToFirst() ) {
            int nameIndex = cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);

            sender = cur.getString(nameIndex);
        }
        cur.close();

        Intent serv = new Intent(context, NotifyService.class);
        serv.setAction(ACTION_SMS_RECEIVED);
        serv.putExtra(INCOMING_SMS_INTENT_SENDER, sender);
        serv.putExtra(INCOMING_SMS_INTENT_MESSAGE, callMessage);
        serv.putExtra(INCOMING_SMS_NUMBER, number);
        context.startService(serv);
    }
}
