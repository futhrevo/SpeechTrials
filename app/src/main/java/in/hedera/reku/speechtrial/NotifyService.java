package in.hedera.reku.speechtrial;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

import com.android.internal.telephony.ITelephony;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static android.media.AudioManager.STREAM_NOTIFICATION;
import static in.hedera.reku.speechtrial.R.string.isRunning;
import static in.hedera.reku.speechtrial.actions.IncomingCall.INCOMING_CALL_INTENT_SENDER;
import static in.hedera.reku.speechtrial.actions.IncomingCall.INCOMING_CALL_IS_STARRED;
import static in.hedera.reku.speechtrial.actions.IncomingCall.INCOMING_CALL_NUMBER;
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
    private static final String SILENCE = ". . . ";
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
            String sender = mIntent.getStringExtra(INCOMING_CALL_INTENT_SENDER);
            String num = mIntent.getStringExtra(INCOMING_CALL_NUMBER);
            Boolean isStarred = mIntent.getBooleanExtra(INCOMING_CALL_IS_STARRED, false);

            String smsmessage = settings.getString("pref_call_reject_sms", getString(R.string.pref_call_reject_sms_default));
            String status = settings.getString("pref_call_action", "-1");
            Log.d(TAG, "settings status is "+ status);
            if(status.equals("0") && !isStarred){
                Log.i(TAG, "Not speaking - Not a favorite");
                incomingCallAction(false);
                sendMySMS(num, smsmessage);
                return;
            }
            if(status.equals("1")){
                incomingCallAction(false);
                sendMySMS(num, smsmessage);
                return;
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


                Log.d(TAG, "Phone call from " + sender);
                mTts.speak(SILENCE + "Phone call from " + sender , TextToSpeech.QUEUE_FLUSH, null, CALL_UTTERENCE_ID);
                incomingCallAction(true);
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
                mTts.speak(SILENCE + "SMS from " + sender + " . . . " + message, TextToSpeech.QUEUE_FLUSH, null, SMS_UTTERENCE_ID);
            }

        }

        else if (action.equals(ACTION_SPEAK)){
            synchronized (sLock) {
                String message = mIntent.getStringExtra(INCOMING_SPEAK_MESSAGE);

                Bundle b = new Bundle();
                b.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, STREAM_SYSTEM_STR);
                mTts.speak(SILENCE + message, TextToSpeech.QUEUE_FLUSH, null, SPEAK_UTTERENCE_ID);
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

    @SuppressWarnings("unchecked")
    private void incomingCallAction(boolean bool){
        Log.e(TAG, "incomingCallAction "+ bool );
        ITelephony telephonyService;
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try{
            Class c = Class.forName(telephony.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            telephonyService = (ITelephony)m.invoke(telephony);
            if(bool){
                Log.d(TAG, "Trying to Answer call");
                telephonyService.silenceRinger();
                answercall();
            }else{
                Log.d(TAG, "Trying to disconnect call");
                telephonyService.endCall();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void rejectCall(){

    }

    public void sendMySMS(String phone, String message) {
        Log.d(TAG, "Sensing sms to "+ phone);
        SmsManager sms = SmsManager.getDefault();
        // if message length is too long messages are divided
        List<String> messages = sms.divideMessage(message);
        for (String msg : messages) {

            PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT"), 0);
            PendingIntent deliveredIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED"), 0);
            sms.sendTextMessage(phone, null, msg, sentIntent, deliveredIntent);

        }
    }

    public void answerCall() {

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Log.d(TAG, "Answer the call ====");
                    Runtime.getRuntime().exec("input keyevent " + Integer.toString(KeyEvent.KEYCODE_HEADSETHOOK));
                } catch (IOException e) {
                    Log.e(TAG, "IOException on answerCall ========== ");
                    // Runtime.exec(String) had an I/O problem, try to fall back
                    String enforcedPerm = "android.permission.CALL_PRIVILEGED";
                    Intent btnDown = new Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
                    Intent btnUp = new Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));

                    sendOrderedBroadcast(btnDown, enforcedPerm);
                    sendOrderedBroadcast(btnUp, enforcedPerm);
                }
            }

        }).start();
    }

    public void answercall(){
        if (Build.VERSION.SDK_INT < 21) {
            tryMediaAction();
        }else if(tryMediaController()){
            Log.d(TAG, "AnswerType.MEDIA_CONTROLLER");
        } else {
            tryKeyEvent();
        }
    }

    private void tryMediaAction() {
        Log.d(TAG, "Answering incoming call via media action");
        try {
            Intent intent = new Intent("android.intent.action.MEDIA_BUTTON");
            intent.putExtra("android.intent.extra.KEY_EVENT", new KeyEvent(0, 79));
            sendBroadcast(intent, "android.permission.CALL_PRIVILEGED");
        } catch (Throwable e) {
            e.printStackTrace();
            Log.e(TAG, "Cannot answer the incoming Call via MediaAction");
        }
        try {
            Intent intent = new Intent("android.intent.action.MEDIA_BUTTON");
            intent.putExtra("android.intent.extra.KEY_EVENT", new KeyEvent(1, 79));
            sendBroadcast(intent, "android.permission.CALL_PRIVILEGED");
        } catch (Throwable e2) {
            e2.printStackTrace();
            Log.e(TAG, "Cannot answer the incoming Call via MediaAction");
        }
    }

    private boolean tryMediaController() {
        try {
            for (MediaController mediaController : ((MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE)).getActiveSessions(new ComponentName(this, in.hedera.reku.speechtrial.NotificationListenerService.class))) {
                if ("com.android.server.telecom".equals(mediaController.getPackageName())) {
                    Log.d(TAG, "Sending HEADSETHOOK to telecom server");
                    return mediaController.dispatchMediaButtonEvent(new KeyEvent(1, 79));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    private void tryKeyEvent() {
        Log.d(TAG, "Answering incoming call via input keyevent");
        try {
            Runtime.getRuntime().exec("input keyevent 79");
        } catch (Throwable th) {
            Log.d(TAG, "Cannot answer the incoming Call by KeyEvent");
        }
    }
}
