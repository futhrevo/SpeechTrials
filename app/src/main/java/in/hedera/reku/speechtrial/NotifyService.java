package in.hedera.reku.speechtrial;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import static android.media.AudioManager.STREAM_NOTIFICATION;
import static in.hedera.reku.speechtrial.R.string.isRunning;
import static in.hedera.reku.speechtrial.actions.IncomingCall.INCOMING_CALL_INTENT_SENDER;
import static in.hedera.reku.speechtrial.actions.IncomingCall.INCOMING_CALL_IS_STARRED;
import static in.hedera.reku.speechtrial.actions.SimpleSmsReceiver.INCOMING_SMS_INTENT_MESSAGE;
import static in.hedera.reku.speechtrial.actions.SimpleSmsReceiver.INCOMING_SMS_INTENT_SENDER;

public class NotifyService extends Service implements OnInitListener, Runnable{
    public static final String ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE";
    public static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public static final String ACTION_BATTERY_LOW = "android.intent.action.ACTION_BATTERY_LOW";
    public static final String ACTION_SPEAK = "in.hedera.reku.speechtrial.speak";
    public static final String INCOMING_SPEAK_MESSAGE = "speakMessage";
    private static final String TAG = NotifyService.class.getSimpleName();
    private static final String STREAM_SYSTEM_STR = String.valueOf(AudioManager.STREAM_SYSTEM);

    private static final String CALL_UTTERENCE_ID = "call";
    private static final String SMS_UTTERENCE_ID = "sms";
    private static final String SPEAK_UTTERENCE_ID = "speak";
    private static final Object sLock = new Object();
    private TextToSpeech mTts;
    private AudioManager mAudioManager;
    private Intent mIntent;
    private boolean mIsReady = false;
    private int mSysVol;

    public NotifyService() {
    }

    static void start(Context ctx, Intent intent) {
        ctx.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        if (mTts == null) {
            mTts = new TextToSpeech(this, this);
            Log.d(TAG, "TTS created");
        }

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "OnStartCommand");
        mIntent = intent;

        new Thread(this).start();
        // restart in case the Service gets canceled
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onInit(int status) {
        Log.d(TAG, "TTS Initialized");
        mIsReady = true;
        mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d(TAG, "Onstart Speaking");
            }

            @Override
            public void onDone(String utteranceId) {

                Log.d(TAG, "Ondone Speaking");
                resetVolume(utteranceId);
            }

            @Override
            public void onError(String utteranceId) {
                Log.d(TAG, "OnError Speaking");
            }
        });
        synchronized (sLock) {
            sLock.notify();
        }
    }

    @Override
    public void run() {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sharedPref_file_key), Context.MODE_PRIVATE);
        Boolean isStopped = ! sharedPref.getBoolean(getString(isRunning), false);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);


        Log.d(TAG, "run thread");
        while (! mIsReady) {
            synchronized (sLock) {
                try {
                    sLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if(mIntent == null){
            return;
        }
        final String action = mIntent.getAction();

        if (action == null ) {
            return;
        }
        Log.d(TAG, action);
        if (action.equals(ACTION_PHONE_STATE)) {
            if(isStopped){
                Log.i(TAG, "Not speaking - service stopped");
                return;
            }
            Boolean isStarred = mIntent.getBooleanExtra(INCOMING_CALL_IS_STARRED, false);
            String status = settings.getString("pref_call_action", "-1");
            if(status.equals("0") && !isStarred){
                Log.i(TAG, "Not speaking - Not a favorite");
                return;
            }
            if(status.equals(1)){
                rejectIncomingCall();
            }

            final AudioManager am = mAudioManager;
            final int mode = am.getRingerMode();
            if (mode == AudioManager.RINGER_MODE_SILENT ||
                    mode == AudioManager.RINGER_MODE_VIBRATE ) {
                Log.i(TAG, "Not speaking - volume is 0");
                return;
            }


            synchronized (sLock) {
                am.setRingerMode(AudioManager.RINGER_MODE_SILENT);

                String sender = mIntent.getStringExtra(INCOMING_CALL_INTENT_SENDER);
                Log.d(TAG, "Phone call from " + sender);
                mTts.speak("Phone call from " + sender , TextToSpeech.QUEUE_FLUSH, null, CALL_UTTERENCE_ID);
            }

        }

        else if (action.equals(ACTION_SMS_RECEIVED)) {
            final AudioManager am = mAudioManager;
            if(isStopped){
                Log.i(TAG, "Not speaking - service stopped");
                return;
            }
            if (am.getStreamVolume(STREAM_NOTIFICATION) == 0 || isStopped) {
                Log.i(TAG, "Not speaking - volume is 0");
                return;
            }

            synchronized (sLock) {
                String sender = mIntent.getStringExtra(INCOMING_SMS_INTENT_SENDER);
                String message = mIntent.getStringExtra(INCOMING_SMS_INTENT_MESSAGE);

                Bundle b = new Bundle();
                b.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, STREAM_SYSTEM_STR);
                Log.d(TAG, "SMS from " + sender + " . . . " + message);
                mTts.speak("SMS from " + sender + " . . . " + message, TextToSpeech.QUEUE_FLUSH, null, SMS_UTTERENCE_ID);
            }

        }

        else if (action.equals(ACTION_SPEAK)){
            synchronized (sLock) {
                String message = mIntent.getStringExtra(INCOMING_SPEAK_MESSAGE);

                Bundle b = new Bundle();
                b.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, STREAM_SYSTEM_STR);
                mTts.speak(message, TextToSpeech.QUEUE_FLUSH, null, SPEAK_UTTERENCE_ID);
            }
        }
    }

    private void resetVolume(String utteranceId) {
        synchronized (sLock) {

            if (utteranceId.equals(CALL_UTTERENCE_ID)) {
                Log.d(TAG, "resetting call volume");
                mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }

            else if (utteranceId.equals(SMS_UTTERENCE_ID)) {
                Log.d(TAG, "resetting sms volume");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mTts != null) {
            mTts.shutdown();
            mTts = null;
        }
    }

    private void rejectIncomingCall(){


    }
}
