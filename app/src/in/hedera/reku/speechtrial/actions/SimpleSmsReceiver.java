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
import android.widget.Toast;

import in.hedera.reku.speechtrial.MainActivity;


public class SimpleSmsReceiver extends BroadcastReceiver {

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
        Intent i = new Intent(context,MainActivity.class);
        i.putExtra("sms", callMessage);
        i.putExtra("sender", sender);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
