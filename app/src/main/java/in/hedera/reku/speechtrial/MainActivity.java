package in.hedera.reku.speechtrial;

import android.Manifest;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Locale;

import in.hedera.reku.speechtrial.speech.SpeechActivationService;

import static in.hedera.reku.speechtrial.NotifyService.ACTION_SPEAK;
import static in.hedera.reku.speechtrial.NotifyService.INCOMING_SPEAK_MESSAGE;
import static in.hedera.reku.speechtrial.speech.SpeechActivationService.ACTIVATION_RESULT_BROADCAST_NAME;
import static in.hedera.reku.speechtrial.speech.SpeechActivationService.ACTIVATION_RESULT_INTENT_KEY;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BluetoothControllerImpl bluetoothController;
    private int activitiesCount;
    private boolean sco = false;
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private static final int PERMISSIONS_REQUEST_ALL_PERMISSIONS = 101;
    private AudioManager audioManager;

    private View speechLayout;
    private View voiceLayout;
    private View expLayout;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    speechLayout.setVisibility(View.VISIBLE);
                    voiceLayout.setVisibility(View.INVISIBLE);
                    expLayout.setVisibility(View.INVISIBLE);
                    return true;
                case R.id.navigation_dashboard:
                    speechLayout.setVisibility(View.INVISIBLE);
                    voiceLayout.setVisibility(View.VISIBLE);
                    expLayout.setVisibility(View.INVISIBLE);
                    return true;
                case R.id.navigation_notifications:
                    speechLayout.setVisibility(View.INVISIBLE);
                    voiceLayout.setVisibility(View.INVISIBLE);
                    expLayout.setVisibility(View.VISIBLE);
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speechLayout = findViewById(R.id.speechtotext);
        voiceLayout = findViewById(R.id.voicecall);
        expLayout = findViewById(R.id.experimental);
        ToggleButton togglesco = (ToggleButton) findViewById(R.id.toggleSCO);
        togglesco.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked && !sco){
                    bluetoothController.start();
                    sco = true;
                }else{
                    if(sco){
                        bluetoothController.stop();
                        sco = false;
                    }

                }
            }
        });

        ToggleButton togglecvd = (ToggleButton) findViewById(R.id.toggleCVD);
        togglecvd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    startService(SpeechActivationService.makeStartServiceIntent(getApplicationContext()));
                }else{
                    startService(SpeechActivationService.makeStopServiceIntent(getApplicationContext()));
                }

            }
        });
        Button speechInputButton = (Button) speechLayout.findViewById(R.id.speakbutton);
        speechInputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceInput();
            }
        });
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        Button testButton = (Button) findViewById(R.id.button);

        checkDNDsettings();
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TTSspeak("Hello World");
            }
        });
        if (needPermissions(this)) {
            requestPermissions();
        }
        bluetoothController = new BluetoothControllerImpl(this);
//        bluetoothController.start();
        audioManager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    protected void onResume() {
        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(this).registerReceiver(activationBroadcastReceiver, new IntentFilter(ACTIVATION_RESULT_BROADCAST_NAME));
        super.onResume();
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is paused.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(activationBroadcastReceiver);
        super.onPause();
    }


    @Override
    protected void onStop() {
        super.onStop();
//        bluetoothController.stop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "New intent received");
        if(intent.hasExtra("sms")){
            Log.d(TAG, "New SMS Received");
        }
        if(intent.hasExtra("incomingcall")){
            Log.d(TAG, "New Call received");
        }
    }
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSIONS_REQUEST_ALL_PERMISSIONS:
                boolean hasAllPermissions = true;
                for (int i = 0; i < grantResults.length; ++i) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        hasAllPermissions = false;
                        Log.e(TAG, "Unable to get permission " + permissions[i]);
                    }
                }
                if (hasAllPermissions) {
                    Log.d(TAG, "All permissions granted");
                    finish();
                } else {
                    Toast.makeText(this,
                            "Unable to get all required permissions", Toast.LENGTH_LONG).show();
                    TextView recognisedView = (TextView) speechLayout.findViewById(R.id.recognisedtextView);
                    Log.i(TAG, "Permission has been denied by user");
                    recognisedView.setText("Cannot continue without microphone permission");
                    finish();
                    return;
                }
                break;
            default:
                Log.e(TAG, "Unexpected request code");

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        TextView recognisedView = (TextView) speechLayout.findViewById(R.id.recognisedtextView);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    recognisedView.setText(result.get(0));
                    TTSspeak(result.get(0));
                }
                break;
            }


        }
    }


    static public boolean needPermissions(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ;
    }

    private void requestPermissions() {
        Log.d(TAG, "requestPermissions: ");
        String[] permissions = new String[] {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE
        };
        ActivityCompat.requestPermissions(this ,permissions, PERMISSIONS_REQUEST_ALL_PERMISSIONS);
    }

    private void checkDNDsettings(){
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !notificationManager.isNotificationPolicyAccessGranted()) {

            Intent intent = new Intent(
                    android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            startActivity(intent);
        }
    }
    private class BluetoothControllerImpl extends BluetoothController {

        /**
         * Constructor
         *
         * @param context
         */
        public BluetoothControllerImpl(Context context) {
            super(context);
        }

        @Override
        public void onHeadsetDisconnected() {
            Log.d(TAG, "Bluetooth headset disconnected");
        }

        @Override
        public void onHeadsetConnected() {
            Log.d(TAG, "Bluetooth headset connected");
        }

        @Override
        public void onScoAudioDisconnected() {
            Log.d(TAG, "Bluetooth sco audio finished");
            bluetoothController.stop();

        }

        @Override
        public void onScoAudioConnected() {
            Log.d(TAG, "Bluetooth sco audio started");
        }
    }

    // Initialize a new BroadcastReceiver instance
    private BroadcastReceiver activationBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "New activation broadcast");
            // Get the received random number
             boolean result = intent.getBooleanExtra(ACTIVATION_RESULT_INTENT_KEY, false);

            if(result){
                TTSspeak("Welcome to SpeechTrial . . . Local weather is 32 degrees celsius . . . Speak any command");
            }
        }
    };


    private void TTSspeak(String string){
        Log.d(TAG, string);
            Intent speakintent = new Intent(getApplication(), NotifyService.class);
            speakintent.setAction(ACTION_SPEAK);
            speakintent.putExtra(INCOMING_SPEAK_MESSAGE, string);
            startService(speakintent);
        }
}
