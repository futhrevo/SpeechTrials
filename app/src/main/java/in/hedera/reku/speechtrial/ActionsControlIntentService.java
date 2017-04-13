package in.hedera.reku.speechtrial;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import static in.hedera.reku.speechtrial.MainActivity.ST_Notification_ID;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class ActionsControlIntentService extends IntentService {

    public static final String TAG = ActionsControlIntentService.class.getSimpleName();

    public static final String STOP_RECEIVERS = "in.hedera.reku.speechtrial.stopReceivers";
    public static final String START_RECEIVERS = "in.hedera.reku.speechtrial.startReceivers";


    public ActionsControlIntentService() {
        super("ActionsControlIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "Received notification action: " + action);
            if (START_RECEIVERS.equals(action)) {
                handleActionStart();
            } else if (STOP_RECEIVERS.equals(action)) {
                handleActionStop();
            }
        }
    }


    private void handleActionStop() {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sharedPref_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.isRunning), false);
        editor.commit();
        testValue();
        showStartNotification();
    }

    private void handleActionStart() {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sharedPref_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.isRunning), true);
        editor.commit();
        testValue();
        showStopNotification();
    }

    private void testValue(){
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sharedPref_file_key), Context.MODE_PRIVATE);
        Boolean bool = sharedPref.getBoolean(getString(R.string.isRunning), false);
        Log.d(TAG, String.valueOf(bool));
    }


    private void showStartNotification(){

        Intent startintent = new Intent(getBaseContext(), ActionsControlIntentService.class).setAction(ActionsControlIntentService.START_RECEIVERS);

        PendingIntent pIntent = PendingIntent.getService(getBaseContext(), 0, startintent, 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle("SpeechTrial")
                        .setContentText("Stopped")
                        .addAction(R.drawable.ic_stop_black_24dp, "START", pIntent)
                        .setOngoing(true);


        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // mId allows you to update the notification later on.
        mNotificationManager.notify(ST_Notification_ID, mBuilder.build());

    }

    private void showStopNotification(){
        Intent startintent = new Intent(getBaseContext(), ActionsControlIntentService.class).setAction(ActionsControlIntentService.STOP_RECEIVERS);

        PendingIntent pIntent = PendingIntent.getService(getBaseContext(), 0, startintent, 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle("SpeechTrial")
                        .setContentText("Running ...")
                        .addAction(R.drawable.ic_stop_black_24dp, "STOP", pIntent)
                        .setOngoing(true);


        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // mId allows you to update the notification later on.
        mNotificationManager.notify(ST_Notification_ID, mBuilder.build());
    }
}
