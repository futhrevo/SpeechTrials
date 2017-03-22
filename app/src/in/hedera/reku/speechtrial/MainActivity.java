package in.hedera.reku.speechtrial;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Locale;

import in.hedera.reku.speechtrial.audio.AudioDataSaver;
import in.hedera.reku.speechtrial.audio.MsgEnum;
import in.hedera.reku.speechtrial.audio.RecordingThread;
import in.hedera.reku.speechtrial.utils.AppResCopy;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BluetoothControllerImpl bluetoothController;
    private HotwordDetectionThread hotwordDetectionThread;

    private RecordingThread recordingThread;
    private boolean sco = false;
    private boolean isListeningForActivation = false;
    private static long activeTimes = 0;

    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private static final int RECORD_REQUEST_CODE = 101;

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
        Log.d(TAG, "onCreate");
        Log.d(TAG, getIntent().toString());
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
//                    startService(SpeechActivationService.makeStartServiceIntent(getApplicationContext()));
                    recordingThread.startRecording();
                }else{
                    recordingThread.stopRecording();
//                    startService(SpeechActivationService.makeStopServiceIntent(getApplicationContext()));
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
        checkAudioRecordPermission();
        if(!Utils.isSpeechAvailable(this)){
            Log.e(TAG, "Speech not available");
        }
        bluetoothController = new BluetoothControllerImpl(this);
//        bluetoothController.start();
        TTS.init(getApplicationContext());
        AppResCopy.copyResFromAssetsToSD(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(hotwordDetectedReciever,new IntentFilter(HotwordDetectionThread.BROADCAST_TAG));
        activeTimes = 0;
        recordingThread = new RecordingThread(handle, new AudioDataSaver());

//        startHotwordDetectorIfNotStarted();
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

    @Override
    protected void onStop() {
        super.onStop();
//        bluetoothController.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hotwordDetectedReciever);
    }

    private void startHotwordDetectorIfNotStarted(){
        Log.d(TAG, "starting hotword detector if not started");
        hotwordDetectionThread = new HotwordDetectionThread(getApplicationContext());
        hotwordDetectionThread.start();
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
            case RECORD_REQUEST_CODE:
                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {
                    TextView recognisedView = (TextView) speechLayout.findViewById(R.id.recognisedtextView);
                    Log.i(TAG, "Permission has been denied by user");
                    recognisedView.setText("Cannot continue without microphone permission");
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                }

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
                    TTS.speak(result.get(0));
                }
                break;
            }


        }
    }

    protected void checkAudioRecordPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_REQUEST_CODE);
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

    BroadcastReceiver hotwordDetectedReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "hotwordDetectedReciever");
//                hotwordDetectionThread.cancel();

            Intent recIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            recIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, MainActivity.this.getPackageName());
            try {
                startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
            } catch (ActivityNotFoundException a) {

            }
//            speechRecognizer.startListening(recIntent);
        }
    };

    public Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MsgEnum message = MsgEnum.getMsgEnum(msg.what);
            switch(message) {
                case MSG_ACTIVE:
                    activeTimes++;
                    Log.d(TAG," ----> Detected " + activeTimes + " times"+ "green");
                    // Toast.makeText(Demo.this, "Active "+activeTimes, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_INFO:
                    Log.d(TAG," ----> "+message);
                    break;
                case MSG_VAD_SPEECH:
                    Log.d(TAG," ----> normal voice blue");
                    break;
                case MSG_VAD_NOSPEECH:
                    Log.d(TAG," ----> no speech blue");
                    break;
                case MSG_ERROR:
                    Log.d(TAG," ----> " + msg.toString()+ "red");
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };
}
