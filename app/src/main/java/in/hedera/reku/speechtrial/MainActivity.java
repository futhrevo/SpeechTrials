package in.hedera.reku.speechtrial;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BluetoothControllerImpl bluetoothController;
    private int activitiesCount;
    private boolean sco = false;
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
        bluetoothController = new BluetoothControllerImpl(this);
//        bluetoothController.start();
        TTS.init(getApplicationContext());
    }

    @Override
    protected void onStop() {
        super.onStop();
//        bluetoothController.stop();
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
}
