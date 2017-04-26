package in.hedera.reku.speechtrial;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

import java.util.Set;

/**
 * Created by rakeshkalyankar on 15/03/17.
 */

public abstract class BluetoothController {
    private Context mContext;

    private BluetoothAdapter mBluetoothAdapter;

    private AudioManager mAudioManager;

    private boolean mIsCountDownOn;
    private boolean mIsStarting;
    private boolean mIsOnHeadsetSco;
    private boolean mIsStarted;

    private static final String TAG = "BluetoothController";
    private BluetoothHeadset btHeadset;

    /**
     * Constructor
     *
     * @param context
     */
    public BluetoothController(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int profile, BluetoothProfile proxy)
            {
                if (profile == BluetoothProfile.HEADSET)
                {
                    btHeadset = (BluetoothHeadset) proxy;
                }
            }
            public void onServiceDisconnected(int profile)
            {
                if (profile == BluetoothProfile.HEADSET) {
                    btHeadset = null;
                }
            }
        };
        mBluetoothAdapter.getProfileProxy(context, mProfileListener, BluetoothProfile.HEADSET);
    }

    /**
     * Call this to start BluetoothController functionalities.
     *
     * @return The return value of startBluetooth()
     */
    public boolean start() {
        if (!mIsStarted) {
            mIsStarted = true;

            mIsStarted = startBluetooth();
        }

        return mIsStarted;
    }
    /**
     * Should call this on onResume or onDestroy.
     * Unregister broadcast receivers and stop Sco audio connection
     * and cancel count down.
     */
    public void stop() {
        if (mIsStarted) {
            mIsStarted = false;

            stopBluetooth();
        }
    }

    public BluetoothHeadset getHeadset(){
        return btHeadset;
    }

    public BluetoothAdapter getAdapter(){
        return mBluetoothAdapter;
    }
    public abstract void onHeadsetDisconnected();

    public abstract void onHeadsetConnected();

    public abstract void onScoAudioDisconnected();

    public abstract void onScoAudioConnected();

    private boolean startBluetooth(){
        Log.d(TAG, "startBluetooth");
        // Device support bluetooth
        if (mBluetoothAdapter != null) {
            if(mAudioManager.isBluetoothScoAvailableOffCall()){
                mContext.registerReceiver(mBroadcastReceiver,
                        new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
                mContext.registerReceiver(mBroadcastReceiver,
                        new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
                mContext.registerReceiver(mBroadcastReceiver,
                        new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
                // Need to set audio mode to MODE_IN_CALL for call to startBluetoothSco() to succeed.
                mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

                mIsCountDownOn = true;
                // mCountDown repeatedly tries to start bluetooth Sco audio connection.
                mCountDown.start();

                // need for audio sco, see mBroadcastReceiver
                mIsStarting = true;

                return true;
            }
        }
        return false;
    }

    /**
     * Unregister broadcast receivers and stop Sco audio connection
     * and cancel count down.
     */
    private void stopBluetooth() {
        Log.d(TAG, "stopBluetooth");

        if (mIsCountDownOn) {
            mIsCountDownOn = false;
            mCountDown.cancel();
        }
        // Need to stop Sco audio connection here when the app
        // change orientation or close with headset still turns on.
        mContext.unregisterReceiver(mBroadcastReceiver);
        mAudioManager.stopBluetoothSco();
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
    }

    /**
     * Handle headset and Sco audio connection states.
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    BluetoothDevice mConnectedHeadset = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    BluetoothClass bluetoothClass = mConnectedHeadset.getBluetoothClass();
                    if (bluetoothClass != null) {
                        // Check if device is a headset. Besides the 2 below, are there other
                        // device classes also qualified as headset?
                        int deviceClass = bluetoothClass.getDeviceClass();
                        if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
                                || deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
                            // start bluetooth Sco audio connection.
                            // Calling startBluetoothSco() always returns faIL here,
                            // that why a count down timer is implemented to call
                            // startBluetoothSco() in the onTick.
                            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                            mIsCountDownOn = true;
                            mCountDown.start();

                            // override this if you want to do other thing when the device is connected.
                            onHeadsetConnected();
                        }
                    }
                    Log.i(TAG, mConnectedHeadset.getName() + " connected");
                    break;

                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.d(TAG, "Headset disconnected");
                    if (mIsCountDownOn) {
                        mIsCountDownOn = false;
                        mCountDown.cancel();
                    }
                    mAudioManager.setMode(AudioManager.MODE_NORMAL);
                    // override this if you want to do other thing when the device is disconnected.
                    onHeadsetDisconnected();
                    break;

                case AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED:
                    int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE,
                            AudioManager.SCO_AUDIO_STATE_ERROR);
                    switch (state){
                        case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                            mIsOnHeadsetSco = true;
                            if (mIsStarting) {
                                // When the device is connected before the application starts,
                                // ACTION_ACL_CONNECTED will not be received, so call onHeadsetConnected here
                                mIsStarting = false;
                                onHeadsetConnected();
                            }
                            if (mIsCountDownOn) {
                                mIsCountDownOn = false;
                                mCountDown.cancel();
                            }
                            // override this if you want to do other thing when Sco audio is connected.
                            onScoAudioConnected();

                            Log.i(TAG, "Sco connected");
                            break;
                        case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                            Log.i(TAG, "Sco disconnected");

                            // Always receive SCO_AUDIO_STATE_DISCONNECTED on call to startBluetooth()
                            // which at that stage we do not want to do anything. Thus the if condition.
                            if (!mIsStarting) {
                                mIsOnHeadsetSco = false;

                                // Need to call stopBluetoothSco(), otherwise startBluetoothSco()
                                // will not be successful.
                                mAudioManager.stopBluetoothSco();

                                // override this if you want to do other thing when Sco audio is disconnected.
                                onScoAudioDisconnected();
                            }
                            break;
                    }
            }
        }
    };

    /**
     * Try to connect to audio headset in onTick.
     */
    private CountDownTimer mCountDown = new CountDownTimer(10000, 1000) {

        @Override
        public void onTick(long millisUntilFinished) {
            // When this call is successful, this count down timer will be canceled.
            try {
                mAudioManager.startBluetoothSco();
            } catch (Exception ignored) {

            }
            Log.d(TAG, "\nonTick start bluetooth Sco");
        }

        @Override
        public void onFinish() {
            // Calls to startBluetoothSco() in onStick are not successful.
            // Should implement something to inform user of this failure
            mIsCountDownOn = false;
            mAudioManager.setMode(AudioManager.MODE_NORMAL);

            Log.d(TAG, "\nonFinish fail to connect to headset audio");
            Toast.makeText(mContext, "Failed to connect to bluetooth headset",
                    Toast.LENGTH_SHORT).show();
        }
    };
}
