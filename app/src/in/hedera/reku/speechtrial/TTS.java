package in.hedera.reku.speechtrial;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Created by rakeshkalyankar on 09/03/17.
 */

public class TTS {
    private static TextToSpeech textToSpeech;
    private static final String TAG = TTS.class.getSimpleName();

    public static void init(final Context context) {
        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status == TextToSpeech.SUCCESS){
                        setTextToSpeechSettings(null);
                    }else{
                        Log.e(TAG, "error creating text to speech");

                    }
                }
            });
        }
    }

    public static void speak(final String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    public static void shutdown(){
        if(textToSpeech != null){
            stop();
            textToSpeech.shutdown();
        }
    }
    public static void stop(){
        if(textToSpeech != null){
            textToSpeech.stop();
        }
    }

    private static void setTextToSpeechSettings(final Locale locale){
        Locale defaultOrPassedIn = locale;
        if(locale == null){
            defaultOrPassedIn = Locale.getDefault();
        }
        // check if language is available
        switch(textToSpeech.isLanguageAvailable(defaultOrPassedIn)){
            case TextToSpeech.LANG_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                Log.d(TAG, "Locale Supported");
                textToSpeech.setLanguage(defaultOrPassedIn);
                break;
            case TextToSpeech.LANG_MISSING_DATA:
                Log.d(TAG, "Locale Data Missing");
                break;
            case TextToSpeech.LANG_NOT_SUPPORTED:
                Log.d(TAG, "Locale not supported");
                break;
        }
    }
}
