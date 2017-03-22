package in.hedera.reku.speechtrial;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import ai.kitt.snowboy.SnowboyDetect;
import in.hedera.reku.speechtrial.utils.Constants;

/**
 * Created by rakeshkalyankar on 21/03/17.
 */

public class HotwordDetectionThread extends Thread{
    static{
        try{
            System.loadLibrary("snowboy-detect-android");
            System.out.println("Loaded snowboyJNIApi");
        }catch(UnsatisfiedLinkError e){
            //nothing to do
            System.out.println("Couldn't load snowboyJNIApi");
            System.out.println(e.getMessage());
        }
    }
    private static final String TAG = HotwordDetectionThread.class.getSimpleName();
    private Context context;
    public boolean threadActive = false;
    private AudioRecord ar = null;

    private static final int SAMPLE_RATE = 16000;
    private static final int LISTENING_PERIOD_FRAMES = 2000;
    public static final String BROADCAST_TAG = "HotwordDetected";

    private static String strEnvWorkSpace = Constants.DEFAULT_WORK_SPACE;
    private static final String ACTIVE_RES = Constants.ACTIVE_RES;
    private static final String ACTIVE_UMDL = Constants.ACTIVE_UMDL;
    private String activeModel = strEnvWorkSpace+ACTIVE_UMDL;
    private String commonRes = strEnvWorkSpace+ACTIVE_RES;
    private SnowboyDetect detector = new SnowboyDetect(commonRes, activeModel);

    public HotwordDetectionThread(Context context){
        this.context = context;
    }

    @Override
    public void run() {
        Log.d(TAG, "run thread");
        threadActive = true;

        detector.SetSensitivity("0.5");         // Sensitivity for each hotword
        detector.ApplyFrontend(true);
//        snowboyDetector.SetAudioGain(2.0f);              // Audio gain for detection*/

        int minSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        ar = new AudioRecord(MediaRecorder.AudioSource.MIC, detector.SampleRate() ,AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minSize);
        if (ar.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new RuntimeException("AudioRecord initialization failed");
        }
        ar.startRecording();

        short[] audioData = new short[LISTENING_PERIOD_FRAMES];
        while(threadActive) {
            ar.read(audioData, 0, LISTENING_PERIOD_FRAMES);

            int result = detector.RunDetection(audioData, audioData.length);
            if(result > 0) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(HotwordDetectionThread.BROADCAST_TAG));
            }
        }
    }

    public void cancel(){
        threadActive = false;
        if(ar != null) {
            ar.stop();
            ar.release();
            ar = null;
        }
        this.interrupt();
    }
}
