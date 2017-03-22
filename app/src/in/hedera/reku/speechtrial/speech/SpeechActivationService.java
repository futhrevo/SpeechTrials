package in.hedera.reku.speechtrial.speech;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Persistently run a speech activator in the background.
 * Created by rakeshkalyankar on 18/03/17.
 */

public class SpeechActivationService extends Service implements SpeechActivationListener{

    private static final String TAG = SpeechActivationService.class.getSimpleName();
    private boolean isStarted;
    private SpeechActivator activator;

    /**
     * send this when external code wants the Service to stop
     */
    public static final String ACTIVATION_STOP_INTENT_KEY =
            "ACTIVATION_STOP_INTENT_KEY";
    public static final String ACTIVATION_START_INTENT_KEY =
            "ACTIVATION_START_INTENT_KEY";
    public static final String ACTIVATION_RESULT_INTENT_KEY =
            "ACTIVATION_RESULT_INTENT_KEY";
    public static final String ACTIVATION_RESULT_BROADCAST_NAME =
            "in.hedera.reku.speechtrial.speech.ACTIVATION";
    public static final int NOTIFICATION_ID = 19989;

    public static Intent makeStartServiceIntent(Context context){
        Intent i = new Intent(context, SpeechActivationService.class);
        i.putExtra(ACTIVATION_START_INTENT_KEY, true);
        return i;
    }
    public static Intent makeStopServiceIntent(Context context){
        Intent i = new Intent(context, SpeechActivationService.class);
        i.putExtra(ACTIVATION_STOP_INTENT_KEY, true);
        return i;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        isStarted = false;
    }

    /**
     * stop or start an activator based on the activator type and if an
     * activator is currently running
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            if(intent.hasExtra(ACTIVATION_STOP_INTENT_KEY)){
                Log.d(TAG, "stop service intent");
                activated(false);
            }else{
                if(isStarted){
                    Log.d(TAG, "already started this type");
                }else{
                    // activator not started, start it
                    startDetecting();
                }
            }
        }
        // restart in case the Service gets canceled
        return START_REDELIVER_INTENT;
    }

    private void startDetecting(){
        activator = new WordActivator(this, this, "helmet");
        isStarted = true;
        activator.detectActivation();
        startForeground(NOTIFICATION_ID, getNotification());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "On destroy");
        super.onDestroy();
        stopActivator();
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void activated(boolean success) {
        // make sure the activator is stopped before doing anything else
        stopActivator();

        // broadcast result
        Intent intent = new Intent(ACTIVATION_RESULT_BROADCAST_NAME);
        intent.putExtra(ACTIVATION_RESULT_INTENT_KEY, success);
        sendBroadcast(intent);

        // always stop after receive an activation
        stopSelf();
    }

    private void stopActivator() {
        if(activator != null){
            Log.d(TAG, "stopped: " + activator.getClass().getSimpleName());
            activator.stop();
            isStarted = false;
        }
    }


    private Notification getNotification(){
        // determine label based on the class
        String name = "Speech Activator";
        String message = "Detecting in background";
        String title = "Speech Title";
        PendingIntent pi = PendingIntent.getService(this, 0, makeStopServiceIntent(this), 0);
        Notification notification;

        Notification.Builder builder = new Notification.Builder(this);
        builder.setWhen(System.currentTimeMillis()).setTicker(message)
                .setContentTitle(title).setContentText(message)
                .setContentIntent(pi);
        notification = builder.build();

        return notification;
    }
}
