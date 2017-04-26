package in.hedera.reku.speechtrial;

import android.Manifest;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
import static in.hedera.reku.speechtrial.NotifyService.ACTION_SPEAK;
import static in.hedera.reku.speechtrial.NotifyService.INCOMING_SPEAK_MESSAGE;
import static in.hedera.reku.speechtrial.speech.activation.SpeechActivationService.ACTIVATION_RESULT_INTENT_KEY;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private BluetoothControllerImpl bluetoothController;
    private int activitiesCount;
    private boolean sco = false;
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private static final int PERMISSIONS_REQUEST_ALL_PERMISSIONS = 101;
    private AudioManager audioManager;
    private AlertDialog enableNotificationListenerAlertDialog;

    public static final int ST_Notification_ID = 912;

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.app_name));
        }

        speechLayout = findViewById(R.id.speechtotext);
        voiceLayout = findViewById(R.id.voicecall);
        expLayout = findViewById(R.id.experimental);


//        ToggleButton togglecvd = (ToggleButton) findViewById(R.id.toggleCVD);
//        togglecvd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if(isChecked){
//                    startService(SpeechActivationService.makeStartServiceIntent(getApplicationContext()));
//                }else{
//                    startService(SpeechActivationService.makeStopServiceIntent(getApplicationContext()));
//                }
//
//            }
//        });
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
        audioManager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        showNotification();

        // If the user did not turn the notification listener service on we prompt him to do so
        if(!isNotificationServiceEnabled()){
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
            enableNotificationListenerAlertDialog.show();
        }
        Intent serv = new Intent(this, NotifyService.class);
        startService(serv);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if(sco){
            bluetoothController.stop();
            sco = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        Boolean isScoEnabled = PreferenceManager
                .getDefaultSharedPreferences(this).getBoolean("pref_sco_key", false);


        Log.i(TAG, "SCO is set to " + String.valueOf(isScoEnabled));
        if(bluetoothController != null){
            if(isScoEnabled){
                bluetoothController.start();
                sco = true;
            }else{
                if(sco){
                    bluetoothController.stop();
                    sco = false;
                }
            }
        }

    }
    @Override
    protected void onStop() {
        super.onStop();
//        bluetoothController.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Configure the search info and add any event listeners...

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                Intent intent = new Intent(this,SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_speak:
                startVoiceInput();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }

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
        BluetoothAdapter btAdapter = bluetoothController.getAdapter();
        BluetoothHeadset btHeadset = bluetoothController.getHeadset();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if(btAdapter.isEnabled())
        {
            for (BluetoothDevice tryDevice : pairedDevices)
            {
                //This loop tries to start VoiceRecognition mode on every paired device until it finds one that works(which will be the currently in use bluetooth headset)
                if (btHeadset.startVoiceRecognition(tryDevice))
                {
                    break;
                }
            }
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
//        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
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
//                    BluetoothAdapter btAdapter = bluetoothController.getAdapter();
//                    BluetoothHeadset btHeadset = bluetoothController.getHeadset();
//                    Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
//
//                    if(btAdapter.isEnabled())
//                    {
//                        for (BluetoothDevice tryDevice : pairedDevices)
//                        {
//                            //This loop tries to start VoiceRecognition mode on every paired device until it finds one that works(which will be the currently in use bluetooth headset)
//                            if (btHeadset.stopVoiceRecognition(tryDevice))
//                            {
//                                break;
//                            }
//                        }
//                    }

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
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED;
    }


    private void requestPermissions() {
        Log.d(TAG, "requestPermissions: ");
        String[] permissions = new String[] {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE
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

    private void showNotification(){
        Intent stopintent = new Intent(getApplication(), ActionsControlIntentService.class).setAction(ActionsControlIntentService.START_RECEIVERS);
        startService(stopintent);
    }

    /**
     * Is Notification Service Enabled.
     * Verifies if the notification listener service is enabled.
     * Got it from: https://github.com/kpbird/NotificationListenerService-Example/blob/master/NLSExample/src/main/java/com/kpbird/nlsexample/NLService.java
     * @return True if eanbled, false otherwise.
     */
    private boolean isNotificationServiceEnabled(){
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Build Notification Listener Alert Dialog.
     * Builds the alert dialog that pops up if the user has not turned
     * the Notification Listener Service on yet.
     * @return An alert dialog which leads to the notification enabling screen
     */
    private AlertDialog buildNotificationServiceAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.notification_listener_service);
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation);
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return(alertDialogBuilder.create());
    }
}
